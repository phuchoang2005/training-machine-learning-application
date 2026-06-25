---
title: "System Context Diagram"
tags: [diagram, hld, context, architecture]
diagram-type: mermaid
aliases: [System Context]
---

# System Context Diagram

Shows all actors and external systems that interact with the AI Training Management Platform, and the nature of each interaction.

```mermaid
flowchart LR
    User["User<br/>AI Engineer / Student"]
    Admin["Admin"]

    System["AI Training Management Platform<br/><br/>Quản lý project, config YAML,<br/>training job, queue, log, progress,<br/>artifact, model version"]

    GitHub["GitHub<br/>Public Repository"]
    Docker["Docker Engine<br/>Isolated Training Containers"]
    Google["Google Workspace<br/>Email Notification"]
    Storage["Platform Storage<br/>Source / ZIP / Logs / Artifacts / Models"]
    DB["Platform Database<br/>Users / Projects / Jobs / Queue / Audit"]

    User -->|"Upload ZIP / Register GitHub repo"| System
    User -->|"Edit YAML config / Start training / Cancel / Retry"| System
    User -->|"View job status, logs, progress, artifacts"| System

    Admin -->|"View project ownership<br/>Cancel jobs / Delete projects"| System

    System -->|"Clone public repository"| GitHub
    System -->|"Build/run isolated container<br/>Execute main.py"| Docker
    Docker -->|"stdout / stderr logs<br/>progress events<br/>generated artifacts"| System

    System -->|"Send training notification"| Google
    System -->|"Store metadata, queue state,<br/>audit logs, job records"| DB
    System -->|"Store source, extracted ZIP,<br/>logs, artifacts, model files"| Storage
```

## Context Notes

- **Users** are AI Engineers who register projects and manage training jobs
- **Admins** have limited visibility: project names, ownership, and the ability to cancel jobs or delete projects — they cannot access source code, logs, or artifacts
- **GitHub** — only public repos; no auth tokens required
- **Docker Engine** — runs on the same server as the backend; containers are the execution boundary
- **Google Workspace** — email notifications on SUCCESS/FAILED (failure doesn't affect job status)
- **Storage** — local POSIX filesystem under `/data`; see [[storage-layout-diagram]]
- **Database** — MongoDB 8; see [[erd]] for the full data model

## Related
- [[high-level-component-diagram]] — Internal module breakdown
- [[deployment-diagram]] — Physical server topology
- [[security-architecture-diagram]] — Auth and RBAC context
- [[ADR-006]] — Docker execution decision
- [[ADR-007]] — Authentication decision
- [[ADR-009]] — Storage decision
- [[ADR-010]] — Notification decision
