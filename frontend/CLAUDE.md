# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Notice [IMPORTANT]**: ANYTHING CHANGES IN THE LOGIC CODE SHOULD BE APPROVED BY USER AND REFLECTED TO ../docs/

## Scope

This is the **frontend** package of the `training-machine-learning-application` monorepo (sibling `../backend` is Spring Boot, `../docs` holds the canonical product/architecture docs and OpenAPI contract). The frontend is a React 19 + TypeScript + Vite SPA for configuring, running, monitoring, and administering ML training jobs.

## Commands

Run from `frontend/`. Requires Node.js >= 24.

- `npm run dev` — Vite dev server on `:5173`, proxies `/api` → `http://localhost:8080`.
- `npm run typecheck` — `tsc --noEmit` (also runs first in `build`).
- `npm run lint` — ESLint.
- `npm run test` — Vitest (jsdom) once; `npm run test:watch` for watch mode.
- Run a single test file: `npx vitest run src/tests/app.test.tsx`. Filter by name: `npx vitest run -t "<pattern>"`.
- `npm run e2e` — Playwright; auto-starts `npm run preview` on `:4173` (build first).
- `npm run build` — typecheck + Vite production build.

## Architecture

### Mock-driven state (important)

The app currently runs **entirely off in-memory mock data**, not the live backend. Redux Toolkit slices in `src/store/slices/` initialize their state from `src/store/mock-data.ts`, which aggregates fixtures from `src/store/mock/{jobs,projects,support,users,time}.ts`. Mutations (register project, save config, start/cancel job) are plain reducers that update this mock state.

The real backend integration layer **exists but is not yet wired into components**:

- `src/shared/api/axios-client.ts` — configured Axios instance (`/api/v1`, error normalization to `ApiError`). Referenced only by itself so far.
- `src/shared/realtime/job-stream-client.ts` — WebSocket client for live job/log events with reconnect + `lastEventId` resume. Connects to `/api/v1/ws/jobs/:jobId`.

When adding backend connectivity, replace mock reads in slices/components with `apiClient` calls and use `createJobStreamClient` for live job streams.

### Store

`src/store/store.ts` wires reducers (`auth`, `projects`, `jobs`, `notifications`, `admin`, `theme`) and re-exports a flat `actions` object merging every slice's actions. Use the typed hooks from `src/store/hooks.ts` (`useAppDispatch`, `useAppSelector`) — never the raw react-redux hooks. `RootState`/`AppDispatch` are exported from `store.ts`.

### Auth (mock)

`src/store/session.ts` defines `sampleAccounts` (`user@example.com` / `admin@example.com`, password `password`) and persists the logged-in email in `sessionStorage` under `ai-training-session-email`. Route protection lives in `src/app/auth/guards.tsx`: `RequireAuth` (redirects to `/login`), `AdminGuard` (role `ADMIN` else `/403`). The dev backend authenticates by a bearer token equal to the user's email.

### Routing

`src/app/App.tsx` declares all routes. Public: `/login`, `/register`, `/login/callback`. Authenticated routes render inside `RequireAuth` → `AppShell` (`src/app/layout/`). `App.tsx` also syncs the `theme` slice to the OS `prefers-color-scheme`.

### Types

Hand-authored domain types in `src/shared/api/types/{job,project,support,user}.ts`, re-exported via the `src/shared/api/types.ts` barrel — **import app types from `@/shared/api/types`**. These are the single source of truth; keep them in sync with `../docs/solution-architect/low-level-design/api-contracts/openapi.yaml` by hand when the contract changes.

### UI layer

The app is styled with hand-authored semantic CSS classes (`.button.primary`, `.login-card`, `.dialog`, …) split across `src/assets/styles/*.css` and aggregated by `global.css` (imported in `main.tsx`). Tailwind v4 (`@tailwindcss/vite`, theme tokens in `src/assets/styles/tailwind.css`) is wired up and used by the one shadcn-style primitive that remains — `src/shared/ui/tabs.tsx` — via the `cn()` helper in `src/shared/utils/cn.ts`. Higher-level composed components live in `src/shared/components/`. `components.json` aliases: `components`/`ui` → `@/shared/ui`, `utils`/`lib` → `@/shared/utils`, `hooks` → `@/shared/hooks`. Enter/transition animations are plain CSS keyframes (`fade-in-up`, `dialog-in` in `layout.css`) gated behind `prefers-reduced-motion`.

### Path alias

`@` → `src` (configured in both `tsconfig.json` and `vite.config.ts`). Existing files mix `@/...` and relative imports; prefer `@/...` for cross-feature imports.

## Pages

Feature pages live in `src/pages/{admin,auth,jobs,notifications,projects}/`, organized by domain (projects dashboard/detail/config editor, job detail with log panel, admin users/queue/audit).

