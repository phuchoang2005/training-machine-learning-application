import { apiClient } from "../axios-client";
import type { BuildStatus, ProjectDetail, ProjectSummary, ProjectConfigContent } from "../types";

/** Immediate response from project creation; the image build continues in the background. */
type CreateProjectResult = { projectId: string; buildStatus: BuildStatus; buildLog?: string };

/** Cursor-based page envelope used by all list endpoints. */
type ApiPage<T> = { data: T[]; page: { limit: number; nextCursor?: string | null; hasMore: boolean } };

/** Config summary returned by the list endpoint — does not include yamlContent. */
type ConfigSummary = Pick<ProjectConfigContent, "configId" | "configName" | "configPath" | "isDefault" | "updatedAt">;

export const projectService = {
  /**
   * Lists projects visible to the authenticated user (ownership + ADMIN sees all).
   * Returns `ProjectSummary` records — call `get()` for the full detail with entrypoint/owner.
   */
  list: (params?: { query?: string; limit?: number; latestStatus?: string }) =>
    apiClient.get<ApiPage<ProjectSummary>>("/projects", { params }).then((r) => r.data),

  /** Fetches the full project detail including `repositoryUrl`, `trainingEntrypoint`, and `owner`. */
  get: (projectId: string) =>
    apiClient.get<ProjectDetail>(`/projects/${projectId}`).then((r) => r.data),

  /**
   * Permanently deletes a project and all associated data (Docker image, job containers,
   * on-disk source). Owner may delete their own; ADMIN may delete any. Returns 204 No Content.
   */
  delete: (projectId: string) =>
    apiClient.delete<void>(`/projects/${projectId}`).then((r) => r.data),

  /**
   * Registers a public GitHub repository as a project. Returns immediately with the new `projectId`
   * and `buildStatus: "BUILDING"` — the image build runs in the background; poll `get()` until the
   * build reaches `READY`/`FAILED`.
   */
  createGithub: (body: { projectName: string; description?: string; repositoryUrl: string; trainingEntrypoint: string }) =>
    apiClient.post<CreateProjectResult>("/projects", body).then((r) => r.data),

  /**
   * Lists config summaries for a project.
   * Summaries do NOT include `yamlContent` — call `getConfig()` with the selected `configId`.
   */
  listConfigs: (projectId: string) =>
    apiClient.get<{ data: ConfigSummary[] }>(`/projects/${projectId}/configs`).then((r) => r.data),

  /**
   * Fetches the full config content (including `yamlContent`) for a specific config version.
   * The `configId` from this response must be passed to `jobService.start()`.
   */
  getConfig: (projectId: string, configId: string) =>
    apiClient.get<ProjectConfigContent>(`/projects/${projectId}/configs/${configId}`).then((r) => r.data),

  /**
   * Persists an updated YAML to the config record.
   * Note: starting a job does NOT require saving first — `jobService.start()` accepts
   * `yamlContent` directly and creates an immutable snapshot regardless.
   */
  saveConfig: (projectId: string, configId: string, yamlContent: string) =>
    apiClient.put<ProjectConfigContent>(`/projects/${projectId}/configs/${configId}`, { yamlContent }).then((r) => r.data),

  /**
   * Uploads a ZIP archive and registers it as a new project.
   * Sends multipart/form-data with a JSON `metadata` part and the `file` part.
   * Returns immediately with `buildStatus: "BUILDING"`; poll `get()` for the terminal build state.
   */
  createZip: (body: { projectName: string; description?: string; trainingEntrypoint: string }, file: File) => {
    const form = new FormData();
    form.append("metadata", new Blob([JSON.stringify(body)], { type: "application/json" }));
    form.append("file", file);
    return apiClient.post<CreateProjectResult>("/projects/upload-zip", form).then((r) => r.data);
  },

  /**
   * Server-side YAML validation.
   * Returns `{ valid: true }` or `{ valid: false, errors: [...] }`.
   * Does not persist anything.
   */
  validateConfig: (projectId: string, yamlContent: string) =>
    apiClient.post<{ valid: boolean; normalizedPreview?: object; errors?: string[] }>(`/projects/${projectId}/configs/validate`, { yamlContent }).then((r) => r.data),
};
