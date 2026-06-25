# Architectural Decision Records - ADR

## Context

This document records the high-level architecture decisions for the AI Training Management Platform. The MVP supports authenticated users launching and monitoring Docker-based training jobs on one centralized training server with 2 concurrent running jobs, FIFO queueing, real-time logs/progress, artifact registration, and project isolation.

Version-sensitive decisions were reviewed on 2026-06-06. Use the newest LTS release where the technology publishes an LTS line. If a project does not publish LTS releases, use the latest stable supported release and pin the exact version during implementation.

---

## ADR-001: Backend Framework

**Status:** Accepted

**Decision:** Use Spring Boot with Java for the backend API and worker orchestration.

**Version Target:** Spring Boot 4.0.x latest stable supported release, Java 25 LTS.

**Rationale:** Spring Boot fits the preferred technology stack, supports production-grade REST APIs, validation, security, persistence, scheduling, observability, and integration with Docker execution workflows. Java 25 is the newest LTS Java line available through common JDK vendors.

**Consequences:** The team must plan for Spring Boot 4 migration differences and keep framework dependencies managed through the Spring Boot BOM.

---

## ADR-002: Frontend Framework and Language

**Status:** Accepted

**Decision:** Use ReactJS with TypeScript for the web frontend.

**Version Target:** React 19.2 latest stable, TypeScript latest stable, Node.js 24 LTS for local development and CI.

**Rationale:** React matches the preferred technology stack and is appropriate for dashboard, project detail, log viewer, progress bar, and artifact browsing screens. TypeScript improves maintainability for API contracts and UI state.

**Consequences:** React does not publish an LTS line, so the project should pin stable major versions and upgrade intentionally. Frontend state, API, and UI decisions are captured in ADR-011 through ADR-014.

---

## ADR-003: Frontend Build Tool

**Status:** Accepted

**Decision:** Use Vite for React application build and development.

**Version Target:** Latest stable Vite, running on Node.js 24 LTS.

**Rationale:** Vite provides fast local development, simple React integration, and production builds without unnecessary framework complexity.

**Consequences:** Server-side rendering is not included in the MVP. The frontend will be a static SPA served by the web tier or a reverse proxy.

---

## ADR-011: Frontend State Management

**Status:** Accepted

**Decision:** Use Redux with TypeScript for frontend application state and client-side server-state coordination.

**Version Target:** Latest stable Redux Toolkit and React Redux compatible with the selected React version.

**Rationale:** The platform has shared cross-route state for authentication, project/job caches, notification state, WebSocket connection state, filters, and live job monitoring. Redux Toolkit provides predictable state transitions, typed slices, async thunks or listener middleware, and a single integration point for WebSocket event synchronization.

**Consequences:** State should be organized by domain slices such as `auth`, `projects`, `configurations`, `jobs`, `logs`, `artifacts`, `notifications`, `admin`, `ui`, and `theme`. Redux is the authoritative client cache; component-local state remains appropriate for transient UI state such as open dialogs and unsaved field text.

---

## ADR-012: Frontend API Client

**Status:** Accepted

**Decision:** Use Axios as the browser HTTP client for REST API integration.

**Version Target:** Latest stable Axios compatible with Node.js 24 LTS tooling and target browsers.

**Rationale:** Axios provides request and response interceptors, consistent error normalization, cancellation support, multipart upload support, and straightforward integration with Redux async workflows.

**Consequences:** All REST calls must go through a shared Axios instance configured with `/api/v1`, authentication behavior, correlation ID extraction, idempotency headers where needed, and normalized `ApiError` handling. Feature components must not construct raw endpoint calls directly.

---

## ADR-013: Frontend Styling and Component System

**Status:** Accepted

**Decision:** Use TailwindCSS for styling, Radix UI primitives for accessible low-level interactions, and shadcn/ui as the component composition baseline.

**Version Target:** Latest stable TailwindCSS, Radix UI packages, and shadcn/ui component templates compatible with the selected React version.

**Rationale:** TailwindCSS supports consistent utility-based styling, dark-mode variants, and compact operational UI construction. Radix UI provides accessible primitives for dialogs, menus, tabs, tooltips, popovers, and other interactive components. shadcn/ui gives a pragmatic component baseline that can be owned directly in the repository and adapted to the platform design system.

**Consequences:** Shared UI components should wrap or compose shadcn/ui and Radix primitives instead of introducing unrelated component systems. Tailwind design tokens must align with the documented design system. Components must remain accessible and must not rely on color alone for status communication.

