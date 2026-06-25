package com.example.aitraining.dto;

import com.example.aitraining.domain.Enums.JobStatus;
import com.example.aitraining.domain.Enums.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b>DTO Pattern</b> — request/response transfer objects for the project and configuration
 * API endpoints.
 */
public final class ProjectDtos {
  private ProjectDtos() {
  }

  /**
   * Request body for {@code POST /projects} (GitHub-source project).
   *
   * @param repositoryUrl     remote Git URL; cloned at training time
   * @param trainingEntrypoint shell command to run inside the container
   */
  public record CreateGithubProjectRequest(@NotBlank @Size(max = 120) String projectName,
      @Size(max = 1000) String description,
      @NotBlank String repositoryUrl,
      @NotBlank String trainingEntrypoint) {
  }

  /**
   * JSON metadata part for {@code POST /projects/upload-zip} multipart request.
   */
  public record ZipProjectMetadata(@NotBlank String projectName, String description,
      @NotBlank String trainingEntrypoint) {
  }

  /** Response from a successful project creation of either source type. */
  public record CreateProjectResponse(UUID projectId, String projectName, SourceType sourceType, String status,
      Instant createdAt) {
  }

  /**
   * Lightweight project summary used in list responses.
   *
   * @param latestJobStatus   status of the most recent job; {@code null} if no jobs yet
   * @param lastTrainingTime  {@code createdAt} of the most recent job; {@code null} if no jobs yet
   * @param lastTrainingOwner full name of the user who triggered the most recent job
   */
  public record ProjectSummary(UUID projectId, String projectName, String description, SourceType sourceType,
      JobStatus latestJobStatus, Instant lastTrainingTime, String lastTrainingOwner) {
  }

  /** Full project detail including owner user summary and latest job info. */
  public record ProjectDetail(UUID projectId, String projectName, String description, SourceType sourceType,
      JobStatus latestJobStatus, Instant lastTrainingTime, String lastTrainingOwner,
      String repositoryUrl, String trainingEntrypoint, CommonDtos.UserSummary owner,
      Instant createdAt, Instant updatedAt) {
  }

  /** Paginated project list. */
  public record ProjectPage(List<ProjectSummary> data, CommonDtos.Page page) {
  }

  /** Config summary in list responses (no YAML content to keep the payload small). */
  public record ProjectConfigSummary(UUID configId, String configName, String configPath, boolean isDefault,
      Instant updatedAt) {
  }

  /** Config list wrapper. */
  public record ProjectConfigListResponse(List<ProjectConfigSummary> data) {
  }

  /**
   * Full config detail including YAML content and a content hash.
   *
   * @param contentHash SHA-256 hex of {@code yamlContent}; can be used for conditional fetch
   */
  public record ProjectConfigContent(UUID configId, String configPath, String yamlContent, String contentHash) {
  }

  /** Request body for {@code POST /projects/{projectId}/configs/validate}. */
  public record ValidateYamlRequest(@NotBlank String yamlContent) {
  }

  /**
   * Validation result returned without persisting.
   *
   * @param valid             {@code true} if the YAML parsed without errors
   * @param normalizedPreview top-level key → value map; empty on failure
   * @param errors            parse error messages; empty on success
   */
  public record ValidateYamlResponse(boolean valid, Map<String, Object> normalizedPreview, List<String> errors) {
  }
}
