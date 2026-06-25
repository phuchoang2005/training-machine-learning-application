---
title: "ADR Index — Architectural Decision Records"
tags: [adr, index, moc, architecture]
aliases: [ADR Index, ADRs]
---

# Architectural Decision Records

All decisions reviewed 2026-06-06. Use the newest LTS release where available; pin exact versions during implementation. Accepted ADRs are immutable — new decisions require a new ADR entry (see [[non-functional-requirements]] NFR-MAINT-005).

## Stack Decisions

| ADR | Area | Decision | Status |
|---|---|---|---|
| [[ADR-001]] | Backend | Spring Boot 4.x / Java 25 LTS | Accepted |
| [[ADR-002]] | Frontend | React 19.2 / TypeScript | Accepted |
| [[ADR-003]] | Build | Vite | Accepted |
| [[ADR-004]] | Database | MongoDB 8 (revised from PostgreSQL) | Accepted |
| [[ADR-005]] | Queue | MongoDB-backed FIFO, Spring scheduler | Accepted |
| [[ADR-006]] | Execution | Docker containers per job | Accepted |
| [[ADR-007]] | Auth | Google Workspace OIDC + RBAC | Accepted |
| [[ADR-008]] | Realtime | WebSocket primary, REST polling fallback | Accepted |
| [[ADR-009]] | Storage | Local POSIX filesystem under `/data` | Accepted |
| [[ADR-010]] | Notifications | Google Workspace email | Accepted |
| [[ADR-011]] | State | Redux Toolkit (typed slices + thunks) | Accepted |
| [[ADR-012]] | HTTP | Axios shared instance | Accepted |
| [[ADR-013]] | Styling | TailwindCSS + Radix UI + shadcn/ui | Accepted |
| [[ADR-014]] | Theme | System-driven light/dark (`prefers-color-scheme`) | Accepted |
| [[ADR-015]] | Patterns | 8 backend design patterns | Accepted |

## Related
- [[architecture-moc]] — Architecture overview
- [[design-patterns]] — Detail on ADR-015 patterns
- [[low-level-design]] — LLD built on these decisions
- [[_index]] — Home
