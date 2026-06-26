---
title: "Frontend Architecture"
tags: [frontend, architecture, react, redux, vite]
aliases: [Frontend Architecture, SPA Architecture, frontend-architecture]
related:
  - "[[ADR-002]]"
  - "[[ADR-003]]"
  - "[[ADR-008]]"
  - "[[ADR-011]]"
  - "[[ADR-012]]"
  - "[[ADR-013]]"
  - "[[ADR-014]]"
  - "[[frontend-architecture-context]]"
  - "[[api-integration-flow]]"
  - "[[realtime-state-flow]]"
  - "[[route-guard-flow]]"
  - "[[ux-overview]]"
---

# Frontend Architecture Document

## 1. Purpose

This document defines the frontend architecture for the Future MVP. It covers the React application structure, state management design, routing architecture, API integration, WebSocket monitoring, security boundaries, and test strategy.

The frontend is a TypeScript single-page application built with React, Vite, Redux, Axios, TailwindCSS, Radix UI, and shadcn/ui, aligned with ADR-002, ADR-003, ADR-008, ADR-011, ADR-012, ADR-013, and ADR-014.

## 2. Architecture Context

The frontend provides authenticated web workflows for:

* Viewing authorized projects.
* Registering GitHub or ZIP-based projects.
* Viewing and editing YAML training configuration.
* Starting, cancelling, and retrying training jobs.
* Monitoring real-time status, progress, and logs.
* Reviewing job history, notifications, artifacts, and administrative views.

Backend authorization remains authoritative for all REST endpoints and WebSocket subscriptions. The frontend must hide unavailable actions based on user role and resource ownership, but it must never treat UI checks as security enforcement.

Diagram: [[frontend-architecture-context]]

## 3. Technology Baseline

| Concern | Decision |
| --- | --- |
| Framework | React with TypeScript |
| Build tool | Vite |
| Routing | React Router |
| State management | Redux Toolkit with typed slices, async thunks or listener middleware |
| HTTP client | Axios shared instance with request and response interceptors |
| Styling | Hand-authored semantic CSS classes (`src/assets/styles/*.css`) with TailwindCSS v4 wired up for the remaining shadcn-style primitive |
| UI primitives | Radix UI (`@radix-ui/react-tabs`) for the tabs primitive |
| Component baseline | shadcn-style `Tabs` owned in the repository; other primitives are plain semantic-CSS components |
| Motion | Plain CSS keyframes (`fade-in-up`, `dialog-in`) for route, dialog, and list transitions, gated behind `prefers-reduced-motion` |
| Theme | System-driven light mode and dark mode through `prefers-color-scheme` |
| Form state | Controlled React state or a lightweight form helper integrated with Redux actions where needed |
| Schema validation | Runtime validators generated from or aligned with OpenAPI when needed |
| Real-time updates | Browser WebSocket API with REST polling fallback |
| API contract | OpenAPI under `docs/solution-architect/low-level-design/api-contracts/openapi.yaml` |
| Test approach | Unit tests, component tests, contract-aware API mocks, end-to-end tests |
| Static container | Frontend-owned Nginx image built by `frontend/Dockerfile` |

## 4. Proposed Folder Structure

The frontend code uses route/page modules with shared platform modules under `src/app`, `src/pages`, `src/shared`, and `src/store`. Human-maintained source files should stay small and focused, with page composition split into widgets before files grow beyond roughly 50 lines. Generated OpenAPI output is exempt from that line-size guideline.

