# Overview

## Purpose

This document defines performance and UX optimization requirements for the React, TypeScript, Vite, Redux, Axios, TailwindCSS, Radix UI, and shadcn/ui frontend of the AI Training Management Platform. The frontend must support project discovery, configuration editing, job launch, real-time job monitoring, log inspection, artifact download, notifications, and administrator views without unnecessary latency or browser resource usage.

## Performance Goals

| Goal | Target |
| --- | --- |
| Dashboard initial load | Complete within 3 seconds for MVP data volume. |
| Job status update visibility | WebSocket event normally appears in browser within 5 seconds. |
| Log streaming latency | Docker stdout/stderr events reach browser within 5 seconds under normal load. |
| Log search first page | Return and render first page within 5 seconds for a single job. |
| Mobile support | iPhone SE 2020 or greater: `375px x 667px` CSS viewport baseline. |
| Desktop support | Browser resolution equal to or greater than 1080p: `1920px x 1080px` baseline. |
| Interaction response | UI feedback for clicks, form submit, and route transition begins within 100 ms. |

## UX Goals

* Show a useful loading, empty, or degraded state for every remote dependency.
* Keep running jobs observable even when WebSocket reconnects or falls back to polling.
* Keep long logs and large tables responsive through virtualization, pagination, and bounded rendering.
* Avoid layout shift during route loading, table loading, and live log append.
* Keep destructive actions explicit and confirmation-based.
* Preserve accessible focus and readable status information without relying on color alone.
* Follow system light or dark mode without flashing the wrong theme on first paint.

## Constraints

* Frontend technology baseline is React, TypeScript, Vite, Redux, Axios, TailwindCSS, Radix UI, and shadcn/ui.
* The MVP is a static SPA served by the web tier or reverse proxy.
* WebSocket is the primary real-time channel; REST polling is fallback.
* Backend authorization is authoritative for all REST and WebSocket access.
* Log and artifact downloads must stream through authorized backend endpoints.
* The MVP supports at most 7 concurrent active users, 2 running jobs, and 50 queued jobs.

## Optimization Principles

* Load only the route, data, and UI code needed for the current workflow.
* Keep the dashboard and job detail route fast because they are the highest-frequency surfaces.
* Use route-level lazy loading first; use component-level lazy loading for heavy editors and viewers.
* Prefer data pagination, virtualization, and incremental rendering over loading everything at once.
* Use semantic HTML and stable dimensions so skeletons, errors, and live updates do not shift layout.
* Treat security and performance together: avoid persisting sensitive data and avoid over-fetching sensitive resources.
