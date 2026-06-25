# Backend Design Patterns

## Status

Accepted — implemented in `backend/` as of 2026-06-25. Formalised in ADR-015.

---

## Overview

Eight GoF and enterprise design patterns have been formally adopted for the Spring Boot backend. They are enforced at the layer/naming level and documented in JavaDoc on every class.

The most structurally significant change was introducing **`AbstractTrainingRunner`** (Template Method), which extracted the entire job-execution lifecycle out of `DockerTrainingRunner` into a reusable, sealed base class.

---

## Pattern Catalogue

### 1. Template Method — `AbstractTrainingRunner`

**Problem:** `DockerTrainingRunner` contained the full job lifecycle (workspace, config, source, execute, artifact collection, notification, cleanup) mixed with Docker-specific code. Adding a second runner (e.g., Kubernetes) would duplicate all lifecycle logic.

**Solution:** Introduce `AbstractTrainingRunner` as a sealed abstract class. Its `run()` method is the _template_: it defines the fixed sequence of steps and calls the `execute()` hook at the right moment.

```
AbstractTrainingRunner.run(job, project)          ← SEALED (final)
  1. createDirectories(workspace)
  2. assertDiskSpace()
  3. loadConfigSnapshot() → write config.yaml
  4. prepareSource()                               ← shared helper
  5. parseEntrypoint()                             ← shared helper
  6. ws.publish("STATUS_CHANGE", RUNNING)
  7. execute(job, workspace, sourcePath, ep)       ← ABSTRACT HOOK
  8. transitionToTerminal(SUCCESS | FAILED)
  9. collectArtifacts() on success
  10. queue.refreshPositions()
  11. ws.publish("STATUS_CHANGE", terminal)
  12. notifications.notifyJobTerminal()
  finally: deleteQuietly(workspace)
```

**Subclasses:** `DockerTrainingRunner` extends `AbstractTrainingRunner` and only implements `execute()` + Docker-specific helpers (`buildDockerCommand`, `streamLines`).

**Adding a new runner:** Create a class that `extends AbstractTrainingRunner` and implement `execute()`. Annotate with `@Component`. No other code changes are required.

**Files:**
- `backend/src/main/java/com/example/aitraining/runner/AbstractTrainingRunner.java`
- `backend/src/main/java/com/example/aitraining/runner/DockerTrainingRunner.java`

---

### 2. Strategy — `TrainingRunner`

**Problem:** The dispatcher must launch jobs without being coupled to the specific execution engine.

**Solution:** `TrainingRunner` is the strategy interface. `JobDispatcherService` (the context) calls `runner.run(job, project)` without knowing whether the engine is Docker, Kubernetes, or local process.

```
«interface» TrainingRunner
    +run(job, project)
         ↑
AbstractTrainingRunner (implements, sealed lifecycle)
         ↑
DockerTrainingRunner   (concrete strategy — Docker engine)
```

`JobDispatcherService` receives `TrainingRunner` via Spring constructor injection — swapping the active runner requires only registering a different `@Component` bean.

**Files:**
- `runner/TrainingRunner.java` — strategy interface
- `runner/AbstractTrainingRunner.java` — strategy base
- `runner/DockerTrainingRunner.java` — concrete strategy
- `service/JobDispatcherService.java` — strategy context

---

### 3. Repository — `*Repository`

**Problem:** Persistence queries must not leak into service or controller code; the storage technology must be replaceable.

**Solution:** All MongoDB queries are isolated in dedicated `@Repository` classes. Services depend on the repository interface, not on `MongoTemplate` directly.

| Repository | Collection(s) |
|---|---|
| `UserRepository` | `users` |
| `ProjectRepository` | `projects`, `project_configs`, `training_jobs`, `job_queue_entries`, … (cascade delete) |
| `ConfigRepository` | `project_configs`, `config_snapshots` |
| `JobRepository` | `training_jobs`, `job_progress_events` |
| `JobQueueRepository` | `job_queue_entries` |
| `SupportRepository` | `job_log_events`, `job_progress_events`, `artifacts`, `audit_logs`, `notification_dedupe` |

**Rule:** MongoDB queries must never appear outside a `*Repository` class. Controllers and services call repository methods only.

---

### 4. Observer — `JobStreamWebSocketHandler`

**Problem:** Multiple browser tabs may subscribe to the same job stream simultaneously. The training runner must publish events without knowing how many listeners exist.

