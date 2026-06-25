# Low-Level Design - AI Training Management Platform

## Purpose

This Low-Level Design (LLD) translates the approved requirements, HLD, and ADR decisions into implementation-level guidance for the MVP. It assumes Spring Boot, ReactJS with TypeScript, Vite, Redux, Axios, TailwindCSS, Radix UI, shadcn/ui, MongoDB, Docker, Google Workspace authentication, local filesystem storage, and WebSocket-based real-time updates.

## Document Set

| Document | Purpose |
| --- | --- |
| [01-backend-design.md](./01-backend-design.md) | Spring Boot modules, packages, services, workers, and job execution design |
| [02-api-and-websocket-contracts.md](./02-api-and-websocket-contracts.md) | REST API endpoints, WebSocket channel contract, request/response examples |
| [03-database-design.md](./03-database-design.md) | MongoDB collections, document shapes, indexes, and consistency rules |
| [04-frontend-design.md](./04-frontend-design.md) | React screens, state management, API client, WebSocket client, and UI behavior |
| [05-security-and-operations.md](./05-security-and-operations.md) | Authorization, audit logging, storage rules, failure handling, and operational jobs |

## Technology Baseline

| Layer | Decision |
| --- | --- |
| Backend | Spring Boot 4.0.x, Java 25 LTS |
| Frontend | React 19.2, TypeScript, Vite, Redux, Axios, TailwindCSS, Radix UI, shadcn/ui, Node.js 24 LTS |
| Database | MongoDB 8 |
| Execution | Docker Engine, one centralized training server |
| Realtime | WebSocket primary, REST polling fallback |
| Authentication | Google Workspace OpenID Connect / OAuth 2.0 |
| Storage | Local POSIX filesystem under `/data` |

## MVP Constraints

* Maximum 7 active users.
* Maximum 2 concurrent `RUNNING` jobs.
* FIFO queue persisted in MongoDB.
* Queued and interrupted jobs must survive restart.
* Running jobs interrupted by restart are requeued and rerun from the beginning.
* Dataset management remains inside project Python code.
* Administrators cannot access project source code, detailed logs, or artifacts unless ownership rules allow it.

## MVP Authentication Bootstrap

The production authentication target remains Google Workspace OpenID Connect / OAuth 2.0. Until that integration is configured, the backend may run with a replaceable development bearer-token resolver for Docker-based implementation and testing.

* `Authorization: Bearer <email-or-user-id>` resolves to an `ACTIVE` user in the MongoDB `users` collection.
* A startup seeder (`MongoSeedConfig`) idempotently seeds one non-production `USER` and one non-production `ADMIN` account for local validation.
* Controllers, authorization checks, audit logging, and ownership rules must depend on the current-user abstraction, not on the bearer-token parser directly.
* The development resolver must be isolated so Google Workspace/OIDC can replace it without changing resource controllers or RBAC services.

Seeded non-production accounts:

| Role | Email | Password | Development bearer token |
| --- | --- | --- | --- |
| User | `user@example.com` | `password` | `user@example.com` |
| Admin | `admin@example.com` | `password` | `admin@example.com` |

The password is used only by the frontend development login/register phase. The current backend resolver validates the bearer token against an active database user and does not store or verify passwords.

## Docker Setup and Topology

The backend's Java/Maven runtime runs on the host (local device), not in Docker. Docker provides only MongoDB (plus the static frontend image). There is no `api` container and no backend load balancer.

```text
Browser
  -> frontend nginx :80
     -> static Future frontend assets
     -> /api/* reverse proxy
        -> backend (host-run Spring Boot) :8080
           -> mongodb (Docker) :27017
```

Implementation rules:

* The root `docker-compose.yml` owns the integrated local stack and runs `mongodb` and `frontend`; the backend Spring Boot app runs on the host (`mvn spring-boot:run`).
* `frontend/docker-compose.yml` owns the standalone frontend static Nginx setup and proxies `/api/` to a configurable `API_UPSTREAM`.
* `backend/docker-compose.yml` owns only the `mongodb` service, published on host port `27017`.
* The `frontend` container proxies `/api/` to the host-run backend via `http://host.docker.internal:8080`.
* Frontend static assets are built into the frontend image and served by frontend Nginx.
* The backend exposes `GET /api/v1/health` as an unauthenticated health endpoint.
* WebSocket routes are proxied with HTTP upgrade headers through frontend Nginx.

## Backend Code Organization

The implemented backend keeps controllers thin and isolates persistence in `MongoTemplate`-based repositories.

* DTO records are grouped by concern under `com.example.aitraining.dto`: `CommonDtos`, `UserDtos`, `ProjectDtos`, `JobDtos`, and `SupportDtos`.
* Repositories use `MongoTemplate`: the four `@Document` domain records (`User`, `Project`, `ProjectConfig`, `TrainingJob`) map automatically, while internal collections (snapshots, queue entries, logs, progress, artifacts, audit) are read/written as `org.bson.Document`s.
* Queue persistence is isolated in `JobQueueRepository`; `JobRepository` owns training job CRUD/status changes.
* Services orchestrate authorization, persistence, audit logging, and response DTO assembly while controllers keep request routing and parameter binding.

## Backend Runner Architecture

The `runner/` package follows the **Template Method** + **Strategy** patterns (ADR-015):

```
«interface» TrainingRunner
       ↑
AbstractTrainingRunner   ← sealed run() lifecycle; execute() is abstract
       ↑
DockerTrainingRunner     ← Docker-specific execute() implementation
```

`AbstractTrainingRunner.run()` owns the full job lifecycle: workspace → config → source → execute → terminal transition → artifact collection → queue refresh → WebSocket event → notification → workspace cleanup.

Adding a new execution engine (Kubernetes, local process, etc.) requires only a new subclass implementing `execute()`. No service or dispatcher changes are needed.

## Key References

* HLD diagrams: `docs/solution-architect/high-level-design/diagram/`
* ADR: `docs/solution-architect/md/architectural-decision-records.md`
* Design patterns: `docs/solution-architect/md/design-patterns.md`
* Architecture refinement: `docs/solution-architect/md/sa-refinement.md`
* Product and BA requirements: `docs/po-requirement.md`, `docs/ba-refine.md`
