---
title: "Training Execution Sequence Diagram"
tags: [diagram, hld, training, sequence, execution]
diagram-type: mermaid
aliases: [Training Sequence, Execution Flow]
---

# Training Execution Sequence Diagram

End-to-end flow from the user clicking "Train" to job completion, including Docker container lifecycle, artifact registration, and email notification.

```mermaid
flowchart TD
    U[User] --> FE[Frontend - Training Page]
    FE --> API[Backend API]

    API --> V[Validate Project + YAML Config]
    V --> D{Disk >= 5GB?}
    D -- No --> N1[Notify User: Not Enough Disk]
    D -- Yes --> S[Create Job Record]

    S --> Q[Persistent FIFO Queue - DB]
    Q --> SCH[Job Scheduler]

    SCH --> C{Running Jobs < 2?}
    C -- No --> Q
    C -- Yes --> R[Start Training Job]

    R --> SNAP[Create Immutable Config Snapshot]
    SNAP --> SRC[Prepare Source Code]

    SRC --> G{Source Type}
    G -- GitHub --> CLONE[Clone Public Repo if not exists]
    G -- ZIP Upload --> EXTRACT[Extract ZIP + Detect Structure]

    CLONE --> BUILD[Build Docker Image / Install requirements.txt]
    EXTRACT --> BUILD

    BUILD --> RUN[Run Isolated Docker Container]
    RUN --> PY[Execute Training Entry Point: main.py]

    PY --> LOG[Capture stdout / stderr]
    PY --> PROG[Parse Progress Events]

    LOG --> LS[Log Service]
    PROG --> PS[Progress Service]

    LS --> STORELOG[Store Job Logs]
    PS --> STOREPROG[Store Latest Progress]

    LS --> WS[WebSocket Gateway]
    PS --> WS
    WS --> FE

    PY --> DONE{Container Finished}

    DONE -- Success --> ART[Scan artifact_path from YAML]
    ART --> REG[Register Artifacts]
    REG --> MV[Create Model Version]
    MV --> SUCCESS[Mark Job SUCCESS]

    DONE -- Failed --> FAILED[Mark Job FAILED]

    SUCCESS --> EMAIL[Send Email Notification]
    FAILED --> EMAIL

    EMAIL --> NOTE{Email Sent?}
    NOTE -- Yes --> END[End]
    NOTE -- No --> WARN[Show Email Delivery Failed Notification]

    subgraph Recovery["Restart Recovery"]
        RR[Server Restart] --> FIND[Find RUNNING Jobs in DB]
        FIND --> REQ[Requeue Interrupted Jobs]
        REQ --> Q
    end

    subgraph Cancel["Cancel Flow"]
        ADM[Admin / User Cancel] --> STOP[Stop Docker Container]
        STOP --> CANCELLED[Mark Job CANCELLED]
    end
```

## Related
- [[job-lifecycle-state-diagram]] — State machine overview
- [[queue-flow-diagram]] — Queue and dispatch detail
- [[recovery-flow-diagram]] — Restart recovery path
- [[artifact-flow-diagram]] — Artifact copy and registration
- [[progress-event-flow-diagram]] — Progress event parsing
- [[log-streaming-architecture-diagram]] — Log WebSocket streaming
- [[configuration-management-flow-diagram]] — Config snapshot creation
- [[ADR-006]] — Docker execution
- [[ADR-008]] — WebSocket streaming
- [[ADR-009]] — Storage for logs and artifacts
- [[ADR-010]] — Email notification
