# Design System and Component Specification

## 1. Purpose

This document defines the design system and reusable UI component specification for the Future frontend. It supports the React, TypeScript, Vite, Redux, TailwindCSS (with hand-authored semantic CSS), Radix UI, and CSS-based motion architecture described in `docs/solution-architect/frontend-architecture-document/README.md`.

The design system is optimized for operational workflows: project discovery, configuration review, training launch, real-time job monitoring, log inspection, artifact download, notifications, and administrator queue or user management.

## 2. Design Principles

* Treat `Future` as the product name across app chrome, page titles, and browser metadata.
* Use a star-based logo mark that aligns with the Pleiades/Sirius celestial visual direction without reducing operational clarity.
* Prioritize scanability over decoration. Users must quickly compare projects, jobs, statuses, queue positions, durations, and failure messages.
* Keep actions predictable. Destructive actions such as cancel and delete must require confirmation.
* Do not rely on color alone. Job status, alerts, and progress states need text, icon, or shape indicators.
* Preserve data density on desktop while keeping mobile workflows usable from iPhone SE 2020 size and above.
* Keep security boundaries visible. Disabled or hidden actions must reflect role and ownership rules, while backend authorization remains authoritative.
* Make degraded states explicit. WebSocket reconnect, platform busy, missing progress, and failed downloads need clear UI states.
* Respect system theme preference. The website must switch between light mode and dark mode based on the user's operating system or browser setting.

## 3. Product UI Surfaces

| Surface | Primary User Goal | Main Components |
| --- | --- | --- |
| Project Dashboard | Find an authorized AI project and inspect latest training status. | App shell, search input, filter controls, project table, status badge, empty state. |
| Project Registration | Register GitHub or ZIP project source. | Form field, file upload, validation message, action button, progress indicator. |
| Project Detail | Review source, dataset, configuration, and training history. | Page header, summary panel, tabs, YAML editor, data table, start training dialog. |
| Job Detail | Monitor job status, logs, progress, artifacts, cancel, and retry. | Status panel, progress indicator, log viewer, connection banner, action toolbar, artifact list. |
| Notifications | Review job outcome notifications and delivery failures. | Notification list, badge, status marker, timestamp, mark-read action. |
| Admin Console | Manage users and queue visibility without exposing sensitive project data. | Admin table, queue snapshot, status badge, confirmation dialog. |

## 4. Design Tokens

Design tokens are implemented through TailwindCSS v4 theme variables, shadcn/ui CSS variables, and shared tokens in `src/assets/styles/tailwind.css`.

### Color Tokens

Use a restrained operational palette with neutral surfaces and semantic colors. Do not encode job status using color only.

| Token | Light Value | Dark Value | Usage |
| --- | --- | --- | --- |
| `--background` | `#f8fafc` | `#020617` | Application background. |
| `--foreground` | `#0f172a` | `#e2e8f0` | Primary text. |
| `--card` | `#ffffff` | `#0f172a` | Panels, tables, dialogs. |
| `--muted` | `#f1f5f9` | `#1e293b` | Secondary panels and table header background. |
| `--border` | `#cbd5e1` | `#334155` | Component borders and dividers. |
| `--muted-foreground` | `#475569` | `#94a3b8` | Secondary text and metadata. |
| `--primary` | `#2563eb` | `#60a5fa` | Primary action and active navigation. |
| `--primary-hover` | `#1d4ed8` | `#93c5fd` | Primary action hover. |
| `--success` | `#15803d` | `#4ade80` | `SUCCESS` status and successful validation. |
| `--warning` | `#b45309` | `#fbbf24` | `QUEUED`, retry, degraded WebSocket state. |
| `--destructive` | `#b91c1c` | `#f87171` | `FAILED`, destructive action, validation failure. |
| `--info` | `#0369a1` | `#38bdf8` | `RUNNING`, progress, informational alerts. |
| `--disabled` | `#94a3b8` | `#64748b` | Disabled controls and unavailable actions. |
| `--code-background` | `#0b1220` | `#020617` | Log viewer and code editor background. |
| `--code-foreground` | `#e2e8f0` | `#e2e8f0` | Log viewer text. |

### Theme Mode

The frontend must support light and dark mode using system preference by default.

