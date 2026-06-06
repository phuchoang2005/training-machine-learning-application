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

## ADR-002: Frontend Framework

**Status:** Accepted

**Decision:** Use ReactJS with TypeScript for the web frontend.

**Version Target:** React 19.2 latest stable, TypeScript latest stable, Node.js 24 LTS for local development and CI.

**Rationale:** React matches the preferred technology stack and is appropriate for dashboard, project detail, log viewer, progress bar, and artifact browsing screens. TypeScript improves maintainability for API contracts and UI state.

**Consequences:** React does not publish an LTS line, so the project should pin stable major versions and upgrade intentionally.

---

## ADR-003: Frontend Build Tool

**Status:** Accepted

**Decision:** Use Vite for React application build and development.

**Version Target:** Latest stable Vite, running on Node.js 24 LTS.

**Rationale:** Vite provides fast local development, simple React integration, and production builds without unnecessary framework complexity.

**Consequences:** Server-side rendering is not included in the MVP. The frontend will be a static SPA served by the web tier or a reverse proxy.

---

## ADR-004: Database

**Status:** Accepted

**Decision:** Use PostgreSQL as the primary relational database.

**Version Target:** PostgreSQL 18 latest supported major release.

**Rationale:** The platform requires durable records for users, projects, config snapshots, jobs, queue entries, logs metadata, artifacts, model versions, notifications, and audit logs. PostgreSQL provides strong consistency and mature indexing for job history and access-control queries.

**Consequences:** Queue state is persisted in PostgreSQL for MVP. A dedicated message broker can be introduced later if multi-worker scaling is required.

---

## ADR-005: Job Queue and Scheduling

**Status:** Accepted

**Decision:** Implement the MVP queue as a persistent FIFO queue in PostgreSQL, managed by the Spring Boot scheduler.

**Version Target:** Spring Boot 4.0.x, PostgreSQL 18.

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

## Version References

* Spring Boot stable version reference: https://docs.spring.io/spring-boot/appendix/dependency-versions/index.html
* Spring Boot project support reference: https://github.com/spring-projects/spring-boot/wiki
* React latest stable reference: https://react.dev/versions
* Node.js LTS release schedule: https://nodejs.org/tr/download/releases
* PostgreSQL supported release reference: https://www.postgresql.org/
