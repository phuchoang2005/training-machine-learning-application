---
title: "Primary User Flow Diagram"
tags: [diagram, ux, user-flow, training]
diagram-type: mermaid
aliases: [Primary User Flow, Core Workflow]
---

# Primary User Flow Diagram

Core training workflow from platform open to training completion and next steps.

```mermaid
flowchart TD
    A[Open Platform] --> B{Authenticated?}
    B -- No --> C[Login]
    C --> D[Load Authorized Projects]
    B -- Yes --> D

    D --> E[Project Dashboard]
    E --> F[Open Project Detail]
    F --> G[Review Branch Dataset Configuration]
    G --> H{Configuration Valid?}
    H -- No --> I[Show Validation Errors]
    I --> G
    H -- Yes --> J[Start Training Dialog]
    J --> K{Confirm Launch?}
    K -- No --> F
    K -- Yes --> L[Create Job]
    L --> M{Validation Accepted?}
    M -- No --> N[Show Launch Error]
    N --> F
    M -- Yes --> O[Job Detail Status CREATED or QUEUED]
    O --> P{Capacity Available?}
    P -- No --> Q[Show Queue Position]
    Q --> O
    P -- Yes --> R[RUNNING]
    R --> S[Stream Progress and Logs]
    S --> T{Terminal Status?}
    T -- No --> S
    T -- SUCCESS --> U[Review Artifacts]
    T -- FAILED --> V[Review Error Summary and Logs]
    T -- CANCELLED --> W[Review Cancelled State]
    V --> X{Retry?}
    W --> X
    X -- Yes --> L
    X -- No --> Y[Return to Project History]
    U --> Y

    classDef action fill:#dbeafe,stroke:#2563eb,color:#0f172a
    classDef decision fill:#f8fafc,stroke:#64748b,color:#0f172a
    classDef terminal fill:#dcfce7,stroke:#15803d,color:#0f172a
    classDef problem fill:#fee2e2,stroke:#b91c1c,color:#0f172a

    class A,C,D,E,F,G,I,J,L,N,O,Q,R,S,Y action
    class B,H,K,M,P,T,X decision
    class U terminal
    class V,W problem
```

## Related
- [[user-flows]] — Task-level flow documentation
- [[information-architecture-diagram]] — Navigation hierarchy
- [[journey-maps]] — End-to-end journey context
- [[job-lifecycle-state-diagram]] — Job states in this flow
- [[queue-flow-diagram]] — Queue position mechanics
