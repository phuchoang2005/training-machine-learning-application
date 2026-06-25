---
title: "Security Architecture Diagram"
tags: [diagram, hld, security, rbac, authorization]
diagram-type: mermaid
aliases: [Security Architecture, Auth Layers]
---

# Security Architecture Diagram

Shows the authentication and authorization layers from the frontend to backend services, including the admin restriction zone.

```mermaid
flowchart TB
    U[User] --> FE[Frontend Web UI]
    A[Admin] --> FE

    FE -->|HTTPS + Session/JWT| API[Backend API Gateway / Controller]

    API --> AUTH[Authentication Service]
    API --> AUTHZ[Authorization Service<br/>RBAC + Ownership Check]

    AUTH --> DB[(Database)]
    AUTHZ --> DB

    API --> PS[Project Service]
    API --> JS[Job Service]
    API --> LS[Log Service]
    API --> AS[Artifact Service]
    API --> NS[Notification Service]
    API --> AUD[Audit Service]

    PS -->|Own Project Only| SRC[(Project Source Storage)]
    JS --> QUEUE[(Persistent FIFO Queue)]
    JS --> DOCKER[Docker Runner]

    DOCKER -->|Isolated Container| C[Training Container]
    C --> LOGS[(Job Logs)]
    C --> ART[(Artifact Storage)]

    LS -->|User Ownership Required| LOGS
    AS -->|User Ownership Required| ART

    NS --> EMAIL[Google Workspace Email]

    AUD --> AUDDB[(Audit Logs)]

    AUTHZ -. blocks .-> ADMIN_BLOCK[Admin Restricted Zone<br/>No source code<br/>No artifact download<br/>No detailed project content<br/>No business data inspection]

    API --> ADMIN_BLOCK
```

## Key Security Rules

- All REST endpoints AND WebSocket subscriptions require authentication
- `AuthorizationService` (Facade pattern) is the single entry point for all RBAC decisions
- Admins can cancel jobs and delete projects but cannot see source code, logs, or artifacts
- Every request goes through `WebConfig` filter (Chain of Responsibility) to resolve user identity

## Related
- [[request-authorization-flow]] — Request-level auth sequence
- [[access-control-matrix]] — Per-action permission table
- [[security-model]] — RBAC + Ownership model explanation
- [[ADR-007]] — Authentication decision
- [[ADR-015]] — Facade (AuthorizationService) and Chain of Responsibility (WebConfig)
- [[non-functional-requirements]] — NFR-SEC-001 to NFR-SEC-008
