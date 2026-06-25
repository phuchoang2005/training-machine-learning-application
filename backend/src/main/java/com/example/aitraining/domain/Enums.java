package com.example.aitraining.domain;

/**
 * All domain enumeration types used across the application.
 *
 * <p>Enums are stored as their string names in MongoDB (not as ordinals) so that renaming
 * an ordinal position never silently corrupts existing documents.  Spring Data MongoDB
 * handles the conversion automatically when the field type is an enum.
 */
public final class Enums {
  private Enums() {}

  /**
   * Roles that govern a user's access level throughout the system.
   *
   * <ul>
   *   <li>{@link #USER}  — can create and manage their own projects and jobs.</li>
   *   <li>{@link #ADMIN} — can view all projects/jobs and cancel/delete any of them,
   *       but cannot read sensitive artefacts (logs, artifacts) unless they are also
   *       the project owner (NFR-SEC-005).</li>
   * </ul>
   */
  public enum UserRole {
    /** Standard user with access restricted to personally owned resources. */
    USER,
    /** Platform administrator with elevated but still bounded cross-tenant visibility. */
    ADMIN
  }

  /**
   * Lifecycle status of a user account.
   */
  public enum UserStatus {
    /** Account is functional and the bearer token is accepted. */
    ACTIVE,
    /** Account is deactivated; bearer tokens are rejected by {@code WebConfig}. */
    DISABLED
  }

  /**
   * The origin type of a project's source code.
   */
  public enum SourceType {
    /** Source is fetched from a remote Git repository via {@code git clone}. */
    GITHUB,
    /** Source was uploaded as a ZIP archive and extracted to local storage. */
    ZIP
  }

  /**
   * The lifecycle status of a {@link Models.TrainingJob}.
   *
   * <p>Valid transitions:
   * <pre>
   * CREATED → QUEUED → RUNNING → SUCCESS
   *                             → FAILED
   *                             → CANCELLED
   *         ↑
   * RETRYING ─────────────────────────────┘  (after restart reconciler)
   * </pre>
   *
   * <p>Terminal states ({@code SUCCESS}, {@code FAILED}, {@code CANCELLED}) cannot be
   * transitioned further.  The helper {@link Models.TrainingJob#terminal()} tests for them.
   */
  public enum JobStatus {
    /** Created but not yet placed on the queue (transient state, rarely visible). */
    CREATED,
    /** Waiting in the queue for a free execution slot. */
    QUEUED,
    /** Currently executing in the training runner. */
    RUNNING,
    /** Training completed with exit code 0. Terminal. */
    SUCCESS,
    /** Training process exited non-zero or an unrecoverable error occurred. Terminal. */
    FAILED,
    /** Cancelled by the user or an administrator before completion. Terminal. */
    CANCELLED,
    /** Job was RUNNING when the server restarted; re-queued by the reconciler. */
    RETRYING
  }

  /**
   * Status of an entry in the {@code job_queue_entries} collection.
   *
   * <p>The dispatcher atomically transitions entries {@code WAITING → DISPATCHED} using a
   * MongoDB {@code findAndModify} to prevent duplicate dispatch under concurrent scheduler
   * invocations (NFR-DATA-003).
   */
  public enum QueueStatus {
    /** Entry is in the queue waiting to be claimed by the dispatcher. */
    WAITING,
    /** Entry has been atomically claimed; the job is being transitioned to RUNNING. */
    DISPATCHED,
    /** Entry was cancelled before it was dispatched. */
    CANCELLED
  }

  /**
   * The output stream that produced a log line.
   */
  public enum StreamType {
    /** Standard output (fd 1) of the training container. */
    STDOUT,
    /** Standard error (fd 2) of the training container. */
    STDERR
  }

  /**
   * Coarse category of a stored training artifact.
   *
   * <p>Used to filter artifact listings and to choose storage sub-paths. The category is
   * inferred heuristically from the file name by {@link com.example.aitraining.service.ArtifactService}.
   */
  public enum ArtifactType {
    /** Trained model weights (e.g. {@code .pt}, {@code .onnx}, {@code .h5}). */
    MODEL,
    /** Intermediate training checkpoint that can be used to resume training. */
    CHECKPOINT,
    /** Evaluation metrics, loss curves, and similar structured result files. */
    METRIC,
    /** Any artifact that does not match the above categories. */
    OTHER
  }
}