| Concern | Requirement |
| --- | --- |
| Source of truth | Use `prefers-color-scheme` on first load and store the resolved mode in Redux `theme` state. |
| Tailwind mode | Configure Tailwind dark variants and theme tokens through `src/assets/styles/tailwind.css` and a root `.dark` class or equivalent selector. |
| Initial paint | Apply the resolved theme class before React renders to avoid flash of incorrect theme. |
| System changes | Listen for `prefers-color-scheme` changes and update the root theme class when no manual override exists. |
| shadcn/ui | Keep shadcn/ui CSS variables mapped for both light and dark themes. |
| Accessibility | Maintain contrast in both themes for status badges, focus rings, log text, and destructive actions. |

### Status Tokens

| Job Status | Token | Icon Guidance | Text Label |
| --- | --- | --- | --- |
| `CREATED` | `--muted-foreground` | Clock or document icon | Created |
| `QUEUED` | `--warning` | Queue icon | Queued |
| `RUNNING` | `--info` | Activity or spinner icon | Running |
| `RETRYING` | `--warning` | Refresh icon | Retrying |
| `SUCCESS` | `--success` | Check icon | Success |
| `FAILED` | `--destructive` | Alert icon | Failed |
| `CANCELLED` | `--disabled` | Stop icon | Cancelled |

### Typography Tokens

| Token | Desktop | Mobile | Usage |
| --- | --- | --- | --- |
| `--font-family-sans` | `Aptos, SF Pro Display, Segoe UI Variable, Inter, system-ui, sans-serif` | Same | Application text with a softer, modern operational feel. |
| `--font-family-mono` | `JetBrains Mono, ui-monospace, monospace` | Same | Logs, YAML, IDs. |
| `--font-size-xs` | `12px` | `12px` | Table metadata, hints. |
| `--font-size-sm` | `14px` | `14px` | Body text, form fields. |
| `--font-size-md` | `16px` | `16px` | Dialog copy, important labels. |
| `--font-size-lg` | `20px` | `18px` | Section heading. |
| `--font-size-xl` | `24px` | `20px` | Page title. |
| `--line-height-tight` | `1.25` | `1.25` | Headings. |
| `--line-height-normal` | `1.5` | `1.5` | Body content. |
| `--line-height-code` | `1.45` | `1.45` | Log and YAML text. |

### Spacing Tokens

Use a 4px spacing base.

| Token | Value | Usage |
| --- | --- | --- |
| `--space-1` | `4px` | Tight gaps. |
| `--space-2` | `8px` | Inline controls and compact groups. |
| `--space-3` | `12px` | Form field groups. |
| `--space-4` | `16px` | Panel padding on mobile. |
| `--space-5` | `20px` | Form sections. |
| `--space-6` | `24px` | Page section gaps. |
| `--space-8` | `32px` | Desktop page section gaps. |

### Shape and Elevation Tokens

| Token | Value | Usage |
| --- | --- | --- |
| `--radius-sm` | `4px` | Inputs, badges, small controls. |
| `--radius-md` | `6px` | Buttons, table containers, dialogs. |
| `--radius-lg` | `8px` | Maximum card radius. |
| `--shadow-dialog` | `0 18px 48px rgba(15, 23, 42, 0.18)` | Dialogs and popovers. |
| `--focus-ring` | `0 0 0 3px color-mix(in srgb, hsl(var(--ring)) 28%, transparent)` | Keyboard focus state. |

### Motion Tokens

Use plain CSS keyframe animations for frontend motion. Motion should make transitions feel calm, vivid, and hopeful without reducing scanability or slowing operational work.

| Token | Value | Usage |
| --- | --- | --- |
| `--motion-duration-fast` | `120ms` | Hover feedback, small icon state changes. |
| `--motion-duration-standard` | `220ms` | Page, panel, and card reveal transitions. |
| `--motion-duration-slow` | `420ms` | Sparse background glow or one-time empty-state reveal. |
| `--motion-ease-out` | `[0.16, 1, 0.3, 1]` | Default entrance easing. |
| `--motion-ease-in` | `[0.4, 0, 1, 1]` | Exit transitions. |
| `--motion-y-small` | `8px` | Subtle vertical lift for route and panel entry. |
| `--motion-scale-dialog` | `0.98` to `1` | Dialog entrance scale. |

### Motion Design Rules

