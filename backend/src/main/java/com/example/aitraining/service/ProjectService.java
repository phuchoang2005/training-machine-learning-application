package com.example.aitraining.service;

import com.example.aitraining.config.AppProperties;
import com.example.aitraining.domain.Enums.SourceType;
import com.example.aitraining.domain.Models.Project;
import com.example.aitraining.domain.Models.User;
import com.example.aitraining.dto.CommonDtos.*;
import com.example.aitraining.dto.ProjectDtos.*;
import com.example.aitraining.repo.ConfigRepository;
import com.example.aitraining.repo.JobRepository;
import com.example.aitraining.repo.ProjectRepository;
import com.example.aitraining.repo.SupportRepository;
import com.example.aitraining.repo.UserRepository;
import com.example.aitraining.runner.SourceLayout;
import com.example.aitraining.runner.ZipExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * <b>Service Layer Pattern</b> — manages the lifecycle of ML training projects: creation
 * (GitHub and ZIP sources), listing, detail retrieval, and deletion.
 *
 * <p>ZIP uploads are extracted through {@link com.example.aitraining.runner.ZipExtractor} to
 * prevent path-traversal attacks before the project record is committed (NFR-SEC-006).
 * Every mutating operation records an audit entry.
 */
@Service
public class ProjectService {
  private final ProjectRepository projects;
  private final ConfigRepository configs;
  private final UserRepository users;
  private final JobRepository jobs;
  private final SupportRepository support;
  private final ImageBuildService imageBuilder;
  private final AppProperties props;

  public ProjectService(ProjectRepository projects, ConfigRepository configs, UserRepository users,
      JobRepository jobs, SupportRepository support, ImageBuildService imageBuilder, AppProperties props) {
    this.projects = projects;
    this.configs = configs;
    this.users = users;
    this.jobs = jobs;
    this.support = support;
    this.imageBuilder = imageBuilder;
    this.props = props;
  }

  public ProjectPage list(User user, String query, int limit) {
    List<ProjectSummary> data = projects.listVisible(user.userId(), user.isAdmin(), query, limit).stream()
        .map(this::summary)
        .toList();
    return new ProjectPage(data, new Page(limit, null, false));
  }

  public CreateProjectResponse createGithub(User user, CreateGithubProjectRequest request) {
    Project project = projects.create(user.userId(), request.projectName(), request.description(), SourceType.GITHUB,
        request.repositoryUrl(), null, request.trainingEntrypoint());

    Path sourceDir = Path.of(props.docker().sourcesRoot()).resolve(project.projectId().toString()).toAbsolutePath();
    try {
      cloneRepository(request.repositoryUrl(), sourceDir);
      validateProjectStructure(findProjectRoot(sourceDir));
    } catch (IllegalArgumentException e) {
      projects.delete(project.projectId());
      deleteRecursively(sourceDir);
      throw new IllegalArgumentException("Repository rejected: " + e.getMessage());
    } catch (IOException e) {
      projects.delete(project.projectId());
      deleteRecursively(sourceDir);
      throw new IllegalStateException("Failed to clone repository: " + e.getMessage(), e);
    }

    String buildLog = buildImageOrFail(project, findProjectRoot(sourceDir), sourceDir);

    configs.createDefault(project.projectId());
    support.audit(user.userId(), project.projectId(), null, "PROJECT_CREATED", "PROJECT",
        project.projectId().toString());
    return new CreateProjectResponse(project.projectId(), project.projectName(), project.sourceType(), project.status(),
        project.createdAt(), buildLog);
  }

  public CreateProjectResponse createZip(User user, ZipProjectMetadata metadata, MultipartFile file) {
    String sourcePath = "projects/" + UUID.randomUUID();
    Project project = projects.create(user.userId(), metadata.projectName(), metadata.description(), SourceType.ZIP,
        null, sourcePath, metadata.trainingEntrypoint());

    Path extractTarget = Path.of(props.storageRoot()).resolve(sourcePath).toAbsolutePath();
    Path projectRoot;
    try {
      Path tmpZip = Files.createTempFile("upload-", ".zip");
      try {
        file.transferTo(tmpZip);
        ZipExtractor.extract(tmpZip, extractTarget);
        projectRoot = findProjectRoot(extractTarget);
        validateProjectStructure(projectRoot);
      } finally {
        Files.deleteIfExists(tmpZip);
      }
    } catch (IllegalArgumentException e) {
      projects.delete(project.projectId());
      deleteRecursively(extractTarget);
      throw new IllegalArgumentException("ZIP file rejected: " + e.getMessage());
    } catch (IOException e) {
      projects.delete(project.projectId());
      deleteRecursively(extractTarget);
      throw new IllegalStateException("Failed to extract ZIP file: " + e.getMessage(), e);
    }

    String buildLog = buildImageOrFail(project, projectRoot, extractTarget);

    configs.createDefault(project.projectId());
    support.audit(user.userId(), project.projectId(), null, "PROJECT_CREATED", "PROJECT",
        project.projectId().toString());
    return new CreateProjectResponse(project.projectId(), project.projectName(), project.sourceType(), project.status(),
        project.createdAt(), buildLog);
  }

  /**
   * Builds the per-project Docker image synchronously. On failure the project (and the given source
   * directory) are removed and a {@code 400} is thrown carrying the build log so the caller can show
   * it. On success the full build log is returned.
   *
   * @param buildContext directory whose contents become the image (the project root)
   * @param sourceToClean directory to delete if the build fails (clone/extract target)
   */
  private String buildImageOrFail(Project project, Path buildContext, Path sourceToClean) {
    ImageBuildService.BuildResult result = imageBuilder.build(project.projectId(), buildContext);
    if (!result.success()) {
      projects.delete(project.projectId());
      deleteRecursively(sourceToClean);
      throw new IllegalArgumentException("Docker image build failed:\n" + result.log());
    }
    return result.log();
  }

