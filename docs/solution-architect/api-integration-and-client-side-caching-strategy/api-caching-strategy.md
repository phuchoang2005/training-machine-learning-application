---
title: "API Integration and Client-Side Caching Strategy"
tags: [frontend, api, caching, redux]
aliases: [API Caching Strategy, api-caching-strategy]
related:
  - "[[frontend-architecture]]"
  - "[[ADR-011]]"
  - "[[ADR-012]]"
  - "[[api-integration-flow]]"
---

# API Integration and Client-Side Caching Strategy

## Document Index

This documentation set defines the frontend API integration and client-side caching strategy for the AI Training Management Platform.

Read the modules in this order:

| Module | Purpose |
| --- | --- |
| [overview.md](overview.md) | Purpose, scope, and integration principles. |
| [api-service-layer.md](api-service-layer.md) | API folder structure, Axios client contract, service catalog, and layer boundaries. |
| [caching-and-synchronization.md](caching-and-synchronization.md) | Redux slice keys, cache policy, invalidation, WebSocket synchronization, polling fallback, and pagination. |
| [error-handling.md](error-handling.md) | Normalized error shape, status handling, presentation rules, and degraded behavior. |
| [retry-policies.md](retry-policies.md) | Retry classification, backoff rules, mutation retry policy, and optimistic updates. |
| [security-and-testing.md](security-and-testing.md) | Browser security controls, test strategy, and acceptance checklist. |

## Source References

* `docs/solution-architect/low-level-design/api-contracts/openapi.yaml`
* `docs/solution-architect/frontend-architecture-document/README.md`
* `docs/solution-architect/low-level-design/non-functional-requirements.md`