---

## ADR-014: Frontend Theme Mode

**Status:** Accepted

**Decision:** Support light mode and dark mode using the operating system or browser `prefers-color-scheme` setting by default.

**Version Target:** TailwindCSS dark mode configured for class-based or selector-based theming with an initial system-mode resolver.

**Rationale:** Users may monitor long-running training jobs for extended periods, and system-based light/dark mode improves visual comfort while respecting user device preferences.

**Consequences:** The frontend must initialize theme from system preference, react to system preference changes, and avoid a flash of incorrect theme during initial load. A future manual override may be added, but MVP default behavior is system-driven.

---

## ADR-004: Database

**Status:** Accepted (revised — supersedes the original PostgreSQL decision)

**Decision:** Use MongoDB as the primary database, accessed from the backend through Spring Data MongoDB (`MongoTemplate`). Documents use `UUID` `_id`s; relationships are stored by reference and resolved in the service/repository layer.

**Version Target:** MongoDB 8.

**Rationale:** The platform stores documents for users, projects, config snapshots, jobs, queue entries, logs metadata, artifacts, and audit logs. A document model fits the per-aggregate access patterns, removes the need for SQL migrations, and keeps the schema flexible for the MVP. Strong cross-document transactions are not required; ownership/access checks are enforced in the backend.

**Consequences:** Queue state is persisted in MongoDB for the MVP and queue positions are recomputed in the application layer. There are no foreign keys, so cascading deletes are performed by the backend (`ProjectRepository.delete`). A dedicated message broker can still be introduced later if multi-worker scaling is required. The earlier PostgreSQL/Flyway/JDBC approach is retired.

---

## ADR-005: Job Queue and Scheduling

**Status:** Accepted

**Decision:** Implement the MVP queue as a persistent FIFO queue in MongoDB, managed by the Spring Boot scheduler.

**Version Target:** Spring Boot 4.0.x, MongoDB 8.

**Rationale:** The MVP has one training server, 2 concurrent running jobs, and up to 50 queued jobs. A database-backed queue is simpler to operate and satisfies restart recovery requirements.

**Consequences:** This avoids Redis/RabbitMQ/Kafka for MVP. If the platform later adds multiple workers, GPU scheduling, or distributed execution, replace or supplement this with a dedicated message broker.

---

## ADR-006: Training Execution Isolation

**Status:** Accepted

**Decision:** Run every training job inside an isolated Docker container.

**Version Target:** Current stable Docker Engine release, pinned during implementation.

**Rationale:** Each AI project can have different Python dependencies and runtime requirements. Containers prevent host-level Python execution and support predictable workspace, log, and artifact paths.

**Consequences:** Projects must provide `main.py`, `requirements.txt`, and configuration under `./configs/`. The platform must validate disk space before launch and clean up interrupted containers during recovery.

---

## ADR-007: Authentication and Authorization

**Status:** Accepted

**Decision:** Use Google Workspace authentication with backend-enforced RBAC plus ownership-based project access.

**Version Target:** OpenID Connect / OAuth 2.0 standards supported by Google Workspace.

**Rationale:** Requirements specify company credentials, project isolation, and administrator restrictions. Authorization must be enforced in the backend for projects, jobs, logs, artifacts, and admin actions.

**Consequences:** Administrators may manage platform operations but must not access project source code, detailed business data, logs, or artifacts unless explicitly authorized by ownership rules.

---

## ADR-008: Real-Time Logs and Progress

**Status:** Accepted

**Decision:** Use WebSocket for MVP log, progress, and job status streaming, with REST polling as fallback for reconnect or degraded network scenarios.

**Version Target:** Standard browser WebSocket support with Spring Boot 4.0.x WebSocket support.

**Rationale:** The website needs a persistent real-time channel for training logs, progress updates, job status changes, and future interactive controls such as cancel, retry confirmation, or operator messages. WebSocket is well supported by React clients and Spring Boot backends and gives the platform a single channel for bidirectional real-time features.

**Consequences:** The backend must authenticate the WebSocket handshake and enforce project ownership for every subscribed job stream. The frontend must handle reconnect, duplicate event protection, and fallback polling when the WebSocket connection is unavailable.

---

## ADR-009: File and Artifact Storage

**Status:** Accepted

**Decision:** Store source snapshots, uploaded ZIP files, workspaces, logs, and artifacts on local host storage for MVP.

