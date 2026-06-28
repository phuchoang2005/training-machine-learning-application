import { Link } from "react-router-dom";
import { AlertTriangle, Loader2, X } from "lucide-react";
import { StatusBadge } from "../../shared/components/Badges";
import { formatDate } from "../../shared/format/formatters";
import type { ProjectSummary } from "../../shared/api/types";
import type { PendingBuild } from "../../store/slices/projectSlice";

export function ProjectTable({
  projects,
  pendingBuilds = [],
  onDismissBuild,
}: {
  projects: ProjectSummary[];
  pendingBuilds?: PendingBuild[];
  onDismissBuild?: (tempId: string) => void;
}) {
  return (
    <div className="data-table project-table">
      <div className="table-head">
        <span>Project</span><span>Status</span><span>Source</span><span>Last training</span><span>Owner</span><span>Action</span>
      </div>
      {pendingBuilds.map((build) => (
        <div key={build.tempId} className="table-row">
          <div><strong>{build.projectName}</strong><small>{build.description}</small></div>
          {build.status === "BUILDING" ? (
            <span className="status-badge building" title="Building the project's Docker image">
              <Loader2 size={14} className="spin" /> Building image…
            </span>
          ) : (
            <span className="status-badge failed" title={build.error ?? "Image build failed"}>
              <AlertTriangle size={14} /> Build failed
            </span>
          )}
          <span>{build.sourceType}</span>
          <span>—</span>
          <span>—</span>
          {build.status === "FAILED" && onDismissBuild ? (
            <button className="button secondary" onClick={() => onDismissBuild(build.tempId)}><X size={15} /> Dismiss</button>
          ) : (
            <span className="muted-text">Please wait…</span>
          )}
        </div>
      ))}
      {projects.map((project) => (
        <div key={project.projectId} className="table-row">
          <div><strong>{project.projectName}</strong><small>{project.description}</small></div>
          {project.buildStatus === "BUILDING" ? (
            <span className="status-badge building" title="Building the project's Docker image">
              <Loader2 size={14} className="spin" /> Building image…
            </span>
          ) : project.buildStatus === "FAILED" ? (
            <span className="status-badge failed" title="Image build failed — open to view the log">
              <AlertTriangle size={14} /> Build failed
            </span>
          ) : project.latestJobStatus ? <StatusBadge status={project.latestJobStatus} /> : <span className="badge neutral">No jobs</span>}
          <span>{project.sourceType}</span>
          <span>{formatDate(project.lastTrainingTime)}</span>
          <span>{project.lastTrainingOwner ?? "Not trained"}</span>
          <Link className="button secondary" to={`/projects/${project.projectId}`}>Open</Link>
        </div>
      ))}
    </div>
  );
}