```text
frontend/
  package.json
  package-lock.json
  vite.config.ts
  eslint.config.js
  playwright.config.ts
  components.json
  Dockerfile
  docker-compose.yml
  tsconfig.json
  index.html
  nginx/
    default.conf.template
  src/
    main.tsx
    app/
      App.tsx
      auth/
        guards.tsx
      layout/
        AppShell.tsx
        Brand.tsx
        NavLinks.tsx
        Topbar.tsx
        UserPanel.tsx
    assets/
      styles/
        global.css
        base.css
        layout.css
        controls.css
        tables.css
        workflows.css
        auth-media.css
        tailwind.css
    shared/
      api/
        axios-client.ts
        types.ts
        generated/
          types.ts
        types/
          job.ts
          project.ts
          support.ts
          user.ts
      realtime/
        job-stream-client.ts
        job-stream-types.ts
      components/
        Badges.tsx
        CelestialBackground.tsx
        Dialog.tsx
        Feedback.tsx
        Form.tsx
        Page.tsx
      ui/
        button.tsx
        dialog.tsx
        tabs.tsx
      motion/
        variants.ts
      format/
        formatters.ts
      utils/
        cn.ts
    store/
      store.ts
      hooks.ts
      mock-data.ts
      session.ts
      mock/
        jobs.ts
        projects.ts
        support.ts
        time.ts
        users.ts
      slices/
        authSlice.ts
        jobSlice.ts
        projectSlice.ts
        supportSlices.ts
    pages/
      ErrorPage.tsx
      auth/
        AuthPage.tsx
        LoginCallbackPage.tsx
        LoginPage.tsx
        RegisterAccountPage.tsx
      jobs/
        ActionToolbar.tsx
        CancelJobDialog.tsx
        JobDetailPage.tsx
        JobSidebar.tsx
        LogPanel.tsx
      projects/
        ConfigEditor.tsx
        ProjectDashboardPage.tsx
        ProjectDetailPage.tsx
        ProjectTable.tsx
        RegisterProjectPage.tsx
        TrainingHistory.tsx
      notifications/
        NotificationListPage.tsx
      admin/
        AdminAuditPage.tsx
        AdminQueuePage.tsx
        AdminUsersPage.tsx
    tests/
      setup.ts
      app.test.tsx
      e2e/
        smoke.spec.ts
      mocks/
        handlers.ts
        server.ts
```

### Folder Responsibilities

| Folder | Responsibility |
| --- | --- |
| `app/` | Application route table, route guards, shell layout, navigation, and top bar composition. |
| `shared/api/` | Axios client, hand-authored domain types, error normalization, request correlation handling. |
| `shared/realtime/` | WebSocket connection lifecycle, reconnect, duplicate-event protection, polling fallback helpers. |
| `shared/components/` | App-specific reusable UI composition such as badges, feedback, page frames, and dialogs. |
| `shared/ui/` | shadcn-style `Tabs` primitive (Radix) owned by the repository. |
| `store/` | Redux root store, typed hooks, domain slices, development session helpers, and fixture data. |
| `pages/` | Route-level pages split by workflow and further decomposed into small widgets. |
| `tests/` | Frontend test fixtures and API mocks. |

### Applied Tooling

The frontend package applies the documented stack through:

* Hand-authored semantic CSS in `src/assets/styles/*.css` (aggregated by `global.css`), with TailwindCSS v4 via `@tailwindcss/vite` and theme tokens in `src/assets/styles/tailwind.css`.
* The Radix-based shadcn-style `Tabs` wrapper under `src/shared/ui/`.
* React 19 with TypeScript, Vite, Redux Toolkit, Axios, and React Router.
* ESLint flat config, Vitest/jsdom component tests, MSW-ready API mocking dependencies, and Playwright E2E configuration.
* Hand-authored domain types under `src/shared/api/types/`, kept aligned with the OpenAPI contract by hand.
* Browser WebSocket client scaffolding under `src/shared/realtime/job-stream-client.ts`.
* Focused source modules: route pages live under `src/pages`, layout under `src/app/layout`, reusable UI composition under `src/shared/components`, Redux slices under `src/store/slices`, and CSS is split into small files imported by `global.css`.

## 5. Frontend Module Model

Diagram: [[frontend-module-model]] (`diagrams/frontend-module-model.puml`)

## 6. State Management Design

Frontend state is split into Redux-managed domain state, client UI state, form state, and real-time stream state.

| State Category | Examples | Owner | Persistence |
| --- | --- | --- | --- |
| Server-backed domain state | Current user, projects, configs, jobs, logs, artifacts, notifications, queue snapshot | Redux slices | In memory; refreshed through Axios services and WebSocket events |
| Client UI state | Active tabs, dialogs, table filters, local search text, sidebar state | React component state or Redux `ui` slice | In memory or URL query params |
| Form state | Project registration fields, YAML editor draft, retry mode, cancellation reason | Form library + component state | In memory until submitted |
| Real-time stream state | WebSocket status, last event ID, appended log buffer, reconnect attempts | `shared/realtime` and job detail feature | In memory; resumed from REST |
| Auth session state | Current user profile, role, session loading state | Redux `auth` slice hydrated by `/auth/me` | Server-owned session or token |
| Theme state | System theme mode, resolved light or dark theme | Redux `theme` slice and document root class | System preference by default |

### Redux State Rules

* Every API resource has a stable Redux slice or normalized entity map under its entity module.
* Mutations refresh only the affected slices.
* Start, cancel, and retry mutations update job detail optimistically only for pending UI state; final status must come from REST or WebSocket.
* API errors must be normalized into a shared `ApiError` shape with `code`, `message`, `correlationId`, and optional field details.
* Downloads for logs and artifacts should use direct authorized backend endpoints and should not load large files fully into memory.

