import { Link } from "react-router-dom";
import { StatusBadge } from "../../shared/components/Badges";
import { EmptyState } from "../../shared/components/Feedback";
import { duration, formatDate } from "../../shared/format/formatters";
import type { JobStatus } from "../../shared/api/types";

export type HistoryJob = {
  jobId: string;
  status: JobStatus;
  createdAt: string;
  startedAt?: string | null;
  endedAt?: string | null;
};

export function TrainingHistory({ jobs, compact = false }: { jobs: HistoryJob[]; compact?: boolean }) {
  if (jobs.length === 0) {
    return <EmptyState title="No training jobs" message="Start training to create the first job history row." compact />;
  }

  return (
    <div className={compact ? "data-table compact" : "data-table"}>
      <div className="table-head"><span>Job</span><span>Status</span><span>Started</span><span>Duration</span><span>Action</span></div>
      {jobs.map((job) => (
        <div className="table-row" key={job.jobId}>
          <span>{job.jobId}</span>
          <StatusBadge status={job.status} />
          <span>{formatDate(job.startedAt ?? job.createdAt)}</span>
          <span>{duration(job)}</span>
          <Link className="button secondary" to={`/jobs/${job.jobId}`}>Open</Link>
        </div>
      ))}
    </div>
  );
}
