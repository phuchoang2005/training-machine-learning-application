import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { FolderKanban, Plus, Search } from "lucide-react";
import { Banner, EmptyState } from "../../shared/components/Feedback";
import { Page, PageHeader } from "../../shared/components/Page";
import { Toolbar } from "../../shared/components/Form";
import { actions } from "../../store/store";
import { useAppDispatch, useAppSelector } from "../../store/hooks";
import { ProjectTable } from "./ProjectTable";
import type { JobStatus } from "../../shared/api/types";

const statusOrder: Array<JobStatus | "ALL"> = ["ALL", "CREATED", "QUEUED", "RUNNING", "SUCCESS", "FAILED", "CANCELLED", "RETRYING"];

export function ProjectDashboardPage() {
  const dispatch = useAppDispatch();
  const projects = useAppSelector((state) => state.projects.items);
  const pendingBuilds = useAppSelector((state) => state.projects.pendingBuilds);
  const filter = useAppSelector((state) => state.projects.activeStatusFilter);
  const loading = useAppSelector((state) => state.projects.loading);
  const error = useAppSelector((state) => state.projects.error);
  const [query, setQuery] = useState("");

  useEffect(() => { dispatch(actions.fetchProjects()); }, [dispatch]);

  // The per-project Docker image is built in the background after registration, so a freshly
  // registered project lands as BUILDING. Poll the list until no build is in flight (the row then
  // flips to READY/FAILED). This is independent of the create request, so it survives a tab close.
  const anyBuilding = projects.some((project) => project.buildStatus === "BUILDING")
    || pendingBuilds.some((build) => build.status === "BUILDING");
  useEffect(() => {
    if (!anyBuilding) return;
    const interval = window.setInterval(() => { dispatch(actions.fetchProjects()); }, 4000);
    return () => window.clearInterval(interval);
  }, [dispatch, anyBuilding]);

  const visibleProjects = projects.filter((project) =>
    `${project.projectName} ${project.description}`.toLowerCase().includes(query.toLowerCase()) &&
    (filter === "ALL" || project.latestJobStatus === filter),
  );
  // In-flight builds have no job status yet, so they only show under the "ALL" filter.
  const visibleBuilds = filter === "ALL"
    ? pendingBuilds.filter((build) => `${build.projectName} ${build.description}`.toLowerCase().includes(query.toLowerCase()))
    : [];

  return (
    <Page>
      <PageHeader title="Project Dashboard" subtitle="Authorized projects, latest training state, and direct access to configuration and monitoring workflows." action={<Link className="button primary" to="/projects/new"><Plus size={17} /> Register Project</Link>} />
      {error && <Banner tone="danger">{error}</Banner>}
      <Toolbar>
        <label className="search-field"><Search size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search projects" /></label>
        <div className="segmented" role="tablist" aria-label="Status filter">
          {statusOrder.map((status) => <button key={status} className={filter === status ? "active" : ""} onClick={() => dispatch(actions.setStatusFilter(status))}>{status}</button>)}
        </div>
      </Toolbar>
      {loading && projects.length === 0 && visibleBuilds.length === 0 ? (
        <EmptyState title="Loading projects…" message="Fetching authorized projects from the API." />
      ) : visibleProjects.length === 0 && visibleBuilds.length === 0 ? (
        <EmptyState icon={<FolderKanban />} title="No authorized projects" message="No projects match the current filters." />
      ) : (
        <ProjectTable projects={visibleProjects} pendingBuilds={visibleBuilds} onDismissBuild={(tempId) => dispatch(actions.dismissBuild(tempId))} />
      )}
    </Page>
  );
}
