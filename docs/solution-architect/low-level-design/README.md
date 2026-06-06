# Low-Level Design - AI Training Management Platform

## Purpose

This Low-Level Design (LLD) translates the approved requirements, HLD, and ADR decisions into implementation-level guidance for the MVP. It assumes Spring Boot, ReactJS, PostgreSQL, Docker, Google Workspace authentication/email, local filesystem storage, and WebSocket-based real-time updates.

## Document Set

| Document | Purpose |
| --- | --- |
| [01-backend-design.md](./01-backend-design.md) | Spring Boot modules, packages, services, workers, and job execution design |
| [02-api-and-websocket-contracts.md](./02-api-and-websocket-contracts.md) | REST API endpoints, WebSocket channel contract, request/response examples |
| [03-database-design.md](./03-database-design.md) | PostgreSQL tables, enums, indexes, constraints, and transaction rules |
| [04-frontend-design.md](./04-frontend-design.md) | React screens, state management, API client, WebSocket client, and UI behavior |
| [05-security-and-operations.md](./05-security-and-operations.md) | Authorization, audit logging, storage rules, failure handling, and operational jobs |

## Technology Baseline

| Layer | Decision |
| --- | --- |
| Backend | Spring Boot 4.0.x, Java 25 LTS |
| Frontend | React 19.2, TypeScript, Vite, Node.js 24 LTS |
| Database | PostgreSQL 18 |
| Execution | Docker Engine, one centralized training server |
| Realtime | WebSocket primary, REST polling fallback |
| Authentication | Google Workspace OpenID Connect / OAuth 2.0 |
| Notifications | Google Workspace SMTP relay or Gmail API |
| Storage | Local POSIX filesystem under `/data` |

## MVP Constraints

* Maximum 7 active users.
* Maximum 2 concurrent `RUNNING` jobs.
* FIFO queue persisted in PostgreSQL.
* Queued and interrupted jobs must survive restart.
* Running jobs interrupted by restart are requeued and rerun from the beginning.
* Dataset management remains inside project Python code.
* Administrators cannot access project source code, detailed logs, or artifacts unless ownership rules allow it.

## Key References

* HLD diagrams: `docs/sa/HLD/diagram/`
* ADR: `docs/sa/md/architectural-decision-records.md`
* Architecture refinement: `docs/sa/md/sa-refinement.md`
* Product and BA requirements: `docs/po-requirement.md`, `docs/ba-refine.md`
