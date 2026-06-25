import { Download, Search } from "lucide-react";
import { useState } from "react";
import { ConnectionBadge } from "../../shared/components/Badges";
import { Toolbar } from "../../shared/components/Form";
import { jobService } from "../../shared/api/services/jobs";
import type { JobStatus, LogEvent, StreamType } from "../../shared/api/types";

const TERMINAL_STATUSES = new Set<JobStatus>(["SUCCESS", "FAILED", "CANCELLED"]);

export function LogPanel({ logs, connection, jobId, jobStatus }: {
  logs: LogEvent[];
  connection: string;
  jobId: string;
  jobStatus: JobStatus;
}) {
  const [filter, setFilter] = useState<StreamType | "ALL">("ALL");
  const [query, setQuery] = useState("");
  const visibleLogs = logs.filter((line) =>
    (filter === "ALL" || line.streamType === filter) &&
    line.message.toLowerCase().includes(query.toLowerCase()),
  );
  const canDownload = TERMINAL_STATUSES.has(jobStatus);

  return (
    <section className="panel log-panel">
      <div className="panel-header">
        <h2>Live Logs</h2>
        <ConnectionBadge state={connection} />
      </div>
      <Toolbar>
        <label className="search-field">
          <Search size={17} />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search logs" />
        </label>
        <div className="segmented">
          {(["ALL", "STDOUT", "STDERR"] as const).map((item) => (
            <button key={item} className={filter === item ? "active" : ""} onClick={() => setFilter(item)}>{item}</button>
          ))}
        </div>
        {canDownload && (
          <a
            className="button secondary"
            href={jobService.logsDownloadUrl(jobId)}
            download
            aria-label="Download full log file"
          >
            <Download size={16} /> Download
          </a>
        )}
      </Toolbar>
      <div className="log-viewer" role="log" aria-live="polite">
        {visibleLogs.map((line) => (
          <div className="log-line" key={line.logEventId}>
            <span>{line.sequenceNo.toString().padStart(4, "0")}</span>
            <span>{line.streamType}</span>
            <code>{line.message}</code>
          </div>
        ))}
        {visibleLogs.length === 0 && logs.length === 0 && (
          <p className="log-empty">No log output yet.</p>
        )}
      </div>
    </section>
  );
}
