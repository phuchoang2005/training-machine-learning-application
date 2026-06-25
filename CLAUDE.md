# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Full-stack web app for configuring, running, monitoring, and administering ML training jobs. Three top-level parts:

- `backend/` — Spring Boot 4 / Java 21 API on MongoDB. **Has its own `backend/CLAUDE.md`** (layered architecture, auth filter, job queue, seeding) — read it before backend work.
- `frontend/` — React 19 + TypeScript + Vite SPA. **Has its own `frontend/CLAUDE.md`** (Redux store, mock-driven state, routing, UI layer) — read it before frontend work.
- `docs/` — product, UX, architecture, security, and delivery docs.

This root file covers only what spans both modules; the module files are the source of truth for their own internals.

## Cross-cutting sources of truth

- **API contract**: `docs/solution-architect/low-level-design/api-contracts/openapi.yaml` is canonical. Frontend domain types (`frontend/src/shared/api/types/`) are **hand-authored to match it** — when the contract changes, update both the backend and the frontend types by hand in the same change.
- **Architecture docs** under `docs/solution-architect/` are the source of truth for stack decisions. When behavior, architecture, tooling, or runtime setup changes, update the matching doc in the same change.
- **Dev auth scheme (shared convention)**: there is no real auth. The bearer token is literally the user's email. Seeded/sample accounts are `user@example.com` (USER) and `admin@example.com` (ADMIN). The frontend uses these as login identities; the backend resolves the same strings from `Authorization: Bearer <token>`. Keep these in sync across both sides.
- All routes are served under the `/api/v1` context path. WebSocket job streaming is at `/api/v1/ws/jobs/:jobId` (token via `Authorization` header or `?token=` query param).

## Runtime topology (important)

**Docker provides only MongoDB and the static frontend image. The backend Spring Boot app runs on the host via Maven, not in a container.** The frontend Docker image is Nginx serving a host-built `frontend/dist/` bundle (no Node runtime in the image) and proxies `/api/` + WebSocket traffic to `host.docker.internal:8080` (the host-run backend).

Consequence: bring up the full stack in this order — build the frontend bundle on the host (`npm ci && npm run build`), then `docker compose up -d --build` from the repo root (Mongo + frontend), then `mvn spring-boot:run` in `backend/` for the API the frontend proxies to. See `README.md` for the exact commands and entry points (`http://localhost/`, health `/healthz`, API `/api/v1/health`).

## Commands

Run module commands from each module directory. Full detail lives in each module's `CLAUDE.md`; the essentials:

```bash
# backend/ (needs MongoDB on :27017)
mvn spring-boot:run        # run API on host :8080
mvn package                # build + tests (note: no tests exist yet)

# frontend/ (needs Node >= 24)
npm run dev                # Vite dev server :5173, proxies /api -> :8080
npm run typecheck && npm run lint && npm run test && npm run build
npx vitest run src/tests/app.test.tsx     # single test file
npx vitest run -t "<pattern>"             # filter by name
npm run e2e                # Playwright (builds + previews on :4173)
```

Frontend is currently driven entirely by in-memory mock data (`frontend/src/store/mock-data.ts`); the Axios/WebSocket integration layer exists but is not yet wired into components. See `frontend/CLAUDE.md`.

## Conventions

- Conventional Commits, imperative, lowercase after the type, no trailing period: `feat(frontend): ...`, `fix(api): ...`, `docs: ...`. See `docs/github-commit-strategy.md`.

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
