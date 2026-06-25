**# API Service Layer

The frontend uses Axios as the shared REST transport and Redux Toolkit as the client-side state and synchronization layer.

## Recommended Folder Structure

```text
frontend/src/
  shared/
    api/
      axios-client.ts
      api-error.ts
      request-context.ts
      download-client.ts
      generated/
        types.ts
        client.ts
    realtime/
      job-stream-client.ts
      reconnect-policy.ts
      event-dedupe.ts
  entities/
    user/
      selectors.ts
      service.ts
    project/
      selectors.ts
      thunks.ts
      service.ts
    configuration/
      selectors.ts
      thunks.ts
      service.ts
    training-job/
      selectors.ts
      thunks.ts
      service.ts
    log/
      selectors.ts
      service.ts
    artifact/
      selectors.ts
      service.ts
    notification/
      selectors.ts
      thunks.ts
      service.ts
    admin/
      selectors.ts
      thunks.ts
      service.ts
  store/
    root-reducer.ts
    listener-middleware.ts
    hooks.ts
    slices/
      auth-slice.ts
      project-slice.ts
      configuration-slice.ts
      job-slice.ts
      log-slice.ts
      artifact-slice.ts
      notification-slice.ts
      admin-slice.ts
      ui-slice.ts
      theme-slice.ts
```

## Layer Responsibilities

| Layer                  | Responsibility                                                                               | Must Not Do                         |
| ---------------------- | -------------------------------------------------------------------------------------------- | ----------------------------------- |
| `axios-client.ts`      | Shared Axios instance, base URL, credentials, headers, interceptors, response normalization. | Contain feature-specific logic.     |
| `api-error.ts`         | Convert backend error responses into `ApiError`.                                             | Render UI messages directly.        |
| `download-client.ts`   | Stream authorized log and artifact downloads.                                                | Load large files fully into memory. |
| Generated client/types | Provide OpenAPI-aligned request and response types.                                          | Override backend contract manually. |
| Entity service         | Expose resource-specific Axios functions.                                                    | Store React state.                  |
| Redux slices/thunks    | Define state shape, request status, cache timestamps, invalidation, optimistic updates.      | Implement raw Axios details.        |
| Feature components     | Compose Redux selectors, thunks/actions, and UI states.                                      | Build endpoint URLs manually.       |

## Axios Client Contract

```typescript
import axios, { AxiosError, AxiosRequestConfig } from 'axios';

export type ApiError = {
  code: string;
  message: string;
  correlationId: string;
  status: number;
  details?: Array<{
    field?: string;
    reason: string;
  }>;
};

export const axiosClient = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
});

export type ApiRequestConfig = AxiosRequestConfig & {
  idempotencyKey?: string;
};
```

## Required HTTP Behavior

| Concern | Requirement |
| --- | --- |
| Base URL | Use `/api/v1`. |
| Auth | Support secure session cookie or bearer token based on backend configuration. |
| Credentials | Include credentials when session-cookie authentication is used. |
| Headers | Send `Content-Type: application/json` for JSON requests. |
| Interceptors | Use Axios interceptors for auth handling, correlation ID extraction, and `ApiError` normalization. |
| Idempotency | Send `Idempotency-Key` for start, cancel, retry, upload, and admin status mutations when supported. |
| Abort | Pass `AbortSignal` from Redux async thunk or component lifecycle. |
| Error shape | Normalize `ErrorResponse` into `ApiError`. |
| Correlation ID | Preserve backend `correlationId` for support and troubleshooting. |
| Downloads | Use blob or streamed download path through authorized backend endpoint. |

## Service Catalog

| Service                | Endpoint Coverage                                                                                                                                                    | Primary Consumers                           |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------- |
| `authService`          | `GET /auth/me`, `POST /auth/logout`                                                                                                                                  | Route guards, app shell, user menu.         |
| `userService`          | `GET /admin/users`, `POST /admin/users/{userId}/status`                                                                                                              | Admin user management.                      |
| `projectService`       | `GET /projects`, `POST /projects`, `POST /projects/upload-zip`, `GET /projects/{projectId}`                                                                          | Dashboard, registration, project detail.    |
| `configurationService` | `GET /projects/{projectId}/configs`, `GET /projects/{projectId}/configs/{configId}`, `POST /projects/{projectId}/configs/validate`                                   | Config editor, start training dialog.       |
| `jobService`           | `GET /projects/{projectId}/jobs`, `POST /projects/{projectId}/jobs`, `GET /jobs/{jobId}`, `POST /jobs/{jobId}/cancel`, `POST /jobs/{jobId}/retry`, `GET /jobs/queue` | Project history, job detail, admin queue.   |
| `logService`           | `GET /jobs/{jobId}/logs`, `GET /jobs/{jobId}/logs/download`                                                                                                          | Log viewer, log download.                   |
| `artifactService`      | `GET /jobs/{jobId}/artifacts`, `GET /artifacts/{artifactId}/download`                                                                                                | Job detail artifact list.                   |
| `notificationService`  | `GET /notifications`, `POST /notifications/{notificationId}/read`                                                                                                    | Notification menu and list.                 |
| `auditService`         | `GET /audit-logs`                                                                                                                                                    | Admin or owner audit views when authorized. |
