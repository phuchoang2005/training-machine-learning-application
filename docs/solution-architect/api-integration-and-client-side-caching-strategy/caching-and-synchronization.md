# Caching and Synchronization

## Redux Slice Key Design

Redux slice keys and request cache keys must be deterministic and scoped by resource identifiers and filters.

```typescript
export const requestKeys = {
  authMe: 'auth/me',
  projectList: (filters: ProjectListFilters) => `projects/list/${stableHash(filters)}`,
  projectDetail: (projectId: string) => `projects/detail/${projectId}`,
  projectConfigs: (projectId: string) => `projects/${projectId}/configs`,
  jobHistory: (projectId: string, filters: JobListFilters) =>
    `jobs/project/${projectId}/${stableHash(filters)}`,
  jobDetail: (jobId: string) => `jobs/detail/${jobId}`,
  jobLogs: (jobId: string, filters: LogFilters) => `jobs/${jobId}/logs/${stableHash(filters)}`,
  jobArtifacts: (jobId: string) => `jobs/${jobId}/artifacts`,
  queue: 'jobs/queue',
  notificationList: (filters: NotificationFilters) => `notifications/${stableHash(filters)}`,
};
```

## Cache Policy Matrix

| Resource | Request | Stale Time | Retention | Refresh Trigger |
| --- | --- | ---: | ---: | --- |
| Current user | `/auth/me` | 5 minutes | Redux store session | App startup, focus, route guard. |
| Project list | `/projects` | 30 seconds | 10 minutes | Dashboard load, project mutation, focus. |
| Project detail | `/projects/{projectId}` | 30 seconds | 10 minutes | Detail load, start job, retry job. |
| Config list | `/projects/{projectId}/configs` | 60 seconds | 10 minutes | Project detail load, config changes. |
| Config content | `/projects/{projectId}/configs/{configId}` | 60 seconds | 10 minutes | Config tab load. |
| Job history | `/projects/{projectId}/jobs` | 10 seconds | 10 minutes | Project detail load, job mutation, WebSocket terminal event. |
| Job detail | `/jobs/{jobId}` | 5 seconds while active, 60 seconds when terminal | 10 minutes | Job detail load, WebSocket event, polling fallback. |
| Job logs | `/jobs/{jobId}/logs` | 0 seconds while active, 60 seconds when terminal | 10 minutes | Log viewer load, WebSocket missed-event resume, search/filter change. |
| Job artifacts | `/jobs/{jobId}/artifacts` | 60 seconds | 10 minutes | Terminal `SUCCESS`, artifact tab load. |
| Queue snapshot | `/jobs/queue` | 5 seconds | 2 minutes | Admin queue load, start/cancel/retry, polling. |
| Notifications | `/notifications` | 30 seconds | 10 minutes | Notification menu open, mark-read mutation, focus. |
| Admin users | `/admin/users` | 60 seconds | 10 minutes | Admin page load, status mutation. |
| Audit logs | `/audit-logs` | 60 seconds | 10 minutes | Audit page load, filter change. |

Terminal job statuses are `SUCCESS`, `FAILED`, and `CANCELLED`. Active job statuses are `CREATED`, `QUEUED`, `RUNNING`, and `RETRYING`.

## Redux Refresh Rules

| Mutation | Refresh | Direct Redux Update |
| --- | --- | --- |
| Create GitHub project | Project list | Add created project to first dashboard page only when filters allow it. |
| Upload ZIP project | Project list | Do not store upload body in Redux. |
| Validate YAML | None | Store validation result in form state, not Redux domain state. |
| Start training job | Project detail, project job history, job detail, queue snapshot | Seed new job detail from start response. |
| Cancel job | Job detail, project job history, queue snapshot | Mark cancel request pending until backend confirms status. |
| Retry job | Original job detail, new job detail, project job history, queue snapshot | Seed new job detail from retry response. |
| Mark notification read | Notification list | Mark item read optimistically, rollback on failure. |
| Update user status | Admin users | Update row optimistically, rollback on failure. |

## REST and WebSocket Roles

| Mechanism | Responsibility |
| --- | --- |
| REST | Initial resource load, pagination, search, downloads, mutations, reconnect recovery. |
| WebSocket | Live job status, progress, and log event updates. |
| Polling fallback | Job detail and logs when WebSocket is unavailable. |

## Job Detail Synchronization Flow

```text
Open /jobs/{jobId}
  -> Fetch job detail through REST
  -> Fetch initial logs through REST
  -> Connect to job WebSocket
  -> Dispatch status, progress, and log events into Redux slices
  -> Track last applied event ID or log offset
  -> On disconnect, reconnect with backoff
  -> On reconnect, fetch missed logs with last seen cursor or offset
  -> If reconnect fails, poll job detail and logs
  -> On terminal status, stop active polling and refetch artifacts
```

## WebSocket Event Application

| Event Type | Cache Update |
| --- | --- |
| `job.status.changed` | Update `jobSlice.byId[jobId]` and refresh project job history when needed. |
| `job.progress.updated` | Update `jobSlice.byId[jobId].progress`. |
| `job.log.appended` | Append to active log buffer and update log slice when matching filters. |
| `job.terminal` | Refetch job detail, project job history, queue snapshot, artifacts, and notifications. |
| `heartbeat` | Update connection status only. |
| `error` | Show connection or authorization state; do not overwrite job state without REST confirmation. |

## Polling Fallback

| Condition | Polling Behavior |
| --- | --- |
| WebSocket connecting | Do not poll for first 5 seconds. |
| WebSocket reconnecting | Poll job detail every 10 seconds. |
| WebSocket unavailable | Poll active job detail every 5 seconds and logs every 5 seconds with `after=lastSeen`. |
| Job terminal | Stop polling after final REST confirmation. |
| Browser tab hidden | Reduce active polling to every 30 seconds. |

## Pagination, Search, and Filtering

| Resource | Pagination Rule |
| --- | --- |
| Project list | Preserve search and filters in URL query params. |
| Job history | Cursor pages are scoped by `projectId` and filters. |
| Logs | Use cursor or offset for initial page, missed event recovery, search, and stream type filters. |
| Notifications | Cursor pages are scoped by notification status filter. |
| Audit logs | Cursor pages are scoped by user, resource, action, and time filters. |

Search and filter values should be part of the Redux request key. Local-only transient input can be debounced before updating the URL and request key.
