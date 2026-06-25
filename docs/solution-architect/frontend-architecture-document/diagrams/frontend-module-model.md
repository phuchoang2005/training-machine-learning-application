---
title: "Frontend Module Model"
tags: [diagram, frontend, modules, plantuml]
diagram-type: plantuml
aliases: [Frontend Module Model, frontend-module-model]
related:
  - "[[frontend-architecture]]"
  - "[[frontend-architecture-context]]"
  - "[[ADR-011]]"
  - "[[ADR-012]]"
---

# Frontend Module Model

PlantUML diagram showing frontend module dependencies and layer boundaries.

Source: `diagrams/frontend-module-model.puml`

## Layer Boundaries

```
src/app/           ← Route table, guards, shell layout
src/pages/         ← Route-level page modules (by workflow)
src/store/         ← Redux slices, hooks, mock data
src/shared/api/    ← Axios client, domain types, error normalization
src/shared/realtime/ ← WebSocket connect/reconnect/dedup
src/shared/components/ ← App-specific reusable UI composition
src/shared/ui/     ← shadcn-style Tabs primitive (Radix-based)
src/assets/styles/ ← CSS keyframes and semantic classes
```

## Related
- [[frontend-architecture]] — Full architecture document
- [[frontend-architecture-context]] — C4 context diagram
- [[api-integration-flow]] — Axios call chain
- [[realtime-state-flow]] — WebSocket reconnect flow
