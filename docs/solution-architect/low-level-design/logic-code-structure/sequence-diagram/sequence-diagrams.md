---
title: "Sequence Diagrams"
tags: [lld, diagram, sequence-diagram, plantuml]
aliases: [Sequence Diagrams, sequence-diagrams]
related:
  - "[[class-diagrams]]"
  - "[[low-level-design]]"
  - "[[training-execution-sequence-diagram]]"
  - "[[api-interaction-diagram]]"
---

# Sequence Diagrams

## Purpose

This folder contains modular docs-as-code sequence diagrams for the AI Training Management Platform LLD. The diagrams focus on runtime interactions between React, Spring Boot modules, PostgreSQL, Docker, WebSocket, local storage, GitHub, and Google Workspace.

## Files

| File | Description |
| --- | --- |
| [sequence-diagrams.puml](./sequence-diagrams.puml) | Standalone index diagram for the sequence module set |
| [diagrams/01-auth-project-dashboard-sequence.puml](./diagrams/01-auth-project-dashboard-sequence.puml) | Login and project dashboard loading |
| [diagrams/02-project-registration-sequence.puml](./diagrams/02-project-registration-sequence.puml) | GitHub and ZIP project registration |
| [diagrams/03-start-training-job-sequence.puml](./diagrams/03-start-training-job-sequence.puml) | Training job creation, queueing, and dispatch |
| [diagrams/04-docker-log-progress-sequence.puml](./diagrams/04-docker-log-progress-sequence.puml) | Docker execution, logs, progress, and WebSocket streaming |
| [diagrams/05-artifact-notification-sequence.puml](./diagrams/05-artifact-notification-sequence.puml) | Artifact registration, model versioning, and notifications |
| [diagrams/06-cancel-retry-recovery-sequence.puml](./diagrams/06-cancel-retry-recovery-sequence.puml) | Cancel, retry, and restart recovery |

## Render Commands

PlantUML CLI is not currently installed in this workspace. When available, render the index:

```bash
rtk plantuml -tsvg docs/sa/LLD/sequence-diagram/sequence-diagrams.puml
```

Render a single module:

```bash
rtk plantuml -tsvg docs/sa/LLD/sequence-diagram/diagrams/03-start-training-job-sequence.puml
```

The `.puml` files are the canonical source. Generated images should be committed only if the team wants rendered diagram artifacts in reviews.
