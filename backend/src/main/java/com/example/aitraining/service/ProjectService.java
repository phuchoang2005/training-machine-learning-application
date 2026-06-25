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
  private final AppProperties props;

  public ProjectService(ProjectRepository projects, ConfigRepository configs, UserRepository users,
      JobRepository jobs, SupportRepository support, AppProperties props) {
    this.projects = projects;
    this.configs = configs;
    this.users = users;
    this.jobs = jobs;
    this.support = support;
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
    configs.createDefault(project.projectId(), project.trainingEntrypoint());
    support.audit(user.userId(), project.projectId(), null, "PROJECT_CREATED", "PROJECT",
        project.projectId().toString());
    return new CreateProjectResponse(project.projectId(), project.projectName(), project.sourceType(), project.status(),
        project.createdAt());
  }

  public CreateProjectResponse createZip(User user, ZipProjectMetadata metadata, MultipartFile file) {
    String sourcePath = "projects/" + UUID.randomUUID();
    Project project = projects.create(user.userId(), metadata.projectName(), metadata.description(), SourceType.ZIP,
        null, sourcePath, metadata.trainingEntrypoint());

    try {
      Path extractTarget = Path.of(props.storageRoot()).resolve(sourcePath).toAbsolutePath();
      Path tmpZip = Files.createTempFile("upload-", ".zip");
      try {
        file.transferTo(tmpZip);
        ZipExtractor.extract(tmpZip, extractTarget);
      } finally {
        Files.deleteIfExists(tmpZip);
      }
    } catch (IllegalArgumentException e) {
      projects.delete(project.projectId());
      throw new IllegalArgumentException("ZIP file rejected: " + e.getMessage());
    } catch (IOException e) {
      projects.delete(project.projectId());
      throw new IllegalStateException("Failed to extract ZIP file: " + e.getMessage(), e);
    }

    configs.createDefault(project.projectId(), project.trainingEntrypoint());
    support.audit(user.userId(), project.projectId(), null, "PROJECT_CREATED", "PROJECT",
        project.projectId().toString());
    return new CreateProjectResponse(project.projectId(), project.projectName(), project.sourceType(), project.status(),
        project.createdAt());
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
    projects.delete(project.projectId());
    support.audit(user.userId(), project.projectId(), null, "PROJECT_DELETED", "PROJECT",
        project.projectId().toString());
  }

  private ProjectSummary summary(Project project) {
    var latest = jobs.listByProject(project.projectId(), null, 1).stream().findFirst();
    String owner = latest.map(j -> users.get(j.triggeredByUserId()).fullName()).orElse(null);
    return new ProjectSummary(project.projectId(), project.projectName(), project.description(), project.sourceType(),
        latest.map(com.example.aitraining.domain.Models.TrainingJob::status).orElse(null),
        latest.map(com.example.aitraining.domain.Models.TrainingJob::createdAt).orElse(null),
        owner);
  }
}
