# Measurement and Validation

## Performance Metrics

| Metric | Target |
| --- | --- |
| Dashboard initial load | Within 3 seconds under MVP data volume. |
| Route transition feedback | Visible within 100 ms. |
| WebSocket update render | Within 5 seconds under normal load. |
| Log append render | No visible UI freeze during normal stream volume. |
| Log search first page | Within 5 seconds. |
| Mobile interaction target | Minimum `44px x 44px`. |
| Cumulative layout shift | Avoid visible layout shift from route and component loading. |

## Bundle Budgets

| Bundle | Target |
| --- | ---: |
| Initial unauthenticated shell | 150 KB gzip |
| Initial authenticated dashboard | 250 KB gzip |
| Project detail route chunk | 150 KB gzip |
| Job detail route chunk excluding log viewer | 180 KB gzip |
| Log viewer chunk | 150 KB gzip |
| Admin chunk | 150 KB gzip |

## Validation Tools

When the frontend project exists, add commands for:

```bash
npm run build
npm run test
npm run lint
npm run typecheck
npm run e2e
```

If bundle analyzer tooling is added, document the exact command and artifact location in this module.

## Manual UX Validation

| Viewport | Checks |
| --- | --- |
| Mobile `375px x 667px` | Dashboard, project detail, job detail, dialogs, log viewer, YAML editor. |
| Desktop `1920px x 1080px` | Dashboard density, job detail split layout, admin tables, log viewer height. |
| Slow network | Route loading, skeleton stability, mutation pending states, fallback polling. |
| WebSocket disconnect | Reconnect banner, log preservation, REST recovery. |
| Authorization failure | Forbidden state without sensitive data leakage. |
| System theme change | Website switches between light and dark mode without reload or incorrect-theme flash. |

## Test Coverage

| Test Type | Scope |
| --- | --- |
| Unit tests | Retry policy, event dedupe, Redux request keys, permission helpers, theme resolver, formatting utilities. |
| Component tests | Loading states, empty states, status badges, dialogs, log viewer controls. |
| Integration tests | Start training, cancel, retry, WebSocket fallback, Redux slice refresh. |
| E2E tests | Project dashboard to job detail, live monitoring, artifact download, admin access. |
| Accessibility checks | Keyboard navigation, focus management, status text, dialog behavior. |

## Release Acceptance Checklist

* Route-level code splitting is implemented for major feature areas.
* Heavy components such as YAML editor and log viewer are lazy-loaded.
* Dashboard does not load admin code.
* Built asset sizes are reviewed against bundle budgets.
* Static assets use compression and long-lived caching where safe.
* Long log output remains responsive.
* Mobile and desktop viewport checks pass.
* System-based light and dark mode checks pass.
* Security checklist is reviewed before release.
* Performance regressions are documented with mitigation or explicit acceptance.