## Conventions

- Commit style: Conventional Commits (e.g. `feat(frontend): ...`, `fix(api): ...`), per `../docs/github-commit-strategy.md`.
- When behavior, architecture, or tooling changes, update the matching docs under `../docs/` in the same change. The architecture docs are the source of truth for stack decisions; the OpenAPI contract is the source of truth for API shape.

## Notice [important]: Always refering these commands instead of original shell bash command

### Commands

#### Files

```bash
rtk ls .                        # Token-optimized directory tree
rtk read file.rs                # Smart file reading
rtk read file.rs -l aggressive  # Signatures only (strips bodies)
rtk smart file.rs               # 2-line heuristic code summary
rtk find "*.rs" .               # Compact find results
rtk grep "pattern" .            # Grouped search results
rtk diff file1 file2            # Condensed diff (exit 1 if files differ)
```

#### Git

```bash
rtk git status                  # Compact status
rtk git log -n 10               # One-line commits
rtk git diff                    # Condensed diff
rtk git add                     # -> "ok"
rtk git commit -m "msg"         # -> "ok abc1234"
rtk git push                    # -> "ok main"
rtk git pull                    # -> "ok 3 files +10 -2"
```

#### GitHub CLI

```bash
rtk gh pr list                  # Compact PR listing
rtk gh pr view 42               # PR details + checks
rtk gh issue list               # Compact issue listing
rtk gh run list                 # Workflow run status
```

#### Test Runners

```bash
rtk jest                        # Jest compact (failures only)
rtk vitest                      # Vitest compact (failures only)
rtk playwright test             # E2E results (failures only)
rtk pytest                      # Python tests (-90%)
rtk go test                     # Go tests (NDJSON, -90%)
rtk cargo test                  # Cargo tests (-90%)
rtk rake test                   # Ruby minitest (-90%)
rtk rspec                       # RSpec tests (JSON, -60%+)
rtk err <cmd>                   # Filter errors only from any command
rtk test <cmd>                  # Generic test wrapper - failures only (-90%)
```

#### Build & Lint

```bash
rtk lint                        # ESLint grouped by rule/file
rtk lint biome                  # Supports other linters
rtk tsc                         # TypeScript errors grouped by file
rtk next build                  # Next.js build compact
rtk prettier --check .          # Files needing formatting
rtk cargo build                 # Cargo build (-80%)
rtk cargo clippy                # Cargo clippy (-80%)
rtk ruff check                  # Python linting (JSON, -80%)
rtk golangci-lint run           # Go linting (JSON, -85%)
rtk rubocop                     # Ruby linting (JSON, -60%+)
```

#### Package Managers

```bash
rtk pnpm list                   # Compact dependency tree
rtk pip list                    # Python packages (auto-detect uv)
rtk pip outdated                # Outdated packages
rtk bundle install              # Ruby gems (strip Using lines)
rtk prisma generate             # Schema generation (no ASCII art)
```

#### AWS

```bash
rtk aws sts get-caller-identity # One-line identity
rtk aws ec2 describe-instances  # Compact instance list
rtk aws lambda list-functions   # Name/runtime/memory (strips secrets)
rtk aws logs get-log-events     # Timestamped messages only
rtk aws cloudformation describe-stack-events  # Failures first
rtk aws dynamodb scan           # Unwraps type annotations
rtk aws iam list-roles          # Strips policy documents
rtk aws s3 ls                   # Truncated with tee recovery
```

#### Containers

```bash
rtk docker ps                   # Compact container list
rtk docker images               # Compact image list
rtk docker logs <container>     # Deduplicated logs
rtk docker compose ps           # Compose services
rtk kubectl pods                # Compact pod list
rtk kubectl logs <pod>          # Deduplicated logs
rtk kubectl services            # Compact service list
rtk oc get pods                 # OpenShift pod summary
rtk oc get services             # OpenShift service list
rtk oc logs <pod>               # Deduplicated logs
```

#### Infrastructure as Code

```bash
rtk pulumi preview              # Strip header/URL/duration noise
rtk pulumi up                   # Compact apply output
rtk pulumi destroy              # Compact destroy output
rtk pulumi refresh              # Drift summary
rtk pulumi stack                # Stack metadata (strips owner/timestamps)
```

#### Data & Analytics

```bash
rtk json config.json            # Structure without values
rtk deps                        # Dependencies summary
rtk env -f AWS                  # Filtered env vars
rtk log app.log                 # Deduplicated logs
rtk curl <url>                  # Truncate + save full output
rtk wget <url>                  # Download, strip progress bars
rtk summary <long command>      # Heuristic summary
rtk proxy <command>             # Raw passthrough + tracking
```
