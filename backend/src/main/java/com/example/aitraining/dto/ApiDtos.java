package com.example.aitraining.dto;

import com.example.aitraining.domain.Enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record Page(int limit, String nextCursor, boolean hasMore) {
    }

    public record ApiError(String code, String message, String correlationId, List<ValidationDetail> details) {
    }

    public record ValidationDetail(String field, String reason) {
    }

    public record ErrorResponse(ApiError error) {
    }

    public record UserSummary(UUID userId, String email, String fullName) {
    }

    public record CurrentUser(UUID userId, String email, String fullName, UserRole role, UserStatus status,
                              Instant lastLoginAt) {
    }

    public record UserPage(List<CurrentUser> data, Page page) {
    }

    public record UpdateUserStatusRequest(@NotNull UserStatus status) {
    }

    public record UserStatusResponse(UUID userId, UserStatus status) {
    }

    public record CreateGithubProjectRequest(@NotBlank @Size(max = 120) String projectName,
                                             @Size(max = 1000) String description,
                                             @NotBlank String repositoryUrl,
                                             @NotBlank String trainingEntrypoint) {
    }

    public record ZipProjectMetadata(@NotBlank String projectName, String description,
                                     @NotBlank String trainingEntrypoint) {
    }

    public record CreateProjectResponse(UUID projectId, String projectName, SourceType sourceType, String status,
                                        Instant createdAt) {
    }

    public record ProjectSummary(UUID projectId, String projectName, String description, SourceType sourceType,
                                 JobStatus latestJobStatus, Instant lastTrainingTime, String lastTrainingOwner) {
    }

    public record ProjectDetail(UUID projectId, String projectName, String description, SourceType sourceType,
                                JobStatus latestJobStatus, Instant lastTrainingTime, String lastTrainingOwner,
                                String repositoryUrl, String trainingEntrypoint, UserSummary owner,
                                Instant createdAt, Instant updatedAt) {
    }

    public record ProjectPage(List<ProjectSummary> data, Page page) {
    }

    public record ProjectConfigSummary(UUID configId, String configName, String configPath, boolean isDefault,
                                       Instant updatedAt) {
    }

    public record ProjectConfigListResponse(List<ProjectConfigSummary> data) {
    }

    public record ProjectConfigContent(UUID configId, String configPath, String yamlContent, String contentHash) {
    }

    public record ValidateYamlRequest(@NotBlank String yamlContent) {
    }

    public record ValidateYamlResponse(boolean valid, Map<String, Object> normalizedPreview, List<String> errors) {
    }

    public record StartJobRequest(@NotNull UUID configId, String yamlContent) {
    }

    public record StartJobResponse(UUID jobId, UUID projectId, JobStatus status, Integer queuePosition,
                                   UUID configSnapshotId, Instant createdAt) {
    }

    public record ProgressResponse(boolean available, Integer value, Integer epoch, Integer totalEpoch,
                                   Instant updatedAt) {
    }

    public record JobDetail(UUID jobId, UUID projectId, String projectName, UserSummary triggeredBy,
                            JobStatus status, Integer queuePosition, ProgressResponse progress, UUID retryOfJobId,
                            int retryAttempt, Instant createdAt, Instant queuedAt, Instant startedAt,
                            Instant endedAt, String failureReason) {
    }

    public record JobPage(List<JobDetail> data, Page page) {
    }

    public record CancelJobRequest(String reason) {
    }

    public record CancelJobResponse(UUID jobId, JobStatus status, Instant endedAt) {
    }

    public record RetryJobRequest(String mode, String yamlContent) {
    }

    public record RetryJobResponse(UUID originalJobId, UUID retryJobId, JobStatus status, Integer queuePosition) {
    }

    public record QueueSnapshot(int runningCount, int runningLimit, int queuedCount, List<QueueItem> items) {
    }

    public record QueueItem(UUID jobId, String projectName, JobStatus status, Integer queuePosition,
                            Instant enqueuedAt) {
    }

    public record LogEventResponse(UUID logEventId, int sequenceNo, StreamType streamType, String message,
                                   Instant emittedAt) {
    }

    public record LogEventPage(List<LogEventResponse> data, Page page) {
    }

    public record ArtifactResponse(UUID artifactId, String artifactName, ArtifactType artifactType,
                                   long fileSizeBytes, String checksum, Instant createdAt) {
    }

    public record ArtifactListResponse(List<ArtifactResponse> data) {
    }

    public record NotificationResponse(UUID notificationId, UUID jobId, String type, NotificationChannel channel,
                                       NotificationStatus status, String message, Instant createdAt) {
    }

    public record NotificationPage(List<NotificationResponse> data, Page page) {
    }

    public record NotificationStatusResponse(UUID notificationId, NotificationStatus status) {
    }

    public record AuditLogResponse(UUID auditId, UserSummary actor, UUID projectId, UUID jobId, String action,
                                   String resourceType, String resourceId, Map<String, Object> metadata,
                                   Instant createdAt) {
    }

    public record AuditLogPage(List<AuditLogResponse> data, Page page) {
    }
}
