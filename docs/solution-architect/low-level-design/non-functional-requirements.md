# Detailed Non-Functional Requirements

## 1. Purpose and Scope

This document defines low-level non-functional requirements for the AI Training Management Platform MVP. It covers runtime quality attributes for the React frontend, Spring Boot backend, PostgreSQL database, Docker-based training execution, WebSocket monitoring, local storage, notification delivery, and operational support.

## 2. Capacity and Scalability

| Requirement ID | Requirement |
| --- | --- |
| NFR-CAP-001 | The MVP shall support a maximum of 7 concurrent active users. |
| NFR-CAP-002 | The platform shall allow a maximum of 2 concurrent `RUNNING` training jobs. |
| NFR-CAP-003 | The queue shall support at least 50 queued jobs. |
| NFR-CAP-004 | Jobs beyond execution capacity shall be placed into a persistent FIFO queue. |
| NFR-CAP-005 | The system shall reject or delay new activity with a clear platform busy message when active user or queue capacity is exceeded. |

Future scalability targets include multi-worker execution, Kubernetes orchestration, GPU scheduling, message broker queueing, and object storage for artifacts/logs.

## 3. Performance

| Requirement ID | Requirement |
| --- | --- |
| NFR-PERF-001 | Project dashboard initial load shall complete within 3 seconds for normal MVP data volume. |
| NFR-PERF-002 | Job detail status refresh through WebSocket shall normally reach the browser within 5 seconds. |
| NFR-PERF-003 | Log streaming latency from Docker stdout/stderr to browser shall be 5 seconds or less under normal load. |
| NFR-PERF-004 | Server-side log search shall return the first page within 5 seconds for a single job log session. |
| NFR-PERF-005 | Artifact download shall stream through the backend without loading the full artifact into application memory. |
| NFR-PERF-006 | Queue dispatch checks shall run at least every 2 seconds while the backend is healthy. |

## 4. Reliability and Recovery

| Requirement ID | Requirement |
| --- | --- |
| NFR-REL-001 | Browser closure shall not stop a running training job. |
| NFR-REL-002 | User logout shall not stop a running training job. |
| NFR-REL-003 | Queued jobs shall survive backend restart, server restart, and application redeployment. |
| NFR-REL-004 | Interrupted `RUNNING` jobs shall be marked `RETRYING`, requeued, and rerun from the beginning. |
| NFR-REL-005 | Job status transitions shall be persisted transactionally before WebSocket events are published. |
| NFR-REL-006 | Email delivery failure shall not change training job terminal status. |
| NFR-REL-007 | Artifact registration failure shall not change a successful training job to failed. |

## 5. Availability

| Requirement ID | Requirement |
| --- | --- |
| NFR-AVL-001 | MVP target availability during business hours should be at least 99.0%, excluding planned maintenance. |
| NFR-AVL-002 | Planned maintenance should be announced before backend restart when running jobs may be requeued. |
| NFR-AVL-003 | Health checks shall cover backend, PostgreSQL, local storage, Docker Engine, and email integration. |
| NFR-AVL-004 | The frontend shall show degraded states when WebSocket, Docker, storage, or email services are unavailable. |

## 6. Security

| Requirement ID | Requirement |
| --- | --- |
| NFR-SEC-001 | Authentication shall use Google Workspace OpenID Connect or OAuth 2.0. |
| NFR-SEC-002 | Authorization shall combine RBAC with project ownership checks. |
| NFR-SEC-003 | Backend authorization shall be enforced for every REST endpoint and WebSocket subscription. |
| NFR-SEC-004 | Users shall access only their own projects, jobs, logs, artifacts, notifications, and detailed audit records. |
| NFR-SEC-005 | Administrators may cancel running jobs and delete projects but shall not inspect project source, detailed logs, business data, or artifacts unless also project owner. |
| NFR-SEC-006 | ZIP extraction shall reject path traversal, absolute paths, and unsafe symlinks. |
| NFR-SEC-007 | Artifact and log downloads shall be served only through authorized backend endpoints. |
| NFR-SEC-008 | Secrets, tokens, local filesystem paths, and infrastructure credentials shall not be exposed in API responses or WebSocket payloads. |

## 7. Data Integrity and Consistency

| Requirement ID | Requirement |
| --- | --- |
| NFR-DATA-001 | Training configuration snapshots shall be immutable once a job is created. |
| NFR-DATA-002 | Job queue entries and job status changes shall be persisted in PostgreSQL. |
| NFR-DATA-003 | The scheduler shall claim queued jobs using database locking to prevent duplicate dispatch. |
| NFR-DATA-004 | Retry shall always create a new job ID and preserve the original job relationship. |
| NFR-DATA-005 | Audit records shall be append-only from application code. |
| NFR-DATA-006 | Model versions shall remain linked to project, job, artifact, and config snapshot. |

