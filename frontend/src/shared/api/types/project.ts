import type { JobStatus } from "./job";
import type { UserSummary } from "./user";

export type SourceType = "GITHUB" | "ZIP";

/**
 * State of a project's per-project Docker image build. The image is built off the request thread
 * at registration, so a project is `BUILDING` until it finalizes; jobs may only be started once
 * it is `READY`. `FAILED` projects keep their record so the build log can be inspected.
 */
export type BuildStatus = "BUILDING" | "READY" | "FAILED";

/**
 * Lightweight project record returned in paginated list responses.
 * `latestJobStatus` is null/undefined for projects that have never been trained.
 */
export type ProjectSummary = {
  projectId: string;
  projectName: string;
  description: string;
  sourceType: SourceType;
  /** Image-build state; polled by the dashboard until it leaves `BUILDING`. */
  buildStatus?: BuildStatus;
  /** Null/undefined when the project has never had a training job. */
  latestJobStatus?: JobStatus | null;
  lastTrainingTime?: string;
  /** Email of the user who triggered the last training run. */
  lastTrainingOwner?: string;
};

/** Full project record returned by GET /projects/{projectId}. */
export type ProjectDetail = ProjectSummary & {
  /** Absent for ZIP-uploaded projects. */
  repositoryUrl?: string;
  /** Shell command the runner executes, e.g. `python train.py --config training.yaml`. */
  trainingEntrypoint: string;
  /** The user who registered the project; controls ownership-based access. */
  owner: UserSummary;
  createdAt: string;
  updatedAt: string;
  /**
   * Combined `docker build` log from baking the per-project image at registration. Persisted with
   * the project and returned by GET; populated once the build reaches `READY` or `FAILED`.
   */
  buildLog?: string;
};

/**
 * Active YAML configuration for a project.
 * `configId` must be passed to POST /projects/{id}/jobs alongside `yamlContent`
 * so the backend can link the job to its config template.
 * `isDefault` marks the configuration that is selected when starting a job.
 */
export type ProjectConfigContent = {
  configId: string;
  configName?: string;
  configPath: string;
  /** True for the configuration that gets used when starting a job. */
  isDefault?: boolean;
  /** ISO-8601 timestamp of the last save. */
  updatedAt?: string;
  /** Raw YAML that the runner receives as its config file. */
  yamlContent: string;
  /** SHA-256 of the saved YAML, used for cache-busting and integrity checks. */
  contentHash: string;
};
