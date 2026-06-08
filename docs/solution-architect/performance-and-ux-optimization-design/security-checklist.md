# Security Checklist

## Authentication and Session

| Check | Requirement |
| --- | --- |
| Session storage | Prefer secure HTTP-only cookies. Do not store bearer tokens in local storage. |
| Logout | Clear Redux slices, WebSocket state, user-scoped UI state, and in-memory sensitive buffers. |
| Route guards | Use `/auth/me` for current user state; do not rely on client-only role assumptions. |
| Idle state | If backend session expires, redirect to login after `401`. |

## Authorization UX

| Check | Requirement |
| --- | --- |
| Project data | Fetch only through authorized backend endpoints. |
| Admin views | Do not expose source code, detailed logs, or artifacts unless backend authorizes ownership. |
| Forbidden resources | Show `403` or not-found state without leaking resource details. |
| Hidden actions | Hide or disable actions based on role and ownership, but still depend on backend enforcement. |

## API and WebSocket

| Check | Requirement |
| --- | --- |
| Base URL | Use configured `/api/v1`; avoid hardcoded external hosts in feature code. |
| Error handling | Preserve correlation ID and avoid showing internal paths or secrets. |
| Idempotency | Use `Idempotency-Key` for start, cancel, retry, upload, and admin mutations when supported. |
| WebSocket auth | Treat unauthorized stream errors as terminal for that connection. |
| Reconnect | Do not reconnect indefinitely on `401` or `403`. |
| Request cancellation | Abort stale requests when route params change. |

## Browser Storage and Cache

| Check | Requirement |
| --- | --- |
| Redux store | Keep API-backed state in memory unless persistent storage is explicitly approved. |
| Sensitive data | Do not persist logs, artifacts, source code, tokens, secrets, local paths, or config snapshots in browser storage. |
| Logout clearing | Clear all user-scoped cached data on logout. |
| Shared devices | Do not rely on browser autocomplete for sensitive fields unless approved. |

## File Upload and Download

| Check | Requirement |
| --- | --- |
| ZIP upload | Validate file extension, size, and content type in UI before upload; backend remains authoritative. |
| Download URLs | Use authorized backend endpoints, not direct filesystem paths. |
| Artifact content | Do not preview or parse artifacts in browser unless separately approved. |
| Log download | Do not store downloaded logs in app cache. |

## Content Security Policy

The frontend should be compatible with a restrictive CSP.

| Directive | Recommendation |
| --- | --- |
| `default-src` | `'self'` |
| `script-src` | `'self'` plus approved build/runtime requirements only. |
| `style-src` | `'self'` and avoid unsafe inline styles where practical. |
| `img-src` | `'self' data:` only unless external images are approved. |
| `connect-src` | `'self'` plus approved API and WebSocket origins. |
| `frame-ancestors` | `'none'` unless embedding is explicitly required. |

## Dependency Security

* Pin major framework and tooling versions intentionally.
* Run dependency audit in CI when the frontend package is introduced.
* Avoid libraries that require broad runtime permissions or unsafe HTML injection.
* Sanitize any rendered user-controlled Markdown or rich text if introduced later.
* Avoid `dangerouslySetInnerHTML` unless a sanitization design is documented.

## Security Acceptance Checklist

* Logout clears Redux state and WebSocket state.
* No sensitive data is persisted in browser storage.
* Download paths never reveal backend filesystem locations.
* WebSocket reconnect stops on authorization errors.
* CSP-compatible asset and script loading is documented.
* UI authorization checks are treated as UX only, not enforcement.
