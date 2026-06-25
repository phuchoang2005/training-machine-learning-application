package com.example.aitraining.dto;

import com.example.aitraining.domain.Enums.JobStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * <b>DTO Pattern</b> — request/response transfer objects for the training-job API endpoints.
 */
public final class JobDtos {
  private JobDtos() {
  }

  /**
   * Request body for {@code POST /projects/{projectId}/jobs}.
   *
   * @param configId    the config to base this run on; must belong to the project
   * @param yamlContent optional override YAML; when non-blank it replaces the stored config for
   *                    this run only without persisting the change
   */
  public record StartJobRequest(@NotNull UUID configId, String yamlContent) {
  }

  /** Response from a successful job start; the job is in {@code QUEUED} state. */
  public record StartJobResponse(UUID jobId, UUID projectId, JobStatus status, Integer queuePosition,
      UUID configSnapshotId, Instant createdAt) {
  }

  /**
   * Latest training progress for a job.
   *
   * @param available  {@code false} when no progress data exists yet (UI shows "N/A")
   * @param value      integer percentage [0, 100]; {@code null} when {@code available=false}
   * @param epoch      current epoch; {@code null} when not available
   * @param totalEpoch total epochs; {@code null} when not available
   */
  public record ProgressResponse(boolean available, Integer value, Integer epoch, Integer totalEpoch,
      Instant updatedAt) {
  }

  /** Full job detail including nested user summary and latest progress. */
  public record JobDetail(UUID jobId, UUID projectId, String projectName, CommonDtos.UserSummary triggeredBy,
      JobStatus status, Integer queuePosition, ProgressResponse progress, UUID retryOfJobId,
      int retryAttempt, Instant createdAt, Instant queuedAt, Instant startedAt,
      Instant endedAt, String failureReason) {
  }

  /** Paginated list of job details. */
  public record JobPage(List<JobDetail> data, CommonDtos.Page page) {
  }

  /**
   * Optional request body for {@code POST /jobs/{jobId}/cancel}.
   *
   * @param reason human-readable reason stored on the job as {@code failureReason}
   */
  public record CancelJobRequest(String reason) {
  }

  /** Response from a successful cancel; always in {@code CANCELLED} state. */
  public record CancelJobResponse(UUID jobId, JobStatus status, Instant endedAt) {
  }

  /**
   * Request body for {@code POST /jobs/{jobId}/retry}.
   *
   * @param mode        reserved for future use; currently ignored
   * @param yamlContent optional override YAML; when blank the original config snapshot is reused
   */
  public record RetryJobRequest(String mode, String yamlContent) {
  }

  /** Response from a successful retry; the new job is in {@code QUEUED} state. */
  public record RetryJobResponse(UUID originalJobId, UUID retryJobId, JobStatus status, Integer queuePosition) {
  }

  /**
   * Admin queue snapshot for {@code GET /jobs/queue}.
   *
   * @param runningCount  current number of RUNNING jobs
   * @param runningLimit  configured concurrency cap
   * @param queuedCount   number of WAITING entries in the queue
   */
  public record QueueSnapshot(int runningCount, int runningLimit, int queuedCount, List<QueueItem> items) {
  }

  /** A single entry in the queue snapshot. */
  public record QueueItem(UUID jobId, String projectName, JobStatus status, Integer queuePosition,
      Instant enqueuedAt) {
  }
}
