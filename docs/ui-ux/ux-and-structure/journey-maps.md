---
title: "User Journey Maps"
tags: [ux, journey-maps, users]
aliases: [Journey Maps, User Journeys, journey-maps]
related:
  - "[[personas]]"
  - "[[user-flows]]"
  - "[[primary-user-flow-diagram]]"
  - "[[ux-overview]]"
---

# User Journey Maps

## 1. AI Engineer Journey: Launch and Monitor Training

| Stage | User Action | User Question | UX Requirement | Risk or Pain Point |
| --- | --- | --- | --- | --- |
| Authenticate | Opens platform and signs in with company credentials. | Can I access my projects? | Fast login, clear loading state, redirect to intended page. | Session failure can block urgent training. |
| Find project | Searches or filters the project dashboard. | Which project needs training? | Search, status filters, latest status, last training owner. | Too many projects make scanning slow. |
| Review project | Opens project detail and checks source, dataset, and config. | Is this the correct setup? | Project summary, repository, branch, dataset version, editable configuration snapshot. | Starting with wrong branch or dataset wastes compute. |
| Launch job | Opens start dialog and confirms configuration. | What exactly will run? | Confirmation summary with branch, dataset, overrides, and expected queue behavior. | Hidden defaults reduce trust. |
| Queue wait | Lands on job detail with `CREATED` or `QUEUED` status. | Is my job accepted and when will it run? | Queue position, status badge, timestamps, refresh or real-time state. | Waiting state can look like a failure if unclear. |
| Monitor | Watches status, progress, and logs during `RUNNING`. | Is training healthy? | Real-time log streaming, progress, elapsed time, auto-scroll controls, connection banner. | WebSocket disconnect may hide progress. |
| React to failure | Receives failed status or email notification. | Why did it fail and what should I do next? | Error summary, highlighted error logs, retry action, log download. | Raw logs may be too noisy. |
| Complete | Reviews `SUCCESS` result and artifacts. | Where are the outputs? | Artifact list, metadata, download actions, history link. | Artifact names may be ambiguous. |

## 2. Project Owner Journey: Review Project Health

| Stage | User Action | User Question | UX Requirement | Risk or Pain Point |
| --- | --- | --- | --- | --- |
| Enter project | Opens project from dashboard or notification link. | What is the current project state? | Project header with latest job status and owner. | Latest job can be confused with best job. |
| Review history | Opens training history tab. | How often do jobs succeed or fail? | Sortable history by status, time, owner, duration. | Missing filters make audits slow. |
| Inspect result | Opens a specific job detail. | What happened in this run? | Status timeline, config snapshot, logs, artifacts. | Without config snapshots, runs are not reproducible. |
| Download artifact | Selects a model artifact. | Is this the artifact I need? | Artifact name, job ID, created time, file size, checksum if available. | Wrong artifact download can affect downstream use. |
| Check access | Reviews allowed users or asks admin support. | Who can run jobs for this project? | Clear access state or project membership area when in scope. | Cross-project access is a governance risk. |

## 3. Platform Administrator Journey: Manage Queue and Access

| Stage | User Action | User Question | UX Requirement | Risk or Pain Point |
| --- | --- | --- | --- | --- |
| Open admin console | Navigates to admin area. | Is the platform healthy? | Admin landing with queue snapshot and recent failures. | Admin screens should not expose unnecessary project details. |
| Inspect queue | Opens queue monitor. | Which jobs are waiting or running? | Queue table with status, project, owner, created time, queue position, duration. | Stuck jobs need fast identification. |
| Triage issue | Opens job details from queue. | Why is this job failing or stalled? | Troubleshooting view with logs, status history, and correlation ID. | Missing audit context slows incident response. |
| Take action | Cancels a job or changes user role. | Is this action allowed and recorded? | Confirmation dialog, reason field, audit metadata. | Accidental destructive actions affect users. |
| Follow up | Confirms queue recovery and notifies user. | Did the system recover? | Updated queue state and job status stream. | Users may not know why their job was cancelled. |

## 4. Cross-Journey UX Requirements

* Deep links from notifications must return users to the correct job detail page after authentication.
* Dashboard and project detail views must never expose unauthorized projects.
* Job detail pages must handle browser refresh without losing status, logs, or artifact access.
* Terminal statuses are `SUCCESS`, `FAILED`, and `CANCELLED`.
* Active statuses are `CREATED`, `QUEUED`, `RUNNING`, and `RETRYING`.
* Every action that starts, cancels, retries, or downloads must show clear feedback.

