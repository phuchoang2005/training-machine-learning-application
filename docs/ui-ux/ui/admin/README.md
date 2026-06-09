# Admin Wireframe Specification (SVG Format)

This directory contains administrator-role visual SVG wireframes for the AI Training Management Platform. The screens intentionally match the user-role UI direction in `docs/ui-ux/ui/user/`: the same 1280px by 800px desktop canvas, persistent app shell, Pleiades/Sirius light and dark themes, translucent operational panels, status badges, and WebSocket utility header.

The admin shell includes the `Projects`, `Notifications`, and `Admin Console` navigation items. The profile area uses a platform administrator identity (`admin@co.com`) to make the role boundary visible.

## Directory Index

* **09. Admin Queue** (`/admin/queue`)
  - Light: `docs/ui-ux/ui/admin/light/09-admin-queue.svg`
  - Dark: `docs/ui-ux/ui/admin/dark/09-admin-queue.svg`
  - Shows single-server capacity, FIFO queue depth, running and pending jobs, failure count, and controlled cancellation actions.
* **10. Admin Users** (`/admin/users`)
  - Light: `docs/ui-ux/ui/admin/light/10-admin-users.svg`
  - Dark: `docs/ui-ux/ui/admin/dark/10-admin-users.svg`
  - Shows user search, role and state filters, role distribution metrics, activation state, and access scope summaries.
* **11. Admin Audit** (`/admin/audit`)
  - Light: `docs/ui-ux/ui/admin/light/11-admin-audit.svg`
  - Dark: `docs/ui-ux/ui/admin/dark/11-admin-audit.svg`
  - Shows audit search, time/action filters, a focused security review callout, correlation IDs, privileged actions, actors, and results.

## Regeneration

Run the generator after changing admin UI data, layout, or theme tokens:

```bash
rtk node docs/ui-ux/ui/admin/generate_svgs.js
```

The generator emits both `light/` and `dark/` variants for all admin screens.
