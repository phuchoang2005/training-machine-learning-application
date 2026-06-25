# Training Machine Learning Application Training Machine Learning Application is a full-stack web application for configuring, running, monitoring, and administering training jobs. The repository contains the product and architecture documentation plus an implemented React frontend, Spring Boot backend, MongoDB persistence, and Docker-based runtime topology

## Tech Stack

- Frontend: React 19, TypeScript, Vite, Redux Toolkit, React Router, TailwindCSS v4 with hand-authored semantic CSS, Radix UI tabs primitive, Axios.
- Backend: OpenJDK 21, Spring Boot 4, Spring Web, Spring WebSocket, Spring Data MongoDB (MongoTemplate), MongoDB.
- Testing and quality: ESLint, TypeScript type checking, Vitest, Testing Library, Playwright, Maven tests.
- Runtime: Docker Compose, Nginx frontend proxy, MongoDB 8. The frontend image serves a host-built bundle and contains no Node.js runtime.
- API contract: OpenAPI at `docs/solution-architect/low-level-design/api-contracts/openapi.yaml`; frontend types are hand-authored to match it.

## Repository Layout

```text
.
|-- backend/              # Spring Boot API and backend Docker setup
|-- docs/                 # Product, UX, architecture, API, security, and delivery docs
|-- frontend/             # React/Vite app, frontend Docker setup, tests, shared UI
|-- docker-compose.yml    # Full-stack (MongoDB + backend + frontend containers)
|-- docker-compose.dev.yml  # Dev mode (MongoDB + DB GUI only; app runs on host)
`-- README.md
```

Key documentation:

- `docs/po-requirement.md`: product requirements.
- `docs/ba-refine.md`: business analysis refinements.
- `docs/solution-architect/low-level-design/low-level-design.md`: backend/runtime design and Docker topology.
- `docs/solution-architect/frontend-architecture-document/frontend-architecture.md`: frontend architecture and applied tooling.
- `docs/solution-architect/low-level-design/api-contracts/openapi.yaml`: canonical API contract.

---

## Local Development (recommended)

Runs the backend and frontend directly on the host with hot-reload. Docker provides only MongoDB.

**Prerequisites**: Java 21, Maven, Node.js ≥ 24, Docker.

```bash
# 1. Start MongoDB (and optional Mongo Express UI on http://localhost:8081)
docker compose -f docker-compose.dev.yml up -d

# 2. Run the backend (auto-restarts on class changes via Spring DevTools)
cd backend && mvn spring-boot:run

# 3. In a separate terminal — run the frontend with HMR
cd frontend && npm run dev
```

Entry points:

| Service                | URL                                 |
| ---------------------- | ----------------------------------- |
| Frontend (Vite HMR)    | <http://localhost:5173/>              |
| Backend API            | <http://localhost:8080/api/v1/health> |
| Mongo Express (DB GUI) | <http://localhost:8081/>              |

Stop infrastructure:

```bash
docker compose -f docker-compose.dev.yml down
```

---

## Run the Full Stack (containerised)

All three services run as containers. No local Java or Node required to serve the app.

Build the frontend bundle on the host first (the image only serves the pre-built `dist/`), then start the compose stack and the host-run backend:

```bash
# 1. Build frontend bundle
(cd frontend && npm ci && npm run build)

# 2. Start MongoDB + backend container + frontend Nginx container
docker compose up -d --build

# 3. (optional) Follow logs
docker compose logs -f
```

The root Compose stack starts:

- `mongodb`: MongoDB database.
- `backend`: Containerised Spring Boot API on :8080, drives the host Docker daemon to launch training containers.
- `frontend`: Nginx-hosted frontend on :80 that proxies `/api/` and WebSocket traffic to the backend container.

Default entry points:

| Service                     | URL                                 |
| --------------------------- | ----------------------------------- |
| Frontend                    | <http://localhost/>                   |
| Frontend health             | <http://localhost/healthz>            |
| Backend API (through proxy) | <http://localhost/api/v1/health>      |
| Backend API (direct)        | <http://localhost:8080/api/v1/health> |

Stop the full stack:

```bash
docker compose down
```

---

## Run Backend Only

Bring up MongoDB and run Spring Boot on the host. Useful for backend-focused work without a frontend.

```bash
cd backend
docker compose up -d        # MongoDB on :27017
mvn spring-boot:run         # API on :8080
```

Entry points:

- API: <http://localhost:8080/api/v1/health>

Stop:

```bash
cd backend && docker compose down
```

---

## Run Frontend Only (Nginx + pre-built dist)

Serves the host-built `dist/` through Nginx. By default it proxies API traffic to `http://host.docker.internal:8080`, so run the backend first when API calls are needed.

```bash
cd frontend
npm ci && npm run build
docker compose up -d --build
```

Entry points:

- Frontend: <http://localhost:5173/>
- Health: <http://localhost:5173/healthz>

Stop:

```bash
cd frontend && docker compose down
```

---

## Development Commands

**Frontend** (requires Node.js ≥ 24):

```bash
cd frontend
npm run dev          # Vite HMR on :5173
npm run typecheck
npm run lint
npm run test
npm run build
npm run e2e          # Playwright (auto-builds + previews on :4173)
```

**Backend** (requires Java 21 + Maven; needs MongoDB on :27017):

```bash
cd backend
mvn spring-boot:run
mvn package          # build + tests
```

**Docker config check**:

```bash
docker compose config --quiet
docker compose -f docker-compose.dev.yml config --quiet
(cd backend  && docker compose config --quiet)
(cd frontend && docker compose config --quiet)
```

---

## Smoke Test

After any stack is running:

```bash
curl -fsS http://localhost/healthz
curl -fsS http://localhost/api/v1/health
```

For an authenticated development request (bearer token is the user's email in dev):

```bash
curl -fsS -H "Authorization: Bearer admin@example.com" http://localhost/api/v1/auth/me
```

---

## Documentation Rules

When behavior, architecture, tooling, or runtime setup changes, update the matching documentation under `docs/` in the same change. The architecture docs are the source of truth for intended stack decisions, and the OpenAPI contract is the source of truth for API shape.

## Commit Style

Use Conventional Commits as described in `docs/github-commit-strategy.md`, for example:

```text
feat(frontend): add training job form
fix(api): handle missing project
docs: update docker setup notes
```
