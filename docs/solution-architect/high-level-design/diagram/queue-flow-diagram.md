---
title: "Queue Flow Diagram"
tags: [diagram, hld, queue, scheduler]
diagram-type: mermaid
aliases: [Queue Flow, FIFO Queue]
---

# Queue Flow Diagram

Shows the complete flow from a user clicking "Train" to job dispatch, execution, and artifact registration. Includes the FIFO queue waiting path.

```mermaid
flowchart TD
    A[User Click Train] --> B[Create Job Record]
    B --> C[Status = CREATED]
    C --> D[Validate Project]
    D --> E{Valid?}

    E -- No --> F[Status = FAILED]
    E -- Yes --> G[Check Disk Space >= 5GB]

    G --> H{Enough Disk?}
    H -- No --> I[Status = FAILED<br/>Notify User]
    H -- Yes --> J{Running Jobs < 2?}

    J -- Yes --> K[Start Job Immediately]
    K --> L[Status = RUNNING]
    L --> M[Launch Docker Container]
    M --> N[Stream Logs / Progress]
    N --> O{Container Result}

    O -- Success --> P[Status = SUCCESS]
    P --> Q[Register Artifacts]
    Q --> R[Create Model Version]
    R --> S[Notify User]

    O -- Failed --> T[Status = FAILED]
    T --> U[Notify User]

    O -- Cancelled --> V[Status = CANCELLED]
    V --> W[Stop Container]

    J -- No --> X[Status = QUEUED]
    X --> Y[Persist FIFO Queue in DB]
    Y --> Z[Wait for Available Slot]

    P --> AA[Scheduler Checks Queue]
    T --> AA
    V --> AA

    AA --> AB{Queued Jobs Exist<br/>and Running Jobs < 2?}
    AB -- Yes --> AC[Pick Oldest QUEUED Job]
    AC --> K
    AB -- No --> AD[Idle]
```

## Dispatch Rules

- Max **2 concurrent RUNNING jobs** at any time
- Queue capacity: **50 queued jobs**
- Dispatcher runs every **2 seconds** (see [[non-functional-requirements]] NFR-PERF-006)
- Jobs are dispatched in FIFO order (by `enqueued_at`)

## Related
- [[job-lifecycle-state-diagram]] — State machine for all transitions
- [[recovery-flow-diagram]] — What happens to RUNNING jobs on restart
- [[training-execution-sequence-diagram]] — Detailed execution sequence
- [[capacity-scalability-view]] — MVP limits and future scalability
- [[ADR-005]] — Queue persistence decision
- [[sa-refinement]] — Queue survival requirements (section 5)