**Solution:** `JobStreamWebSocketHandler` is the _subject_. Connected `WebSocketSession` objects are the _observers_, stored in a `ConcurrentHashMap<UUID, Map<String, WebSocketSession>>` keyed by job ID. The runner calls `ws.publish(jobId, type, payload)` to fan out to all subscribers for that job.

**Event types published:**

| Event type | Published when |
|---|---|
| `CONNECTED` | WebSocket handshake completes |
| `STATUS_CHANGE` | Job transitions to a new lifecycle state |
| `LOG` | A single stdout/stderr line is produced |
| `PROGRESS` | A progress marker is parsed from output |

**Files:**
- `realtime/JobStreamWebSocketHandler.java`
- `config/WebSocketConfig.java` — mounts handler + handshake interceptor

---

### 5. Facade — `AuthorizationService`

**Problem:** RBAC rules (ownership, role, visibility, sensitivity) are spread across controllers and services, leading to inconsistent enforcement.

**Solution:** `AuthorizationService` is a single access point for all permission decisions. Every controller call that touches a project, job, or artifact goes through one of its `require*()` methods.

| Method | Who can pass |
|---|---|
| `requireProjectOwner` | Owner only (admin rejected) |
| `requireProjectVisible` | Owner or ADMIN |
| `requireJobVisible` | Owner or ADMIN |
| `requireSensitiveJobOwner` | Owner only (admin rejected — NFR-SEC-005) |
| `requireAdmin` | ADMIN only |

**File:** `service/AuthorizationService.java`

---

### 6. Service Layer — `*Service`

**Problem:** Business logic in controllers makes them untestable and violates single-responsibility.

**Solution:** Controllers are kept thin (request parsing + response mapping only). All business logic, authorization, persistence orchestration, and audit logging live in `@Service` classes.

| Service | Responsibility |
|---|---|
| `JobService` | Start, list, detail, cancel, retry jobs |
| `ProjectService` | Create (GitHub/ZIP), list, detail, delete projects |
| `ConfigService` | List, get, validate YAML configs |
| `JobDispatcherService` | Scheduled dispatcher — claims and executes queued jobs |
| `JobReconcilerService` | Startup reconciler — re-queues orphaned RUNNING jobs |
| `NotificationService` | Email delivery on SUCCESS / FAILED |
| `ArtifactService` | Copy and register training output files |
| `DownloadService` | Resolve artifact paths with path-confinement check |

---

### 7. Chain of Responsibility — `WebConfig`

**Problem:** Authentication must happen before any controller code runs, in a reusable, transparent way.

**Solution:** `WebConfig extends OncePerRequestFilter` sits in Spring's servlet filter chain. It extracts the `Authorization: Bearer` header, resolves the user via `UserRepository.findActiveByToken`, and stores the result in `CurrentUserContext` (thread-local). Controllers and services call `CurrentUserContext.require()` without knowing about the filter.

**Only exception:** `GET /api/v1/health` passes through unauthenticated (checked in `isPublicHealthCheck`).

**Files:**
- `config/WebConfig.java` — the filter
- `auth/CurrentUserContext.java` — thread-local user store

---

### 8. DTO — `*Dtos.java`

**Problem:** Domain records (`User`, `Project`, `TrainingJob`) must not be serialised directly to HTTP responses (leaks internal fields, couples API shape to persistence model).

**Solution:** Dedicated Java records carry data across the API boundary. Domain objects are never serialised. Controllers return DTO records assembled by service methods.

| DTO file | Content |
|---|---|
| `CommonDtos.java` | `Page`, `ApiError`, `ErrorResponse`, `UserSummary`, `ValidationDetail` |
| `UserDtos.java` | `CurrentUser`, `UserPage`, `UpdateUserStatusRequest`, `UserStatusResponse` |
| `ProjectDtos.java` | Project create/list/detail requests and responses; config list/content/validate |
| `JobDtos.java` | Job start/list/detail/cancel/retry requests and responses; queue snapshot |
| `SupportDtos.java` | Log events, artifact metadata, audit log entries |

---

## Diagram

See `docs/solution-architect/low-level-design/logic-code-structure/class-diagram/diagrams/02-backend-service-class-diagram.puml` for the PlantUML class diagram annotated with pattern roles.

## ADR Reference

ADR-015 in `docs/solution-architect/md/architectural-decision-records.md`.
