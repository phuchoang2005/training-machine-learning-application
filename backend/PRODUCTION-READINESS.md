# Backend — Production Readiness Roadmap

> Derived strictly from `docs/` (the single source of truth): `po-requirement.md`, `ba-refine.md`,
> `solution-architect/low-level-design/non-functional-requirements.md` (NFR-*), the LLD README, and the
> OpenAPI contract under `low-level-design/api-contracts/`.
>
> **Current reality (verified in code):** the API persists users/projects/configs/jobs, enforces
> RBAC + ownership via `AuthorizationService`, writes audit rows, snapshots configs, and enqueues jobs.
> But **a started job never runs** — there is no dispatcher, no Docker runner, no log/progress capture,
> no notifications, no recovery, and no real auth. Jobs sit in `QUEUED` forever. This file lists what
> the docs require to close that gap, in priority order. Frontend has a parallel file
> (`frontend/PRODUCTION-READINESS.md`); the **OpenAPI contract + WebSocket envelope + dev-auth scheme**
> are the coordination boundary between the two — change them deliberately and in sync.

---

## P0 — Execution engine (without this there is no product)

### B1. Queue dispatcher / scheduler
- **Target:** NFR-PERF-006 (dispatch check ≥ every 2s), NFR-CAP-002 (max 2 `RUNNING`), NFR-CAP-003/004
  (≥50 FIFO, persistent), NFR-DATA-003 (claim with DB locking, no duplicate dispatch).
- **Now:** none. `@EnableScheduling`/`@Scheduled` absent; `JobService.start` only `queue.enqueue(...)`.
- **Do:** add a scheduled dispatcher that, while `runningCount < app.queue.running-limit`, atomically claims
  the oldest `WAITING` entry (`findAndModify` WAITING→DISPATCHED as the lock), transitions the job
  `QUEUED→RUNNING`, and hands it to the runner. Call `JobQueueRepository.refreshPositions()` after each
  claim/cancel. Persist status **before** publishing any WS event (NFR-REL-005).

### B2. Docker training runner
- **Target:** NFR-COMP-004 (jobs run in Docker, not on host), BA §10 workflow, NFR-STO-001 (`/data` paths),
  NFR-STO-002 (validate ≥5 GB free before start), NFR-DATA-001 (immutable config snapshot per run).
- **Now:** none. No `ProcessBuilder`/Docker client; no `runner` module.
- **Do:** add a `runner` module that materializes the workspace under `/data/{sources,workspaces,...}`,
  writes the config snapshot into it, checks disk space, launches the training container, and tracks the
  container lifecycle → terminal status (`SUCCESS`/`FAILED`). Keep it behind an interface so it is testable
  and swappable (local single-server now; Kubernetes later per the future-scalability note).

### B3. Log capture + streaming wiring
- **Target:** PO §6, BA §16, NFR-PERF-003 (stdout/stderr→browser ≤5s), NFR-PERF-004 (server-side log search
  first page ≤5s), NFR-OBS-004 (retain until user deletes).
- **Now:** `JobStreamWebSocketHandler.publish(...)` exists but **nothing calls it**; no log persistence.
- **Do:** stream container stdout/stderr into `job_log_events` (append-only, with `lastEventId` for WS
  resume) **and** push each line through `handler.publish(jobId, "LOG", ...)`. Implement the log-search and
  log-download endpoints from the OpenAPI `paths/logs.yaml`.

### B4. Progress events
- **Target:** NFR-TEST-001 (progress parsing), NFR-UX-002 (`Progress Information Not Available` when none).
- **Now:** `jobs.latestProgress()` reads `job_progress_events`, but nothing writes them.
- **Do:** parse recognized progress markers from training output into `job_progress_events`; publish
  `PROGRESS` WS events. Emit nothing (let the UI show the "not available" state) when the code emits none.

---

## P1 — Reliability, notifications, artifacts

### B5. Recovery on restart
- **Target:** NFR-REL-003 (queued jobs survive restart), NFR-REL-004 (interrupted `RUNNING` → `RETRYING`,
  requeued, rerun from start). The `RETRYING` enum value already exists.
- **Now:** none. After a restart, previously-`RUNNING` jobs are orphaned.
- **Do:** a startup reconciler that finds `RUNNING` jobs with no live container, marks them `RETRYING`, and
  re-enqueues them. Queue entries are already DB-backed, so they persist; verify positions are rebuilt.

### B6. Email notifications
- **Target:** PO §7, BA §19 (success **and** failure email; **no** email on cancel), US4 (no duplicates),
  NFR-REL-006 (email failure must not change job terminal status).
- **Now:** none. Grep hits for "mail" are only DTO `email` fields; no `spring-boot-starter-mail`, no sender.
- **Do:** add a notification module triggered on `RUNNING→SUCCESS` and `RUNNING→FAILED`. Include the
  doc-specified fields (project, job id, timestamp, error/artifact summary, deep link). Make sending
  idempotent (dedupe key per job+transition) and isolated so a send failure is logged, not propagated.

