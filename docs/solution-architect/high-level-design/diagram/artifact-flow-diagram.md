---
title: "Artifact Flow Diagram"
tags: [diagram, hld, artifacts, model-versioning]
diagram-type: mermaid
aliases: [Artifact Flow, Model Artifacts]
---

# Artifact Flow Diagram

Shows how training artifacts (model files, metrics, checkpoints) are discovered, copied, registered, and linked to a model version after a successful job.

```mermaid
flowchart TD
    A[Training Job RUNNING] --> B[Docker Container]
    B --> C[Python Training Code]
    C --> D[Read config.yaml]
    D --> E[artifact_path inside container<br/>Example: outputs/models]

    C --> F[Generate Artifacts<br/>model.pkl, metrics.json, plots, checkpoints]
    F --> G[Artifacts stored in container path]

    G --> H{Job Finished?}

    H -->|FAILED / CANCELLED| I[Do not register artifacts<br/>Keep logs only]
    H -->|SUCCESS| J[Artifact Scanner Service]

    J --> K[Inspect container artifact_path]
    K --> L{Artifacts found?}

    L -->|No| M[Mark Artifact Status: NOT_FOUND<br/>Notify user]
    L -->|Yes| N[Copy artifacts from container<br/>to host storage]

    N --> O[Validate artifact files<br/>size, name, checksum, path safety]
    O --> P{Validation OK?}

    P -->|No| Q[Artifact Registration FAILED<br/>Training remains SUCCESS<br/>Notify user]
    P -->|Yes| R[Persist artifact metadata]

    R --> S[(Database<br/>artifacts table)]
    R --> T[Host Artifact Storage<br/>/data/artifacts/project_id/job_id/]

    S --> U[Create Model Version<br/>linked to Project + Job]
    T --> U

    U --> V[Artifact available for download]
    V --> W[Audit Log<br/>artifact_registered / artifact_downloaded]
```

## Key Rules
- Artifacts are only registered on `SUCCESS` — not on `FAILED` or `CANCELLED`
- Artifact registration failure does NOT change the job status (`SUCCESS` remains `SUCCESS`)
- `artifact_path` is read from the YAML configuration snapshot (not hardcoded)
- Path traversal and unsafe symlinks are rejected during validation

## Related
- [[configuration-management-flow-diagram]] — Where `artifact_path` is defined
- [[storage-layout-diagram]] — Physical path `/data/artifacts/`
- [[erd]] — `ARTIFACTS` and `MODEL_VERSIONS` tables
- [[ADR-009]] — Storage decision
- [[non-functional-requirements]] — NFR-REL-007, NFR-SEC-006, NFR-STO-003
- [[failure-handling-matrix]] — Artifact registration failure row
