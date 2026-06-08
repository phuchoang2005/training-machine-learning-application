# Overview

## Purpose

This document defines the frontend API integration and client-side caching strategy for the AI Training Management Platform. It covers browser-side communication from the React, TypeScript, Redux, and Axios frontend to REST and WebSocket backend interfaces.

The strategy aligns with:

* `docs/solution-architect/low-level-design/api-contracts/openapi.yaml`
* `docs/solution-architect/frontend-architecture-document/README.md`
* `docs/solution-architect/low-level-design/non-functional-requirements.md`

## Scope

The strategy applies to browser-side communication with:

* Authentication endpoints.
* Project and project registration endpoints.
* Configuration endpoints.
* Training job endpoints.
* Log and artifact endpoints.
* Notification endpoints.
* Admin user and queue endpoints.
* WebSocket job monitoring streams.

Backend authorization remains authoritative. Client-side caching must never be used as an access-control decision.

## API Integration Principles

* Treat the OpenAPI contract as the source of truth for REST payloads and response shapes.
* Keep Axios transport concerns in the shared API layer, not in feature components.
* Keep domain service functions grouped by resource: auth, users, projects, configurations, jobs, logs, artifacts, notifications, and audit.
* Use Redux slices for API-backed state and local React state only for transient UI state.
* Prefer targeted Redux slice refreshes over broad state resets.
* Use WebSocket events to update live job detail, progress, and logs, with REST polling as fallback.
* Normalize API errors into one frontend error shape before rendering.
* Retry only safe or explicitly idempotent operations.
