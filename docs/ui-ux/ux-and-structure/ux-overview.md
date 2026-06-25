---
title: "UX Overview"
tags: [ux, frontend, navigation, design]
aliases: [UX Overview, UX Structure, ux-overview]
related:
  - "[[po-requirement]]"
  - "[[ba-refine]]"
  - "[[frontend-architecture]]"
  - "[[design-system]]"
  - "[[information-architecture-diagram]]"
  - "[[primary-user-flow-diagram]]"
---

# UX and Structure Diagram Document

## 1. Purpose

This documentation package defines the user experience structure for the AI Training Management Platform MVP. It covers the core user personas, journey maps, information architecture, and primary user flows that guide frontend screen design and navigation.

The UX model is aligned with:

* Product requirements in [[po-requirement]].
* Business refinement in [[ba-refine]].
* Frontend architecture in [[frontend-architecture]].
* Design system guidance in [[design-system]].

## 2. UX Scope

### In Scope

* Authenticated project discovery.
* Project detail review.
* Training configuration review and override.
* Training job launch.
* Queue and job status visibility.
* Real-time monitoring of progress and logs.
* Cancel, retry, and historical job review.
* Artifact discovery and download.
* Notification review.
* Administrator access to user and queue management.

### Out of Scope

* Public marketing pages.
* Model deployment workflows.
* Dataset upload management.
* GPU scheduling controls.
* Distributed training orchestration screens.
* Automatic hyperparameter tuning workflows.

## 3. Document Map

| Document | Purpose |
| --- | --- |
| [User Personas](personas.md) | Defines primary and secondary users, goals, permissions, and UX implications. |
| [User Journey Maps](journey-maps.md) | Maps end-to-end workflows, user expectations, pain points, and UX opportunities. |
| [Information Architecture](information-architecture.md) | Defines navigation hierarchy, route groups, page ownership, and content relationships. |
| [User Flows](user-flows.md) | Defines task-level flows for launch, monitor, cancel, retry, artifact review, and admin operations. |
| [[information-architecture-diagram]] | Mermaid structure diagram for product navigation. |
| [[primary-user-flow-diagram]] | Mermaid flow diagram for the core training workflow. |

## 4. UX Principles

* Keep the product operational and task-first; avoid marketing-style layouts.
* Make current job status visible at every decision point.
* Treat logs, errors, artifacts, and queue position as first-class workflow information.
* Keep project boundaries explicit so users understand which resources they are allowed to access.
* Support leaving and returning to a training job without losing context.
* Make destructive actions reversible only through retry or new job creation, not hidden side effects.
* Make degraded real-time states visible when WebSocket updates are delayed or disconnected.

## 5. Status Vocabulary

The UX should use the architecture and API status model:

```text
CREATED
QUEUED
RUNNING
SUCCESS
FAILED
CANCELLED
RETRYING
```

Older product text uses `PENDING` for a waiting job. In UI copy, waiting jobs should be displayed as `QUEUED` after job validation succeeds. If legacy data exposes `PENDING`, map it to a neutral waiting label until backend migration is complete.