### Redux Slice Shape

```typescript
type EntityRequestState = {
  status: 'idle' | 'loading' | 'succeeded' | 'failed';
  error?: ApiError;
  lastFetchedAt?: string;
};

type JobSliceState = {
  byId: Record<string, JobDetail>;
  idsByProjectId: Record<string, string[]>;
  logsByJobId: Record<string, LogBufferState>;
  artifactsByJobId: Record<string, ArtifactSummary[]>;
  queue?: QueueSnapshot;
  requests: Record<string, EntityRequestState>;
};
```

### Mutation Effects

| Mutation | Endpoint | Redux Effect | Navigation Effect |
| --- | --- | --- | --- |
| Register GitHub project | `POST /projects` | Add optimistic "building" entry to project list, replace with created project on success / flip to "build failed" on error | Return to project dashboard immediately to watch build status |
| Upload ZIP project | `POST /projects/upload-zip` | Add optimistic "building" entry to project list, replace with created project on success / flip to "build failed" on error | Return to project dashboard immediately to watch build status |
| Validate config | `POST /projects/{projectId}/configs/validate` | Store validation result in form-local state | Show validation result inline |
| Start training | `POST /projects/{projectId}/jobs` | Upsert new job, refresh project jobs and queue | Open job detail |
| Cancel job | `POST /jobs/{jobId}/cancel` | Set local cancel-requested state, then refresh job and queue | Stay on job detail |
| Retry job | `POST /jobs/{jobId}/retry` | Upsert retry job, refresh original job, project jobs, and queue | Open new job detail |
| Mark notification read | `POST /notifications/{notificationId}/read` | Optimistically mark notification read, rollback on failure | Stay in current view |
| Disable user | `POST /admin/users/{userId}/status` | Optimistically update admin user row, rollback on failure | Stay in admin console |

### Real-Time State Flow

Diagram: [[realtime-state-flow]]

### Job Stream Event Handling

The `Job Detail` feature should process events using idempotent handlers. Each incoming event should include or derive a monotonic sequence, timestamp, or log offset. Duplicate events are ignored.

```text
Incoming event
  -> validate shape
  -> check jobId matches active route
  -> check event id or offset has not been applied
  -> dispatch Redux actions for status/progress
  -> append log line to in-memory log buffer
  -> refetch terminal resources when status enters SUCCESS, FAILED, or CANCELLED
```

## 7. Routing Architecture

Routes are grouped by authentication requirement, ownership-sensitive resources, and administrator-only features.

```text
/
  /login
  /register
  /login/callback
  /projects
  /projects/new
  /projects/:projectId
  /projects/:projectId/configs/:configId
  /projects/:projectId/jobs/:jobId
  /jobs/:jobId
  /notifications
  /admin/users
  /admin/queue
  /403
  /404
```

### Route Table

| Route | Component | Guard | Data Dependencies |
| --- | --- | --- | --- |
| `/login` | `LoginPage` | Public route | Development sample accounts or company login entry point |
| `/register` | `RegisterAccountPage` | Public development-only route | Development onboarding form |
| `/login/callback` | `LoginCallbackPage` | Public callback route | Auth provider result |
| `/projects` | `ProjectDashboardPage` | Authenticated | `/auth/me`, `/projects` |
| `/projects/new` | `RegisterProjectPage` | Authenticated user, non-admin create rule | `/auth/me` |
| `/projects/:projectId` | `ProjectDetailPage` | Authenticated; backend verifies ownership | `/projects/{projectId}`, configs, jobs |
| `/projects/:projectId/configs/:configId` | `ProjectDetailPage` config tab | Authenticated; backend verifies ownership | `/projects/{projectId}/configs/{configId}` |
| `/projects/:projectId/jobs/:jobId` | Redirect to canonical job route or nested job detail | Authenticated; backend verifies ownership | `/jobs/{jobId}` |
| `/jobs/:jobId` | `JobDetailPage` | Authenticated; backend verifies ownership or admin cancel scope | `/jobs/{jobId}`, logs, artifacts, WebSocket |
| `/notifications` | `NotificationListPage` | Authenticated | `/notifications` |
| `/admin/users` | `UserManagementPage` | Admin role | `/admin/users` |
| `/admin/queue` | `QueueMonitorPage` | Admin role | `/jobs/queue` |
| `/403` | `ForbiddenPage` | Public | None |
| `/404` | `NotFoundPage` | Public | None |

