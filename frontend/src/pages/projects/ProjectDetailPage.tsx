import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Play, Trash2 } from "lucide-react";
import { StatusBadge } from "../../shared/components/Badges";
import { Dialog } from "../../shared/components/Dialog";
import { Banner, KeyValue } from "../../shared/components/Feedback";
import { Page, PageHeader } from "../../shared/components/Page";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../shared/ui/tabs";
import { actions } from "../../store/store";
import { useAppDispatch, useAppSelector } from "../../store/hooks";
import { ErrorPage } from "../ErrorPage";
import { ConfigEditor, DEFAULT_CONFIG_YAML } from "./ConfigEditor";
import { TrainingHistory } from "./TrainingHistory";
import type { ProjectDetail } from "../../shared/api/types";

export function ProjectDetailPage({ initialTab = "overview" }: { initialTab?: "overview" | "config" | "history" }) {
  const { projectId } = useParams();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const project = useAppSelector((state) => state.projects.detailByProjectId[projectId ?? ""]);
  const config = useAppSelector((state) => state.projects.configsByProjectId[projectId ?? ""]);
  const jobs = useAppSelector((state) => state.jobs.jobsByProjectId[projectId ?? ""] ?? []);
  const error = useAppSelector((state) => state.projects.error);
  const [tab, setTab] = useState(initialTab);
  const [yaml, setYaml] = useState(DEFAULT_CONFIG_YAML);
  const [validation, setValidation] = useState<"idle" | "valid" | "invalid">("idle");
  const [startOpen, setStartOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [starting, setStarting] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (!projectId) return;
    dispatch(actions.fetchProjectById(projectId));
    dispatch(actions.fetchProjectConfig(projectId));
    dispatch(actions.fetchJobsByProject(projectId));
  }, [dispatch, projectId]);

  useEffect(() => {
    if (config?.yamlContent) setYaml(config.yamlContent);
  }, [config?.yamlContent]);

  if (!projectId) return <ErrorPage code="404" title="Project not found" message="No project ID in route." />;
  if (error && !project) return <ErrorPage code="500" title="Failed to load project" message={error} />;
  if (!project) return (
    <Page>
      <PageHeader title="Loading…" subtitle="Fetching project details." />
    </Page>
  );

  const handleSave = async () => {
    if (!config) return;
    setSaving(true);
    try {
      await dispatch(actions.saveConfigAsync({ projectId: project.projectId, configId: config.configId, yamlContent: yaml })).unwrap();
    } finally {
      setSaving(false);
    }
  };

  const handleValidate = async () => {
    if (!projectId) return;
    try {
      const result = await dispatch(actions.validateConfigAsync({ projectId, yamlContent: yaml })).unwrap();
      setValidation(result.valid ? "valid" : "invalid");
    } catch {
      setValidation("invalid");
    }
  };

  const deleteProject = async () => {
    setDeleting(true);
    try {
      await dispatch(actions.deleteProjectAsync(project.projectId)).unwrap();
      navigate("/projects");
    } catch {
      setDeleting(false);
      setDeleteOpen(false);
    }
  };

  const startTraining = async () => {
    if (!config) return;
    setStarting(true);
    setStartOpen(false);
    try {
      const result = await dispatch(actions.startJobAsync({ projectId: project.projectId, configId: config.configId, yamlContent: yaml })).unwrap();
      navigate(`/jobs/${result.jobId}`);
    } catch {
      setStarting(false);
    }
  };

  return (
    <Page>
      <PageHeader
        title={project.projectName}
        subtitle={project.description}
        action={
          <>
            <button className="button danger" onClick={() => setDeleteOpen(true)} disabled={deleting}>
              <Trash2 size={17} /> {deleting ? "Deleting…" : "Delete"}
            </button>
            <button className="button primary" onClick={() => setStartOpen(true)} disabled={starting || !config || project.buildStatus === "BUILDING" || project.buildStatus === "FAILED"}>
              <Play size={17} /> {starting ? "Starting…" : "Start Training"}
            </button>
          </>
        }
      />
      {project.buildStatus === "BUILDING" && (
        <Banner tone="warning">The project's Docker image is still building. Training can start once the build is ready.</Banner>
      )}
      {project.buildStatus === "FAILED" && (
        <Banner tone="danger">The project's Docker image build failed. See the build log below; you may delete and re-register the project.</Banner>
      )}
      {project.buildStatus !== "BUILDING" && project.buildStatus !== "FAILED" && !config && <Banner tone="warning">No configuration found for this project.</Banner>}
      <div className="split-layout">
        <ProjectSummaryPanel project={project} />
        <section className="panel">
          <Tabs value={tab} onValueChange={(value) => setTab(value as typeof tab)}>
            <TabsList>{["overview", "config", "history"].map((item) => <TabsTrigger key={item} value={item}>{item === "config" ? "Configuration" : item[0].toUpperCase() + item.slice(1)}</TabsTrigger>)}</TabsList>
            <TabsContent value="overview"><TrainingHistory jobs={jobs.slice(0, 3)} compact /></TabsContent>
            <TabsContent value="config">
              <ConfigEditor
                yaml={yaml}
                setYaml={(v) => { setYaml(v); setValidation("idle"); }}
                validation={validation}
                onValidate={handleValidate}
                onSave={handleSave}
                saving={saving}
              />
            </TabsContent>
            <TabsContent value="history"><TrainingHistory jobs={jobs} /></TabsContent>
          </Tabs>
        </section>
      </div>
      {startOpen && (
        <StartDialog yaml={yaml} onClose={() => setStartOpen(false)} onStart={startTraining} />
      )}
      {deleteOpen && (
        <DeleteDialog projectName={project.projectName} onClose={() => setDeleteOpen(false)} onDelete={deleteProject} />
      )}
    </Page>
  );
}

