import { createAsyncThunk, createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type { Artifact, JobDetail, JobStatus, LogEvent, QueueSnapshot } from "../../shared/api/types";
import type { JobSummary } from "../../shared/api/services/jobs";
import { jobService } from "../../shared/api/services/jobs";

/**
 * Loads the job list for a project into `state.jobsByProjectId[projectId]`.
 * Returns `JobSummary` records (no progress/triggeredBy); call `fetchJobById` for the full detail.
 */
export const fetchJobsByProject = createAsyncThunk(
  "jobs/fetchByProject",
  async (projectId: string) => {
    const data = await jobService.listByProject(projectId, { limit: 50 });
    return { projectId, jobs: data.data };
  },
);

/**
 * Fetches the full `JobDetail` and writes it to `state.detailByJobId[jobId]`.
 * Called on JobDetailPage mount and during polling fallback.
 */
export const fetchJobById = createAsyncThunk("jobs/fetchById", (jobId: string) =>
  jobService.get(jobId),
);

/**
 * Submits a new training job.
 * Prepends a `JobSummary` stub to `state.jobsByProjectId[projectId]` immediately
 * so the project history updates without waiting for the next list fetch.
 * Navigate to `/jobs/{jobId}` after the thunk resolves.
 */
export const startJobAsync = createAsyncThunk(
  "jobs/start",
  async ({ projectId, configId, yamlContent }: { projectId: string; configId: string; yamlContent: string }) => {
    const res = await jobService.start(projectId, { configId, yamlContent });
    return { ...res, projectId };
  },
);

/**
 * Requests cancellation of a QUEUED or RUNNING job.
 * Updates `state.detailByJobId[jobId].status` to `CANCELLED` optimistically on fulfillment.
 * The backend is still authoritative — the WebSocket STATUS_CHANGE event will confirm.
 */
export const cancelJobAsync = createAsyncThunk(
  "jobs/cancel",
  async ({ jobId, reason }: { jobId: string; reason?: string }) => jobService.cancel(jobId, reason),
);

/**
 * Queues a retry of a FAILED or CANCELLED job using its previous configuration.
 * The new job gets a fresh `jobId`; the caller should navigate to `/jobs/{newJobId}`.
 */
export const retryJobAsync = createAsyncThunk("jobs/retry", async (jobId: string) => {
  const res = await jobService.retry(jobId);
  return res;
});

/** Refreshes the global queue snapshot in `state.admin.queue`. */
export const fetchQueue = createAsyncThunk("jobs/fetchQueue", () => jobService.getQueue());

/**
 * Fetches a page of log events and merges them into `state.logsByJobId[jobId]`.
 * Deduplicates by `logEventId` so this can be called repeatedly during polling fallback.
 */
export const fetchJobLogs = createAsyncThunk(
  "jobs/fetchLogs",
  async ({ jobId, cursor }: { jobId: string; cursor?: string }) => {
    const data = await jobService.getLogs(jobId, { limit: 200, cursor });
    return { jobId, events: data.data, hasMore: data.page.hasMore, nextCursor: data.page.nextCursor };
  },
);

/** Loads all artifacts for a job into `state.artifactsByJobId[jobId]`. */
export const fetchJobArtifacts = createAsyncThunk("jobs/fetchArtifacts", async (jobId: string) => {
  const data = await jobService.getArtifacts(jobId);
  return { jobId, artifacts: data.data };
});

/**
 * `jobsByProjectId` holds summary lists per project (populated by fetchJobsByProject).
 * `detailByJobId` holds full records keyed by jobId (populated by fetchJobById / startJobAsync).
 * Logs are capped at 1000 lines per job to bound memory; older lines are dropped.
 * `connection` drives the visible indicator in JobDetailPage and triggers polling fallback.
 */
type JobsState = {
  /** Summary lists from GET /projects/{id}/jobs, keyed by projectId. */
  jobsByProjectId: Record<string, JobSummary[]>;
  /** Full job records from GET /jobs/{id}, keyed by jobId. */
  detailByJobId: Record<string, JobDetail>;
  /** Live log lines, capped at 1000 per job; appended by WebSocket LOG events and fetchJobLogs. */
  logsByJobId: Record<string, LogEvent[]>;
  artifactsByJobId: Record<string, Artifact[]>;
  queue: QueueSnapshot;
  connection: "CONNECTED" | "RECONNECTING" | "FALLBACK POLLING" | "DISCONNECTED";
  loading: boolean;
  error?: string;
};

const emptyQueue: QueueSnapshot = { runningCount: 0, runningLimit: 1, queuedCount: 0, items: [] };

export const jobSlice = createSlice({
  name: "jobs",
  initialState: {
    jobsByProjectId: {},
    detailByJobId: {},
    logsByJobId: {},
    artifactsByJobId: {},
    queue: emptyQueue,
    connection: "DISCONNECTED",
    loading: false,
    error: undefined,
  } as JobsState,
  reducers: {
    appendLog(state, action: PayloadAction<{ jobId: string; event: LogEvent }>) {
      const { jobId, event } = action.payload;
      const lines = state.logsByJobId[jobId] ?? [];
      if (!lines.some((l) => l.logEventId === event.logEventId)) {
        state.logsByJobId[jobId] = [...lines, event].slice(-1000);
      }
    },
    updateJobProgress(state, action: PayloadAction<{ jobId: string; progress: JobDetail["progress"] }>) {
      const job = state.detailByJobId[action.payload.jobId];
      if (job) job.progress = action.payload.progress;
    },
    updateJobStatus(state, action: PayloadAction<{ jobId: string; status: JobStatus; endedAt?: string; failureReason?: string }>) {
      const job = state.detailByJobId[action.payload.jobId];
      if (job) {
        job.status = action.payload.status;
        if (action.payload.endedAt) job.endedAt = action.payload.endedAt;
        if (action.payload.failureReason) job.failureReason = action.payload.failureReason;
      }
    },
    setConnection(state, action: PayloadAction<JobsState["connection"]>) {
      state.connection = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchJobsByProject.pending, (state) => { state.loading = true; state.error = undefined; })
      .addCase(fetchJobsByProject.fulfilled, (state, action) => {
        state.loading = false;
        state.jobsByProjectId[action.payload.projectId] = action.payload.jobs;
      })
      .addCase(fetchJobsByProject.rejected, (state, action) => { state.loading = false; state.error = action.error.message; })

      .addCase(fetchJobById.fulfilled, (state, action) => {
        state.detailByJobId[action.payload.jobId] = action.payload;
      })

      .addCase(startJobAsync.fulfilled, (state, action) => {
        const { jobId, projectId, status, queuePosition, createdAt } = action.payload;
        const summary: JobSummary = { jobId, status: status as JobStatus, queuePosition, createdAt };
        const existing = state.jobsByProjectId[projectId] ?? [];
        state.jobsByProjectId[projectId] = [summary, ...existing];
      })

      .addCase(cancelJobAsync.fulfilled, (state, action) => {
        const job = state.detailByJobId[action.payload.jobId];
        if (job) {
          job.status = action.payload.status as JobStatus;
          job.endedAt = action.payload.cancelAcceptedAt;
        }
      })

      .addCase(retryJobAsync.fulfilled, (state, action) => {
        const summary: JobSummary = { jobId: action.payload.jobId, status: action.payload.status as JobStatus, createdAt: new Date().toISOString() };
        // Prepend to the project list for the original job's project — we look it up from the retryOf detail
        for (const projectId of Object.keys(state.jobsByProjectId)) {
          if (state.jobsByProjectId[projectId].some((j) => j.jobId === action.payload.retryOfJobId)) {
            state.jobsByProjectId[projectId] = [summary, ...state.jobsByProjectId[projectId]];
            break;
          }
        }
      })

      .addCase(fetchQueue.fulfilled, (state, action) => { state.queue = action.payload; })

      .addCase(fetchJobLogs.fulfilled, (state, action) => {
        const { jobId, events } = action.payload;
        const existing = state.logsByJobId[jobId] ?? [];
        const seen = new Set(existing.map((l) => l.logEventId));
        const newEvents = events.filter((e) => !seen.has(e.logEventId));
        state.logsByJobId[jobId] = [...existing, ...newEvents].slice(-1000);
      })

      .addCase(fetchJobArtifacts.fulfilled, (state, action) => {
        state.artifactsByJobId[action.payload.jobId] = action.payload.artifacts;
      });
  },
});
