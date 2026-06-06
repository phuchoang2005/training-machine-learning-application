# Database Low-Level Design

## Database

Use PostgreSQL 18. All primary keys are UUIDs. Use `timestamptz` for timestamps. Store immutable audit and execution records; avoid hard deletion for business-critical history unless an explicit delete requirement applies.

## Enums

Suggested PostgreSQL enums:

```sql
CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'DISABLED');
CREATE TYPE source_type AS ENUM ('GITHUB', 'ZIP');
CREATE TYPE job_status AS ENUM ('CREATED', 'QUEUED', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELLED', 'RETRYING');
CREATE TYPE queue_status AS ENUM ('WAITING', 'DISPATCHED', 'CANCELLED');
CREATE TYPE stream_type AS ENUM ('STDOUT', 'STDERR');
CREATE TYPE artifact_type AS ENUM ('MODEL', 'CHECKPOINT', 'METRIC', 'OTHER');
CREATE TYPE notification_channel AS ENUM ('IN_APP', 'EMAIL');
CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'FAILED', 'READ');
```

## Tables

The logical tables follow the HLD ERD:

* `users`
* `projects`
* `project_configs`
* `config_snapshots`
* `training_jobs`
* `job_queue_entries`
* `job_log_sessions`
* `job_log_events`
* `job_progress_events`
* `artifacts`
* `model_versions`
* `notifications`
* `audit_logs`

## Required Constraints

| Table | Constraint |
| --- | --- |
| `users` | `email` unique and non-null |
| `projects` | `owner_user_id` references `users(user_id)` |
| `project_configs` | `(project_id, config_path)` unique |
| `config_snapshots` | `content_hash` non-null |
| `training_jobs` | `retry_of_job_id` nullable self-reference |
| `job_queue_entries` | one active `WAITING` or `DISPATCHED` row per job |
| `job_log_events` | `(log_session_id, sequence_no)` unique |
| `artifacts` | `file_path` non-null and normalized |
| `model_versions` | `(project_id, version_number)` unique |
| `audit_logs` | append-only; no update path in application code |

## Indexes

```sql
CREATE INDEX idx_projects_owner ON projects(owner_user_id);
CREATE INDEX idx_jobs_project_created ON training_jobs(project_id, created_at DESC);
CREATE INDEX idx_jobs_status ON training_jobs(status);
CREATE INDEX idx_queue_waiting ON job_queue_entries(queue_status, enqueued_at);
CREATE INDEX idx_log_events_session_seq ON job_log_events(log_session_id, sequence_no);
CREATE INDEX idx_progress_job_time ON job_progress_events(job_id, emitted_at DESC);
CREATE INDEX idx_artifacts_job ON artifacts(job_id);
CREATE INDEX idx_notifications_user_status ON notifications(user_id, status, created_at DESC);
CREATE INDEX idx_audit_project_time ON audit_logs(project_id, created_at DESC);
```

## Transaction Rules

### Start Job

In one transaction:

1. Validate project ownership.
2. Insert `config_snapshots`.
3. Insert `training_jobs` with `CREATED`.
4. Insert `job_queue_entries` with `WAITING`.
5. Update job to `QUEUED`.
6. Insert `audit_logs` record.

### Dispatch Job

In one transaction:

1. Lock the oldest waiting queue row:

```sql
SELECT *
FROM job_queue_entries
WHERE queue_status = 'WAITING'
ORDER BY enqueued_at
FOR UPDATE SKIP LOCKED
LIMIT 1;
```

2. Mark queue entry `DISPATCHED`.
3. Mark job `RUNNING`.
4. Set `started_at`.

### Complete Job

In one transaction:

1. Mark job terminal status.
2. Set `ended_at`.
3. Persist final progress if available.
4. Insert notification request.
5. Insert audit record.

Artifact registration can run in a separate transaction after `SUCCESS`.

## Retention

* Audit logs: retained indefinitely until explicitly deleted.
* Training logs: retained until user deletion.
* Artifacts: retained until user deletion.
* Config snapshots: immutable and retained with job history.