| Area | Requirement |
| --- | --- |
| Library | Use plain CSS keyframes; do not introduce a JavaScript animation library for React components. |
| Scope | Keep reusable keyframes in `src/assets/styles/layout.css` and apply them through semantic classes (`.page`, `.table-row`, `.dialog`). |
| Purpose | Use motion to preserve spatial orientation, show live system state, soften loading, and support the Pleiades/Sirius celestial direction. |
| Performance | Animate `opacity`, `transform`, lightweight SVG accents, and small visible lists. Do not animate table layout, log stream rows, or large DOM collections. |
| Reduced motion | Honor `prefers-reduced-motion`; disable the keyframe animations and fall back to static state. |
| Timing | Keep workflow motion under `450ms`; long ambient background motion must be subtle and non-blocking. |
| Data integrity | Never delay status, error, or security feedback for animation completion. |

## 5. Responsive Styling Guide

### Supported Viewports

| Target | Minimum Resolution | Layout Intent |
| --- | --- | --- |
| Mobile | iPhone SE 2020 or greater: `375px x 667px` CSS viewport baseline | Single-column workflows, compact controls, horizontal overflow only for dense data grids. |
| Desktop Browser | Equal or greater than 1080p: `1920px x 1080px` baseline | Dense operational dashboard with persistent navigation and multi-column content. |

### Breakpoints

| Breakpoint | Width | Usage |
| --- | --- | --- |
| `mobile` | `375px` and above | Required minimum layout support. |
| `tablet` | `768px` and above | Two-column panels and wider dialogs. |
| `desktop` | `1200px` and above | Persistent sidebar and dense tables. |
| `wide` | `1440px` and above | Wider log viewer and job detail split layout. |

### Mobile Rules

* Use a single-column page layout from `375px` width.
* Keep the primary action visible near the page heading or sticky bottom action area when the action is workflow-critical.
* Use `16px` minimum font size for inputs to avoid mobile browser zoom.
* Use minimum touch target size of `44px x 44px`.
* Collapse table columns into stacked row summaries for project, job, notification, and artifact lists.
* Allow horizontal scrolling only for log viewer and YAML editor content.
* Keep dialogs within viewport using `max-height: calc(100dvh - 32px)` and internal scrolling.
* Keep page padding at `16px`; reduce dense table padding only where row readability remains acceptable.
* Avoid hover-only interactions. Every tooltip action must have a tap-accessible label, menu, or detail view.
* On job detail, stack status panel, progress panel, action toolbar, log viewer, and artifacts vertically.

### Desktop Rules

* Use persistent left navigation for `1200px` and above.
* Use constrained content width for form-heavy pages: `960px` to `1120px`.
* Use wider operational pages for dashboards and job monitoring: up to `1600px`.
* Use two-column project detail layout when space permits: metadata/config summary beside history or actions.
* Use split job detail layout on wide screens: status and actions in a side column, log viewer as the main area.
* Keep data tables dense but readable: `40px` to `48px` row height, sticky header where table height exceeds viewport.
* Keep log viewer height at least `480px` on 1080p desktop.
* Do not use oversized hero sections; this is an operational tool.
* Preserve visible focus rings for keyboard users.

### Layout Grid

```css
@layer base {
  :root {
    --page-padding-mobile: 16px;
    --page-padding-desktop: 32px;
    --content-max-form: 1120px;
    --content-max-ops: 1600px;
    --sidebar-width: 248px;
    --topbar-height: 56px;
  }

  .dark {
    color-scheme: dark;
  }
}
```

### Responsive Page Patterns

| Pattern | Mobile | Desktop |
| --- | --- | --- |
| App shell | Top bar with menu button; navigation in drawer. | Persistent sidebar plus top utility area. |
| Dashboard | Search and filters stacked above card-style row summaries. | Search and filters in toolbar above data table. |
| Project detail | Tabs and panels stacked. | Summary plus config/history split where useful. |
| Job detail | Status, actions, logs, artifacts stacked. | Log viewer primary, status/actions secondary. |
| Dialog | Full-width inset dialog with internal scroll. | Centered dialog with fixed max width. |
| Tables | Stacked row summaries; optional horizontal scroll for admin grids. | Full table with sticky header, sortable columns. |

## 6. UI Component Catalog

### Foundation Components