  /** Shallow-clones {@code repositoryUrl} into {@code target} (created fresh). */
  private void cloneRepository(String repositoryUrl, Path target) throws IOException {
    deleteRecursively(target);
    Files.createDirectories(target);
    try {
      Process p = new ProcessBuilder("git", "clone", "--depth=1", repositoryUrl, target.toString())
          .redirectErrorStream(true).start();
      String out = new String(p.getInputStream().readAllBytes());
      if (p.waitFor() != 0) {
        throw new IOException("git clone failed: " + out.strip());
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("git clone interrupted", e);
    }
  }

  public ProjectDetail detail(Project project) {
    User owner = users.get(project.ownerUserId());
    ProjectSummary s = summary(project);
    return new ProjectDetail(project.projectId(), project.projectName(), project.description(), project.sourceType(),
        s.latestJobStatus(), s.lastTrainingTime(), s.lastTrainingOwner(), project.repositoryUrl(),
        project.trainingEntrypoint(), new UserSummary(owner.userId(), owner.email(), owner.fullName()),
        project.createdAt(), project.updatedAt());
  }

  public void delete(User user, Project project) {
    // Capture the project's job IDs before the cascade removes them, so we can also delete each
    // job's on-disk artifact tree (artifacts/{jobId}) — the DB records are removed by projects.delete.
    List<UUID> jobIds = jobs.listByProject(project.projectId(), null, Integer.MAX_VALUE).stream()
        .map(com.example.aitraining.domain.Models.TrainingJob::jobId)
        .toList();

    projects.delete(project.projectId());
    // Best-effort: remove the project's Docker image and any of its job containers so they don't
    // leak after the project record is gone. Never fails the delete.
    String cleanup = imageBuilder.cleanupProject(project.projectId());
    // Also remove the on-disk source tree (cloned repo or extracted ZIP), if any.
    if (project.localSourcePath() != null) {
      deleteRecursively(Path.of(props.storageRoot()).resolve(project.localSourcePath()).toAbsolutePath());
    }
    deleteRecursively(Path.of(props.docker().sourcesRoot()).resolve(project.projectId().toString()).toAbsolutePath());
    // Remove each job's stored artifact files (storageRoot/artifacts/{jobId}).
    Path artifactsRoot = Path.of(props.storageRoot()).resolve("artifacts");
    for (UUID jobId : jobIds) {
      deleteRecursively(artifactsRoot.resolve(jobId.toString()).toAbsolutePath());
    }
    support.audit(user.userId(), project.projectId(), null, "PROJECT_DELETED", "PROJECT",
        cleanup);
  }

  private ProjectSummary summary(Project project) {
    var latest = jobs.listByProject(project.projectId(), null, 1).stream().findFirst();
    String owner = latest.map(j -> users.get(j.triggeredByUserId()).fullName()).orElse(null);
    return new ProjectSummary(project.projectId(), project.projectName(), project.description(), project.sourceType(),
        latest.map(com.example.aitraining.domain.Models.TrainingJob::status).orElse(null),
        latest.map(com.example.aitraining.domain.Models.TrainingJob::createdAt).orElse(null),
        owner);
  }

  /**
   * Validates that the project tree at {@code root} contains the three files required by
   * README-PYTHON.md: {@code main.py}, {@code requirements.txt}, and {@code configs/} with at least
   * one YAML file. Applies to both ZIP-extracted and Git-cloned sources.
   */
  private static void validateProjectStructure(Path root) {
    if (!Files.isRegularFile(root.resolve("main.py"))) {
      throw new IllegalArgumentException("Missing required file: main.py");
    }
    if (!Files.isRegularFile(root.resolve("requirements.txt"))) {
      throw new IllegalArgumentException("Missing required file: requirements.txt");
    }
    Path configsDir = root.resolve("configs");
    if (!Files.isDirectory(configsDir)) {
      throw new IllegalArgumentException("Missing required directory: configs/");
    }
    boolean hasYaml = false;
    try (var stream = Files.list(configsDir)) {
      hasYaml = stream.anyMatch(p -> {
        String name = p.getFileName().toString();
        return name.endsWith(".yaml") || name.endsWith(".yml");
      });
    } catch (IOException ignored) {
      // treat unreadable configs/ as empty
    }
    if (!hasYaml) {
      throw new IllegalArgumentException("configs/ directory must contain at least one .yaml file");
    }
  }

  /**
   * Returns the effective project root inside {@code dir} (handling the macOS ZIP wrapper layout).
   * Delegates to {@link SourceLayout#resolveProjectRoot} so the build context resolved here matches
   * the {@code /source} root the runner mounts and rebuilds from at job time.
   */
  private static Path findProjectRoot(Path dir) {
    return SourceLayout.resolveProjectRoot(dir);
  }

  /** Recursively deletes a directory tree, suppressing all errors (best-effort cleanup). */
  private static void deleteRecursively(Path dir) {
    try {
      if (!Files.exists(dir)) return;
      try (var walk = Files.walk(dir)) {
        walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
          try { Files.delete(p); } catch (IOException ignored) {}
        });
      }
    } catch (IOException ignored) {
      // best-effort
    }
  }
}
