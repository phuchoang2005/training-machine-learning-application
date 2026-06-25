import { apiClient } from "../axios-client";
import type { JobDetail, LogEvent, Artifact, QueueSnapshot } from "../types";

/** Cursor-based page envelope used by list endpoints. */
type ApiPage<T> = { data: T[]; page: { limit: number; nextCursor?: string | null; hasMore: boolean } };

/**
 * Lightweight record returned by the job list endpoint.
 * Does not include `triggeredBy`, `progress`, or retry metadata.
 * The full `JobDetail` is fetched by `get()`.
 */
export type JobSummary = {
  jobId: string;
  status: JobDetail["status"];
  /** Integer 0–100 if progress is available, otherwise null. */
  progressValue?: number | null;
  queuePosition?: number | null;
  createdAt: string;
  startedAt?: string | null;
  endedAt?: string | null;
};

export const jobService = {
  /**
   * Lists training jobs for a project in reverse-chronological order.
   * Returns `JobSummary` records; call `get()` for the full detail including progress and logs.
   */
  listByProject: (projectId: string, params?: { limit?: number; cursor?: string }) =>
    apiClient.get<ApiPage<JobSummary>>(`/projects/${projectId}/jobs`, { params }).then((r) => r.data),

  /** Fetches the full `JobDetail` including progress, retry metadata, and failure reason. */
  get: (jobId: string) =>
    apiClient.get<JobDetail>(`/jobs/${jobId}`).then((r) => r.data),

  /**
   * Submits a training job.
   * Both `configId` (from the saved config) and `yamlContent` are required so the backend
   * can link the job to its template while capturing an immutable YAML snapshot.
   * Returns only the generated `jobId` and initial status; navigate to /jobs/{jobId} to monitor.
   */
  start: (projectId: string, body: { configId: string; yamlContent: string }) =>
    apiClient.post<{ jobId: string; projectId: string; status: string; queuePosition?: number | null; configSnapshotId: string; createdAt: string }>(`/projects/${projectId}/jobs`, body).then((r) => r.data),

  /**
   * Requests cancellation of a QUEUED or RUNNING job.
   * The backend is authoritative for final status — the client reflects the change only after
   * the cancel response or the next WebSocket STATUS_CHANGE event.
   */
  cancel: (jobId: string, reason?: string) =>
    apiClient.post<{ jobId: string; status: string; cancelAcceptedAt: string }>(`/jobs/${jobId}/cancel`, { reason }).then((r) => r.data),

  /**
   * Queues a new attempt using the same configuration as the original FAILED/CANCELLED job.
   * The new job gets a fresh `jobId`; the original job's record is unchanged.
   */
  retry: (jobId: string) =>
    apiClient.post<{ jobId: string; retryOfJobId: string; status: string; retryAttempt: number }>(`/jobs/${jobId}/retry`, { mode: "PREVIOUS_CONFIGURATION" }).then((r) => r.data),

  /** Returns the current global queue snapshot including running and queued items. */
  getQueue: () =>
    apiClient.get<QueueSnapshot>("/jobs/queue").then((r) => r.data),

  /**
   * Fetches a page of log events.
   * Pass `cursor` from the previous page's `page.nextCursor` to paginate forward.
   * The WebSocket stream appends live events; this endpoint is used for the initial
   * batch load and the polling fallback.
   */
  getLogs: (jobId: string, params?: { cursor?: string; limit?: number }) =>
    apiClient.get<ApiPage<LogEvent>>(`/jobs/${jobId}/logs`, { params }).then((r) => r.data),

  /** Returns all artifacts produced by the job. Only populated in terminal states. */
  getArtifacts: (jobId: string) =>
    apiClient.get<{ data: Artifact[] }>(`/jobs/${jobId}/artifacts`).then((r) => r.data),

  /**
   * Full log download URL (plain text / CSV).
   * Use as an `<a href>` — the browser will stream the file directly.
   * Only meaningful in terminal job states (SUCCESS, FAILED, CANCELLED).
   */
  logsDownloadUrl: (jobId: string) => `/api/v1/jobs/${jobId}/logs/download`,

  /**
   * Artifact file download URL.
   * Use as an `<a href>` — the browser will stream the file directly.
   */
  artifactDownloadUrl: (artifactId: string) => `/api/v1/artifacts/${artifactId}/download`,
};
