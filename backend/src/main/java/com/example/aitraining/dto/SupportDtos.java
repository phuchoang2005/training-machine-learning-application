package com.example.aitraining.dto;

import com.example.aitraining.domain.Enums.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b>DTO Pattern</b> — transfer objects for support data: job logs, artifacts, and audit trail.
 */
public final class SupportDtos {
  private SupportDtos() {
  }

  /**
   * A single log line emitted by the training container.
   *
   * @param sequenceNo monotonically increasing per-job sequence number for ordered replay
   * @param streamType {@code STDOUT} or {@code STDERR}
   * @param message    a single output line without a trailing newline
   * @param emittedAt  server-side timestamp when the line was stored
   */
  public record LogEventResponse(UUID logEventId, int sequenceNo, StreamType streamType, String message,
      Instant emittedAt) {
  }

  /** Paginated log event list. */
  public record LogEventPage(List<LogEventResponse> data, CommonDtos.Page page) {
  }

  /**
   * Metadata for a stored training artifact.
   *
   * @param artifactName   relative path within the job's output directory
   * @param artifactType   heuristically inferred category
   * @param fileSizeBytes  bytes on disk after copy to managed storage
   * @param checksum       hex-encoded SHA-256 of the stored file for integrity verification
   */
  public record ArtifactResponse(UUID artifactId, String artifactName, ArtifactType artifactType,
      long fileSizeBytes, String checksum, Instant createdAt) {
  }

  /** Artifact list response (not paginated; all artifacts for a job are returned). */
  public record ArtifactListResponse(List<ArtifactResponse> data) {
  }

  /**
   * A single audit trail entry.
   *
   * @param actor        the user who performed the action; may be {@code null} for system events
   * @param action       verb identifying what happened (e.g. {@code "JOB_STARTED"})
   * @param resourceType the type of resource affected (e.g. {@code "TRAINING_JOB"})
   * @param metadata     reserved for future structured context; currently always empty
   */
  public record AuditLogResponse(UUID auditId, CommonDtos.UserSummary actor, UUID projectId, UUID jobId,
      String action, String resourceType, String resourceId,
      Map<String, Object> metadata, Instant createdAt) {
  }

  /** Paginated audit log list. */
  public record AuditLogPage(List<AuditLogResponse> data, CommonDtos.Page page) {
  }
}
