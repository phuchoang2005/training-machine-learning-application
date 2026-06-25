---
title: "Real-Time State Flow"
tags: [diagram, frontend, websocket, redux, realtime]
diagram-type: mermaid
aliases: [Realtime Flow, WebSocket State]
---

# Real-Time State Flow

Shows how the Job Detail page loads initial data, connects to WebSocket, handles reconnect, and falls back to REST polling.

```mermaid
sequenceDiagram
    participant Page as Job Detail Page
    participant Store as Redux Store
    participant WS as Job WebSocket Client
    participant API as REST API

    Page->>API: GET /jobs/{jobId}
    API-->>Store: Job detail
    Page->>API: GET /jobs/{jobId}/logs?cursor=...
    API-->>Store: Initial log page
    Page->>WS: Connect /ws/jobs/{jobId}
    WS-->>Page: connection_open
    WS-->>Store: status/progress/log event
    Store-->>Page: Render updated job and appended logs

    WS--xPage: connection_lost
    Page->>Page: Show reconnect state
    WS->>WS: Retry with backoff
    alt reconnect succeeds
        WS-->>Page: connection_open
        Page->>API: GET /jobs/{jobId}/logs?after=lastSeen
        API-->>Store: Missed log events
    else reconnect fails
        Page->>API: Poll GET /jobs/{jobId}
        Page->>API: Poll GET /jobs/{jobId}/logs?after=lastSeen
    end
```

## Reconnect Behavior

1. Show degraded connection state in UI
2. Retry WebSocket connection with exponential backoff
3. If reconnect succeeds: fetch missed events since `lastSeen`
4. If reconnect fails: fall back to REST polling

Duplicate events are ignored using monotonic sequence or log offset.

## Related
- [[api-integration-flow]] — REST call chain
- [[log-streaming-architecture-diagram]] — Backend streaming architecture
- [[frontend-architecture]] — WebSocket design section
- [[ADR-008]] — WebSocket + REST fallback decision
- [[non-functional-requirements]] — NFR-UX-005 (reconnect state visible)
