package com.phuchoang2005.aitraining.service;

import com.phuchoang2005.aitraining.config.AppProperties;
import com.phuchoang2005.aitraining.domain.Enums.SourceType;
import com.phuchoang2005.aitraining.domain.Models.Project;
import com.phuchoang2005.aitraining.domain.Models.User;
import com.phuchoang2005.aitraining.dto.CommonDtos.*;
import com.phuchoang2005.aitraining.dto.ProjectDtos.*;
import com.phuchoang2005.aitraining.repo.ConfigRepository;
import com.phuchoang2005.aitraining.repo.JobRepository;
import com.phuchoang2005.aitraining.repo.ProjectRepository;
import com.phuchoang2005.aitraining.repo.SupportRepository;
import com.phuchoang2005.aitraining.repo.UserRepository;
import com.phuchoang2005.aitraining.runner.SourceLayout;
import com.phuchoang2005.aitraining.runner.ZipExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
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
 * <p>ZIP uploads are extracted through {@link com.phuchoang2005.aitraining.runner.ZipExtractor} to
 * prevent path-traversal attacks before the project record is committed (NFR-SEC-006).
 * Every mutating operation records an audit entry.
 */
@Service
public class ProjectService {
  private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

  private final ProjectRepository projects;
  private final ConfigRepository configs;
  private final UserRepository users;
  private final JobRepository jobs;
  private final SupportRepository support;
  private final ImageBuildService imageBuilder;
  private final AppProperties props;
  private final AsyncTaskExecutor imageBuildExecutor;

  public ProjectService(ProjectRepository projects, ConfigRepository configs, UserRepository users,
      JobRepository jobs, SupportRepository support, ImageBuildService imageBuilder, AppProperties props,
      @Qualifier("imageBuildExecutor") AsyncTaskExecutor imageBuildExecutor) {
    this.projects = projects;
    this.configs = configs;
    this.users = users;
    this.jobs = jobs;
    this.support = support;
    this.imageBuilder = imageBuilder;
    this.props = props;
    this.imageBuildExecutor = imageBuildExecutor;
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

    return buildAndFinalize(user, project, findProjectRoot(sourceDir), sourceDir);
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

    return buildAndFinalize(user, project, projectRoot, extractTarget);
  }

  /**
   * Kicks off the per-project Docker image build <em>fire-and-forget</em> and returns immediately.
   *
   * <p>The build is the slow part of registration (it can run for minutes). Blocking the request
   * thread on it tied registration to the client connection: if the user closed the browser tab
   * mid-build, the request died and the build — plus the config seeding and audit that follow it —
   * was stranded, leaving a half-registered project. Here the whole build-and-commit unit runs on
   * {@code imageBuildExecutor} and the request returns at once with {@code buildStatus = "BUILDING"};
   * the frontend polls {@code GET /projects/{id}} for the terminal state. Nothing about the build is
   * coupled to the request, so closing the tab can no longer affect it.
   *
   * <p>On success the task marks the project {@code READY} (storing the build log), seeds the default
   * config, and writes the audit entry. On failure it marks the project {@code FAILED} (keeping the
   * record so the user can read the build log) and deletes the on-disk source directory.
   *
   * @param buildContext  directory whose contents become the image (the project root)
   * @param sourceToClean directory to delete if the build fails (clone/extract target)
   */
  private CreateProjectResponse buildAndFinalize(User user, Project project, Path buildContext, Path sourceToClean) {
    imageBuildExecutor.execute(() -> {
      try {
        ImageBuildService.BuildResult result = imageBuilder.build(project.projectId(), buildContext);
        if (!result.success()) {
          projects.markBuildFailed(project.projectId(), result.log());
          deleteRecursively(sourceToClean);
          return;
        }
        projects.markBuilt(project.projectId(), result.log());
        configs.createDefault(project.projectId());
        support.audit(user.userId(), project.projectId(), null, "PROJECT_CREATED", "PROJECT",
            project.projectId().toString());
      } catch (RuntimeException e) {
        log.error("Image build finalization failed for project {}: {}", project.projectId(), e.getMessage(), e);
        projects.markBuildFailed(project.projectId(), "Image build finalization failed: " + e.getMessage());
        deleteRecursively(sourceToClean);
      }
    });
    return new CreateProjectResponse(project.projectId(), project.projectName(), project.sourceType(),
        project.status(), project.buildStatus(), project.createdAt(), null);
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
        project.buildStatus(), project.buildLog(), s.latestJobStatus(), s.lastTrainingTime(), s.lastTrainingOwner(),
        project.repositoryUrl(), project.trainingEntrypoint(),
        new UserSummary(owner.userId(), owner.email(), owner.fullName()),
        project.createdAt(), project.updatedAt());
  }

  public void delete(User user, Project project) {
    // Capture the project's job IDs before the cascade removes them, so we can also delete each
    // job's on-disk artifact tree (artifacts/{jobId}) — the DB records are removed by projects.delete.
    List<UUID> jobIds = jobs.listByProject(project.projectId(), null, Integer.MAX_VALUE).stream()
        .map(com.phuchoang2005.aitraining.domain.Models.TrainingJob::jobId)
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
        project.buildStatus(),
        latest.map(com.phuchoang2005.aitraining.domain.Models.TrainingJob::status).orElse(null),
        latest.map(com.phuchoang2005.aitraining.domain.Models.TrainingJob::createdAt).orElse(null),
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
