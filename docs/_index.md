---
title: "Future — AI Training Management Platform"
tags: [moc, home, index]
aliases: [Home, Vault Index, Index]
---

# Future — AI Training Management Platform

> Centralized web platform for launching, monitoring, and managing ML training jobs on a dedicated server.

## Maps of Content

| MOC | Covers |
|---|---|
| [[requirements-moc]] | Business requirements, product vision, SA clarifications, NFRs |
| [[architecture-moc]] | HLD, ADRs, design patterns, security model |
| [[design-moc]] | LLD, database design, API contracts |
| [[frontend-moc]] | Frontend architecture, UI screens, UX design |

---

## Key Documents

### Requirements & Context
- [[ba-refine]] — Business Requirement Specification (BRS)
- [[po-requirement]] — Product Requirements & User Stories
- [[sa-refinement]] — Architecture Clarifications (approved inputs for SA phase)
- [[non-functional-requirements]] — NFR catalogue (CAP, PERF, SEC, REL, OBS)

### Architecture
- [[adr-index]] — All 15 Architectural Decision Records
- [[design-patterns]] — 8 backend design patterns (ADR-015)
- [[security-model]] — RBAC + Ownership access control model
- [[access-control-matrix]] — Permission matrix per role
- [[failure-handling-matrix]] — Error handling per scenario

### System Diagrams (HLD)
- [[system-context-diagram]] — Platform actors and external integrations
- [[high-level-component-diagram]] — Backend module map
- [[deployment-diagram]] — Single-server deployment topology
- [[job-lifecycle-state-diagram]] — Training job state machine
- [[queue-flow-diagram]] — FIFO queue and dispatcher logic
- [[training-execution-sequence-diagram]] — End-to-end training flow
- [[recovery-flow-diagram]] — Restart recovery logic
- [[erd]] — Full entity-relationship model
- [[security-architecture-diagram]] — Auth and authorization layers
- [[storage-layout-diagram]] — File storage structure under `/data`

### LLD — Code Structure
- [[low-level-design]] — LLD index and technology baseline
- [[database-design]] — MongoDB schema design
- [[physical-schema-design]] — Physical collection shapes
- [[class-diagrams]] — Backend class diagrams
- [[sequence-diagrams]] — Detailed sequence diagrams

### Frontend & UX
- [[frontend-architecture]] — React SPA architecture (ADR-002/011/012/013/014)
- [[ux-overview]] — UX scope, principles, and document map
- [[primary-user-flow-diagram]] — Core training workflow
- [[information-architecture-diagram]] — Navigation hierarchy
- [[api-integration-flow]] — Axios + Redux integration flow
- [[route-guard-flow]] — Authentication and role routing

### API
- `solution-architect/low-level-design/api-contracts/openapi.yaml` — OpenAPI contract (source of truth)

### Operations
- [[github-commit-strategy]] — Conventional Commits guide

---

## Platform Constraints at a Glance

| Constraint                  | Value                                  |
| --------------------------- | -------------------------------------- |
| Max active users            | 7                                      |
| Max concurrent RUNNING jobs | 2                                      |
| Queue capacity              | 50                                     |
| Disk pre-check              | ≥ 5 GB                                 |
| Auth (dev)                  | `Authorization: Bearer <email>`        |
| Auth (prod)                 | Google Workspace OIDC                  |
| Backend                     | Spring Boot 4.x / Java 21 LTS          |
| Frontend                    | React 19.2 / TypeScript / Vite / Redux |
| Database                    | MongoDB 8                              |
| Execution                   | Docker Engine (isolated containers)    |
| Storage                     | Local POSIX `/data`                    |