## 8. Observability

| Requirement ID | Requirement |
| --- | --- |
| NFR-OBS-001 | Backend logs shall use structured logging with correlation IDs. |
| NFR-OBS-002 | API error responses shall include a correlation ID. |
| NFR-OBS-003 | Metrics should include active users, queue length, running job count, job duration, Docker failures, WebSocket connections, artifact failures, and email failures. |
| NFR-OBS-004 | Training logs shall be retained until explicitly deleted by the user. |
| NFR-OBS-005 | Audit logs shall be retained indefinitely unless formal deletion is approved. |
| NFR-OBS-006 | Spring Boot health endpoints shall report application, database, storage, and Docker readiness. |

## 9. Maintainability

| Requirement ID | Requirement |
| --- | --- |
| NFR-MAINT-001 | Backend modules shall remain separated by responsibility: auth, project, config, job, queue, runner, log, progress, artifact, notification, audit, storage, and websocket. |
| NFR-MAINT-002 | REST DTOs shall align with the OpenAPI contract. |
| NFR-MAINT-003 | Database changes shall be introduced through versioned migrations. |
| NFR-MAINT-004 | PlantUML, Mermaid, Markdown, and OpenAPI docs shall be treated as docs-as-code artifacts. |
| NFR-MAINT-005 | Accepted ADRs shall not be rewritten; superseding decisions require a new ADR entry. |

## 10. Testability

| Requirement ID | Requirement |
| --- | --- |
| NFR-TEST-001 | Unit tests shall cover job status transitions, authorization decisions, YAML validation, path validation, ZIP validation, and progress parsing. |
| NFR-TEST-002 | Integration tests shall cover PostgreSQL repositories, queue claim locking, migrations, and idempotency keys. |
| NFR-TEST-003 | Contract tests should validate API behavior against the OpenAPI specification. |
| NFR-TEST-004 | WebSocket tests shall cover authorization, reconnect, resume, duplicate event handling, and fallback behavior. |
| NFR-TEST-005 | End-to-end tests should cover project registration, job start, live monitoring, cancel, retry, and artifact download. |

## 11. Usability and Accessibility

| Requirement ID | Requirement |
| --- | --- |
| NFR-UX-001 | Job statuses shall be clearly visible and not rely on color alone. |
| NFR-UX-002 | Progress shall show `Progress Information Not Available` when the training code emits no progress events. |
| NFR-UX-003 | Long log output shall support scrolling, search, and download. |
| NFR-UX-004 | Destructive actions such as cancel and delete shall require confirmation. |
| NFR-UX-005 | WebSocket disconnect and reconnect states shall be visible on the Training Detail page. |

## 12. Storage and Backup

| Requirement ID | Requirement |
| --- | --- |
| NFR-STO-001 | Local storage shall use stable paths under `/data` for sources, uploads, workspaces, logs, artifacts, and config snapshots. |
| NFR-STO-002 | The runner shall validate at least 5 GB free disk space before starting a training job. |
| NFR-STO-003 | Artifacts shall be copied from workspace/container output to platform-managed artifact storage before registration. |
| NFR-STO-004 | PostgreSQL and local file storage backups shall be restorable from a consistent point in time. |
| NFR-STO-005 | Database-only restore is insufficient because logs and artifacts are stored on the filesystem. |

## 13. Compatibility and Technology Constraints

| Requirement ID | Requirement |
| --- | --- |
| NFR-COMP-001 | Backend implementation shall target Spring Boot and Java according to the ADR baseline. |
| NFR-COMP-002 | Frontend implementation shall target ReactJS, TypeScript, Vite, Redux, Axios, TailwindCSS, Radix UI, and shadcn/ui according to the ADR baseline. |
| NFR-COMP-003 | The MVP shall use PostgreSQL as the primary database. |
| NFR-COMP-004 | Training jobs shall run inside Docker containers, not directly on the host OS. |
| NFR-COMP-005 | WebSocket shall be the primary real-time mechanism, with REST polling as fallback. |

## 14. Acceptance Checklist

The NFRs are considered implemented for MVP when:

* A running job survives browser closure and user logout.
* Queued jobs survive backend restart.
* Interrupted running jobs are requeued and rerun.
* Dashboard and job monitoring meet the defined latency targets under MVP load.
* Project ownership is enforced for REST and WebSocket access.
* Logs, progress, artifacts, notifications, and audit records are persisted.
* PostgreSQL migrations, OpenAPI contract, and docs-as-code diagrams are versioned with the repository.
