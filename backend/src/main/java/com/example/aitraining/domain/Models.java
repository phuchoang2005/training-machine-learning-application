package com.example.aitraining.domain;

import com.example.aitraining.domain.Enums.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable domain record types mapped to MongoDB collections via Spring Data.
 *
 * <p>All identifiers are {@link UUID}s stored as the document's {@code _id} field.
 * {@link com.example.aitraining.config.MongoConfig} configures the driver to use the
 * standard UUID BSON representation so UUIDs round-trip consistently.
 *
 * <p>Collections are created lazily on first write. Index definitions live in
 * {@link com.example.aitraining.config.MongoSeedConfig}.
 */
public final class Models {
  private Models() {}

  /**
   * A registered platform user.
   *
   * <p>In the current development-grade auth scheme the bearer token is the user's
   * {@code email} (case-insensitive) or the raw {@code userId} string.
   * See {@link com.example.aitraining.repo.UserRepository#findActiveByToken}.
   *
   * <p>Collection: {@code users}.  Unique index on {@code email}.
   */
  @Document(collection = "users")
  public record User(
      /** Stable surrogate key; also the bearer-token identity when passed as a UUID string. */
      @Id UUID userId,
      /** Unique login identity; matched case-insensitively as a bearer token. */
      String email,
      /** Display name shown in the UI and in email notifications. */
      String fullName,
      /** Role governing cross-project visibility and admin actions. */
      UserRole role,
      /** {@code DISABLED} users cannot authenticate. */
      UserStatus status,
      Instant createdAt,
      /** {@code null} until the user logs in at least once. */
      Instant lastLoginAt) {

    /**
     * Returns {@code true} when this user holds the {@link UserRole#ADMIN} role.
     *
     * <p>Prefer calling this helper over comparing {@code role == ADMIN} directly so that
     * future multi-role support requires only a single change here.
     */
    public boolean isAdmin() {
      return role == UserRole.ADMIN;
    }
  }

  /**
   * An ML training project that groups configuration, jobs, and artifacts.
   *
   * <p>Collection: {@code projects}.
   */
  @Document(collection = "projects")
  public record Project(
      @Id UUID projectId,
      /** References the {@link User} who created this project. */
      UUID ownerUserId,
      String projectName,
      String description,
      /** Determines how the source tree is obtained before training. */
      SourceType sourceType,
      /** Remote Git URL; present only when {@code sourceType == GITHUB}. */
      String repositoryUrl,
      /** Path relative to {@code app.storage-root}; present only when {@code sourceType == ZIP}. */
      String localSourcePath,
      /** Default training command; can be overridden in the config snapshot. */
      String trainingEntrypoint,
      /** Soft status ({@code "ACTIVE"} / {@code "ARCHIVED"}); not an enum to allow future values. */
      String status,
      Instant createdAt,
      Instant updatedAt) {}

  /**
   * A named YAML configuration associated with a project.
   *
   * <p>Each project has at least one default config created at project creation time.
   * When a job is started the chosen config is snapshotted immutably into
   * {@code config_snapshots} so that subsequent config edits do not retroactively change
   * historical runs (NFR-DATA-001).
   *
   * <p>Collection: {@code project_configs}.  Compound unique index on
   * {@code (projectId, configPath)}.
   */
  @Document(collection = "project_configs")
  public record ProjectConfig(
      @Id UUID configId,
      UUID projectId,
      /** Human-readable label; defaults to {@code "default"} for the first config. */
      String configName,
      /** Logical path within the project (e.g. {@code "configs/default.yaml"}). */
      String configPath,
      /** The full YAML text; validated on write via {@link com.example.aitraining.service.ConfigService}. */
      String yamlContent,
      /** {@code true} for the config created automatically at project creation. */
      boolean defaultConfig,
      Instant updatedAt) {}

  /**
   * A single run of an ML training job.
   *
   * <p>The lifecycle is managed by {@link com.example.aitraining.service.JobService} for
   * user-facing transitions (start, cancel, retry) and by
   * {@link com.example.aitraining.runner.AbstractTrainingRunner} for execution-phase transitions
   * (RUNNING → SUCCESS / FAILED).
   *
   * <p>Collection: {@code training_jobs}.
   */
  @Document(collection = "training_jobs")
  public record TrainingJob(
      @Id UUID jobId,
      UUID projectId,
      /** The user who triggered this job (start or retry). */
      UUID triggeredByUserId,
      /** References the immutable {@code config_snapshots} document used for this run. */
      UUID configSnapshotId,
      /** Non-null when this job was created by a retry action; points to the original job. */
      UUID retryOfJobId,
      /** Current lifecycle status; see {@link Enums.JobStatus} for valid transitions. */
      JobStatus status,
      /** Number of times this job has been automatically retried (0 for first runs). */
      int retryAttempt,
      /**
       * 1-based position in the WAITING queue; {@code null} when the job is not queued.
       * Recomputed by {@link com.example.aitraining.repo.JobQueueRepository#refreshPositions()}
       * after every enqueue or cancel.
       */
      Integer queuePosition,
      Instant queuedAt,
      /** Set when the dispatcher transitions the job to {@code RUNNING}. */
      Instant startedAt,
      /** Set when the job reaches a terminal state. */
      Instant endedAt,
      /** Human-readable reason for {@code FAILED} or {@code CANCELLED}; {@code null} on success. */
      String failureReason,
      Instant createdAt) {

    /**
     * Returns {@code true} when the job is in a terminal state that can no longer change.
     *
     * <p>Callers use this to guard against illegal transitions such as cancelling a job that
     * has already succeeded.
     */
    public boolean terminal() {
      return status == JobStatus.SUCCESS || status == JobStatus.FAILED || status == JobStatus.CANCELLED;
    }
  }
}
