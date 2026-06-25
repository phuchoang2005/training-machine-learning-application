---
title: "Requirements MOC"
tags: [moc, requirements, ba, product]
aliases: [Requirements]
---

# Requirements — Map of Content

All requirement-level documents for the Future platform.

## Business & Product Requirements
- [[ba-refine]] — Business Requirement Specification: functional scope, stakeholders, job queue, audit, NFRs, acceptance criteria
- [[po-requirement]] — Product Requirements: vision, user stories, training lifecycle, notifications, history
- [[sa-refinement]] — Architecture Clarifications: agreed assumptions feeding SA phase (Docker isolation, queue, recovery, auth, artifacts)

## Non-Functional Requirements
- [[non-functional-requirements]] — Full NFR catalogue: CAP, PERF, REL, AVL, SEC, DATA, OBS, MAINT, TEST, UX, STO, COMP

## Scope Boundaries

### In Scope (MVP)
Authentication, project management (GitHub/ZIP), training job management (start/cancel/retry), real-time log streaming, artifact versioning, email notifications, audit logging, admin queue/user management.

### Out of Scope (MVP)
Distributed multi-node training, GPU scheduling, hyperparameter tuning, model deployment, auto-scaling, dataset upload management.

## Related
- [[_index]] — Home
- [[architecture-moc]] — How requirements become architecture decisions
