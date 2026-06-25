---
title: "UI Screens Index"
tags: [ui, wireframes, screens]
aliases: [UI Screens, Wireframes, ui-screens]
related:
  - "[[user-screens]]"
  - "[[admin-screens]]"
  - "[[ux-overview]]"
  - "[[design-system]]"
  - "[[po-requirement]]"
---

# UI Wireframe Specification

This directory contains the detailed user interface wireframes and layout specifications for the AI Training Management Platform. The designs are structured around the product requirements (`docs/po-requirement.md`), business refinements (`docs/ba-refine.md`), and the component specification (`docs/solution-architect/design-system-and-component-specification/README.md`).

## 1. Directory Structure

This wireframe package is organized into individual files, each detailing the layout, components, responsive adaptations, and interactivity of a specific route in the application:

Role-specific SVG mockups are stored separately:

* User-role UI set: `docs/ui-ux/ui/user/`
* Admin-role UI set: `docs/ui-ux/ui/admin/`

* **01. Login Page** (`/login`): Authentication gate for user credential login.
  - [Markdown Specification](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/01-login.md) | [SVG Visual Wireframe](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/01-login.svg)
* **02. Project Dashboard** (`/projects`): Searchable, filterable directory of all authorized projects with active status indicators.
  - [Markdown Specification](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/02-project-dashboard.md) | [SVG Visual Wireframe](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/02-project-dashboard.svg)
* **03. Project Registration** (`/projects/register`): Single or repository-based source registration interface.
  - [Markdown Specification](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/03-project-registration.md) | [SVG Visual Wireframe](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/03-project-registration.svg)
* **04. Project Detail** (`/projects/:projectId`): Core information hub for individual projects, featuring a "Start Training" action.
  - [Markdown Specification](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/04-project-detail.md) | [SVG Visual Wireframe](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/04-project-detail.svg)
* **05. Configuration Editor** (`/projects/:projectId/configuration`): Monospace editor for override validation and hyperparameter configuration.
  - [Markdown Specification](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/05-configuration-editor.md) | [SVG Visual Wireframe](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/05-configuration-editor.svg)
* **06. Training History** (`/projects/:projectId/history`): Searchable archive of all historical jobs run under the project.
  - [Markdown Specification](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/06-training-history.md) | [SVG Visual Wireframe](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/06-training-history.svg)
* **07. Job Detail** (`/projects/:projectId/jobs/:jobId`): Dense, split-screen live monitor displaying status, metrics, real-time log viewer, and artifact browser.
  - [Markdown Specification](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/07-job-detail.md) | [SVG Visual Wireframe](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/07-job-detail.svg)
* **08. Notifications** (`/notifications`): Activity log for success and failure job events.
  - [Markdown Specification](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/08-notifications.md) | [SVG Visual Wireframe](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/08-notifications.svg)
* **09. Admin Queue** (`/admin/queue`): Global queue capacity metrics and FIFO job cancellation dashboard.
  - [Markdown Specification](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/09-admin-queue.md) | [SVG Visual Wireframe](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/09-admin-queue.svg)
* **10. Admin Users** (`/admin/users`): Role assignment and user activation controls.
  - [Markdown Specification](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/10-admin-users.md) | [SVG Visual Wireframe](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/10-admin-users.svg)
* **11. Admin Audit** (`/admin/audit`): Auditable trace logs with correlation IDs.
  - [Markdown Specification](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/11-admin-audit.md) | [SVG Visual Wireframe](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/11-admin-audit.svg)
