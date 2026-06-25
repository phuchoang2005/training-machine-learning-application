---
title: "High-Level Component Diagram"
tags: [diagram, hld, components, architecture]
diagram-type: mermaid
aliases: [Component Diagram, HLD Components]
---

# High-Level Component Diagram

Shows the internal modules of the monolithic backend and how they connect to external infrastructure.

```mermaid
flowchart TB
    U[User / Admin] --> FE[Web Frontend]

    FE --> BE[Monolithic Backend Application]

    subgraph BE[Monolithic Backend Application]
        AUTH[Auth & Access Control Module]
        PROJECT[Project Management Module]
        CONFIG[Configuration Module]
        JOB[Training Job Module]
        QUEUE[Queue & Scheduler Module]
        RUNNER[Docker Runner Module]
        LOG[Log & Progress Module]
        ARTIFACT[Artifact & Model Versioning Module]
        AUDIT[Audit Logging Module]
        ADMIN[Admin Management Module]
    end

    PROJECT --> GITHUB[GitHub Public Repository]
    PROJECT --> ZIP[ZIP Upload]

    RUNNER --> DOCKER[Docker Engine]
    DOCKER --> CONTAINER[Isolated Training Containers]

    CONTAINER --> LOG
    CONTAINER --> ARTIFACT

    BE --> DB[(MongoDB)]
    BE --> FS[(File Storage)]

    LOG --> FE

    DB --- AUTH
    DB --- PROJECT
    DB --- CONFIG
    DB --- JOB
    DB --- QUEUE
    DB --- ARTIFACT
    DB --- AUDIT

    FS --- PROJECT
    FS --- LOG
    FS --- ARTIFACT
```

## Module Responsibilities

| Module | Responsibility |
|---|---|
| Auth & Access Control | Identity verification, RBAC, ownership checks (→ `AuthorizationService` Facade) |
| Project Management | GitHub clone, ZIP upload/extract, project CRUD |
| Configuration | YAML editing, validation, immutable snapshot creation |
| Training Job | Job lifecycle management, status transitions |
| Queue & Scheduler | FIFO queue persistence, dispatcher loop (2 concurrent jobs) |
| Docker Runner | Container build/run/stream (Template Method + Strategy patterns) |
| Log & Progress | stdout/stderr capture, WebSocket fan-out |
| Artifact & Model | Artifact copy, registration, model versioning |
| Audit Logging | Append-only audit records for all user actions |
| Admin Management | User status management, system-level audit view |

## Related
- [[system-context-diagram]] — External context
- [[deployment-diagram]] — How these modules are deployed
- [[ADR-015]] — Design patterns applied to these modules
- [[design-patterns]] — Pattern detail
- [[low-level-design]] — LLD detail per module
