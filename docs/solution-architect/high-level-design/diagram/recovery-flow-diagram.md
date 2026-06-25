---
title: "Recovery Flow Diagram"
tags: [diagram, hld, recovery, restart]
diagram-type: mermaid
aliases: [Recovery Flow, Restart Recovery]
---

# Recovery Flow Diagram

Shows the startup recovery process when the server or application restarts with RUNNING jobs in the database.

```mermaid
flowchart TD
    A[Application / Server Restart] --> B[Bootstrap Recovery Service]
    B --> C[Load Jobs from DB]

    C --> D[Find Jobs with Status = RUNNING]
    D --> E{Any RUNNING Jobs?}

    E -- No --> F[Start Normal Scheduler]
    E -- Yes --> G[Mark Interrupted Jobs as RETRYING]

    G --> H[Stop / Clean Orphan Docker Containers if Exist]
    H --> I[Create Recovery Execution Record]
    I --> J[Requeue Job]
    J --> K[Status = QUEUED]
    K --> L[Preserve Retry Link / Original Job Reference]

    L --> M[Start Scheduler]
    M --> N{Running Jobs < 2<br/>and Queue Not Empty?}

    N -- No --> O[Wait]
    N -- Yes --> P[Pick Oldest QUEUED Job]
    P --> Q[Status = RUNNING]
    Q --> R[Launch New Docker Container]
    R --> S[Run Training From Beginning]

    S --> T{Execution Result}
    T -- Success --> U[Status = SUCCESS]
    T -- Failed --> V[Status = FAILED]
    T -- Cancelled --> W[Status = CANCELLED]

    U --> X[Register Artifacts / Model Version]
    V --> Y[Notify User]
    W --> Z[Notify User]
```

## Recovery Rules

- Interrupted jobs run from the beginning (no checkpointing in MVP)
- `RUNNING → RETRYING → QUEUED` — the original job reference is preserved
- Orphan Docker containers are stopped and cleaned up
- Recovery runs at application bootstrap (`JobReconcilerService`)

## Related
- [[job-lifecycle-state-diagram]] — RETRYING state in the state machine
- [[queue-flow-diagram]] — Normal dispatch flow reused after recovery
- [[ADR-005]] — Queue persistence survives restart
- [[non-functional-requirements]] — NFR-REL-003, NFR-REL-004
- [[sa-refinement]] — Section 6: Running Job Recovery requirements
- [[failure-handling-matrix]] — "Server or app restart" row
