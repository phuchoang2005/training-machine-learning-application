# Frontend Low-Level Design

## Stack

Use React 19.2, TypeScript, Vite, Node.js 24 LTS, and a component-based SPA architecture. The frontend consumes REST APIs for commands and queries, and WebSocket for live job updates.

## Suggested Folder Structure

```text
src/
  app/
  api/
  auth/
  components/
  features/
    projects/
    jobs/
    logs/
    artifacts/
    admin/
  hooks/
  routes/
  types/
  utils/
```

## Main Routes

| Route | Screen |
| --- | --- |
| `/login` | Redirects to Google Workspace login when unauthenticated |
| `/projects` | Project dashboard with search and latest job status |
| `/projects/:projectId` | Project detail, configs, history, start training action |
| `/jobs/:jobId` | Training detail, status, progress, logs, artifacts |
| `/admin/projects` | Admin project management, without source/log/artifact access |

## State Management

Use React Query or TanStack Query for REST data fetching and cache invalidation. Keep local component state for forms and filters. Keep WebSocket job stream state inside a dedicated hook:

```text
useJobStream(jobId)
  status
  progress
  logs
  reconnecting
  lastEventId
```

## API Client

Create one typed API client around `fetch`:

* Attach credentials or bearer token.
* Parse common error envelope.
* Add correlation ID to client logs when present.
* Retry only safe idempotent `GET` requests.

## WebSocket Client Behavior

1. Connect to `/ws/jobs/{jobId}` on the training detail page.
2. Store the latest `eventId` received.
3. On disconnect, reconnect with exponential backoff.
4. Send `{ "type": "RESUME", "lastEventId": n }` after reconnect.
5. De-duplicate events by `eventId`.
6. Fall back to REST polling for status when reconnect fails repeatedly.

## Key Components

| Component | Responsibility |
| --- | --- |
| `ProjectTable` | List authorized projects and latest status |
| `ProjectDetailHeader` | Project metadata and action buttons |
| `YamlConfigEditor` | Full YAML editing with validation errors |
| `StartTrainingDialog` | Confirm config snapshot and submit job |
| `JobStatusBadge` | Stable visual mapping for job statuses |
| `ProgressPanel` | Show percent, elapsed time, and unavailable state |
| `LogViewer` | Append live logs, search historical logs server-side, download |
| `ArtifactList` | List and download artifacts for authorized users |
| `NotificationCenter` | In-app notification display |

## UI Rules

* Status labels must use the backend enum values.
* Progress shows `Progress Information Not Available` when no progress event exists.
* Log search calls the server; do not search only the browser buffer.
* Disable cancel for terminal jobs.
* Disable artifact download until artifacts are registered.
* Admin views must not expose source paths, detailed logs, or artifact download links unless ownership allows access.

## Validation

YAML validation happens server-side before job creation. The UI may run client-side syntax highlighting, but server validation is authoritative.