* **12. Error Pages** (`/403`, `/404`): User states for permission blocks and missing routes.
  - [Markdown Specification](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/12-error-pages.md)
  - [403 Forbidden SVG Visual Wireframe](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/12-error-403.svg) | [404 Not Found SVG Visual Wireframe](file:///Users/phuchoang/Local_Document/training-model-tool/docs/ui-ux/ui/wireframe/13-error-404.svg)

---

## 2. Common Layout Patterns

Except for the Login Page and Error pages, all screens share a common **App Shell** container designed to optimize layout consistency and data density.

### App Shell Structure (Desktop Mode)
```text
+---------------------------------------------------------------------------------------+
|  LOGO  | BREADCRUMBS: Projects > Fraud Detection              | WS [Connected] | [U]  | (Header)
+--------+------------------------------------------------------+-----------------------+
| (Side) |                                                                              |
| (Nav)  |                                                                              |
| Projs  |                                                                              |
| Notifs |                                                                              |
| Admin  |                     MAIN OPERATIONAL VIEPORT AREA                            |
|        |                                                                              |
| (Bot)  |                                                                              |
| User   |                                                                              |
+--------+------------------------------------------------------------------------------+
```

### App Shell Structure (Mobile Mode)
```text
+-----------------------------------------------------+
| [=] LOGO                              [WS] [o] [U]  | (Top Bar Header)
+-----------------------------------------------------+
|                                                     |
|                                                     |
|            MAIN OPERATIONAL VIEWPORT                |
|                                                     |
|                                                     |
+-----------------------------------------------------+
```

### Common Elements
1. **Side Navigation Sidebar (Desktop)**:
   - **Width**: `248px` width on screens `1200px` and wider. Persistent, non-collapsible to maintain spatial predictability.
   - **Logo**: Top-left branding area (`AI Training Management Platform`).
   - **Navigation Items**: List of primary links: `Projects`, `Notifications`, `Admin Console` (only rendered if user role is `ADMIN`).
   - **Bottom Profile Area**: Displays current user's email, name, and their active security role (`Admin`, `Project Owner`, or `AI Engineer`). Clicking details opens a popover with logout option.
2. **Top Bar Header**:
   - **Height**: `56px` tall. Sticky positioned at the top of the viewport.
   - **Breadcrumbs**: Shows logical path hierarchy: e.g. `Projects / Fraud Detection / Job #10245`. Clickable segments for easy backtracking.
   - **WebSocket Connection Status (WS)**: Indicates real-time connection state:
     - `CONNECTED` (Green dot + solid border): WebSocket is active, logs/status stream live.
     - `RECONNECTING` (Amber rotating indicator): Connection lost; app is attempting to reconnect.
     - `FALLBACK POLLING` (Orange dot + dashed border): Connection failed; active polling at 5s interval is functioning.
     - `DISCONNECTED` (Red dot + flash animation): Connection dead; manual page refresh required.
   - **Notification Quick Trigger**: Badge icon representing unread notification count. Clicking opens a dropdown showing the last 5 notifications.
   - **User Menu Trigger**: Access point for user profile settings and logout.

3. **Mobile Menu Drawer**:
   - On screens smaller than `1200px`, the sidebar disappears.
   - A menu hamburger icon `[=]` appears on the left of the Top Bar.
   - Tap-triggering slides in a full-height overlay drawer containing the sidebar links and user profile actions.

---

## 3. Wireframe Styling Tokens Reference

To align with the project design system (`docs/solution-architect/design-system-and-component-specification/README.md`), all wireframe layouts use the following structure conventions:

* **Grid/Spacing**: Based on a `4px` grid system.
  - Page padding: `16px` on mobile, `32px` on desktop.
  - Component gap: `12px` to `20px` depending on spacing needs.
* **Colors**: RESTRICTED palette used for visual hierarchy.
  - Main surfaces: Neutral `Background` and `Card` surfaces.
  - Primary Action / Highlight: Cobalt/Light Blue (`#2563eb` / `#60a5fa`).
  - Terminal/Running Info: Sky Blue (`#0369a1` / `#38bdf8`).
  - Successful operations: Emerald Green (`#15803d` / `#4ade80`).
  - Warning / Queued states: Amber/Yellow (`#b45309` / `#fbbf24`).
  - Failures / Destructive actions: Crimson Red (`#b91c1c` / `#f87171`).
* **Typography**:
  - Primary text: Inter, high readability.
  - Code/Data: JetBrains Mono. Font size `12px` (mobile) to `14px` (desktop) with line-height of `1.45` to avoid eye strain.
* **Component Interactions**:
  - All interactive elements must maintain a minimum touch target size of `44px x 44px` on mobile viewports.
  - Key focus rings must be visible for keyboard navigating users.
* **CSS Motion Interaction Layer**:
  - Use plain CSS keyframe animations for route fades, panel reveals, dialog enter transitions, small list item transitions, and live-state indicators.
  - Keep motion subtle: route and panel transitions should use opacity plus `translateY: 8px -> 0` and complete in roughly `220ms`.
  - Use ambient motion only for the light-mode celestial background, such as slow aurora drift or star opacity changes; it must sit behind all data surfaces.
  - Do not animate high-volume log lines, large tables, YAML editor text, or any status update that must appear immediately.
  - Respect `prefers-reduced-motion`; use static or opacity-only states when reduced motion is requested.
