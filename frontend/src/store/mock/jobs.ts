import type { Artifact, JobDetail, LogEvent, QueueSnapshot } from "../../shared/api/types";
import { currentUser } from "./users";
import { iso } from "./time";

export const jobs: JobDetail[] = [
  { jobId: "j-10245", projectId: "p-fraud", projectName: "Fraud Detection", triggeredBy: currentUser, status: "RUNNING", queuePosition: 0, progress: { available: true, value: 58, epoch: 14, totalEpoch: 24, updatedAt: iso(1) }, retryAttempt: 0, createdAt: iso(28), queuedAt: iso(28), startedAt: iso(24) },
  { jobId: "j-10190", projectId: "p-vision", projectName: "Vision Classifier", triggeredBy: { userId: "u-200", email: "owner@co.com", fullName: "Project Owner" }, status: "SUCCESS", progress: { available: true, value: 100, epoch: 30, totalEpoch: 30, updatedAt: iso(350) }, retryAttempt: 0, createdAt: iso(420), queuedAt: iso(420), startedAt: iso(410), endedAt: iso(360) },
  { jobId: "j-10003", projectId: "p-churn", projectName: "Churn Forecast", triggeredBy: currentUser, status: "FAILED", progress: { available: true, value: 42, epoch: 5, totalEpoch: 12, updatedAt: iso(1430) }, retryAttempt: 1, createdAt: iso(1510), queuedAt: iso(1510), startedAt: iso(1490), endedAt: iso(1440), failureReason: "Validation split produced empty minority class." },
];

export const logsByJobId: Record<string, LogEvent[]> = {
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

export const artifactsByJobId: Record<string, Artifact[]> = {
  "j-10190": [
    { artifactId: "a-1", artifactName: "model.onnx", artifactType: "MODEL", fileSizeBytes: 83123302, checksum: "sha256:cafebabe", createdAt: iso(360) },
    { artifactId: "a-2", artifactName: "metrics.json", artifactType: "METRIC", fileSizeBytes: 12344, checksum: "sha256:91ab", createdAt: iso(360) },
  ],
};

export const queue: QueueSnapshot = {
  runningCount: 1,
  runningLimit: 1,
  queuedCount: 2,
  items: [
    { jobId: "j-10245", projectName: "Fraud Detection", status: "RUNNING", queuePosition: 0, enqueuedAt: iso(28) },
    { jobId: "j-10310", projectName: "Demand Forecast", status: "QUEUED", queuePosition: 1, enqueuedAt: iso(8) },
    { jobId: "j-10311", projectName: "Claims Routing", status: "QUEUED", queuePosition: 2, enqueuedAt: iso(4) },
  ],
};
