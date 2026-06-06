# Security and Operations Low-Level Design

## Authentication

Use Google Workspace OpenID Connect through Spring Security. The backend stores or maps authenticated users into the `users` table by email. Disabled users cannot access APIs or WebSocket streams.

## Authorization Rules

Use RBAC plus ownership checks.

| Action | User | Admin |
| --- | --- | --- |
| View own projects | Allowed | Project name/owner only |
| Start own training job | Allowed | Not allowed unless owner |
| Cancel own job | Allowed | Allowed for operational control |
| Retry own job | Allowed | Not allowed unless owner |
| View own logs | Allowed | Not allowed unless owner |
| Download own artifacts | Allowed | Not allowed unless owner |
| Delete project | Owner allowed | Allowed |
| View audit logs | Own actions only | Allowed for platform audit |

Every REST endpoint and WebSocket subscription must call the same authorization service. UI hiding is not considered security.

## WebSocket Security

* Authenticate the handshake.
* Authorize the requested `jobId` before accepting subscription.
* Re-check authorization on reconnect.
* Do not allow subscribing to wildcard project or all-job streams for normal users.
* Include no secrets in WebSocket URLs.
* Close unauthorized streams with a structured `ERROR` event when possible.

## Storage Security

Use a single storage root, for example `/data`.

```text
/data/sources/{project_id}
/data/uploads/{project_id}
/data/workspaces/{job_id}
/data/logs/{project_id}/{job_id}
/data/artifacts/{project_id}/{job_id}
```

Rules:

* Resolve all paths through `StorageService`.
* Reject `..`, absolute user-supplied paths, symlinks escaping storage root, and hidden control files.
* Validate ZIP entries before extraction to prevent Zip Slip.
* Validate artifact paths against the configured job workspace.
* Generate download responses through authorized backend endpoints only.

## Audit Logging

Create audit records for:

* Login
* Project registration and deletion
* Training start, cancel, retry
* Config snapshot creation
* Job status transition
* Artifact registration and download
* Authorization denial for sensitive resources

Audit records should include actor, project, job, action, resource type, resource ID, timestamp, and metadata JSON.

## Operational Workers

| Worker | Trigger | Responsibility |
| --- | --- | --- |
| Queue Scheduler | Every 2 seconds | Dispatch waiting jobs while running count is below 2 |
| Recovery Worker | Application startup | Requeue interrupted running jobs |
| Log Collector | During container execution | Persist stdout/stderr and publish WebSocket events |
| Artifact Scanner | After job success | Register artifacts and model version |
| Notification Worker | Async after terminal status | Send email and update notification status |
| Cleanup Worker | Manual or scheduled later | Remove deleted project files when retention rules allow |

## Failure Handling

| Failure | Handling |
| --- | --- |
| GitHub clone failed | Do not start container; mark job failed or reject registration |
| Invalid ZIP | Reject upload and do not create project |
| Disk below 5 GB | Mark job failed before Docker launch |
| Docker start failed | Mark job failed and persist runner error |
| Training exits non-zero | Mark job failed and send failure email |
| Server restarts | Requeue interrupted running jobs |
| Artifact registration fails | Keep training success; notify user of artifact failure |
| Email fails | Keep job status unchanged; show in-app notification |

## Observability

Use structured application logs with correlation IDs. Recommended metrics:

* Active users
* Jobs by status
* Queue length
* Running job count
* Job duration
* Docker start failures
* WebSocket active connections
* Email delivery failures
* Artifact registration failures

Expose Spring Boot Actuator health endpoints for application, database, storage, and Docker availability.
