---
title: "Design MOC"
tags: [moc, lld, database, api, design]
aliases: [Low-Level Design, LLD]
---

# Design — Map of Content

Low-level design, database schema, API contracts, and class/sequence diagrams.

## LLD Index
- [[low-level-design]] — LLD overview, technology baseline, MVP constraints, backend runner architecture

## Database Design
- [[database-design]] — MongoDB design index
- [[physical-schema-design]] — Physical collection shapes and indexes
- [[erd]] — Entity-Relationship Diagram
- `solution-architect/low-level-design/database-design/physical-erd.puml` — Physical ERD (PlantUML source)

## API Contracts
- `solution-architect/low-level-design/api-contracts/openapi.yaml` — OpenAPI root (source of truth)
- `api-contracts/paths/` — Individual path files (auth, projects, jobs, logs, artifacts, audit, users)
- `api-contracts/components/schemas/` — Schema definitions

## Class Diagrams
- [[class-diagrams]] — Class diagram index
- `diagrams/01-domain-model-class-diagram.puml` — Domain model
- `diagrams/02-backend-service-class-diagram.puml` — Service layer (annotated with pattern roles)
- `diagrams/03-api-dto-class-diagram.puml` — DTO records
- `diagrams/04-websocket-event-class-diagram.puml` — WebSocket events

## Sequence Diagrams
- [[sequence-diagrams]] — Sequence diagram index
- `diagrams/01-auth-project-dashboard-sequence.puml` — Auth and dashboard load
- `diagrams/02-project-registration-sequence.puml` — Project registration
- `diagrams/03-start-training-job-sequence.puml` — Start training
- `diagrams/04-docker-log-progress-sequence.puml` — Docker log and progress streaming
- `diagrams/05-artifact-notification-sequence.puml` — Artifact registration and notification
- `diagrams/06-cancel-retry-recovery-sequence.puml` — Cancel, retry, and recovery

## Activity Diagrams
- `diagrams/project-registration-activity.puml`
- `diagrams/recovery-cancel-activity.puml`
- `diagrams/training-job-execution-activity.puml`
- `diagrams/websocket-monitoring-activity.puml`

## NFRs
- [[non-functional-requirements]] — Full NFR catalogue

## Related
- [[_index]] — Home
- [[architecture-moc]] — HLD and ADRs
- [[frontend-moc]] — Frontend design details
