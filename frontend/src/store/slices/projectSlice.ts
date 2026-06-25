import { createAsyncThunk, createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type { JobStatus, ProjectConfigContent, ProjectDetail, ProjectSummary } from "../../shared/api/types";
import { projectService } from "../../shared/api/services/projects";

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
 * Makes two sequential API calls: POST /projects (creates, returns projectId)
 * then GET /projects/{id} (fetches full detail) so the return value is a usable `ProjectDetail`.
 */
export const createProjectAsync = createAsyncThunk(
  "projects/create",
  async (body: { projectName: string; description?: string; sourceType: "GITHUB" | "ZIP"; repositoryUrl?: string; trainingEntrypoint: string }) => {
    const { repositoryUrl, trainingEntrypoint, projectName, description } = body;
    const { projectId } = await projectService.createGithub({ projectName, description, repositoryUrl: repositoryUrl!, trainingEntrypoint });
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
    activeStatusFilter: "ALL",
    loading: false,
    error: undefined,
  } as ProjectsState,
  reducers: {
    setStatusFilter(state, action: PayloadAction<JobStatus | "ALL">) {
      state.activeStatusFilter = action.payload;
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
      })

      .addCase(fetchProjectConfig.fulfilled, (state, action) => {
        state.configsByProjectId[action.payload.projectId] = action.payload.config;
      })

      .addCase(saveConfigAsync.fulfilled, (state, action) => {
        state.configsByProjectId[action.payload.projectId] = action.payload.config;
      });
  },
});