**Version Target:** POSIX-compatible filesystem on the training server.

**Rationale:** The MVP runs on a single centralized training server. Local storage is enough for cloned repositories, extracted ZIPs, job logs, generated artifacts, and model versions.

**Consequences:** Use stable path conventions such as `/data/sources/{project_id}`, `/data/workspaces/{job_id}`, `/data/logs/{project_id}/{job_id}`, and `/data/artifacts/{project_id}/{job_id}`. Move to object storage when multi-server execution is introduced.

---

## ADR-010: Notification Delivery

**Status:** Accepted

**Decision:** Send success and failure notifications through Google Workspace email infrastructure and persist notification status in the database.

**Version Target:** Google Workspace SMTP relay or Gmail API, selected during implementation.

**Rationale:** Requirements specify email notification for training outcomes. Persisted notification status allows email failures to be reported without changing the training job result.

**Consequences:** Email failure must not change `SUCCESS`, `FAILED`, or `CANCELLED` job status. The platform should show an in-app notification if email delivery fails.

---

## ADR-015: Backend Design Patterns

**Status:** Accepted

**Decision:** Formally adopt eight GoF and enterprise design patterns for the Spring Boot backend, with the Template Method pattern added structurally via `AbstractTrainingRunner` and the remaining seven patterns codified as naming and layering conventions.

**Patterns adopted:**

| # | Pattern | Category | Where |
|---|---|---|---|
| 1 | **Template Method** | GoF Behavioral | `AbstractTrainingRunner.run()` — defines the fixed job lifecycle; `execute()` is the abstract hook |
| 2 | **Strategy** | GoF Behavioral | `TrainingRunner` interface + `DockerTrainingRunner`; `JobDispatcherService` is the context |
| 3 | **Repository** | Enterprise | All six `*Repository` classes in `com.example.aitraining.repo` |
| 4 | **Observer** | GoF Behavioral | `JobStreamWebSocketHandler` broadcasts events; WebSocket sessions are observers |
| 5 | **Facade** | GoF Structural | `AuthorizationService` — single surface for all RBAC decisions |
| 6 | **Service Layer** | Enterprise | All `*Service` classes separate business logic from persistence and presentation |
| 7 | **Chain of Responsibility** | GoF Behavioral | `WebConfig extends OncePerRequestFilter` in Spring's servlet filter chain |
| 8 | **DTO** | Enterprise | All `*Dtos.java` record files carry data across the API layer boundary |

**Rationale:**
The Template Method pattern was introduced to address a specific structural problem: `DockerTrainingRunner` previously implemented the entire job lifecycle (workspace, config, source prep, execution, artifact collection, notification, cleanup) in one class. Extracting `AbstractTrainingRunner` makes the lifecycle explicit and allows future execution engines (Kubernetes, local process) to be added by only implementing the `execute()` hook — no lifecycle code needs to change.

The remaining seven patterns reflect naming and layering conventions already present in the code; formalising them in an ADR makes the conventions explicit for future contributors and reviewers.

**Consequences:**
- `runner/AbstractTrainingRunner.java` is the lifecycle owner; it must remain the sole entry point for status transitions, WebSocket events, artifact collection, and cleanup within the runner subsystem.
- Adding a new runner (e.g., `KubernetesTrainingRunner`) requires only: create a class that `extends AbstractTrainingRunner`, implement `execute()`, and register it as a Spring `@Component`. No dispatcher or service changes are needed.
- The Strategy pattern means the active runner is determined by which `TrainingRunner`-typed bean is in the Spring context; only one should be active at a time in the MVP.
- Deviating from the Repository pattern (i.e., writing queries in service classes) is not permitted; all MongoDB queries must live in `*Repository` classes.

---

## Version References

* Spring Boot stable version reference: https://docs.spring.io/spring-boot/appendix/dependency-versions/index.html
* Spring Boot project support reference: https://github.com/spring-projects/spring-boot/wiki
* React latest stable reference: https://react.dev/versions
* Node.js LTS release schedule: https://nodejs.org/tr/download/releases
* Redux Toolkit documentation: https://redux-toolkit.js.org/
* Axios documentation: https://axios-http.com/
* TailwindCSS documentation: https://tailwindcss.com/
* Radix UI documentation: https://www.radix-ui.com/
* shadcn/ui documentation: https://ui.shadcn.com/
* MongoDB documentation: https://www.mongodb.com/docs/