| Component | Purpose | Required Variants | Key States |
| --- | --- | --- | --- |
| `Button` | Trigger commands. Use shadcn/ui `Button` as baseline. | Primary, secondary, ghost, danger, icon-only. | Default, hover, focus, loading, disabled. |
| `IconButton` | Compact command with tooltip or accessible label. | Default, danger, selected. | Default, hover, focus, disabled. |
| `TextField` | Text entry for search, names, URLs, reasons. Compose shadcn/ui `Input` with the platform field wrapper. | Text, email, URL, search. | Default, focus, invalid, disabled, loading. |
| `TextArea` | Multi-line entry for cancellation reason and descriptions. | Standard, monospace. | Default, focus, invalid, disabled. |
| `Select` | Choose controlled option sets. | Single select. | Default, focus, invalid, disabled. |
| `Checkbox` | Confirm binary choices. | Standard. | Checked, unchecked, indeterminate, disabled. |
| `SegmentedControl` | Switch between small sets such as source type. | Two or three items. | Selected, focus, disabled. |
| `Tabs` | Switch page sections. | Horizontal; scrollable on mobile. | Active, focus, disabled. |
| `Badge` | Show compact metadata. | Neutral, info, success, warning, danger. | Static. |
| `StatusBadge` | Show job status. | Job status values. | Static, animated only for `RUNNING`. |
| `Tooltip` | Explain icon-only controls. | Text only. | Hover, focus. |
| `Dialog` | Confirmation and focused workflow. Use Radix Dialog through shadcn/ui. | Standard, danger confirmation, form dialog. | Open, loading submit, validation error. |
| `Toast` | Short-lived feedback. | Success, info, warning, danger. | Visible, dismissing. |
| `Banner` | Persistent page-level state. | Info, warning, danger, degraded. | Visible, dismissible when safe. |
| `EmptyState` | Explain empty resource collections. | No projects, no jobs, no artifacts, no notifications. | Static with optional action. |
| `LoadingState` | Indicate pending data. | Skeleton, spinner, inline. | Loading. |
| Motion classes | Shared CSS keyframe classes (`fade-in-up`, `dialog-in`) for route, dialog, and panel enter transitions. | Page, panel, dialog, list. | Entering, visible, reduced-motion. |

### Navigation Components

| Component | Purpose | Required Behavior |
| --- | --- | --- |
| `AppShell` | Defines top-level layout. | Mobile drawer navigation; desktop sidebar; content region landmark. |
| `TopBar` | Shows page utilities. | User menu, notification menu, WebSocket/degraded indicator when global. |
| `SideNav` | Primary navigation. | Projects, notifications, admin items based on role. |
| `Breadcrumbs` | Show resource hierarchy. | Project and job detail pages; collapse on mobile. |
| `UserMenu` | User profile and logout. | Shows email, role, logout action. |
| `NotificationMenu` | Quick access to unread notifications. | Unread count badge, recent notifications, link to full page. |

### Data Display Components

| Component | Purpose | Required Behavior |
| --- | --- | --- |
| `DataTable` | Dense tabular data. | Sort, loading rows, empty state, responsive stacked rows. |
| `ProjectTable` | Project dashboard list. | Project name, description, latest status, last training time, owner. |
| `TrainingHistoryTable` | Project job history. | Job ID, owner, status, started, ended, duration, open job action. |
| `QueueSnapshot` | Admin queue status. | Running count, running limit, queued count, queue items. |
| `ArtifactList` | Job artifact browser. | Type, file name, size, created time, download action. |
| `NotificationList` | User notification history. | Status, subject, timestamp, mark read action. |
| `KeyValueList` | Metadata display. | Project details, job details, dataset and repository metadata. |
| `Timestamp` | Consistent date-time display. | Absolute time with optional relative label. |
| `Duration` | Display job duration. | Running duration updates while job is active. |

### Workflow Components

