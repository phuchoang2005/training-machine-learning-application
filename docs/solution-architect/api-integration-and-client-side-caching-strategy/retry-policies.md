# Retry Policies

## Retry Classification

| Operation Type | Examples | Automatic Retry Policy |
| --- | --- | --- |
| Safe reads | `GET /projects`, `GET /jobs/{jobId}`, `GET /notifications` | Retry up to 2 times for network, `500`, `502`, `503`, `504`, and safe `429`. |
| Live polling reads | Active job detail and logs during fallback polling | Retry continuously with capped backoff while page is open. |
| Downloads | Log and artifact downloads | No automatic retry after browser download starts; allow user-triggered retry. |
| Validation | YAML validation | No automatic retry unless network failure and user explicitly retries. |
| Mutations | Start, cancel, retry, mark notification read, user status update | No automatic retry unless an idempotency key is used and the failure is before response receipt. |
| Uploads | ZIP project upload | No automatic retry by default; user-triggered retry only. |

## Backoff Rules

| Attempt | Delay |
| --- | ---: |
| 1 | 500 ms |
| 2 | 1500 ms |
| 3 | 3000 ms |

Use jitter of `+/- 25%` for concurrent clients. Cap background polling fallback at 30 seconds when the browser tab is hidden.

## Mutation Retry Rules

| Mutation | Retry Policy | Reason |
| --- | --- | --- |
| Start training | Manual retry only; use `Idempotency-Key`. | Avoid duplicate jobs. |
| Cancel job | Manual retry only; use `Idempotency-Key`. | Avoid misleading repeated cancellation state. |
| Retry job | Manual retry only; use `Idempotency-Key`. | Avoid duplicate retry jobs. |
| Mark notification read | Optimistic update with rollback; one automatic retry allowed only for network failure. | Low-risk user preference update. |
| Update user status | Optimistic update with rollback; manual retry. | Admin action must be explicit. |
| Upload ZIP project | Manual retry only. | Large payload and validation-sensitive operation. |

## Optimistic Updates

Use optimistic updates only when rollback is clear and backend conflict behavior is well understood.

| Operation | Optimistic Update | Rollback |
| --- | --- | --- |
| Mark notification read | Set item status to `READ` immediately. | Restore previous status and show error. |
| Update user status | Set row status immediately. | Restore previous status and show admin error. |
| Cancel job | Show `Cancel requested` local pending state, not terminal `CANCELLED`. | Remove pending state if request fails. |
| Start training | Seed new job detail from `StartJobResponse`. | Remove seeded detail only if request fails before job ID is returned. |
| Retry job | Seed new retry job detail from `RetryJobResponse`. | Remove seeded detail only if request fails before job ID is returned. |

