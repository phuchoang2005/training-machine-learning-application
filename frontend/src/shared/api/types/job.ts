import type { UserSummary } from "./user";

/**
 * Lifecycle order: CREATED → QUEUED → RUNNING → SUCCESS | FAILED | CANCELLED
 * RETRYING means a new attempt was queued after a failure; the original job keeps its terminal status.
 */
export type JobStatus = "CREATED" | "QUEUED" | "RUNNING" | "RETRYING" | "SUCCESS" | "FAILED" | "CANCELLED";

/** Log stream origin. STDERR lines are highlighted as warnings in the viewer. */
export type StreamType = "STDOUT" | "STDERR";

/** Backend schema canonical values. CHECKPOINT replaces the old METRICS/LOG names. */
export type ArtifactType = "MODEL" | "CHECKPOINT" | "METRIC" | "OTHER";

/**
 * Training progress snapshot emitted by the runner.
 * When `available` is false the backend has no progress data yet —
 * the UI must show "Progress Information Not Available" (NFR-UX-002),
 * never assume 0 %.
 */
export type ProgressResponse = {
  /** False until the first PROGRESS WebSocket event is received by the runner. */
  available: boolean;
  /** Integer 0–100; undefined when !available. */
  value?: number;
  epoch?: number;
  totalEpoch?: number;
  /** ISO-8601 timestamp of the last update. */
  updatedAt?: string;
};

/** Full training-job record returned by GET /jobs/{jobId}. */
export type JobDetail = {
  jobId: string;
  projectId: string;
  projectName: string;
  triggeredBy: UserSummary;
  status: JobStatus;
  /** 0 when RUNNING, null/undefined when not queued. */
  queuePosition?: number;
  progress: ProgressResponse;
  /** Populated when this job is a retry of an earlier run. */
  retryOfJobId?: string;
  /** 0 on the original run; increments by 1 per retry. */
  retryAttempt: number;
  createdAt: string;
  queuedAt?: string;
  startedAt?: string;
  endedAt?: string;
  /** Human-readable reason populated on FAILED or CANCELLED. */
  failureReason?: string;
};

/** A single line emitted by the training process, keyed by logEventId for deduplication. */
export type LogEvent = {
  logEventId: string;
  /** 1-based monotonically increasing sequence number within the job. */
  sequenceNo: number;
  streamType: StreamType;
  message: string;
  emittedAt: string;
};
