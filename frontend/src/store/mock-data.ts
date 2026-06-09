import type { Artifact, AuditLog, CurrentUser, JobDetail, LogEvent, Notification, ProjectConfigContent, ProjectDetail, QueueSnapshot } from "../shared/api/types";

const now = new Date();
const iso = (minutesAgo: number) => new Date(now.getTime() - minutesAgo * 60_000).toISOString();

const currentUser: CurrentUser = {
  userId: "u-100",
  email: "user@example.com",
  fullName: "Development User",
  role: "USER",
  status: "ACTIVE",
  lastLoginAt: iso(30),
};

const projects: ProjectDetail[] = [
  {
    projectId: "p-fraud",
    projectName: "Fraud Detection",
    description: "Gradient boosted training pipeline for transaction risk scoring.",
    sourceType: "GITHUB",
    latestJobStatus: "RUNNING",
    lastTrainingTime: iso(18),
    lastTrainingOwner: "engineer@co.com",
    repositoryUrl: "https://github.com/company/fraud-detection",
    trainingEntrypoint: "python train.py --config training.yaml",
    owner: currentUser,
    createdAt: iso(9000),
    updatedAt: iso(18),
  },
  {
    projectId: "p-vision",
    projectName: "Vision Classifier",
    description: "Image classification experiment with exported model metrics.",
    sourceType: "ZIP",
    latestJobStatus: "SUCCESS",
    lastTrainingTime: iso(360),
    lastTrainingOwner: "owner@co.com",
    trainingEntrypoint: "python src/train.py",
    owner: { userId: "u-200", email: "owner@co.com", fullName: "Project Owner" },
    createdAt: iso(12000),
    updatedAt: iso(360),
  },
  {
    projectId: "p-churn",
    projectName: "Churn Forecast",
    description: "Customer retention model with configurable feature windows.",
    sourceType: "GITHUB",
    latestJobStatus: "FAILED",
    lastTrainingTime: iso(1440),
    lastTrainingOwner: "engineer@co.com",
    repositoryUrl: "https://github.com/company/churn-forecast",
    trainingEntrypoint: "python train.py",
    owner: currentUser,
    createdAt: iso(16000),
    updatedAt: iso(1440),
  },
];

const defaultYaml = `dataset:
  version: 2026.06
training:
  epochs: 24
  batch_size: 64
  learning_rate: 0.0008
runtime:
  accelerator: gpu
  checkpoint_every: 4`;

const configsByProjectId: Record<string, ProjectConfigContent> = Object.fromEntries(
  projects.map((project) => [
    project.projectId,
    {
      configId: `cfg-${project.projectId}`,
      configPath: "training.yaml",
      yamlContent: defaultYaml,
      contentHash: "sha256:0ac3d8e91",
    },
  ]),
);

const jobs: JobDetail[] = [
  {
    jobId: "j-10245",
    projectId: "p-fraud",
    projectName: "Fraud Detection",
    triggeredBy: currentUser,
    status: "RUNNING",
    queuePosition: 0,
    progress: { available: true, value: 58, epoch: 14, totalEpoch: 24, updatedAt: iso(1) },
    retryAttempt: 0,
    createdAt: iso(28),
    queuedAt: iso(28),
    startedAt: iso(24),
  },
  {
    jobId: "j-10190",
    projectId: "p-vision",
    projectName: "Vision Classifier",
    triggeredBy: { userId: "u-200", email: "owner@co.com", fullName: "Project Owner" },
    status: "SUCCESS",
    progress: { available: true, value: 100, epoch: 30, totalEpoch: 30, updatedAt: iso(350) },
    retryAttempt: 0,
    createdAt: iso(420),
    queuedAt: iso(420),
    startedAt: iso(410),
    endedAt: iso(360),
  },
  {
    jobId: "j-10003",
    projectId: "p-churn",
    projectName: "Churn Forecast",
    triggeredBy: currentUser,
    status: "FAILED",
    progress: { available: true, value: 42, epoch: 5, totalEpoch: 12, updatedAt: iso(1430) },
    retryAttempt: 1,
    createdAt: iso(1510),
    queuedAt: iso(1510),
    startedAt: iso(1490),
    endedAt: iso(1440),
    failureReason: "Validation split produced empty minority class.",
  },
];

