# User Flows

## 1. Overview

User flows define the decision paths for common platform tasks. The primary flow starts from an authenticated project dashboard and ends with either a completed training artifact or a failure review path.

Primary diagram: [primary-user-flow.mermaid](diagrams/primary-user-flow.mermaid)

## 2. Flow: Authenticate and View Projects

| Step | User Action | System Response | Next State |
| --- | --- | --- | --- |
| 1 | Opens platform. | Checks session. | Authenticated users continue; unauthenticated users go to login. |
| 2 | Signs in with company credentials. | Creates or restores session and loads `/auth/me`. | Redirect to intended route or `/projects`. |
| 3 | Views dashboard. | Loads authorized projects only. | User searches, filters, or opens a project. |
| 4 | Attempts unauthorized route. | Backend denies access. | Show `/403` and provide return to projects. |

## 3. Flow: Start Training Job

| Step | User Action | System Response | Next State |
| --- | --- | --- | --- |
| 1 | Opens a project detail page. | Loads project summary, config, latest job, and history preview. | Project detail ready. |
| 2 | Reviews branch, dataset version, and configuration. | Validates visible config data. | User can continue or fix validation issues. |
| 3 | Selects `Start Training`. | Opens confirmation dialog with execution snapshot. | User confirms or cancels. |
| 4 | Confirms launch. | Creates job as `CREATED`, validates snapshot, then moves valid job to `QUEUED`. | Redirect to job detail. |
| 5 | Capacity available. | Scheduler moves job to `RUNNING`. | User monitors progress and logs. |
| 6 | Capacity unavailable. | Job remains `QUEUED` with queue position. | User may leave page and return later. |

## 4. Flow: Monitor Running Job

| Step | User Action | System Response | Next State |
| --- | --- | --- | --- |
| 1 | Opens job detail. | Loads current job, recent logs, artifact state, and stream token. | Monitoring view ready. |
| 2 | Watches progress. | WebSocket sends status, progress, and log events. | UI appends logs and updates status. |
| 3 | Searches logs. | Filters visible log buffer without stopping stream. | User can clear search or download logs. |
| 4 | WebSocket disconnects. | Shows degraded banner and uses polling fallback. | Reconnect or continue with slower updates. |
| 5 | Job completes. | Status changes to `SUCCESS`, `FAILED`, or `CANCELLED`. | Terminal outcome flow. |

## 5. Flow: Cancel Job

| Step | User Action | System Response | Next State |
| --- | --- | --- | --- |
| 1 | Selects cancel on `QUEUED` or `RUNNING` job. | Opens danger confirmation dialog. | User confirms with optional reason or cancels. |
| 2 | Confirms cancellation. | Backend verifies permission and job state. | Valid request proceeds; invalid request returns error. |
| 3 | Job is cancellable. | Marks job `CANCELLED`, stops or removes execution as appropriate. | Job detail shows cancelled terminal state. |
| 4 | Job is already terminal. | Returns state conflict. | UI refreshes job and explains action is no longer available. |

## 6. Flow: Retry Failed or Cancelled Job

| Step | User Action | System Response | Next State |
| --- | --- | --- | --- |
| 1 | Opens terminal failed or cancelled job. | Shows failure summary, prior config snapshot, and retry action when permitted. | User reviews cause. |
| 2 | Selects retry. | Opens dialog explaining that retry creates a new job. | User confirms or cancels. |
| 3 | Confirms retry. | Creates a new job from the selected snapshot or current config policy. | Redirect to new job detail. |
| 4 | New job accepted. | New job follows `CREATED` to `QUEUED` to `RUNNING`. | User monitors new job. |

## 7. Flow: Review Artifacts

| Step | User Action | System Response | Next State |
| --- | --- | --- | --- |
| 1 | Opens successful job detail or artifacts tab. | Loads artifact list for authorized project and job. | Artifact list visible. |
| 2 | Reviews artifact metadata. | Shows name, type, size, created time, and job association. | User chooses download. |
| 3 | Downloads artifact. | Backend authorizes and streams file. | Download starts or error message appears. |
| 4 | Download fails. | Shows error with correlation ID where available. | User may retry or contact admin. |

## 8. Flow: Admin Queue Review

| Step | User Action | System Response | Next State |
| --- | --- | --- | --- |
| 1 | Admin opens queue monitor. | Verifies admin role and loads queue snapshot. | Queue table visible. |
| 2 | Filters by status or project. | Updates visible queue rows. | Admin identifies stuck or failed work. |
| 3 | Opens job from queue. | Loads job detail with admin troubleshooting context. | Admin reviews status and logs. |
| 4 | Cancels job when needed. | Requires confirmation and records audit metadata. | Queue and job detail refresh. |

## 9. Error and Empty States

| Scenario | UX Behavior |
| --- | --- |
| No projects | Show empty state with support contact or registration action when permitted. |
| No training history | Show empty state on history tab and keep `Start Training` available when permitted. |
| No artifacts | Explain that artifacts appear after successful jobs. |
| Unauthorized access | Show `/403` and avoid leaking resource details. |
| Missing job | Show `/404` if not found after authorization check. |
| Stream unavailable | Show degraded monitoring banner and use polling fallback. |
| Validation failure | Show field-level errors and keep user on the current form. |