| Component | Purpose | Required Behavior |
| --- | --- | --- |
| `ProjectSearch` | Search authorized projects. | Debounced input, clear action, URL query sync. |
| `GithubProjectForm` | Register repository project. | Repository URL validation, branch input, submit loading. |
| `ZipProjectUploadForm` | Register ZIP project. | File type and size hint, upload progress, validation errors. |
| `ConfigEditor` | Edit YAML configuration. | Monospace editor, dirty state, validate action, error list. |
| `StartTrainingDialog` | Submit immutable config snapshot and create job. | Shows selected config, validation state, idempotent submit. |
| `CancelJobDialog` | Confirm job cancellation. | Requires confirmation and optional reason. |
| `RetryJobDialog` | Retry failed or cancelled job. | Previous or modified configuration mode. |
| `ArtifactDownloadButton` | Download authorized artifact. | Loading state, error state, no full in-memory artifact loading. |
| `LogDownloadButton` | Download logs after completion. | Disabled until authorized and available. |

### Monitoring Components

| Component | Purpose | Required Behavior |
| --- | --- | --- |
| `JobStatusPanel` | Show current job state. | Status badge, queue position, owner, start/end time, failure reason. |
| `ProgressPanel` | Show emitted progress. | Percent, epoch, updated time, unavailable message. |
| `ConnectionBanner` | Show WebSocket state. | Connected, reconnecting, fallback polling, disconnected. |
| `LogViewer` | Inspect training logs. | Append, scroll, search, filter `STDOUT` and `STDERR`, copy selected line, download. |
| `LogLine` | Render one log event. | Timestamp, stream type, content, match highlight. |
| `FailureSummary` | Explain failed job. | Error message, correlation ID if present, link to relevant logs. |

### Admin Components

| Component | Purpose | Required Behavior |
| --- | --- | --- |
| `UserManagementTable` | Manage user status. | Email, full name, role, status, enable/disable action. |
| `UserStatusBadge` | Show user account state. | `ACTIVE`, `DISABLED`. |
| `AdminQueueTable` | Inspect queue without sensitive project details. | Job ID, project name, status, queue position, enqueued time. |
| `AdminActionConfirmDialog` | Confirm administrative operations. | Summarizes impact and audit expectation. |

## 7. Component State Specifications

### Button

| State | Visual Rule | Behavior |
| --- | --- | --- |
| Default | Token color based on variant. | Click triggers action. |
| Hover | Slightly darker background or border. | Desktop only enhancement. |
| Focus | Visible focus ring. | Keyboard accessible. |
| Loading | Spinner plus stable button width. | Prevent duplicate submit. |
| Disabled | Disabled token color and no pointer action. | Include accessible disabled reason where needed. |

### StatusBadge

| Status | Visual Rule | Required Text |
| --- | --- | --- |
| `CREATED` | Neutral outline. | Created |
| `QUEUED` | Warning outline or fill. | Queued |
| `RUNNING` | Info indicator; optional subtle activity icon. | Running |
| `RETRYING` | Warning indicator with retry icon. | Retrying |
| `SUCCESS` | Success indicator with check icon. | Success |
| `FAILED` | Danger indicator with alert icon. | Failed |
| `CANCELLED` | Muted indicator with stop icon. | Cancelled |

### LogViewer

| Requirement | Specification |
| --- | --- |
| Font | Monospace, `13px` desktop, `12px` mobile. |
| Background | `--code-background`. |
| Text | `--code-foreground`. |
| Height | Minimum `320px` mobile, minimum `480px` desktop. |
| Performance | Virtualize or window large log output. |
| Search | Highlight matches and show match count. |
| Filtering | Allow all, `STDOUT`, and `STDERR`. |
| Auto-scroll | Default on while live; pause when user scrolls away from bottom. |
| Accessibility | Provide copyable text and do not rely on color for stream type. |

### ConfigEditor

| Requirement | Specification |
| --- | --- |
| Font | Monospace, `14px` desktop, `13px` mobile. |
| Validation | Inline validation summary with field or line references when available. |
| Dirty State | Show unsaved changes before navigation or start training. |
| Mobile | Editor may scroll horizontally; toolbar remains accessible above editor. |
| Start Training | Submit exact YAML content to create immutable snapshot. |

## 8. Styling Guide

### CSS Architecture

* Define Tailwind theme tokens and shadcn/ui CSS variables in `tailwind.css`.
* Define application resets and base typography through Tailwind base layers.
* Prefer Tailwind utility classes and shadcn/ui variants before adding custom CSS.
* Use Radix UI primitives for accessible dialogs, dropdowns, popovers, tabs, tooltips, and menus.
* Avoid page-specific color overrides except for semantic status cases.
* Prefer CSS grid for page layouts and flexbox for component internals.
* Use container-aware dimensions for tables, log viewer, YAML editor, and action toolbars to avoid layout shift.

