# Backend Low-Level Design

## Scope

The backend is a Spring Boot monolith that exposes REST APIs, authenticates users, enforces authorization, manages projects and jobs, schedules Docker execution, streams logs/progress through WebSocket, registers artifacts, sends notifications, and writes audit logs.

## Suggested Package Structure

```text
com.company.trainingtool
  auth
  user
  project
  config
  job
  queue
  runner
  log
  progress
  artifact
  notification
  audit
  storage
  websocket
  common
```

## Main Modules

| Module | Responsibility |
| --- | --- |
| `auth` | Google OIDC login, session/JWT validation, current user context |
| `project` | Project registration, GitHub URL validation, ZIP intake, project metadata |
| `config` | YAML discovery, YAML validation, immutable config snapshots |
| `job` | Training job lifecycle, cancel, retry, history, status transitions |
| `queue` | FIFO queue persistence, dispatch when fewer than 2 jobs are running |
| `runner` | Workspace preparation, Docker image/build/run/stop, stdout/stderr capture |
| `log` | Log sessions, log file writing, server-side log search, download |
| `progress` | Parse structured progress JSON and persist latest progress events |
| `artifact` | Scan configured `artifact_path`, validate files, register artifacts/model versions |
| `notification` | In-app and email notifications for success, failure, and delivery errors |
| `audit` | Append-only audit records for security-sensitive actions |
| `storage` | Path resolution, path traversal protection, local filesystem operations |
| `websocket` | Job stream subscriptions, ownership checks, event fan-out |

## Job Lifecycle Service

`TrainingJobService` owns status transitions. Allowed MVP transitions:

```text
CREATED -> QUEUED
CREATED -> FAILED
QUEUED -> RUNNING
QUEUED -> CANCELLED
RUNNING -> SUCCESS
RUNNING -> FAILED
RUNNING -> CANCELLED
RUNNING -> RETRYING
RETRYING -> QUEUED
FAILED -> CREATED     # new retry job
CANCELLED -> CREATED  # new retry job
```

Every status transition must be transactional and must publish a WebSocket event after commit.

## Scheduler and Queue Processor

Use a scheduled worker, for example every 2 seconds:

1. Count jobs with `status = RUNNING`.
2. If running count is below 2, lock the oldest `WAITING` queue row using `FOR UPDATE SKIP LOCKED`.
3. Mark queue entry `DISPATCHED`.
4. Mark job `RUNNING` and set `started_at`.
5. Submit execution to the runner executor.

The scheduler must be idempotent because the app may restart during dispatch.

## Docker Runner Flow

1. Resolve project source path.
2. Create `/data/workspaces/{job_id}`.
3. Copy source into workspace.
4. Write immutable config snapshot as runtime config file.
5. Validate free disk space is at least 5 GB.
6. Build or prepare Docker image from project dependencies.
7. Run container with workspace mounted read/write only for the job.
8. Capture stdout and stderr continuously.
9. Persist logs and emit WebSocket events.
10. Parse progress JSON events when present.
11. On exit code `0`, mark `SUCCESS` and trigger artifact registration.
12. On non-zero exit, mark `FAILED` and notify user.

## Restart Recovery

On backend startup:

1. Find `RUNNING` jobs.
2. Stop orphan containers if container IDs still exist.
3. Mark jobs `RETRYING`.
4. Create or update queue entries as `WAITING`.
5. Mark jobs `QUEUED`.
6. Let the normal scheduler dispatch them.

Partial execution state is not preserved for MVP.

## Error Handling

Use a common error envelope:

```json
{
  "code": "PROJECT_NOT_FOUND",
  "message": "Project does not exist or is not accessible.",
  "correlationId": "req-20260606-001"
}
```

All unexpected backend errors should include a correlation ID in logs and API responses.
