# Error Handling

## Normalized Error Shape

All failed API calls should become `ApiError`.

```typescript
export type ApiError = {
  status: number;
  code: string;
  message: string;
  correlationId: string;
  details?: Array<{
    field?: string;
    reason: string;
  }>;
};
```

## Error Handling Matrix

| HTTP Status | Meaning | UI Behavior | Retry |
| --- | --- | --- | --- |
| `400` | Bad request or validation issue. | Show form or inline validation details. | No automatic retry. |
| `401` | User is unauthenticated. | Clear auth cache and redirect to company login. | No automatic retry after redirect. |
| `403` | User lacks permission. | Show forbidden state without leaking resource details. | No retry. |
| `404` | Resource not found or hidden by access policy. | Show not found state. | No retry. |
| `409` | Conflict, duplicate request, or invalid state transition. | Refresh relevant resource and show conflict message. | No automatic retry. |
| `413` | Payload too large. | Show upload size guidance. | No retry without user change. |
| `415` | Unsupported media type. | Show accepted file type guidance. | No retry without user change. |
| `429` | Rate limit or platform busy. | Show busy message and queue context when available. | Retry with backoff only for safe reads. |
| `500` | Server error. | Show retryable error with correlation ID. | Retry safe reads only. |
| `502/503/504` | Temporary backend or gateway outage. | Show degraded state. | Retry safe reads with backoff. |
| Network error | Browser cannot reach API. | Show offline or connection issue. | Retry safe reads with backoff. |

## Error Presentation Rules

* Field-level validation errors appear beside the affected field.
* Page-level errors show an action to retry safe reads.
* Mutation errors show contextual messages near the triggering action.
* Unknown server errors must display the `correlationId` when available.
* Permission errors must not reveal hidden project, log, source, or artifact details.
* Download errors should show a retry action but must not expose filesystem paths.

## Offline and Degraded Behavior

| Scenario | Behavior |
| --- | --- |
| Browser offline | Pause non-critical requests, show offline banner, keep cached read-only data visible. |
| Backend unavailable | Show degraded state and retry safe reads with backoff. |
| WebSocket disconnected | Show reconnecting banner, keep logs visible, resume with REST recovery. |
| WebSocket unauthorized | Stop reconnect, clear stream state, show permission error. |
| Platform busy | Show queue or capacity message; refresh queue snapshot when authorized. |

The platform does not support offline mutations for MVP. Actions that change backend state must require live connectivity.