function ProjectSummaryPanel({ project }: { project: ProjectDetail }) {
  return (
    <section className="panel">
      <h2>Project Summary</h2>
      <KeyValue label="Source" value={project.sourceType} />
      <KeyValue label="Repository" value={project.repositoryUrl ?? "ZIP upload"} />
      <KeyValue label="Entrypoint" value={project.trainingEntrypoint} />
      <KeyValue label="Owner" value={project.owner.email} />
      <KeyValue label="Image build" value={project.buildStatus ?? "—"} />
      <KeyValue label="Latest status" value={project.latestJobStatus ? <StatusBadge status={project.latestJobStatus} /> : "No jobs yet"} />
      {project.buildLog && (
        <details className="build-log">
          <summary>Build log</summary>
          <pre>{project.buildLog}</pre>
        </details>
      )}
    </section>
  );
}

function DeleteDialog({ projectName, onClose, onDelete }: { projectName: string; onClose: () => void; onDelete: () => void }) {
  return (
    <Dialog title="Delete Project" onClose={onClose}>
      <p>
        Permanently delete <strong>{projectName}</strong> and all associated data — its Docker image,
        job containers, configurations, and on-disk source. This action cannot be undone.
      </p>
      <div className="dialog-actions">
        <button className="button secondary" onClick={onClose}>Cancel</button>
        <button className="button danger" onClick={onDelete}><Trash2 size={17} /> Delete Project</button>
      </div>
    </Dialog>
  );
}

function StartDialog({ yaml, onClose, onStart }: { yaml: string; onClose: () => void; onStart: () => void }) {
  return (
    <Dialog title="Start Training With Care" onClose={onClose}>
      <p>The platform will submit the current YAML as an immutable configuration snapshot for this training job.</p>
      <pre className="yaml-preview">{yaml}</pre>
      <div className="dialog-actions">
        <button className="button secondary" onClick={onClose}>Cancel</button>
        <button className="button primary" onClick={onStart}><Play size={17} /> Start</button>
      </div>
    </Dialog>
  );
}
