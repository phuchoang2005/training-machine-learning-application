---
title: "Information Architecture Diagram"
tags: [diagram, ux, navigation, information-architecture]
diagram-type: mermaid
aliases: [IA Diagram, Navigation Structure]
---

# Information Architecture Diagram

Shows the navigation hierarchy and page ownership for the authenticated application shell.

```mermaid
flowchart TD
    A[Authenticated App Shell] --> B[Projects]
    A --> C[Notifications]
    A --> D[Account]
    A --> E[Admin]

    B --> B1[Project Dashboard]
    B --> B2[Register Project]
    B1 --> B3[Project Detail]
    B3 --> B4[Configuration]
    B3 --> B5[Training History]
    B3 --> B6[Job Detail]
    B6 --> B7[Logs]
    B6 --> B8[Artifacts]

    C --> C1[Notification List]
    C1 --> B6

    E --> E1[Queue Monitor]
    E --> E2[User Management]
    E --> E3[Audit Log]
    E1 --> B6

    A --> F[Forbidden]
    A --> G[Not Found]

    classDef primary fill:#dbeafe,stroke:#2563eb,color:#0f172a
    classDef admin fill:#fef3c7,stroke:#b45309,color:#0f172a
    classDef error fill:#fee2e2,stroke:#b91c1c,color:#0f172a

    class B,B1,B2,B3,B4,B5,B6,B7,B8,C,C1 primary
    class E,E1,E2,E3 admin
    class F,G error
```

## Related
- [[information-architecture]] — IA text documentation
- [[route-guard-flow]] — Route access control
- [[frontend-architecture]] — Route table and component mapping
- [[user-screens]] — User-facing screen mockups (screens 01–08)
- [[admin-screens]] — Admin screen mockups (screens 09–11)