### Route Guard Flow

Diagram: [[route-guard-flow]]

### URL State

Use URL query parameters for state that should be shareable or restorable:

* Project search text and filters.
* Training history pagination cursor and status filter.
* Log search text, stream type filter, and cursor.
* Notification status filter.

Use component state for short-lived state:

* Dialog open or closed.
* Draft cancellation reason.
* YAML editor dirty state before submit.
* Current WebSocket reconnect banner state.

## 8. API Integration Design

The API client must be generated from or manually aligned with the OpenAPI contract. The shared Axios client should provide:

* Shared Axios instance.
* Base URL `/api/v1`.
* Credentials or bearer token support based on backend authentication mode.
* JSON request and response handling.
* Multipart upload for ZIP project registration.
* Download helper for logs and artifacts.
* Correlation ID extraction from error responses.
* Standard handling for `401`, `403`, `404`, `409`, `413`, `415`, and platform-busy responses.

Diagram: [[api-integration-flow]]

## 9. Authorization and UI Permission Design

Client-side permission helpers improve UX by hiding invalid actions, but backend checks remain mandatory.

| UI Action | User Visibility Rule | Backend Enforcement |
| --- | --- | --- |
| View project list | Authenticated user | `/projects` filters authorized projects |
| Create project | `USER` role | `POST /projects`, `POST /projects/upload-zip` |
| View project detail | Project owner or project member | `GET /projects/{projectId}` |
| Edit YAML config | Project owner or project member | Config endpoints |
| Start training | Project owner or project member | `POST /projects/{projectId}/jobs` |
| Cancel own job | Job owner or authorized project member | `POST /jobs/{jobId}/cancel` |
| Cancel any running job | `ADMIN` role | `POST /jobs/{jobId}/cancel` |
| Retry job | Project owner or project member | `POST /jobs/{jobId}/retry` |
| View logs | Project owner or project member | REST logs endpoint and WebSocket subscription |
| Download artifact | Project owner or project member | Artifact download endpoint |
| Manage users | `ADMIN` role | `/admin/users` endpoints |

## 10. Page Composition

### Project Dashboard

Primary components:

* Project search and status filters.
* Authorized project table.
* Latest training status badge using `CREATED`, `QUEUED`, `RUNNING`, `SUCCESS`, `FAILED`, `CANCELLED`, and `RETRYING`.
* Last training time and owner.
* Empty state for no authorized projects.

### Project Detail

Primary components:

* Project summary panel.
* Git branch and dataset information.
* YAML configuration editor with validation.
* Training history table.
* Start training dialog that submits immutable configuration content.

### Job Detail

Primary components:

* Status, queue position, progress, duration, trigger owner, and retry relationship.
* WebSocket connection indicator.
* Log viewer with append, scroll, search, filter, and download.
* Cancel and retry actions with confirmation.
* Artifact list after terminal success.

### Admin Console

Primary components:

* User list and status management.
* Queue snapshot showing running count, running limit, queued count, and queued jobs.
* Admin views must not expose source code, detailed logs, or artifacts unless the backend authorizes the admin as project owner.

## 11. Error Handling Design

| Error Type | UI Behavior |
| --- | --- |
| `401 Unauthenticated` | Clear current session cache and redirect to company login. |
| `403 Forbidden` | Show forbidden page or inline permission message for resource actions. |
| `404 Not Found` | Show not found page for route resources; show inline row state for nested resources. |
| Validation error | Map field errors to the relevant form fields. |
| Conflict | Show contextual message, refresh related Redux slice, and require user confirmation before retry. |
| Platform busy | Show clear capacity message and refresh queue snapshot when relevant. |
| WebSocket disconnect | Show degraded connection state, reconnect with backoff, then fall back to REST polling. |
| Unknown server error | Show retryable error state with correlation ID. |

## 12. Testing Strategy

| Test Type | Scope |
| --- | --- |
| Unit tests | Permission helpers, Redux request key factories, status formatting, duration utilities, event dedupe. |
| Component tests | Project dashboard, config editor validation, job status panel, log viewer, dialogs. |
| API mock tests | Success and error paths for project, job, log, artifact, notification, and admin endpoints. |
| WebSocket tests | Connect, reconnect, resume from last event, duplicate event ignore, fallback polling. |
| End-to-end tests | Login flow, project registration, start training, monitor job, cancel job, retry job, download artifact. |
| Accessibility checks | Status visibility without color-only meaning, keyboard dialog controls, log viewer focus handling. |

