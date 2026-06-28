import type { ProjectConfigContent, ProjectDetail } from "../../shared/api/types";
import { currentUser } from "./users";
import { iso } from "./time";

export const projects: ProjectDetail[] = [
  { projectId: "p-fraud", projectName: "Fraud Detection", description: "Gradient boosted training pipeline for transaction risk scoring.", sourceType: "GITHUB", buildStatus: "READY", latestJobStatus: "RUNNING", lastTrainingTime: iso(18), lastTrainingOwner: "engineer@co.com", repositoryUrl: "https://github.com/company/fraud-detection", trainingEntrypoint: "python main.py", owner: currentUser, createdAt: iso(9000), updatedAt: iso(18) },
  { projectId: "p-vision", projectName: "Vision Classifier", description: "Image classification experiment with exported model metrics.", sourceType: "ZIP", buildStatus: "READY", latestJobStatus: "SUCCESS", lastTrainingTime: iso(360), lastTrainingOwner: "owner@co.com", trainingEntrypoint: "python main.py", owner: { userId: "u-200", email: "owner@co.com", fullName: "Project Owner" }, createdAt: iso(12000), updatedAt: iso(360) },
  { projectId: "p-churn", projectName: "Churn Forecast", description: "Customer retention model with configurable feature windows.", sourceType: "GITHUB", buildStatus: "READY", latestJobStatus: "FAILED", lastTrainingTime: iso(1440), lastTrainingOwner: "engineer@co.com", repositoryUrl: "https://github.com/company/churn-forecast", trainingEntrypoint: "python main.py", owner: currentUser, createdAt: iso(16000), updatedAt: iso(1440) },
];

export const defaultYaml = `dataset:
  version: 2026.06
training:
  epochs: 24
  batch_size: 64
  learning_rate: 0.0008
runtime:
  accelerator: gpu
  checkpoint_every: 4`;

export const configsByProjectId: Record<string, ProjectConfigContent> = Object.fromEntries(
  projects.map((project) => [project.projectId, { configId: `cfg-${project.projectId}`, configPath: "training.yaml", yamlContent: defaultYaml, contentHash: "sha256:0ac3d8e91" }]),
);
