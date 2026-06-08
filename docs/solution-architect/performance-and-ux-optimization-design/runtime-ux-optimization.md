# Runtime UX Optimization

## Loading UX

| State | Design Requirement |
| --- | --- |
| App startup | Show app shell and route-level loading state while `/auth/me` resolves. |
| Dashboard loading | Show table skeleton with stable row heights. |
| Project detail loading | Show page header skeleton and panel skeletons. |
| Job detail loading | Show status panel skeleton and log viewer placeholder. |
| Mutation pending | Disable submit action, preserve button width, show loading indicator. |
| Download pending | Show inline loading state on the download button. |

Skeletons must reserve approximate final dimensions to prevent layout shift.

## Data Table Optimization

* Use server-side pagination for project history, logs, notifications, and audit records.
* Use virtualized rendering for large tables only when row count makes normal rendering slow.
* Keep row height stable: `40px` to `48px` desktop, stacked summaries on mobile.
* Debounce search input before updating URL filters and Redux request keys.
* Keep filters in URL query params when the state should be restorable.
* Avoid rendering hidden columns on mobile when stacked summaries are used.

## Log Viewer Optimization

The log viewer is the most performance-sensitive frontend component.

| Concern | Strategy |
| --- | --- |
| Live append | Append incrementally and batch DOM updates. |
| Large output | Use virtualization or windowing. |
| Auto-scroll | Auto-scroll only when user is already near the bottom. |
| Search | Prefer server-side search for large logs; highlight visible matches only. |
| Filtering | Filter by `STDOUT`, `STDERR`, or all streams through query params. |
| Memory | Cap in-memory live buffer and fetch older lines by cursor. |
| Reconnect | Resume from last event ID, cursor, or log offset. |

## YAML Editor Optimization

* Lazy-load the editor only when configuration editing is visible.
* Keep YAML drafts local until validation or start-training submission.
* Debounce validation requests.
* Show validation results without re-rendering the full project detail page.
* Use horizontal scroll on mobile rather than shrinking code text below readable size.

## WebSocket UX

| Connection State | UI Behavior |
| --- | --- |
| Connected | Show subtle connected indicator on job detail. |
| Connecting | Show non-blocking connection banner. |
| Reconnecting | Keep existing logs visible and show reconnect attempt state. |
| Fallback polling | Show degraded state and continue status/log refresh. |
| Unauthorized | Stop reconnect attempts and show permission message. |
| Disconnected after terminal status | Do not show alarming error if terminal state is already confirmed. |

## Mobile UX

Mobile baseline is iPhone SE 2020 or greater.

* Use a single-column route layout from `375px` width.
* Use touch targets of at least `44px x 44px`.
* Keep form input font size at least `16px`.
* Stack job status, progress, actions, logs, and artifacts.
* Use horizontal scrolling only for logs and YAML editor.
* Avoid hover-only controls.
* Keep dialogs within `calc(100dvh - 32px)` with internal scroll.

## Desktop UX

Desktop baseline is 1080p or greater.

* Use persistent sidebar navigation for `1200px` and above.
* Use dense but readable tables.
* Use split job detail layout on wide screens: logs as primary area, status/actions as secondary area.
* Keep log viewer at least `480px` tall.
* Keep dashboard and admin pages constrained to operational max width, around `1600px`.

## Accessibility Optimization

* Use semantic landmarks for app shell, navigation, main content, and dialogs.
* Ensure all lazy-loaded route fallbacks include accessible labels.
* Preserve focus on route transitions and return focus after dialogs close.
* Use text labels with job statuses.
* Respect reduced motion preferences for spinners and live indicators.

## Acceptance Checklist

* Long logs remain scrollable and responsive.
* Route transitions show stable loading states.
* Active job monitoring remains usable during WebSocket reconnect.
* Mobile layout works from `375px x 667px`.
* Desktop layout uses available 1080p space without oversized marketing-style sections.
