import { useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { Page, PageHeader } from "../../shared/components/Page";
import { Banner } from "../../shared/components/Feedback";
import { actions } from "../../store/store";
import { useAppDispatch, useAppSelector } from "../../store/hooks";
import { createJobStreamClient } from "../../shared/realtime/job-stream-client";
import type { JobStreamEvent } from "../../shared/realtime/job-stream-client";
import { ErrorPage } from "../ErrorPage";
import { ActionToolbar } from "./ActionToolbar";
import { CancelJobDialog } from "./CancelJobDialog";
import { JobSidebar } from "./JobSidebar";
import { LogPanel } from "./LogPanel";
import type { JobDetail, LogEvent } from "../../shared/api/types";

const TERMINAL_STATUSES = new Set(["SUCCESS", "FAILED", "CANCELLED"]);
const POLL_INTERVAL_MS = 5_000;

export function JobDetailPage() {
  const { jobId } = useParams();
  const dispatch = useAppDispatch();
  const job = useAppSelector((state) => state.jobs.detailByJobId[jobId ?? ""]);
  const logs = useAppSelector((state) => state.jobs.logsByJobId[jobId ?? ""] ?? []);
  const artifacts = useAppSelector((state) => state.jobs.artifactsByJobId[jobId ?? ""] ?? []);
  const connection = useAppSelector((state) => state.jobs.connection);
  const token = useAppSelector((state) => state.auth.currentUser?.email);
  const jobStatus = job?.status;
  const [cancelOpen, setCancelOpen] = useState(false);
  const [error, setError] = useState<string | undefined>();
  const lastEventIdRef = useRef<string | undefined>(undefined);
  const pollTimerRef = useRef<number | undefined>(undefined);

  // Initial load
  useEffect(() => {
    if (!jobId) return;
    dispatch(actions.fetchJobById(jobId)).unwrap().catch((e: { message?: string }) => setError(e?.message ?? "Failed to load job."));
    dispatch(actions.fetchJobArtifacts(jobId));
    dispatch(actions.fetchJobLogs({ jobId }));
  }, [dispatch, jobId]);

  // WebSocket stream
  useEffect(() => {
    if (!jobId || !token) return;
    if (jobStatus && TERMINAL_STATUSES.has(jobStatus)) {
      dispatch(actions.setConnection("DISCONNECTED"));
      return;
    }

    const client = createJobStreamClient({
      jobId,
      token,
      lastEventId: lastEventIdRef.current,
      onEvent: (event: JobStreamEvent) => {
        if (event.eventId) lastEventIdRef.current = event.eventId;

        if (event.type === "LOG") {
          const logEvent = event.payload as LogEvent;
          if (logEvent?.logEventId) dispatch(actions.appendLog({ jobId, event: logEvent }));
        } else if (event.type === "PROGRESS") {
          const progress = event.payload as JobDetail["progress"];
          if (progress) dispatch(actions.updateJobProgress({ jobId, progress }));
        } else if (event.type === "STATUS_CHANGE") {
          const payload = event.payload as { status: JobDetail["status"]; endedAt?: string; failureReason?: string };
          if (payload?.status) dispatch(actions.updateJobStatus({ jobId, ...payload }));
        }
      },
      onStateChange: (wsState) => {
        const map = {
          connecting: "RECONNECTING",
          connected: "CONNECTED",
          reconnecting: "RECONNECTING",
          closed: "DISCONNECTED",
          unauthorized: "DISCONNECTED",
          unavailable: "FALLBACK POLLING",
        } as const;
        dispatch(actions.setConnection(map[wsState]));
      },
    });

    return () => client.close();
  }, [dispatch, jobId, token, jobStatus]);

  // Polling fallback when WS unavailable
  useEffect(() => {
    if (connection !== "FALLBACK POLLING" || !jobId) {
      window.clearInterval(pollTimerRef.current);
      return;
    }
    pollTimerRef.current = window.setInterval(() => {
      dispatch(actions.fetchJobById(jobId));
      dispatch(actions.fetchJobLogs({ jobId }));
    }, POLL_INTERVAL_MS);
    return () => window.clearInterval(pollTimerRef.current);
  }, [connection, dispatch, jobId]);

  if (!jobId) return <ErrorPage code="404" title="Job not found" message="No job ID in route." />;
  if (error && !job) return <ErrorPage code="404" title="Job not found" message={error} />;
  if (!job) return (
    <Page>
      <PageHeader title="Loading job…" subtitle="Fetching job details from the API." />
    </Page>
  );

  const cancelJob = async (reason: string) => {
    setCancelOpen(false);
    try {
      await dispatch(actions.cancelJobAsync({ jobId: job.jobId, reason })).unwrap();
    } catch {
      // error already reflected in job status via server response
    }
  };

  const retryJob = async () => {
    try {
      const result = await dispatch(actions.retryJobAsync(job.jobId)).unwrap();
      window.location.href = `/jobs/${result.jobId}`;
    } catch {
      // leave user on current page
    }
  };

  return (
    <Page>
      <PageHeader
        title={`Job ${job.jobId}`}
        subtitle={job.projectName}
        action={<ActionToolbar job={job} onCancel={() => setCancelOpen(true)} onRetry={retryJob} />}
      />
      {connection === "FALLBACK POLLING" && (
        <Banner tone="warning">Real-time stream unavailable — polling for updates every {POLL_INTERVAL_MS / 1000}s.</Banner>
      )}
      <div className="job-layout">
        <JobSidebar job={job} artifacts={artifacts} />
        <LogPanel logs={logs} connection={connection} jobId={job.jobId} jobStatus={job.status} />
      </div>
      {cancelOpen && <CancelJobDialog onClose={() => setCancelOpen(false)} onCancel={cancelJob} />}
    </Page>
  );
}
