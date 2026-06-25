---
title: "Information Architecture"
tags: [ux, navigation, information-architecture]
aliases: [Information Architecture, IA, information-architecture]
related:
  - "[[information-architecture-diagram]]"
  - "[[ux-overview]]"
  - "[[route-guard-flow]]"
  - "[[frontend-architecture]]"
---

# Information Architecture

## 1. IA Goals

The information architecture organizes the platform around the user's operational context: projects, training jobs, artifacts, notifications, and administration. Navigation should make the current project and job state clear without requiring users to understand backend services.

Diagram: [information-architecture.mermaid](diagrams/information-architecture.mermaid)

## 2. Top-Level Navigation

| Navigation Item | Primary Audience | Purpose | Default Visibility |
| --- | --- | --- | --- |
| Projects | AI Engineer, Project Owner | Discover authorized projects and latest training status. | All authenticated users. |
| Notifications | All users | Review job success, failure, and delivery notices. | All authenticated users. |
| Admin | Platform Administrator | Manage queue, users, and platform operations. | Admin users only. |
| Account | All users | View signed-in user, role, and logout action. | All authenticated users. |

## 3. Route Structure

```text
/
  /login
  /register
  /auth/callback
  /projects
  /projects/register
  /projects/:projectId
  /projects/:projectId/configuration
  /projects/:projectId/history
  /projects/:projectId/jobs/:jobId
  /projects/:projectId/jobs/:jobId/logs
  /projects/:projectId/jobs/:jobId/artifacts
  /notifications
  /admin
  /admin/queue
  /admin/users
  /admin/audit
  /403
  /404
```

## 4. Page Inventory

| Page | Core Content | Primary Actions | State Dependencies |
| --- | --- | --- | --- |
| Login | Company authentication entry point with non-production sample account affordance. | Sign in, open registration. | Auth provider availability, development sample account availability. |
| Register Account | Non-production onboarding validation before SSO/OIDC is enabled. | Create local development session, return to login. | Development-only account bootstrap policy. |
| Project Dashboard | Authorized projects, latest status, filters, search. | Open project, register project when permitted. | Current user, project list. |
| Register Project | GitHub or ZIP source details, project metadata. | Submit registration, cancel. | User permission, validation rules. |
| Project Detail | Project summary, repository, dataset, latest job, tabs. | Start training, open config, open history. | Project access, latest job. |
| Configuration | YAML or structured training configuration. | Edit override, validate, save snapshot for launch. | Project config, validation schema. |
| Training History | Job list by project with status, owner, time, duration. | Open job detail, retry eligible job. | Project job history. |
| Job Detail | Status, progress, queue position, logs, actions, artifacts. | Cancel, retry, download logs, download artifacts. | Job, logs, WebSocket or polling. |
| Notifications | Notification list, read state, linked job or project. | Open linked job, mark read. | Notification state. |
| Admin Queue | Running, queued, retrying, and failed jobs. | Open job, cancel with reason. | Admin role, queue snapshot. |
| Admin Users | Users, roles, project access summary. | Change role, disable user where supported. | Admin role, RBAC policy. |
| Admin Audit | Action history and correlation IDs. | Filter, inspect record. | Audit log access. |
| Forbidden | Access denied explanation. | Return to projects. | Authorization failure. |
| Not Found | Missing route or missing resource. | Return to projects. | Routing or resource lookup. |

## 5. Content Model

| Entity | UX-Owned Display Fields | Related Screens |
| --- | --- | --- |
| User | Name, email, role, assigned projects. | Account, Admin Users, job owner fields. |
| Project | Name, description, repository, dataset, latest status, owner list. | Dashboard, Project Detail, Admin Users. |
| Training Configuration | Branch, dataset version, hyperparameters, snapshot timestamp. | Configuration, Start Training dialog, Job Detail. |
| Training Job | Job ID, project, status, owner, start time, end time, duration, queue position, failure summary. | History, Job Detail, Admin Queue, Notifications. |
| Log | Timestamp, level, message, source stream, downloadable file. | Job Detail, Logs tab, failure summary. |
| Artifact | Name, job ID, type, size, created time, checksum if available, download URL. | Job Detail, Artifacts tab, Project Detail. |
| Notification | Type, status, recipient, linked job, timestamp, read state. | Notifications, notification menu. |
| Audit Record | Actor, action, target, timestamp, reason, correlation ID. | Admin Audit, destructive action review. |

## 6. Navigation Rules

* The authenticated default route is `/projects`.
* Unauthenticated users must land on `/login`.
* `/register` is public in non-production only and must be disabled or replaced by SSO/OIDC onboarding in production.
* Notification links should deep-link to `/projects/:projectId/jobs/:jobId`.
* Project routes must enforce project membership or ownership.
* Admin routes must be hidden and blocked for non-admin users.
* Job detail should remain reachable from dashboard, project history, notification, and admin queue links.
* A browser refresh on any authenticated route must rehydrate session and reload required domain data.

## 7. Layout Structure

| Viewport | Navigation Pattern | Content Pattern |
| --- | --- | --- |
| Mobile | Top bar with menu drawer. | Single-column task flow; tables become stacked summaries. |
| Tablet | Top bar or collapsible navigation. | Two-column panels only when content remains readable. |
| Desktop | Persistent sidebar with top utility area. | Dense operational tables and split job-monitoring layout. |
