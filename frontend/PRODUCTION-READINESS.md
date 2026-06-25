# Frontend — Production Readiness Roadmap

> Derived strictly from `docs/` (the single source of truth): `po-requirement.md`, `ba-refine.md`,
> `solution-architect/low-level-design/non-functional-requirements.md` (NFR-\*), the frontend-architecture
> document, the `api-integration-and-client-side-caching-strategy/` and `performance-and-ux-optimization-design/`
> docs, and the OpenAPI contract under `low-level-design/api-contracts/`.
>
> **Current reality (verified):** the SPA runs **entirely on in-memory mock data** (`src/store/mock-data.ts`
> feeding Redux slices). The real integration layer exists but is **not wired into components**:
> `src/shared/api/axios-client.ts` and `src/shared/realtime/job-stream-client.ts` are referenced only by
> themselves. Auth is mock (`sessionStorage`). So the UI looks complete but talks to nothing. This file
> lists what the docs require to make it a real product, in priority order. Backend has a parallel file
> (`backend/PRODUCTION-READINESS.md`); the **OpenAPI contract + WebSocket envelope + dev-auth scheme** are
> the coordination boundary — change them deliberately and in sync.

---

## P0 — Connect to the real backend

### F1. Wire the API service layer

- **Target:** `api-integration-and-client-side-caching-strategy/` (api-service-layer, error-handling,
  retry-policies), NFR-COMP-002 (Axios), NFR-MAINT-002 (DTOs ↔ OpenAPI).
- **Now:** slices read/mutate mock state; `apiClient` is unused.
- **Do:** replace mock reads/mutations in slices and pages with `apiClient` calls per the doc's service-layer
  design — projects list/detail, config get/save, job start/cancel/retry, history, artifacts, admin views.
  Normalize failures to `ApiError` and apply the doc's retry policy. Remove mock fixtures from runtime paths
  (keep them for tests/Storybook only).

### F2. Wire the WebSocket job stream

- **Target:** frontend-architecture `realtime-state-flow`, NFR-PERF-002 (status ≤5s), NFR-PERF-003 (logs
  ≤5s), NFR-TEST-004 (reconnect, resume, duplicate handling).
- **Now:** `createJobStreamClient` exists but no component uses it.
- **Do:** on the Job Detail page, open the stream to `/api/v1/ws/jobs/:jobId`, dispatch `LOG`/`PROGRESS`/
  status events into the store, and use `lastEventId` to resume after reconnect. De-duplicate events by id.

### F3. Real authentication flow

- **Target:** PO §1, NFR-SEC-001 (Google Workspace OIDC is the production target; `/login/callback` route
  already exists). RBAC + ownership is enforced server-side (NFR-SEC-003) — the client only reflects it.
- **Now:** `src/store/session.ts` mock accounts + `sessionStorage` email; guards in `app/auth/guards.tsx`.
- **Do:** authenticate against the backend; send the bearer token on API + WS calls (dev: bearer = email,
  so the existing `user@example.com` / `admin@example.com` keep working). Wire `/login/callback` for the
  OIDC redirect so the switch to real auth is a drop-in. Keep `RequireAuth` / `AdminGuard` behavior.

---

## P1 — Resilience & the monitoring experience

### F4. Polling fallback + degraded states

- **Target:** NFR-COMP-005 (WebSocket primary, REST polling fallback), NFR-AVL-004 (show degraded state when
  WS/Docker/storage/email down), NFR-UX-005 (WS disconnect/reconnect visible on Job Detail).
- **Now:** neither exists (mock data never disconnects).
- **Do:** when the WS is unavailable, fall back to REST polling within the latency targets; surface a visible
  connection indicator and degraded banners.

### F5. Real-time log viewer features

- **Target:** PO §6, BA §16 (live append, scroll, **search/filter**, **download after completion**),
  NFR-UX-003 (long logs support scroll + search + download).
- **Now:** a log panel exists against mock data; verify it does live append + search + download against the
  backend log endpoints.
- **Do:** append streamed `LOG` events live; wire search/filter and post-completion download to the OpenAPI
  log endpoints.

### F6. Progress + status UX rules

- **Target:** NFR-UX-002 (`Progress Information Not Available` when no progress events), NFR-UX-001 (status
  must not rely on color alone), NFR-UX-004 (cancel/delete require confirmation).
- **Now:** mock progress is always present; confirm the empty-progress state, non-color status cues, and
  destructive-action confirmations exist.
- **Do:** render the explicit "not available" state, add text/icon (not color-only) status indicators, and
  confirmation dialogs for cancel and delete.

---

## P2 — Performance, accessibility, tests

### F7. Performance optimization

- **Target:** `performance-and-ux-optimization-design/` (code-splitting-and-lazy-loading, asset-optimization,
  runtime-ux-optimization), NFR-PERF-001 (dashboard initial load ≤3s).
- **Now:** single bundle; no route-level code splitting evident.
- **Do:** apply route-based lazy loading and the asset/runtime optimizations from the doc; validate against
  the measurement guidance (`measurement-and-validation.md`).

### F8. Accessibility & design-system conformance

- **Target:** NFR-UX-001, `design-system-and-component-specification/`, and the reference screens under
  `docs/ui-ux/ui/{user,admin}/{light,dark}/`.
- **Do:** reconcile components/states against the design-system spec and the SVG reference screens
  (login, dashboard, registration, detail, config editor, history, job detail, notifications; admin queue/
  users/audit), including light/dark.

### F9. Tests

- **Target:** NFR-TEST-005 (E2E: project registration, job start, live monitoring, cancel, retry, artifact
  download) plus unit coverage of store logic.
- **Now:** Vitest + Playwright are configured; coverage is minimal.
- **Do:** unit-test slices/selectors and the API/WS clients (mock transport with MSW, already a dep);
  add Playwright E2E for the flows above.

---

## Coordination contract (shared with backend — do not break unilaterally)

1. **OpenAPI is canonical** (`docs/.../api-contracts/openapi.yaml`). Domain types in
   `src/shared/api/types/` are **hand-authored to match it** — resync by hand whenever the backend changes a
   shape (and vice versa). Import app types only from `@/shared/api/types`.
2. **Status vocabulary:** PO §4 says `PENDING`, BA §5 says `QUEUED`, backend code uses
   `CREATED/QUEUED/RUNNING/SUCCESS/FAILED/CANCELLED/RETRYING`. **Reconcile to the OpenAPI contract** and use
   exactly that set in the UI — don't invent client-only statuses.
3. **WebSocket envelope:** `{ type, jobId, payload, occurredAt }` at `/api/v1/ws/jobs/:jobId`; the browser
   cannot set headers on WS, so pass the token via `?token=`. Handle `CONNECTED`, `LOG`, `PROGRESS`, and
   status-change types; support `lastEventId` resume.
4. **Dev-auth:** bearer token = user email; seeded `user@example.com` / `admin@example.com` (password
   `password` is used only by the dev login form). This keeps working until backend OIDC lands.
5. **Authz is server-enforced:** the client reflects RBAC/ownership for UX (hide/disable) but never relies on
   it for security — the backend is the gate (NFR-SEC-003/004).
