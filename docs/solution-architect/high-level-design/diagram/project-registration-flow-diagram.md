---
title: "Project Registration Flow Diagram"
tags: [diagram, hld, project, registration, github, zip]
diagram-type: mermaid
aliases: [Project Registration, GitHub Clone, ZIP Upload]
---

# Project Registration Flow Diagram

Shows how a project is registered from either a GitHub URL or a ZIP upload, including validation and storage steps.

```mermaid
flowchart TD
    A[User Opens Project Registration] --> B{Source Type}

    B -->|GitHub Public Repository| C[Enter Repository URL]
    C --> D[Validate URL Format]
    D --> E{Repository Reachable?}
    E -->|No| F[Reject Registration<br/>Show Clone Validation Error]
    E -->|Yes| G[Clone Repository to Source Storage]

    B -->|ZIP Upload| H[Upload ZIP Package]
    H --> I[Validate File Type and Size]
    I --> J{ZIP Valid?}
    J -->|No| K[Reject Upload<br/>Show Package Error]
    J -->|Yes| L[Extract ZIP to Source Storage]

    G --> M[Detect Project Structure]
    L --> M

    M --> N[Validate Required Files<br/>main.py, requirements.txt, configs/]
    N --> O{Project Valid?}
    O -->|No| P[Reject Project<br/>Show Missing Requirements]
    O -->|Yes| Q[Register Project Metadata]

    Q --> R[(Database<br/>projects, project_configs)]
    Q --> S[Source Storage<br/>/data/sources/project_id/]
    R --> T[Create Audit Log<br/>project_registered]
    S --> U[Project Available for Training]
    T --> U
```

## Required Project Files

Every project must have:
- `main.py` — training entry point
- `requirements.txt` — Python dependencies
- `configs/` — YAML configuration directory

## Related
- [[storage-layout-diagram]] — `/data/sources/` path
- [[configuration-management-flow-diagram]] — Config loading after registration
- [[sa-refinement]] — Sections 2 and 4: source management and config
- [[erd]] — `PROJECTS` and `PROJECT_CONFIGS` tables
- [[non-functional-requirements]] — NFR-SEC-006 (ZIP path traversal validation)
- [[failure-handling-matrix]] — GitHub clone failure and ZIP validation failure rows
