---
title: "Architecture MOC"
tags: [moc, architecture, hld, adr]
aliases: [Architecture]
---

# Architecture — Map of Content

High-level design, architectural decisions, and cross-cutting design concerns.

## Architectural Decision Records
- [[adr-index]] — All ADRs in one index
- [[ADR-001]] — Backend Framework (Spring Boot / Java)
- [[ADR-002]] — Frontend Framework (React / TypeScript)
- [[ADR-003]] — Frontend Build Tool (Vite)
- [[ADR-004]] — Database (MongoDB)
- [[ADR-005]] — Job Queue (MongoDB-backed FIFO)
- [[ADR-006]] — Training Execution (Docker)
- [[ADR-007]] — Authentication (Google Workspace OIDC)
- [[ADR-008]] — Real-Time (WebSocket + REST fallback)
- [[ADR-009]] — File and Artifact Storage (local POSIX)
- [[ADR-010]] — Notification Delivery (Google Workspace email)
- [[ADR-011]] — Frontend State Management (Redux Toolkit)
- [[ADR-012]] — Frontend API Client (Axios)
- [[ADR-013]] — Frontend Styling (TailwindCSS + Radix UI + shadcn/ui)
- [[ADR-014]] — Frontend Theme Mode (system light/dark)
- [[ADR-015]] — Backend Design Patterns (Template Method, Strategy, Repository, Observer, Facade, Service Layer, Chain of Responsibility, DTO)

## High-Level Design
- [[high-level-design-planning]] — HLD questions and diagram inventory
- [[system-context-diagram]] — Platform context: actors and external systems
- [[high-level-component-diagram]] — Backend module breakdown
- [[deployment-diagram]] — Physical deployment topology
- [[training-execution-sequence-diagram]] — End-to-end training flow
- [[job-lifecycle-state-diagram]] — Job state machine
- [[queue-flow-diagram]] — FIFO queue and dispatcher
- [[recovery-flow-diagram]] — Restart recovery
- [[log-streaming-architecture-diagram]] — WebSocket log streaming
- [[progress-event-flow-diagram]] — Progress tracking
- [[artifact-flow-diagram]] — Artifact registration
- [[project-registration-flow-diagram]] — GitHub/ZIP intake
- [[configuration-management-flow-diagram]] — YAML snapshot lifecycle
- [[capacity-scalability-view]] — MVP limits and future roadmap

## Security & Access
- [[security-model]] — RBAC + Ownership hybrid model
- [[security-architecture-diagram]] — Auth and authorization layers
- [[request-authorization-flow]] — Request auth sequence
- [[access-control-matrix]] — Permission matrix per action/role
- [[failure-handling-matrix]] — Error handling per scenario
- [[error-flow-diagram]] — Error handling flowchart

## Data & Storage
- [[erd]] — Full entity-relationship diagram
- [[storage-layout-diagram]] — File storage under `/data`
- [[api-interaction-diagram]] — API call flows

## Design Patterns
- [[design-patterns]] — 8 formally adopted patterns

## Related
- [[_index]] — Home
- [[requirements-moc]] — Requirements feeding these decisions
- [[design-moc]] — LLD detail
