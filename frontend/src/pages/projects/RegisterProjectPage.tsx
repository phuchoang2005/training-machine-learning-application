import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FileCode2, Plus, Upload } from "lucide-react";
import { Banner } from "../../shared/components/Feedback";
import { FileDrop, FormGrid, TextField } from "../../shared/components/Form";
import { Page, PageHeader } from "../../shared/components/Page";
import { actions } from "../../store/store";
import { useAppDispatch } from "../../store/hooks";

export function RegisterProjectPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [sourceType, setSourceType] = useState<"GITHUB" | "ZIP">("GITHUB");
  const [projectName, setProjectName] = useState("");
  const [description, setDescription] = useState("");
  const [repositoryUrl, setRepositoryUrl] = useState("");
  const [trainingEntrypoint, setTrainingEntrypoint] = useState("python train.py --config training.yaml");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | undefined>();
  const valid = projectName.trim() && trainingEntrypoint.trim() && (sourceType === "ZIP" || repositoryUrl.startsWith("https://"));

  const submit = async () => {
    setSubmitting(true);
    setError(undefined);
    try {
      const result = await dispatch(actions.createProjectAsync({ projectName, description, sourceType, repositoryUrl, trainingEntrypoint })).unwrap();
      navigate(`/projects/${result.projectId}`);
    } catch (err: unknown) {
      setError((err as { message?: string })?.message ?? "Failed to register project. Please try again.");
      setSubmitting(false);
    }
  };

  return (
    <Page width="form">
      <PageHeader title="Register Project" subtitle="Create a GitHub or ZIP-backed training project with the source metadata required by the API contract." />
      <section className="panel">
        <div className="segmented large">
          <button className={sourceType === "GITHUB" ? "active" : ""} onClick={() => setSourceType("GITHUB")}><FileCode2 size={16} /> GitHub</button>
          <button className={sourceType === "ZIP" ? "active" : ""} onClick={() => setSourceType("ZIP")}><Upload size={16} /> ZIP Upload</button>
        </div>
        <FormGrid>
          <TextField label="Project name" value={projectName} onChange={setProjectName} />
          <TextField label="Description" value={description} onChange={setDescription} />
          {sourceType === "GITHUB" ? <TextField label="Repository URL" value={repositoryUrl} onChange={setRepositoryUrl} placeholder="https://github.com/company/model" /> : <FileDrop />}
          <TextField label="Training entrypoint" value={trainingEntrypoint} onChange={setTrainingEntrypoint} />
        </FormGrid>
        {error && <Banner tone="danger">{error}</Banner>}
        <div className="form-actions">
          <button className="button primary" disabled={!valid || submitting} onClick={submit}>
            <Plus size={17} /> {submitting ? "Creating…" : "Create Project"}
          </button>
        </div>
      </section>
    </Page>
  );
}