## 13. Implementation Guidelines

* Keep backend DTOs and frontend types aligned with OpenAPI.
* Keep Redux slices small, domain-scoped, and selector-driven.
* Reuse the existing semantic CSS classes in `src/assets/styles/*.css` before adding new component styles.
* Use CSS keyframe transitions for motion patterns that improve orientation, perceived responsiveness, or emotional warmth; do not animate high-frequency log lines or large tables.
* Initialize light or dark mode from `prefers-color-scheme` and update when the system preference changes.
* Use route-level code splitting for large feature areas such as job detail and admin console.
* Keep large log streams virtualized or windowed to avoid slow browser rendering.
* Keep YAML editor drafts local until validation or start-training submission.
* Store no secrets, filesystem paths, or infrastructure credentials in browser storage.
* Prefer backend-provided status and authorization results over inferred client state.
* Show `Progress Information Not Available` when a job has no emitted progress data.
* Require confirmation for destructive actions such as cancel and delete.
* Treat WebSocket as the primary monitoring path and REST polling as degradation support.

### Development Login and Registration Bootstrap

Until Google Workspace/OIDC is configured, the frontend may provide a non-production login and registration phase for Docker validation. This phase must be visibly labeled as development-only and must not weaken backend authorization.

Sample accounts:

| Role | Email | Password | Backend bearer token |
| --- | --- | --- | --- |
| User | `user@example.com` | `password` | `user@example.com` |
| Admin | `admin@example.com` | `password` | `admin@example.com` |

Frontend behavior:

* `/login` accepts the sample accounts above and creates a client session for local UI validation.
* `/register` accepts a name, email, password, and role for non-production onboarding flow validation.
* Registered accounts are local to the frontend mock session until a backend registration endpoint is introduced.
* Authenticated API calls should use the selected account email as the development bearer token when calling the current backend.
* Production builds must replace this phase with company SSO/OIDC and backend-managed sessions.

## 14. Motion (CSS)

Motion is implemented with plain CSS keyframes defined alongside the rest of the styles, not a JavaScript animation library. Motion should support the Pleiades/Sirius visual direction while keeping the product usable for dense engineering workflows.

### Ownership and Constraints

| Concern | Requirement |
| --- | --- |
| Definition | Keep shared keyframes (`fade-in-up`, `dialog-in`, `drift`) in `src/assets/styles/layout.css`; apply them through the existing semantic classes (`.page`, `.table-row`, `.dialog`). |
| Reduced motion | Disable animations under `@media (prefers-reduced-motion: reduce)`; movement falls back to static states. |
| Performance | Animate `opacity`, `transform`, and lightweight background layers only. Avoid animating layout-heavy table rows in bulk, log lines, or thousands of DOM nodes. |

### Recommended Motion Patterns

| Pattern | Use Cases | Guidance |
| --- | --- | --- |
| Route fade and lift | Login, dashboard, project detail, job detail route changes. | `fade-in-up`: `opacity: 0 -> 1`, `translateY: 8px -> 0`, `duration: 0.22s`. |
| Dialog fade-scale | Start training, cancel, retry confirmations. | `dialog-in`: subtle scale from `0.98 -> 1` with fade; never bounce destructive confirmations. |
| Background drift | Light-mode aurora and star-field accents. | `drift`: very slow transform drift only; must not compete with data surfaces. |
| List enter | Notifications, artifacts, project cards on filtered views. | Reuse `fade-in-up` on rows; keep durations short on dense tables. |

### Keyframe Example

```css
@keyframes fade-in-up {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: none; }
}

.page { animation: fade-in-up 0.22s cubic-bezier(0.16, 1, 0.3, 1) both; }

@media (prefers-reduced-motion: reduce) {
  .page, .table-row, .dialog { animation: none; }
}
```

## 15. Open Decisions

| Decision | Recommendation |
| --- | --- |
| Auth storage mode | Prefer secure HTTP-only session cookie if backend supports it; otherwise keep bearer tokens out of local storage. |
| Generated API client | Hand-authored types in `src/shared/api/types/` are the current source; revisit OpenAPI codegen only if drift becomes a maintenance burden. |
| Log rendering library | Use virtualization once log size exceeds normal DOM rendering limits. |
| YAML editor | Use a lightweight code editor with YAML syntax support and validation feedback. |
| WebSocket protocol shape | Define event schema for status, progress, log, terminal, heartbeat, and error events before implementation. |
| Motion depth | Start with shared CSS keyframes for pages, dialogs, and lists; expand only after usability testing confirms value. |