const logsByJobId: Record<string, LogEvent[]> = {
  "j-10245": [
    { logEventId: "l-1", sequenceNo: 1, streamType: "STDOUT", message: "loading transaction dataset version 2026.06", emittedAt: iso(24) },
    { logEventId: "l-2", sequenceNo: 2, streamType: "STDOUT", message: "epoch 14/24 auc=0.944 loss=0.183", emittedAt: iso(1) },
    { logEventId: "l-3", sequenceNo: 3, streamType: "STDOUT", message: "checkpoint written to artifact staging area", emittedAt: iso(1) },
  ],
  "j-10003": [
    { logEventId: "l-4", sequenceNo: 1, streamType: "STDOUT", message: "building feature matrix", emittedAt: iso(1490) },
    { logEventId: "l-5", sequenceNo: 2, streamType: "STDERR", message: "validation failed: empty minority class in holdout split", emittedAt: iso(1440) },
  ],
};

const artifactsByJobId: Record<string, Artifact[]> = {
  "j-10190": [
    { artifactId: "a-1", artifactName: "model.onnx", artifactType: "MODEL", fileSizeBytes: 83123302, checksum: "sha256:cafebabe", createdAt: iso(360) },
    { artifactId: "a-2", artifactName: "metrics.json", artifactType: "METRICS", fileSizeBytes: 12344, checksum: "sha256:91ab", createdAt: iso(360) },
  ],
};

const notifications: Notification[] = [
  { notificationId: "n-1", jobId: "j-10245", type: "JOB_RUNNING", channel: "IN_APP", status: "UNREAD", message: "Fraud Detection is training at epoch 14 of 24.", createdAt: iso(1) },
  { notificationId: "n-2", jobId: "j-10190", type: "JOB_SUCCESS", channel: "IN_APP", status: "READ", message: "Vision Classifier finished successfully. Artifacts are ready.", createdAt: iso(360) },
  { notificationId: "n-3", jobId: "j-10003", type: "JOB_FAILED", channel: "EMAIL", status: "DELIVERY_FAILED", message: "Churn Forecast failed and email delivery needs review.", createdAt: iso(1440) },
];

const queue: QueueSnapshot = {
  runningCount: 1,
  runningLimit: 1,
  queuedCount: 2,
  items: [
    { jobId: "j-10245", projectName: "Fraud Detection", status: "RUNNING", queuePosition: 0, enqueuedAt: iso(28) },
    { jobId: "j-10310", projectName: "Demand Forecast", status: "QUEUED", queuePosition: 1, enqueuedAt: iso(8) },
    { jobId: "j-10311", projectName: "Claims Routing", status: "QUEUED", queuePosition: 2, enqueuedAt: iso(4) },
  ],
};

const users: CurrentUser[] = [
  currentUser,
  { userId: "u-admin", email: "admin@example.com", fullName: "Development Admin", role: "ADMIN", status: "ACTIVE", lastLoginAt: iso(5) },
  { userId: "u-200", email: "owner@co.com", fullName: "Project Owner", role: "USER", status: "ACTIVE", lastLoginAt: iso(220) },
  { userId: "u-300", email: "disabled@co.com", fullName: "Disabled User", role: "USER", status: "DISABLED", lastLoginAt: iso(22000) },
];

const audit: AuditLog[] = [
  { auditId: "au-1", actor: users[1], action: "USER_STATUS_UPDATED", resourceType: "USER", resourceId: "u-300", createdAt: iso(12) },
  { auditId: "au-2", actor: currentUser, projectId: "p-fraud", jobId: "j-10245", action: "JOB_STARTED", resourceType: "JOB", resourceId: "j-10245", createdAt: iso(28) },
];

export const mockState = {
  currentUser,
  projects,
  configsByProjectId,
  jobs,
  logsByJobId,
  artifactsByJobId,
  notifications,
  queue,
  users,
  audit,
  defaultYaml,
};
