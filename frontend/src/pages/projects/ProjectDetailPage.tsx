import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Play } from "lucide-react";
import { StatusBadge } from "../../shared/components/Badges";
import { Dialog } from "../../shared/components/Dialog";
import { Banner, KeyValue } from "../../shared/components/Feedback";
import { Page, PageHeader } from "../../shared/components/Page";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../shared/ui/tabs";
import { actions } from "../../store/store";
import { useAppDispatch, useAppSelector } from "../../store/hooks";
import { ErrorPage } from "../ErrorPage";
import { ConfigEditor } from "./ConfigEditor";
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
  const [yaml, setYaml] = useState("");
  const [validation, setValidation] = useState<"idle" | "valid" | "invalid">("idle");
  const [startOpen, setStartOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [starting, setStarting] = useState(false);

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
          <button className="button primary" onClick={() => setStartOpen(true)} disabled={starting || !config}>
            <Play size={17} /> {starting ? "Starting…" : "Start Training"}
          </button>
        }
      />
      {!config && <Banner tone="warning">No configuration found for this project.</Banner>}
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
      <KeyValue label="Latest status" value={project.latestJobStatus ? <StatusBadge status={project.latestJobStatus} /> : "No jobs yet"} />
    </section>
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
