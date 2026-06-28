import { createAsyncThunk, createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type { JobStatus, ProjectConfigContent, ProjectDetail, ProjectSummary, SourceType } from "../../shared/api/types";
import { projectService } from "../../shared/api/services/projects";

/**
 * Client-side optimistic record bridging the brief gap between submitting the
 * register form and the real project landing in `items`. The backend builds the
 * Docker image off the request thread, so the create thunk resolves quickly with
 * a `BUILDING` project; the dashboard then polls until `buildStatus` becomes
 * `READY`/`FAILED`. We add one of these the moment the form is submitted, then
 * drop it once the real project is upserted (success) or flip it to FAILED if the
 * create request itself fails. `tempId` correlates the thunk arg to its record.
 */
export type PendingBuild = {
  tempId: string;
  projectName: string;
  description: string;
  sourceType: SourceType;
  status: "BUILDING" | "FAILED";
  error?: string;
};

/** Loads all projects visible to the current user into `state.items`. */
export const fetchProjects = createAsyncThunk("projects/fetchAll", () =>
  projectService.list({ limit: 100 }).then((r) => r.data),
);

/**
 * Fetches the full `ProjectDetail` for a single project and writes it to
 * `state.detailByProjectId[projectId]`. Also upserts into `state.items`
 * so the dashboard reflects the latest status without a separate list fetch.
 */
export const fetchProjectById = createAsyncThunk("projects/fetchById", (projectId: string) =>
  projectService.get(projectId),
);

/**
 * Registers a new GitHub project.
 * Makes two sequential API calls: POST /projects (creates, returns projectId) then
 * GET /projects/{id} (fetches the full `BUILDING` detail). The image build runs in the
 * background; the dashboard polls until `buildStatus` leaves `BUILDING`.
 */
export const createProjectAsync = createAsyncThunk(
  "projects/create",
  async (body: { tempId?: string; projectName: string; description?: string; sourceType: "GITHUB" | "ZIP"; repositoryUrl?: string; trainingEntrypoint: string }) => {
    const { repositoryUrl, trainingEntrypoint, projectName, description } = body;
    const { projectId } = await projectService.createGithub({ projectName, description, repositoryUrl: repositoryUrl!, trainingEntrypoint });
    return projectService.get(projectId);
  },
);

/**
 * Uploads a ZIP archive and registers it as a new project.
 * Makes two sequential API calls: POST /projects/upload-zip (creates, returns projectId) then
 * GET /projects/{id} (fetches the full `BUILDING` detail). The image build runs in the background;
 * the dashboard polls until `buildStatus` leaves `BUILDING`.
 */
export const createZipProjectAsync = createAsyncThunk(
  "projects/createZip",
  async (body: { tempId?: string; projectName: string; description?: string; trainingEntrypoint: string; file: File }) => {
    const { file, projectName, description, trainingEntrypoint } = body;
    const { projectId } = await projectService.createZip({ projectName, description, trainingEntrypoint }, file);
    return projectService.get(projectId);
  },
);

/**
 * Loads the default YAML configuration for a project.
 * Makes two sequential API calls: list configs (to find the default configId)
 * then fetch content (to get yamlContent). The result is stored in
 * `state.configsByProjectId[projectId]`.
 */
export const fetchProjectConfig = createAsyncThunk("projects/fetchConfig", async (projectId: string) => {
  const list = await projectService.listConfigs(projectId);
  const summary = list.data.find((c) => c.isDefault) ?? list.data[0];
  if (!summary) throw new Error("No configuration found for project");
  const config = await projectService.getConfig(projectId, summary.configId);
  return { projectId, config };
});

/**
 * Persists updated YAML to the backend and updates `state.configsByProjectId[projectId]`.
 * Note: saving is optional before starting a job — `startJobAsync` sends `yamlContent`
 * directly and the backend creates an immutable snapshot regardless.
 */
export const saveConfigAsync = createAsyncThunk(
  "projects/saveConfig",
  async ({ projectId, configId, yamlContent }: { projectId: string; configId: string; yamlContent: string }) => {
    const config = await projectService.saveConfig(projectId, configId, yamlContent);
    return { projectId, config };
  },
);

/**
 * Permanently deletes a project and all associated data, then removes it from
 * `state.items`, `state.detailByProjectId`, and `state.configsByProjectId`.
 */
export const deleteProjectAsync = createAsyncThunk(
  "projects/delete",
  async (projectId: string) => {
    await projectService.delete(projectId);
    return projectId;
  },
);

/** Server-side YAML validation. Does not persist anything or modify store state. */
export const validateConfigAsync = createAsyncThunk(
  "projects/validateConfig",
  ({ projectId, yamlContent }: { projectId: string; yamlContent: string }) =>
    projectService.validateConfig(projectId, yamlContent),
);

/**
 * Split between `items` (summaries from the list endpoint) and `detailByProjectId`
 * (full records from the get-by-id endpoint) lets the dashboard render immediately
 * from the paginated list while the detail page fetches only what it needs.
 */
type ProjectsState = {
  /** Lightweight records from GET /projects — no repositoryUrl/trainingEntrypoint/owner. */
  items: ProjectSummary[];
  /** Full records keyed by projectId, populated by fetchProjectById or createProjectAsync. */
  detailByProjectId: Record<string, ProjectDetail>;
  /** Active YAML config keyed by projectId; configId here is the real UUID for job start. */
  configsByProjectId: Record<string, ProjectConfigContent>;
  /** Optimistic in-flight image builds shown in the dashboard while the create request runs. */
  pendingBuilds: PendingBuild[];
  activeStatusFilter: JobStatus | "ALL";
  loading: boolean;
  error?: string;
};

export const projectSlice = createSlice({
  name: "projects",
  initialState: {
    items: [],
    detailByProjectId: {},
    configsByProjectId: {},
    pendingBuilds: [],
    activeStatusFilter: "ALL",
    loading: false,
    error: undefined,
  } as ProjectsState,
  reducers: {
    setStatusFilter(state, action: PayloadAction<JobStatus | "ALL">) {
      state.activeStatusFilter = action.payload;
    },
    /** Optimistically register an in-flight image build so it appears in the dashboard immediately. */
    startProjectBuild(state, action: PayloadAction<{ tempId: string; projectName: string; description: string; sourceType: SourceType }>) {
      state.pendingBuilds.unshift({ ...action.payload, status: "BUILDING" });
    },
    /** Remove a pending build (used to dismiss a failed build card). */
    dismissBuild(state, action: PayloadAction<string>) {
      state.pendingBuilds = state.pendingBuilds.filter((b) => b.tempId !== action.payload);
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchProjects.pending, (state) => { state.loading = true; state.error = undefined; })
      .addCase(fetchProjects.fulfilled, (state, action) => { state.loading = false; state.items = action.payload; })
      .addCase(fetchProjects.rejected, (state, action) => { state.loading = false; state.error = action.error.message; })

      .addCase(fetchProjectById.fulfilled, (state, action) => {
        state.detailByProjectId[action.payload.projectId] = action.payload;
        const idx = state.items.findIndex((p) => p.projectId === action.payload.projectId);
        if (idx >= 0) state.items[idx] = action.payload;
        else state.items.unshift(action.payload);
      })

      .addCase(createProjectAsync.fulfilled, (state, action) => {
        state.detailByProjectId[action.payload.projectId] = action.payload;
        state.items.unshift(action.payload);
        state.pendingBuilds = state.pendingBuilds.filter((b) => b.tempId !== action.meta.arg.tempId);
      })
      .addCase(createProjectAsync.rejected, (state, action) => {
        const build = state.pendingBuilds.find((b) => b.tempId === action.meta.arg.tempId);
        if (build) { build.status = "FAILED"; build.error = action.error.message; }
      })

      .addCase(createZipProjectAsync.fulfilled, (state, action) => {
        state.detailByProjectId[action.payload.projectId] = action.payload;
        state.items.unshift(action.payload);
        state.pendingBuilds = state.pendingBuilds.filter((b) => b.tempId !== action.meta.arg.tempId);
      })
      .addCase(createZipProjectAsync.rejected, (state, action) => {
        const build = state.pendingBuilds.find((b) => b.tempId === action.meta.arg.tempId);
        if (build) { build.status = "FAILED"; build.error = action.error.message; }
      })

      .addCase(fetchProjectConfig.fulfilled, (state, action) => {
        state.configsByProjectId[action.payload.projectId] = action.payload.config;
      })

      .addCase(saveConfigAsync.fulfilled, (state, action) => {
        state.configsByProjectId[action.payload.projectId] = action.payload.config;
      })

      .addCase(deleteProjectAsync.fulfilled, (state, action) => {
        const projectId = action.payload;
        state.items = state.items.filter((p) => p.projectId !== projectId);
        delete state.detailByProjectId[projectId];
        delete state.configsByProjectId[projectId];
      });
  },
});
