# Physical Schema Design

## Database Baseline

Target database: MongoDB 8 (accessed via Spring Data MongoDB / `MongoTemplate`).

Conventions:

* `_id` holds a `UUID` (standard BSON UUID representation) generated in Java; it is also the value exposed as the resource id in the API.
* Timestamps are stored as BSON `Date` (mapped from `java.time.Instant`).
* Enums (`role`, `status`, `sourceType`, `status`, `streamType`, `artifactType`) are stored as their string names.
* Relationships are by reference (the referenced document's id), not embedding. There are no database-level foreign keys; cascading deletes are performed by the backend (`ProjectRepository.delete`).
* Backend-mediated access control only; no direct client database access.

There is no SQL DDL and no migration tool. Collections are created lazily on first write. A startup seeder (`MongoSeedConfig`) creates the baseline indexes below and seeds the two development users idempotently. `MongoConfig` pins the driver to the **standard** UUID representation so `UUID` ids encode/decode consistently.

## Enumerations (stored as strings)

| Field | Values |
| --- | --- |
| `users.role` | `USER`, `ADMIN` |
| `users.status` | `ACTIVE`, `DISABLED` |
| `projects.sourceType` | `GITHUB`, `ZIP` |
| `training_jobs.status` | `CREATED`, `QUEUED`, `RUNNING`, `SUCCESS`, `FAILED`, `CANCELLED`, `RETRYING` |
| `job_queue_entries.queueStatus` | `WAITING`, `DISPATCHED`, `CANCELLED` |
| `job_log_events.streamType` | `STDOUT`, `STDERR` |
| `artifacts.artifactType` | `MODEL`, `CHECKPOINT`, `METRIC`, `OTHER` |

## Collections

### Identity and Ownership

* `users` — mapped by the `User` record. Fields: `_id`, `email`, `fullName`, `role`, `status`, `createdAt`, `lastLoginAt`.
* `projects` — mapped by the `Project` record. Fields: `_id`, `ownerUserId`, `projectName`, `description`, `sourceType`, `repositoryUrl`, `localSourcePath`, `trainingEntrypoint`, `status`, `createdAt`, `updatedAt`.

### Configuration

* `project_configs` — mapped by the `ProjectConfig` record. Fields: `_id`, `projectId`, `configName`, `configPath`, `yamlContent`, `defaultConfig`, `updatedAt`.
* `config_snapshots` — internal document. Fields: `snapshotId`, `projectId`, `configId`, `yamlContent`, `contentHash`, `createdAt`. Immutable runtime YAML snapshots for reproducibility.

### Job and Queue

* `training_jobs` — mapped by the `TrainingJob` record. Fields: `_id`, `projectId`, `triggeredByUserId`, `configSnapshotId`, `retryOfJobId`, `status`, `retryAttempt`, `queuePosition`, `queuedAt`, `startedAt`, `endedAt`, `failureReason`, `createdAt`.
* `job_queue_entries` — internal document. Fields: `queueEntryId`, `jobId`, `queueStatus`, `enqueuedAt`, `dispatchedAt`. Persistent FIFO queue; `queuePosition` on `training_jobs` is recomputed (`JobQueueRepository.refreshPositions`) by ranking `WAITING` entries by `enqueuedAt`.

### Logs and Progress

* `job_log_events` — internal document. Fields: `logEventId`, `jobId`, `streamType`, `message`, `emittedAt`, `sequenceNo`.
* `job_progress_events` — internal document. Fields: `jobId`, `progressValue` (0–100), `epoch`, `totalEpoch`, `emittedAt`.

### Artifacts

* `artifacts` — internal document. Fields: `artifactId`, `projectId`, `jobId`, `artifactName`, `artifactType`, `filePath`, `fileSizeBytes`, `checksum`, `createdAt`.

### Audit

* `audit_logs` — internal document. Fields: `auditId`, `actorUserId`, `projectId`, `jobId`, `action`, `resourceType`, `resourceId`, `createdAt`. Append-only.

## Indexes

Created at startup by `MongoSeedConfig`:

* `users` — unique on `email`.
* `project_configs` — unique compound on `(projectId, configPath)`.

Recommended additional indexes for scale (not created automatically in the MVP): `training_jobs (projectId, createdAt desc)`, `training_jobs.status`, `job_queue_entries (queueStatus, enqueuedAt)`, `job_log_events (jobId, sequenceNo)`, `job_progress_events (jobId, emittedAt desc)`, `artifacts.jobId`, `audit_logs (actorUserId, createdAt desc)`.

## Delete and Retention Rules

| Collection | Retention |
| --- | --- |
| `users` | Disable instead of delete for active history |
| `projects` | Deleted only through the backend service, which cascades to the project's configs, snapshots, jobs, queue entries, logs, progress, and artifacts |
| `training_jobs` | Removed when the parent project is deleted; otherwise retained |
| `config_snapshots` | Immutable; retained with job history (removed on project delete) |
| `job_log_events` / `job_progress_events` | Removed on project/job delete |
| `artifacts` | Removed on project delete |
| `audit_logs` | Append-only and retained indefinitely |
