---
title: "Configuration Management Flow Diagram"
tags: [diagram, hld, configuration, yaml, snapshot]
diagram-type: mermaid
aliases: [Config Flow, YAML Snapshot]
---

# Configuration Management Flow Diagram

Shows how a YAML configuration is edited, validated, snapshotted, and used during training execution.

```mermaid
flowchart TD
    A[User Opens Project Detail] --> B[Load Config Files from ./configs/]
    B --> C[Display YAML Editor]
    C --> D[User Edits YAML Configuration]
    D --> E[Server Validates YAML Syntax]
    E --> F{Valid YAML?}

    F -->|No| G[Return Validation Error<br/>Do Not Start Job]
    F -->|Yes| H[Validate Project Access]
    H --> I{Authorized?}

    I -->|No| J[Reject Request<br/>403 Forbidden]
    I -->|Yes| K[Create Immutable Config Snapshot]
    K --> L[Calculate Content Hash]
    L --> M[(Database<br/>config_snapshots)]
    M --> N[Create Training Job<br/>linked to snapshot_id]
    N --> O[Queue Job]

    O --> P[Training Container Starts]
    P --> Q[Mount or Copy Snapshot as Runtime config.yaml]
    Q --> R[Python Training Code Reads Config]
    R --> S[Snapshot Remains Available<br/>for History, Retry, Model Versioning]
```

## Key Rules
- Snapshots are **immutable** once a job is created (see [[non-functional-requirements]] NFR-DATA-001)
- The snapshot is linked to the job, model version, and artifact record for full reproducibility
- Users can freely edit YAML before starting — only the submitted value is snapshotted

## Related
- [[project-registration-flow-diagram]] — Where configs/ directory comes from
- [[artifact-flow-diagram]] — Artifact path is read from the snapshot
- [[erd]] — `CONFIG_SNAPSHOTS` table and relationships
- [[sa-refinement]] — Section 4: configuration management
- [[ADR-004]] — Snapshot persistence in MongoDB
