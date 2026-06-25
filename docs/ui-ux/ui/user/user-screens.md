---
title: "User Screens"
tags: [ui, wireframes, user, screens]
aliases: [User Screens, User Wireframes, user-screens]
related:
  - "[[ui-screens]]"
  - "[[admin-screens]]"
  - "[[primary-user-flow-diagram]]"
  - "[[information-architecture-diagram]]"
  - "[[ADR-014]]"
---

# User Wireframe Specification (SVG Format)

This directory contains the user-role specific user interface wireframes for Future, rendered as visual SVGs. These wireframes are tailored for the standard user roles (**AI Engineer / Data Scientist** and **Project Owner**) and intentionally omit any administrative features or the global "Admin Console" menu link from the App Shell navigation menu.

The current visual direction is intentionally warmer, more vivid, and more emotionally generous than a default enterprise dashboard. It translates the inspiration note in `docs/ui-ux/ui/inspired.md` into a grounded product UI: structure and clarity remain present, but the screens should feel caring, restorative, hopeful, and safe to return to during long-running training work.

## 1. Directory Index & Visual Wireframe Links

* **01. Login Page** (`/login`):
  - Light: `docs/ui-ux/ui/user/light/01-login.svg`
  - Dark: `docs/ui-ux/ui/user/dark/01-login.svg`
  - Features a centered credentials entry card, company SSO button, email/password field states, and a warmer "Welcome Home" entry point.
* **02. Project Dashboard** (`/projects`):
  - Light: `docs/ui-ux/ui/user/light/02-project-dashboard.svg`
  - Dark: `docs/ui-ux/ui/user/dark/02-project-dashboard.svg`
  - Displays all authorized projects, search debouncers, status filter tabs, sorting keys, and latest job run badges under a hopeful project overview.
* **03. Project Registration** (`/projects/register`):
  - Light: `docs/ui-ux/ui/user/light/03-project-registration.svg`
  - Dark: `docs/ui-ux/ui/user/dark/03-project-registration.svg`
  - Layout for project registration via Git repository URL clone parameters or drag-and-drop ZIP uploads.
* **04. Project Detail** (`/projects/:projectId`):
  - Light: `docs/ui-ux/ui/user/light/04-project-detail.svg`
  - Dark: `docs/ui-ux/ui/user/dark/04-project-detail.svg`
  - Shows left-hand repository metadata (branch, dataset version dropdown selectors) and right-hand tabs previewing configurations, baseline parameters, and the "Start Training With Care" confirm trigger.
* **05. Configuration Editor** (`/projects/:projectId/configuration`):
  - Light: `docs/ui-ux/ui/user/light/05-configuration-editor.svg`
  - Dark: `docs/ui-ux/ui/user/dark/05-configuration-editor.svg`
  - Combines a left-side monospace YAML baseline code editor (with line numbers) and a right-side hyperparameter forms card with validation alerts.
* **06. Training History** (`/projects/:projectId/history`):
  - Light: `docs/ui-ux/ui/user/light/06-training-history.svg`
  - Dark: `docs/ui-ux/ui/user/dark/06-training-history.svg`
  - Displays search and date filter toolbars above a dense grid of historical job IDs, owners, git references, status badges, and durations.
* **07. Job Detail & Monitoring** (`/projects/:projectId/jobs/:jobId`):
  - Light: `docs/ui-ux/ui/user/light/07-job-detail.svg`
  - Dark: `docs/ui-ux/ui/user/dark/07-job-detail.svg`
  - Real-time monitor workspace. Displays status control cards, duration counts, and generated artifacts on the left, alongside the live console logs streaming terminal window on the right.
* **08. Notifications** (`/notifications`):
  - Light: `docs/ui-ux/ui/user/light/08-notifications.svg`
  - Dark: `docs/ui-ux/ui/user/dark/08-notifications.svg`
  - Alert inbox showing unread badge indicators, collapsible details, and deep-link details routes.

---

## 2. Wireframe Design System Rules

These wireframe SVGs are rendered on a standard grid matching the design system instructions:
- **Aspect Viewport Size**: `1280px x 800px` (standard desktop proportions).
- **App Shell Structure**: Uses the left sidebar (`240px`) and topbar (`60px`).
- **Sidebar Boundaries**:
  - The "Admin Console" link has been removed, restricting access strictly to user-level screens: `Projects` and `Notifications`.
  - The bottom section displays profile metadata for a standard "AI Engineer" role (`engineer@co.com`).
- **Header Boundaries**: Includes breadcrumbs, active notifications indicator, user menu avatar, and the WebSocket state badge (`CONNECTED`).
- **Emotional Direction**: The UI should feel vivid, beloved, warm, lovely, full of love, healing, and hopeful while still functioning as a precise engineering workspace. The dominant tone is now Pleiades/Sirius blue: calm, luminous, celestial, and restorative. Use caring microcopy sparingly on welcome, overview, and primary-start moments; keep operational labels and status values exact.
- **Light Theme Color Tokens**: Soft star canvas (`#f8fbff`), Pleiadian blue glow (`#dbeafe`), sky mist glow (`#e0f2fe`), Sirius lavender glow (`#eef2ff`), sky-to-star primary gradient (`#0ea5e9` to `#2563eb`), and deep blue text (`#0f172a`).
- **Light Theme Background**: Use a vivid celestial background layer behind the shell: aurora ribbons (`#38bdf8`, `#60a5fa`, `#a78bfa`), a soft central white-blue heart glow, sparse star points, and faint constellation curves. Keep panels translucent enough to feel luminous but readable.
- **Dark Theme Color Tokens**: Deep Sirius canvas (`#061426`), luminous cyan glow (`#38bdf8`), royal blue glow (`#2563eb`), lavender star accent (`#a78bfa`), cyan-to-blue primary gradient (`#38bdf8` to `#60a5fa`), and soft blue card borders.
- **Status Colors**: Preserve the required status labels exactly (`PENDING`, `RUNNING`, `SUCCESS`, `FAILED`, `CANCELLED`). Use blue/cyan for running indicators and connection energy, teal or lavender for successful/healing states, rose/crimson only for failure or unread alerts, and muted slate for cancelled states.
- **CSS Motion Direction**: In implementation, use plain CSS keyframe animations to bring these screens to life without compromising data clarity. Apply soft page fades, card reveals, dialog scale/fade transitions, gentle `RUNNING` status pulses, notification enter transitions, and slow ambient drift for the light-mode aurora background. Disable movement or reduce it to opacity-only transitions when `prefers-reduced-motion` is active.
- **Regeneration Helper**: Run `rtk node docs/ui-ux/ui/user/update_svgs.js` after refreshing generated SVGs to reapply the Pleiades/Sirius blue, hopeful theme consistently across light and dark variants.
