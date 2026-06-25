---
title: "Route Guard Flow"
tags: [diagram, frontend, routing, auth, rbac]
diagram-type: mermaid
aliases: [Route Guards, Auth Routing]
---

# Route Guard Flow

Shows how route access is controlled for public routes, authenticated routes, admin-only routes, and resource-ownership routes.

```mermaid
flowchart TD
    Start[Route matched] --> Public{Public route?}
    Public -- Yes --> Render[Render route]
    Public -- No --> LoadMe[Load /auth/me]
    LoadMe --> Authenticated{Authenticated?}
    Authenticated -- No --> Login[Redirect to company login]
    Authenticated -- Yes --> RoleCheck{Admin-only route?}
    RoleCheck -- Yes --> IsAdmin{role = ADMIN?}
    IsAdmin -- No --> Forbidden[Render /403]
    IsAdmin -- Yes --> Render
    RoleCheck -- No --> ResourceRoute{Resource route?}
    ResourceRoute -- No --> Render
    ResourceRoute -- Yes --> FetchResource[Fetch resource through backend API]
    FetchResource --> ApiResult{API result}
    ApiResult -- 200 --> Render
    ApiResult -- 401 --> Login
    ApiResult -- 403 --> Forbidden
    ApiResult -- 404 --> NotFound[Render /404]
```

## Route Groups

| Route | Guard |
|---|---|
| `/login`, `/register`, `/login/callback` | Public |
| `/projects`, `/projects/new`, `/notifications` | Authenticated |
| `/projects/:projectId`, `/jobs/:jobId` | Authenticated + resource ownership (backend-enforced) |
| `/admin/users`, `/admin/queue` | Admin role |

## Related
- [[frontend-architecture]] — Full routing table and URL state design
- [[api-integration-flow]] — REST calls that enforce ownership
- [[request-authorization-flow]] — Backend auth sequence
- [[access-control-matrix]] — What admins vs users can access
