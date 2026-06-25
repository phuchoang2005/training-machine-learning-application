import { Link } from "react-router-dom";
import { StatusBadge } from "../../shared/components/Badges";
import { formatDate } from "../../shared/format/formatters";
import type { ProjectSummary } from "../../shared/api/types";

export function ProjectTable({ projects }: { projects: ProjectSummary[] }) {
  return (
    <div className="data-table project-table">
      <div className="table-head">
        <span>Project</span><span>Status</span><span>Source</span><span>Last training</span><span>Owner</span><span>Action</span>
      </div>
      {projects.map((project) => (
        <div key={project.projectId} className="table-row">
          <div><strong>{project.projectName}</strong><small>{project.description}</small></div>
          {project.latestJobStatus ? <StatusBadge status={project.latestJobStatus} /> : <span className="badge neutral">No jobs</span>}
          <span>{project.sourceType}</span>
          <span>{formatDate(project.lastTrainingTime)}</span>
          <span>{project.lastTrainingOwner ?? "Not trained"}</span>
          <Link className="button secondary" to={`/projects/${project.projectId}`}>Open</Link>
        </div>
      ))}
    </div>
  );
}
