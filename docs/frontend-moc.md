---
title: "Frontend MOC"
tags: [moc, frontend, react, ux, ui]
aliases: [Frontend, UI/UX]
---

# Frontend — Map of Content

Frontend architecture, UX design, and UI screen inventory.

## Frontend Architecture
- [[frontend-architecture]] — Full React SPA architecture: structure, state, routing, API integration, WebSocket, security, testing
- [[frontend-architecture-context]] — C4 context diagram: React SPA, Redux, Axios, WebSocket, Spring Boot API
- [[frontend-module-model]] — Module dependency diagram (PlantUML)

## State & Data Flow
- [[realtime-state-flow]] — WebSocket reconnect and Redux state flow for job monitoring
- [[api-integration-flow]] — Axios + Redux thunk integration chain
- [[route-guard-flow]] — Authentication, role checks, and resource-ownership routing

## UX Design
- [[ux-overview]] — UX scope, principles, status vocabulary, document map
- [[personas]] — Primary and secondary user personas
- [[journey-maps]] — End-to-end workflow journeys
- [[information-architecture]] — Navigation hierarchy and route ownership
- [[user-flows]] — Task-level flows: launch, monitor, cancel, retry, download
- [[information-architecture-diagram]] — Mermaid navigation structure
- [[primary-user-flow-diagram]] — Mermaid core training workflow

## UI Screens
- [[ui-screens]] — UI screen inventory
- [[user-screens]] — User-facing screens (01–08): login, dashboard, registration, project detail, config editor, training history, job detail, notifications
- [[admin-screens]] — Admin screens (09–11): queue, users, audit

## Design System
- [[design-system]] — Component specification
- [[performance-optimization]] — Performance and UX optimization design

## API Caching Strategy
- [[api-caching-strategy]] — API integration and client-side caching strategy

## Related
- [[_index]] — Home
- [[architecture-moc]] — ADR-011/012/013/014 (state, API client, styling, theme)
- [[design-moc]] — API contracts and OpenAPI spec
