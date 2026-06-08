# Security and Testing

## Security and Privacy Controls

* Do not store bearer tokens in local storage.
* Prefer secure HTTP-only session cookies if supported by backend authentication.
* Do not cache source code, log downloads, artifact downloads, or sensitive filesystem paths in persistent browser storage.
* Clear all Redux slices and in-memory API-backed data on logout.
* Scope cached project, job, log, and artifact data by authenticated user session.
* Treat `403` and `404` responses as non-cacheable for sensitive resource detail routes.
* Do not include secrets, tokens, local filesystem paths, or infrastructure credentials in frontend logs.

## Testing Strategy

| Test Type | Scope |
| --- | --- |
| Unit tests | Redux request key factories, retry classifier, error normalization, event dedupe. |
| Service tests | Endpoint path construction, request bodies, download behavior, idempotency headers. |
| Redux tests | Slice refresh behavior after start, cancel, retry, mark read, and admin status updates. |
| WebSocket tests | Reconnect, missed log recovery, duplicate event handling, unauthorized stream handling. |
| Component integration tests | Error states, loading states, optimistic rollback, fallback polling indicators. |
| End-to-end tests | Project registration, start training, live monitoring, cancel, retry, artifact download. |

## Acceptance Checklist

The API integration and caching strategy is ready for implementation when:

* API calls are routed through entity service functions, not directly from feature components.
* Redux request keys are deterministic and include identifiers and filters.
* Slice refresh rules are implemented for all mutations.
* WebSocket events update job detail, progress, logs, artifacts, queue, and notifications consistently.
* REST polling fallback resumes active job monitoring when WebSocket fails.
* `ApiError` normalization preserves `code`, `message`, `status`, `correlationId`, and field details.
* Retry policies distinguish safe reads, downloads, uploads, and mutations.
* Logout clears all user-scoped Redux data.
* Sensitive data is not persisted in browser storage.
