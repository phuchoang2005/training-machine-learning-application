# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Notice [IMPORTANT]**: ANYTHING CHANGES IN THE LOGIC CODE SHOULD BE APPROVED BY USER AND REFLECTED TO ../docs/

This is the **backend** module (`ai-training-backend`) of the Training Machine Learning Application — a Spring Boot API for configuring, running, monitoring, and administering ML training jobs. The repo root also contains `frontend/` (React, has its own `CLAUDE.md`) and `docs/` (canonical API contract at `docs/solution-architect/low-level-design/api-contracts/openapi.yaml`).

## Stack

OpenJDK 21, Spring Boot 4, Spring Web + WebSocket, **Spring Data MongoDB (`MongoTemplate`, no JPA/ORM)**, MongoDB 8, Maven.

## Commands

```bash
mvn package                       # build + run tests
mvn -DskipTests package           # build without tests
mvn -Dgroups='!integration' test  # run unit tests (no Docker needed)
mvn test                          # run all tests including integration (requires Docker for Testcontainers)
mvn spring-boot:run               # run locally (needs MongoDB on localhost:27017, database "ai_training")

# Docker provides ONLY MongoDB now — Java/Maven run on the host, not in a container.
docker compose up -d      # start MongoDB on :27017
docker compose down       # stop MongoDB
```

Typical local dev loop: `docker compose up -d` to start MongoDB, then `mvn spring-boot:run` to run the API on the host at `:8080`.

Health check: `GET /api/v1/health` (the only unauthenticated endpoint).

## Architecture

Layered, package-by-type under `com.example.aitraining`:

- **`api/`** — `@RestController`s. All routes sit under the `/api/v1` context path (set in `application.yml`), so controller mappings are relative (e.g. `@GetMapping("/auth/me")` → `/api/v1/auth/me`).
- **`service/`** — business logic. `AuthorizationService` centralizes all access checks (`requireProjectOwner`, `requireProjectVisible`, `requireJobVisible`, `requireSensitiveJobOwner`, `requireAdmin`) — call these rather than re-implementing ownership/role checks.
- **`repo/`** — `MongoTemplate`-based repositories. The four domain records map to collections via Spring Data; the internal collections (`config_snapshots`, `job_queue_entries`, `job_log_events`, `job_progress_events`, `artifacts`, `audit_logs`) are read/written as raw `org.bson.Document`s. No JPA/ORM. A missing document throws `EmptyResultDataAccessException` (→ 404).
- **`domain/`** — `Models.java` (Java records annotated `@Document`/`@Id`: `User`, `Project`, `ProjectConfig`, `TrainingJob`) and `Enums.java`. UUID identifiers are kept as the Mongo `_id`; enums are stored as their string names.
- **`dto/`** — request/response records grouped per area in `*Dtos.java` files (e.g. `JobDtos`, `CommonDtos`). `CommonDtos.ErrorResponse` is the standard error envelope.
- **`config/`** — cross-cutting wiring (auth filter, exception handler, websocket, MongoDB UUID representation, DB seeding, properties, async thread-pool config).
- **`realtime/`** — WebSocket handler for live job log/progress streaming.
- **`runner/`** — `TrainingRunner` interface + `DockerTrainingRunner` implementation. Also `ProgressParser` (regex-based stdout parser) and `ZipExtractor` (safe ZIP extraction with path-traversal hardening).

### Auth (development-grade — important)

There is **no real token validation or password auth**. The bearer token is literally the user's email or `user_id`:

- `WebConfig` is a `OncePerRequestFilter` that reads `Authorization: Bearer <token>`, resolves it via `UserRepository.findActiveByToken` (matches `lower(email)` OR `user_id::text`), and stores the `User` in a `ThreadLocal` `CurrentUserContext` for the request. It clears the context in `finally`.
- Controllers/services read the caller with `CurrentUserContext.require()`.
- Seeded users (from the migration): `user@example.com` (USER) and `admin@example.com` (ADMIN). Use these strings as bearer tokens in dev.
- WebSocket auth (`WebSocketConfig.DevelopmentBearerHandshakeInterceptor`) accepts the token via the `Authorization` header **or** a `?token=` query param (for browser WS clients that can't set headers).

### Error handling

`ApiExceptionHandler` (`@RestControllerAdvice`) maps exceptions to the standard `ErrorResponse` envelope with a generated `correlationId`. The convention encodes HTTP status via exception type:
`UnauthorizedException`→401, `ForbiddenException`→403, `EmptyResultDataAccessException`→404, `IllegalArgumentException`→400, `IllegalStateException`→409 (used for invalid state transitions, e.g. cancelling a terminal job), validation errors→400. Throw the matching standard/custom exception rather than building responses by hand.

### Job queue & execution engine

Queue state is DB-backed (`job_queue_entries` + `training_jobs`), not in-memory.

- `JobService.start` snapshots the config YAML (`config_snapshots`), creates the job, and enqueues it.
- `JobQueueRepository.claimNext()` atomically marks one WAITING entry as DISPATCHED (prevents duplicate dispatch).
- `JobQueueRepository.refreshPositions()` recomputes `queue_position` for all WAITING jobs (ranked by `enqueued_at`); call it after any enqueue/cancel so positions stay consistent.
- `JobDispatcherService` runs every 2 s (`@Scheduled`), checks `runningCount < runningLimit`, and dispatches claimed jobs to `DockerTrainingRunner` on the `trainingExecutor` thread pool.
- `JobReconcilerService` runs at startup, finds orphaned RUNNING jobs (no live Docker container), marks them RETRYING, and re-enqueues.
- Concurrency cap is `app.queue.running-limit` (`APP_QUEUE_RUNNING_LIMIT`, default 2).
- Mutating job actions write to `audit_logs` via `SupportRepository.audit(...)`.

### Database & seeding

The app uses MongoDB via Spring Data (`MongoTemplate`); there are no SQL migrations. `MongoConfig` registers a `MongoClientSettingsBuilderCustomizer` forcing the **standard** UUID BSON representation so `UUID` `_id`s round-trip (Spring Boot's default leaves the driver `UNSPECIFIED`, which refuses to encode UUIDs). `MongoSeedConfig`'s `ApplicationRunner` runs on startup to create baseline indexes (unique `users.email`, unique `project_configs (projectId, configPath)`) and idempotently seed the two dev users. Collections are created lazily on first write.

## Conventions

- Commits follow Conventional Commits (`feat(scope):`, `fix(api):`, `docs:`), imperative, lowercase after the type, no trailing period (see `docs/github-commit-strategy.md`).
- New domain types: add the `@Document`/`@Id` record to `Models.java` and write the queries with `MongoTemplate` in `repo/`. No SQL migrations or row mappers.
- When adding endpoints, gate access through `AuthorizationService` and read the caller via `CurrentUserContext`.

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
