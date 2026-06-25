# Training Machine Learning Application

Training Machine Learning Application is a full-stack web application for configuring, running, monitoring, and administering training jobs. The repository contains the product and architecture documentation plus an implemented React frontend, Spring Boot backend, MongoDB persistence, and Docker-based runtime topology.

## Tech Stack

- Frontend: React 19, TypeScript, Vite, Redux Toolkit, React Router, TailwindCSS v4 with hand-authored semantic CSS, Radix UI tabs primitive, Axios.
- Backend: OpenJDK 21, Spring Boot 4, Spring Web, Spring WebSocket, Spring Data MongoDB (MongoTemplate), MongoDB.
- Testing and quality: ESLint, TypeScript type checking, Vitest, Testing Library, Playwright, Maven tests.
- Runtime: Docker Compose, Nginx frontend proxy, MongoDB 8. The frontend image serves a host-built bundle and contains no Node.js runtime.
- API contract: OpenAPI at `docs/solution-architect/low-level-design/api-contracts/openapi.yaml`; frontend types are hand-authored to match it.

## Repository Layout

```text
.
|-- backend/          # Spring Boot API (runs on host), MongoDB-only Docker setup
|-- docs/             # Product, UX, architecture, API, security, and delivery docs
|-- frontend/         # React/Vite app, frontend Docker setup, tests, shared UI
|-- docker-compose.yml
`-- README.md
```

Key documentation:

- `docs/po-requirement.md`: product requirements.
- `docs/ba-refine.md`: business analysis refinements.
- `docs/solution-architect/low-level-design/README.md`: backend/runtime design and Docker topology.
- `docs/solution-architect/frontend-architecture-document/README.md`: frontend architecture and applied tooling.
- `docs/solution-architect/low-level-design/api-contracts/openapi.yaml`: canonical API contract.

## Runtime Notes

This workspace is mounted from `ssh my-ec2`. Follow the local agent convention and run shell commands through `rtk`; runtime commands for this project should execute on the EC2 host:

```bash
rtk ssh my-ec2 'cd ~/training-machine-learning-application && docker compose ps'
```

Frontend local tooling requires Node.js 24 or newer. Backend local tooling requires OpenJDK 21 and Maven. Docker provides only MongoDB (plus the static frontend image); the backend Spring Boot app runs on the host with Maven, not in a container. The frontend Docker image no longer bundles a Node.js runtime: it serves the static bundle that you build on the host with `npm run build`, so produce `frontend/dist/` before building the frontend image.

## Run the Full Stack

Build the frontend bundle on the host first (the frontend image only serves `frontend/dist/`), then bring up the Docker services from the repository root and run the backend on the host:

```bash
rtk ssh my-ec2 'cd ~/training-machine-learning-application/frontend && npm ci && npm run build'
rtk ssh my-ec2 'cd ~/training-machine-learning-application && docker compose up -d --build'
rtk ssh my-ec2 'cd ~/training-machine-learning-application/backend && mvn spring-boot:run'
```

The root Compose stack starts:

- `mongodb`: MongoDB database.
- `frontend`: Nginx-hosted frontend that proxies `/api/` and WebSocket traffic to the host-run backend (`host.docker.internal:8080`).

The backend Spring Boot app runs on the host (`mvn spring-boot:run`), not in a container, so start it for the frontend to have an API to reach.

Default entry points:

- Frontend: `http://localhost/`
- Frontend health: `http://localhost/healthz`
- Backend API health through the frontend proxy: `http://localhost/api/v1/health`

Stop the full stack:

```bash
rtk ssh my-ec2 'cd ~/training-machine-learning-application && docker compose down'
```

## Run Backend Only

The backend's Docker setup provides only MongoDB; run the Spring Boot app on the host with Maven.

```bash
rtk ssh my-ec2 'cd ~/training-machine-learning-application/backend && docker compose up -d'   # start MongoDB on :27017
rtk ssh my-ec2 'cd ~/training-machine-learning-application/backend && mvn spring-boot:run'     # run the API on the host
```

Default backend entry points:

- API (host-run Spring Boot): `http://localhost:8080/`
- API health: `http://localhost:8080/api/v1/health`

Stop MongoDB:

```bash
rtk ssh my-ec2 'cd ~/training-machine-learning-application/backend && docker compose down'
```

## Run Frontend Only

The frontend has its own Docker setup that serves the host-built `dist/` through Nginx. By default it proxies API traffic to `http://host.docker.internal:8080`, so run the backend stack first when API calls are needed. Build the bundle before bringing the image up:

```bash
rtk ssh my-ec2 'cd ~/training-machine-learning-application/frontend && npm ci && npm run build'
rtk ssh my-ec2 'cd ~/training-machine-learning-application/frontend && docker compose up -d --build'
```

Default frontend-only entry points:

- Frontend: `http://localhost:5173/`
- Health: `http://localhost:5173/healthz`

Stop the frontend stack:

```bash
rtk ssh my-ec2 'cd ~/training-machine-learning-application/frontend && docker compose down'
```

## Development Commands

Frontend commands require Node.js 24 or newer on the machine running them. If the EC2 host is still on an older Node.js version, run these inside a Node 24 container or use the Docker build path instead.

Frontend commands with a Node 24 host:

```bash
rtk ssh my-ec2 'cd ~/training-machine-learning-application/frontend && npm run typecheck'
rtk ssh my-ec2 'cd ~/training-machine-learning-application/frontend && npm run lint'
rtk ssh my-ec2 'cd ~/training-machine-learning-application/frontend && npm run test'
rtk ssh my-ec2 'cd ~/training-machine-learning-application/frontend && npm run build'
rtk ssh my-ec2 'cd ~/training-machine-learning-application/frontend && npm run e2e'
```

Frontend validation without installing Node 24 on the EC2 host:

```bash
rtk ssh my-ec2 'cd ~/training-machine-learning-application/frontend && docker run --rm -v "$PWD":/app -w /app node:24-bookworm-slim sh -lc "npm ci && npm run typecheck && npm run lint && npm run test && npm run build"'
```

Backend commands:

```bash
rtk ssh my-ec2 'cd ~/training-machine-learning-application/backend && mvn test'
```

Docker configuration checks:

```bash
rtk ssh my-ec2 'cd ~/training-machine-learning-application && docker compose config --quiet'
rtk ssh my-ec2 'cd ~/training-machine-learning-application/backend && docker compose config --quiet'
rtk ssh my-ec2 'cd ~/training-machine-learning-application/frontend && docker compose config --quiet'
```

## Smoke Test

After the root stack is running:

```bash
rtk ssh my-ec2 'curl -fsS http://localhost/healthz && curl -fsS http://localhost/api/v1/health'
```

For an authenticated development request, the current backend accepts a bearer token containing the email identity:

```bash
rtk ssh my-ec2 'curl -fsS -H "Authorization: Bearer admin@example.com" http://localhost/api/v1/auth/me'
```

## Documentation Rules

When behavior, architecture, tooling, or runtime setup changes, update the matching documentation under `docs/` in the same change. The architecture docs are the source of truth for intended stack decisions, and the OpenAPI contract is the source of truth for API shape.

## Commit Style

Use Conventional Commits as described in `docs/github-commit-strategy.md`, for example:

```text
feat(frontend): add training job form
fix(api): handle missing project
docs: update docker setup notes
```
