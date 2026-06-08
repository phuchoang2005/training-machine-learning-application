# Code Splitting and Lazy Loading Strategy

## Route-Level Splitting

Each major route should be split into its own async chunk. The app shell, route guards, Redux provider, shared Axios client, base shadcn/ui wrappers, Radix primitives used by the shell, and current-user bootstrap should remain in the initial application bundle.

| Route Area | Loading Strategy | Reason |
| --- | --- | --- |
| App shell and route guards | Initial bundle | Needed for all authenticated views. |
| Project dashboard | Initial authenticated route chunk | Primary entry point after login. |
| Project registration | Lazy route chunk | Used less frequently than dashboard. |
| Project detail | Lazy route chunk with prefetch from dashboard row hover/focus | Common after dashboard selection. |
| Job detail | Lazy route chunk with prefetch after start training response | Heavy route with log viewer and WebSocket client usage. |
| Notifications page | Lazy route chunk | Secondary workflow. |
| Admin users | Lazy route chunk gated by role | Admin-only code should not be loaded for normal users. |
| Admin queue | Lazy route chunk gated by role | Admin-only and lower frequency. |
| Error pages | Small shared chunk | Must render quickly on authorization and navigation failures. |

## Component-Level Lazy Loading

Heavy components inside a route should load on demand.

| Component | Lazy Load Trigger | Fallback |
| --- | --- | --- |
| `ConfigEditor` | Project detail config tab selected. | Fixed-height editor skeleton. |
| `LogViewer` | Job detail route loaded. | Log panel skeleton with connection status. |
| `TrainingHistoryTable` | Project detail history tab visible. | Table skeleton. |
| `ArtifactList` | Job terminal state or artifacts tab visible. | List skeleton or empty state. |
| `ZipProjectUploadForm` | ZIP source mode selected. | Compact form skeleton. |
| Admin tables | Admin route resolved and role confirmed. | Table skeleton. |

## Lazy Loading Rules

* Do not lazy-load tiny shared primitives such as `Button`, `Dialog`, `StatusBadge`, or `TextField`.
* Do not lazy-load route guards or authorization checks.
* Do not lazy-load error boundaries needed to catch route failures.
* Lazy chunks must have stable fallbacks with reserved dimensions to avoid layout shift.
* Lazy-loaded admin modules must still rely on backend authorization; role-based code splitting is a UX and bundle optimization only.

## Prefetch Strategy

| Interaction | Prefetch |
| --- | --- |
| Dashboard project row hover or keyboard focus | Project detail route chunk and `/projects/{projectId}`. |
| Project history job row hover or keyboard focus | Job detail route chunk and `/jobs/{jobId}`. |
| Start training success | Job detail route chunk and seed new job in Redux. |
| Job enters terminal `SUCCESS` | Artifact list request and artifact route code if separated. |
| User opens notification menu | Notifications page chunk and first notification page. |

Prefetching should be conservative on mobile and disabled when the browser reports constrained network conditions.

## Suggested React Pattern

```typescript
const ProjectDetailPage = lazy(() => import('../features/project-detail/project-detail-page'));
const JobDetailPage = lazy(() => import('../features/job-detail/job-detail-page'));
const AdminUsersPage = lazy(() => import('../features/admin/user-management-page'));

export function AppRoutes() {
  return (
    <Suspense fallback={<RouteLoadingState />}>
      <Routes>
        <Route path="/projects/:projectId" element={<ProjectDetailPage />} />
        <Route path="/jobs/:jobId" element={<JobDetailPage />} />
        <Route path="/admin/users" element={<AdminRoute element={<AdminUsersPage />} />} />
      </Routes>
    </Suspense>
  );
}
```

## Bundle Boundaries

| Chunk | Contents |
| --- | --- |
| `app-core` | App shell, router, Redux provider, route guards, base shadcn/ui components, Axios client. |
| `project-dashboard` | Dashboard page, project search, project table. |
| `project-detail` | Project summary, tabs, config metadata, start training dialog. |
| `config-editor` | YAML editor and validation UI. |
| `job-detail` | Status panel, WebSocket connection banner, job actions. |
| `log-viewer` | Virtualized log viewer, search, stream filter. |
| `admin` | Admin user management and queue monitoring. |

## Acceptance Checklist

* Dashboard is available without loading admin code.
* Admin chunks are loaded only after admin route access.
* Heavy editor and log viewer code is isolated from dashboard bundle.
* Route loading fallbacks have fixed dimensions and accessible labels.
* Prefetching improves likely next navigation without excessive mobile data usage.
