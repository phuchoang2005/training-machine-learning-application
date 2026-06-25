---
title: "Deployment Diagram"
tags: [diagram, hld, deployment, infrastructure]
diagram-type: mermaid
aliases: [Deployment, Infrastructure Topology]
---

# Deployment Diagram

Shows the physical/logical deployment of the platform on a single server. **The backend runs on the host; Docker provides only MongoDB and the Nginx frontend container.**

```mermaid
flowchart TB
    User[User / Admin]

    subgraph S["Single Physical / Cloud Server"]
        subgraph App["Web / Application Runtime"]
            FE["Frontend SPA / UI"]
            BE["Backend Application"]
            API["REST API"]
            STREAM["WebSocket Log and Progress Streaming"]
        end

        DB[("MongoDB<br/>Users, Projects, Jobs, Queue,<br/>Logs Metadata, Artifacts,<br/>Model Versions, Audit Logs")]

        subgraph Docker["Docker Engine"]
            TC1["Training Container #1<br/>Project Code<br/>main.py<br/>requirements.txt"]
            TC2["Training Container #2<br/>Project Code<br/>main.py<br/>requirements.txt"]
        end

        subgraph Storage["Local File Storage"]
            Repos["/storage/repos"]
            Uploads["/storage/uploads"]
            Logs["/storage/logs"]
            Artifacts["/storage/artifacts"]
            Configs["/storage/config-snapshots"]
        end

        subgraph Workers["Background Workers"]
            Scheduler["Job Scheduler"]
            Queue["FIFO Queue Processor"]
            Runner["Docker Runner"]
            LogCollector["Log Collector"]
            ArtifactScanner["Artifact Scanner"]
        end
    end

    subgraph External["External Services"]
        GitHub["GitHub Public Repository"]
    end

    User --> FE
    FE --> API
    FE --> STREAM

    API --> BE
    BE --> DB
    BE --> Storage
    BE --> Docker
    BE --> GitHub

    Scheduler --> Queue
    Queue --> Runner
    Runner --> TC1
    Runner --> TC2

    TC1 --> LogCollector
    TC2 --> LogCollector
    LogCollector --> STREAM
    LogCollector --> Logs

    ArtifactScanner --> Artifacts
```

## Startup Order

1. Build frontend bundle: `npm ci && npm run build`
2. `docker compose up -d --build` — starts MongoDB + Nginx frontend
3. `mvn spring-boot:run` in `backend/` — starts the Spring Boot API on port 8080

Nginx proxies `/api/` and WebSocket traffic to `host.docker.internal:8080`.

## Related
- [[system-context-diagram]] — External context
- [[high-level-component-diagram]] — Internal modules
- [[storage-layout-diagram]] — Detail on `/data` layout
- [[ADR-001]] — Backend runs on host (not in container)
- [[ADR-004]] — MongoDB in Docker
- [[ADR-006]] — Training containers
- [[ADR-009]] — Local storage