### Density

| Element | Mobile | Desktop |
| --- | --- | --- |
| Page padding | `16px` | `32px` |
| Table row height | Stacked rows, minimum `56px` | `40px` to `48px` |
| Button height | `44px` | `36px` to `40px` |
| Input height | `44px` | `36px` to `40px` |
| Dialog max width | `calc(100vw - 32px)` | `480px`, `640px`, or `800px` by use case |
| Form field gap | `12px` | `16px` |

### Mobile Layout Examples

```css
.page {
  padding: var(--page-padding-mobile);
}

.responsive-stack {
  display: grid;
  gap: var(--space-4);
  grid-template-columns: 1fr;
}

.touch-target {
  min-height: 44px;
  min-width: 44px;
}

@media (min-width: 1200px) {
  .page {
    padding: var(--page-padding-desktop);
  }

  .responsive-stack {
    grid-template-columns: minmax(0, 1fr) 360px;
  }
}
```

### Desktop Layout Examples

```css
.app-shell {
  min-height: 100dvh;
}

@media (min-width: 1200px) {
  .app-shell {
    display: grid;
    grid-template-columns: var(--sidebar-width) minmax(0, 1fr);
  }
}

.ops-content {
  max-width: var(--content-max-ops);
  margin: 0 auto;
  width: 100%;
}

.form-content {
  max-width: var(--content-max-form);
  margin: 0 auto;
  width: 100%;
}
```

## 9. Accessibility Requirements

| Area | Requirement |
| --- | --- |
| Keyboard | All controls, dialogs, tabs, menus, and tables must be keyboard navigable. |
| Focus | Every interactive component must show visible focus state. |
| Color | Status and errors must include text or icon, not color only. |
| Touch | Mobile controls must be at least `44px x 44px`. |
| Forms | Labels must be programmatically associated with inputs. |
| Errors | Validation messages must be associated with fields and summarized in complex forms. |
| Dialogs | Focus must move into dialog on open and return to trigger on close. |
| Logs | Log viewer must expose text content and should not trap keyboard focus. |
| Motion | CSS keyframe transitions must respect reduced-motion preferences and must not hide or delay critical status, error, or permission feedback. |

## 10. Security and Permission UX

| Scenario | UI Behavior |
| --- | --- |
| User lacks project access | Route receives `403` or `404`; show forbidden or not found page without leaking project details. |
| Admin views projects | Show allowed metadata only; do not show source, detailed logs, or artifacts unless backend authorizes ownership. |
| Start training unavailable | Hide or disable action with reason such as missing permission, invalid config, or platform busy. |
| Cancel action | Require confirmation; show job ID, project name if authorized, and current status. |
| Artifact download | Use authorized backend endpoint; show failure with correlation ID when provided. |
| WebSocket unauthorized | Stop reconnect loop and show permission state. |

## 11. Component Naming and File Guidance

Component files should use lowercase, hyphen-separated filenames and PascalCase exports.

```text
shared/ui/button.tsx              -> Button, built from shadcn/ui Button
shared/ui/dialog.tsx              -> Dialog, built from Radix Dialog through shadcn/ui
shared/ui/status-badge.tsx        -> StatusBadge
features/job-detail/log-viewer.tsx -> LogViewer
features/project-detail/config-editor.tsx -> ConfigEditor
```

Component props should be explicit and domain components should use backend-aligned terms such as `projectId`, `jobId`, `status`, `artifactId`, `queuePosition`, and `correlationId`.

## 12. Acceptance Checklist

The design system is ready for implementation when:

* Mobile layouts render correctly at `375px x 667px` and larger.
* Desktop layouts render correctly at `1920px x 1080px` and larger.
* Job statuses are visible without relying on color alone.
* Project, job, artifact, notification, and admin tables have mobile and desktop variants.
* Log viewer supports live append, search, filter, scroll, and download states.
* YAML editor supports validation, dirty state, and mobile horizontal scroll.
* Destructive actions use confirmation dialogs.
* WebSocket disconnected, reconnecting, and fallback polling states are specified.
* Component naming and Tailwind token usage align with the frontend architecture document.
* Light and dark themes follow the system preference and maintain accessible contrast.
