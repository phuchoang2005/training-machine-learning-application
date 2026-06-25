---
title: "Progress Event Flow Diagram"
tags: [diagram, hld, progress, websocket, training]
diagram-type: mermaid
aliases: [Progress Flow, Progress Tracking]
---

# Progress Event Flow Diagram

Shows how Python training code emits structured progress events that flow through the platform to update the UI progress bar.

```mermaid
sequenceDiagram
    participant Py as Python Training Code
    participant C as Docker Container
    participant R as Docker Runner
    participant P as Progress Parser
    participant JS as Job Service
    participant DB as Database
    participant WS as WebSocket Gateway
    participant UI as Training Detail Page

    Py->>C: print JSON progress event
    Note over Py,C: {"event":"progress","value":35,"epoch":7,"total_epoch":20}

    C->>R: stdout stream
    R->>P: forward log line

    alt line is progress JSON
        P->>JS: updateProgress(jobId, value, epoch)
        JS->>DB: save latest progress
        JS->>WS: publish progress event
        WS->>UI: push progress update
        UI->>UI: update progress bar
    else normal log line
        P->>JS: ignore for progress
        JS->>WS: push as normal log event
    end
```

## Progress Contract

Training code should emit structured JSON to stdout:
```json
{"event": "progress", "value": 35, "epoch": 7, "total_epoch": 20}
```

If no progress events are emitted, the UI displays `Progress Information Not Available` (see [[non-functional-requirements]] NFR-UX-002).

## Related
- [[log-streaming-architecture-diagram]] — Log channel (same WebSocket stream)
- [[realtime-state-flow]] — Frontend state updates
- [[sa-refinement]] — Section 15A: progress tracking requirements
- [[non-functional-requirements]] — NFR-UX-002
