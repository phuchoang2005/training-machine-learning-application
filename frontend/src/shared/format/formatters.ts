import type { JobStatus } from "../api/types";

/**
 * Formats an ISO-8601 datetime string for display (e.g. "Jun 25, 02:30").
 * Returns `"-"` for absent/undefined values so tables never show empty cells.
 */
export function formatDate(value?: string) {
  if (!value) return "-";
  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

/** Human-readable file size. Uses SI units (1 MB = 1 000 000 B). */
export function formatBytes(bytes: number) {
  if (bytes > 1_000_000) return `${(bytes / 1_000_000).toFixed(1)} MB`;
  if (bytes > 1_000) return `${(bytes / 1_000).toFixed(1)} KB`;
  return `${bytes} B`;
}

/**
 * Wall-clock duration of a job rounded to the nearest minute.
 * Uses `startedAt` when available, falls back to `createdAt` for QUEUED jobs.
 * Uses `endedAt` when present, otherwise `Date.now()` for running jobs.
 * Minimum returned value is 1 minute to avoid showing "0m".
 */
export function duration(job: { startedAt?: string | null; endedAt?: string | null; createdAt: string }) {
  const start = new Date(job.startedAt ?? job.createdAt).getTime();
  const end = job.endedAt ? new Date(job.endedAt).getTime() : Date.now();
  return `${Math.max(1, Math.round((end - start) / 60_000))}m`;
}

/** Title-cases a `JobStatus` for display (e.g. "RUNNING" → "Running"). */
export function statusLabel(status: JobStatus) {
  return status.charAt(0) + status.slice(1).toLowerCase();
}