### B7. Artifact management
- **Target:** BA §17/§18 (store, version, link to job/branch/dataset/config), NFR-STO-003 (copy workspace
  output to managed storage **before** registration), NFR-REL-007 (registration failure ≠ job failure),
  NFR-SEC-006 (ZIP: reject path traversal/absolute/unsafe symlinks), NFR-PERF-005 (stream download, never
  load full artifact into memory), NFR-SEC-007 (download only via authorized endpoints).
- **Now:** `DownloadService` streams via `FileSystemResource` and blocks path traversal (good start), but
  nothing copies/registers artifacts or creates model versions; ZIP extraction hardening absent.
- **Do:** on success, copy container output into platform artifact storage, register `artifacts` rows +
  model version, then expose the OpenAPI artifact endpoints. Harden ZIP extraction for GitHub/ZIP sources.

---

## P2 — Production auth, observability, hardening, tests

### B8. Real authentication (replace dev bearer)
- **Target:** NFR-SEC-001 (Google Workspace OIDC / OAuth 2.0). The LLD explicitly frames the email-bearer
  resolver as a **replaceable bootstrap**; controllers/RBAC already depend on `CurrentUserContext`, not the
  parser — keep that seam.
- **Now:** `WebConfig` `OncePerRequestFilter` treats the bearer token as email/user-id. No OAuth/OIDC/JWT.
- **Do:** introduce OIDC token validation behind the same `CurrentUserContext` abstraction so no controller
  or `AuthorizationService` call site changes. Keep the dev resolver available for local Docker validation.

### B9. Health, observability, metrics
- **Target:** NFR-AVL-003 / NFR-OBS-006 (health covers app, MongoDB, storage, Docker), NFR-OBS-001
  (structured logs + correlation IDs), NFR-OBS-002 (correlation ID in error responses — **already done** in
  `ApiExceptionHandler`), NFR-OBS-003 (metrics: active users, queue length, running count, job duration,
  Docker/artifact/email failures, WS connections).
- **Now:** `/health` is a static `UP` map; no Actuator; no metrics.
- **Do:** add real readiness checks (Mongo ping, `/data` writability, Docker engine reachable), structured
  logging with correlation IDs, and the metric counters above (Actuator/Micrometer is the natural fit).

### B10. Data integrity & migrations
- **Target:** NFR-MAINT-003 (versioned DB migrations), NFR-DATA-002/005/006 (queue + status persisted,
  audit append-only, model versions linked), BA §21 (audit: login, start, cancel, retry, **config change**,
  **artifact download**).
- **Now:** indexes/seed via `MongoSeedConfig` runner (not versioned migrations); audit is written on
  start/cancel/retry — verify config-change and artifact-download are also audited.
- **Do:** adopt a versioned migration mechanism for index/schema evolution; fill any missing audit events.

### B11. Tests
- **Target:** NFR-TEST-001..004 (unit: status transitions, authz decisions, YAML/path/ZIP validation,
  progress parsing; integration: Mongo repos, queue positions, seeding; contract: OpenAPI; WS: authz,
  reconnect, resume, duplicate handling, fallback).
- **Now:** `src/test` does not exist — **zero tests**.
- **Do:** stand up the test source set and cover the transition/authorization/validation logic first
  (highest risk, pure functions), then repository/queue integration tests.

---

## Coordination contract (shared with frontend — do not break unilaterally)

1. **OpenAPI is canonical** (`docs/.../api-contracts/openapi.yaml`). DTOs must match it (NFR-MAINT-002);
   when you change a shape, update the contract in the same change so the frontend can resync its types.
2. **Status vocabulary:** code uses `CREATED, QUEUED, RUNNING, SUCCESS, FAILED, CANCELLED, RETRYING`.
   PO §4 lists `PENDING` and BA §5 lists `QUEUED` — **reconcile to the OpenAPI contract** and make the
   backend, contract, and frontend agree on one set.
3. **WebSocket envelope:** `{ type, jobId, payload, occurredAt }` at `/api/v1/ws/jobs/:jobId`; auth via
   `Authorization` header **or** `?token=` query param. Event types the frontend will consume: `CONNECTED`,
   `LOG`, `PROGRESS`, status-change. Keep `lastEventId`-based resume working (NFR-TEST-004).
4. **Dev-auth:** `Authorization: Bearer <email>` → active user; seeded `user@example.com` / `admin@example.com`.
   This must keep working for local validation even after B8.
5. **Admin boundary:** NFR-SEC-005 — admins may cancel jobs / delete projects but must **not** read project
   source, detailed logs, or artifacts unless also the owner. Enforce in `AuthorizationService`.
