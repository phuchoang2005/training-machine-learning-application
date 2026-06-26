import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FileCode2, Plus, Upload } from "lucide-react";
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
  const [trainingEntrypoint, setTrainingEntrypoint] = useState("python main.py");
  const [zipFile, setZipFile] = useState<File | undefined>();
  const valid = projectName.trim() && trainingEntrypoint.trim() && (sourceType === "GITHUB" ? repositoryUrl.startsWith("https://") : !!zipFile);

  // The backend builds the Docker image synchronously inside the create request,
  // so rather than blocking the form we register an optimistic "building" entry,
  // fire the create thunk (it resolves later, updating the dashboard row), and
  // hand the user straight back to the project list to watch the build status.
  const submit = () => {
    const tempId = crypto.randomUUID();
    dispatch(actions.startProjectBuild({ tempId, projectName, description, sourceType }));
    if (sourceType === "ZIP") {
      dispatch(actions.createZipProjectAsync({ tempId, projectName, description, trainingEntrypoint, file: zipFile! }));
    } else {
      dispatch(actions.createProjectAsync({ tempId, projectName, description, sourceType, repositoryUrl, trainingEntrypoint }));
    }
    navigate("/projects");
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
          {sourceType === "GITHUB"
            ? <TextField label="Repository URL" value={repositoryUrl} onChange={setRepositoryUrl} placeholder="https://github.com/company/model" />
            : <FileDrop file={zipFile} onChange={setZipFile} />}
          <TextField label="Training entrypoint" value={trainingEntrypoint} onChange={setTrainingEntrypoint} />
        </FormGrid>
        <div className="form-actions">
          <button className="button primary" disabled={!valid} onClick={submit}>
            <Plus size={17} /> Create Project
          </button>
        </div>
      </section>
    </Page>
  );
}
