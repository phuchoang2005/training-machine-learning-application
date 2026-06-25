---
title: "Request Authorization Flow"
tags: [diagram, hld, security, authorization, sequence]
diagram-type: mermaid
aliases: [Auth Flow, Authorization Sequence]
---

# Request Authorization Flow

Shows the authentication and authorization sequence for every protected API request.

```mermaid
sequenceDiagram
    actor Client as User/Admin
    participant FE as Frontend
    participant API as Backend API
    participant Auth as Authentication
    participant Authz as Authorization
    participant Svc as Domain Service
    participant Audit as Audit Log

    Client->>FE: Perform action
    FE->>API: Request with token/session
    API->>Auth: Verify identity
    Auth-->>API: userId, role

    API->>Authz: Check role permission
    Authz->>Authz: Check resource ownership

    alt Allowed
        Authz-->>API: Permit
        API->>Svc: Execute action
        Svc-->>API: Result
        API->>Audit: Record action
        API-->>FE: Success response
    else Denied
        Authz-->>API: Deny
        API->>Audit: Record denied access
        API-->>FE: 403 Forbidden
    end
```

## Implementation Notes

- `WebConfig extends OncePerRequestFilter` extracts `Authorization: Bearer` header and stores user in `CurrentUserContext` (thread-local)
- `AuthorizationService.require*()` methods enforce RBAC + ownership
- Every successful and denied action is recorded in the audit log
- `GET /api/v1/health` is the only unauthenticated endpoint

## Related
- [[security-architecture-diagram]] — Architectural view of auth layers
- [[access-control-matrix]] — What each role can do
- [[security-model]] — RBAC + Ownership model theory
- [[ADR-007]] — Authentication decision
- [[ADR-015]] — Chain of Responsibility (WebConfig) and Facade (AuthorizationService)
