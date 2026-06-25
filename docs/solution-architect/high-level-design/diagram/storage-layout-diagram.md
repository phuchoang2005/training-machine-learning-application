---
title: "Storage Layout Diagram"
tags: [diagram, hld, storage, filesystem]
diagram-type: mermaid
aliases: [Storage Layout, File Storage, /data Layout]
---

# Storage Layout Diagram

Shows the physical directory structure under the host storage root `/data` and how each path relates to Docker containers and database metadata.

```mermaid
flowchart TB
    subgraph Root["Host Storage Root: /data"]
        Sources["/data/sources"]
        Workspaces["/data/workspaces"]
        Logs["/data/logs"]
        Artifacts["/data/artifacts"]
        Uploads["/data/uploads"]
    end

    Sources --> GitProject["project_id/<br/>cloned GitHub repository"]
    Uploads --> ZipFile["project_id/upload.zip"]
    Uploads --> ExtractedZip["project_id/extracted/"]

    GitProject --> Workspace["/data/workspaces/job_id/<br/>runtime source copy"]
    ExtractedZip --> Workspace

    Workspace --> ConfigSnapshot["config.yaml<br/>immutable job snapshot"]
    Workspace --> DockerMount["Mounted into Docker Container"]

    DockerMount --> TrainingOutput["Container output path<br/>artifact_path from YAML"]
    TrainingOutput --> ArtifactCopy["Copy to /data/artifacts/project_id/job_id/"]

    DockerMount --> Stdout["stdout"]
    DockerMount --> Stderr["stderr"]
    Stdout --> JobLogs["/data/logs/project_id/job_id/stdout.log"]
    Stderr --> JobLogs2["/data/logs/project_id/job_id/stderr.log"]

    ArtifactCopy --> Metadata[(Database<br/>artifacts, model_versions)]
    JobLogs --> LogMetadata[(Database<br/>job_log_sessions)]
    JobLogs2 --> LogMetadata
    ConfigSnapshot --> SnapshotMetadata[(Database<br/>config_snapshots)]
```

## Path Convention

| Path | Contents |
|---|---|
| `/data/sources/{project_id}/` | Cloned GitHub repo or extracted ZIP |
| `/data/uploads/{project_id}/` | Raw ZIP upload |
| `/data/workspaces/{job_id}/` | Runtime copy mounted into container |
| `/data/logs/{project_id}/{job_id}/` | stdout.log + stderr.log |
| `/data/artifacts/{project_id}/{job_id}/` | Copied artifact files |

## Related
- [[deployment-diagram]] — Server where `/data` lives
- [[artifact-flow-diagram]] — Artifact copy process
- [[project-registration-flow-diagram]] — Where `/data/sources/` is populated
- [[ADR-009]] — Local storage decision and path conventions
- [[non-functional-requirements]] — NFR-STO-001 to NFR-STO-005
