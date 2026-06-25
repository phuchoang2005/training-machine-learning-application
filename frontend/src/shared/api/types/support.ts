import type { ArtifactType, JobStatus } from "./job";
import type { UserSummary } from "./user";

export type NotificationStatus = "UNREAD" | "READ" | "DELIVERY_FAILED";

/**
 * Training output file produced on job completion.
 * Download URL: `/api/v1/artifacts/{artifactId}/download` — no separate fetch needed.
 */
export type Artifact = {
  artifactId: string;
  artifactName: string;
  artifactType: ArtifactType;
  fileSizeBytes: number;
  /** SHA-256 hex digest for client-side integrity verification. */
  checksum: string;
  createdAt: string;
};

/**
 * Job-outcome notification delivered via the configured channel (email, webhook, etc.).
 * `DELIVERY_FAILED` means the backend attempted delivery but the channel was unreachable.
 */
export type Notification = {
  notificationId: string;
  jobId: string;
  type: string;
  channel: string;
  status: NotificationStatus;
  message: string;
  createdAt: string;
};

/** Lightweight queue entry used in the admin queue snapshot view. */
export type QueueItem = {
  jobId: string;
  projectName: string;
  status: JobStatus;
  /** 0 for the currently running job; absent for completed items. */
  queuePosition?: number;
  enqueuedAt: string;
};

/**
 * Point-in-time view of the global training queue.
 * `runningLimit` is the maximum concurrent jobs the platform allows;
 * when `runningCount >= runningLimit` the platform rejects new starts with HTTP 429.
 */
export type QueueSnapshot = {
  runningCount: number;
  runningLimit: number;
  queuedCount: number;
  items: QueueItem[];
};

/**
 * Immutable audit trail entry for any privileged or training action.
 * `projectId`/`jobId` are populated only when the action is scoped to those resources.
 */
export type AuditLog = {
  auditId: string;
  actor: UserSummary;
  /** Present when the action targets a specific project. */
  projectId?: string;
  /** Present when the action targets a specific training job. */
  jobId?: string;
  action: string;
  resourceType: string;
  resourceId: string;
  createdAt: string;
};
