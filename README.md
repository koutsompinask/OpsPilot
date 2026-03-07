# OpsPilot Monorepo (Phase 4)

Current state provides the baseline monorepo scaffold plus Phase 2 + Phase 4 implementation:
- Gradle multi-project Spring Boot service skeletons
- React + Vite + TypeScript + Tailwind frontend app with complete minimal Phase 2 flow
- Local infrastructure with Docker Compose
- Placeholder infra/docs directories for later phases
- JWT authentication with register/login/refresh in `auth-service`
- Tenant and user management APIs in `tenant-service`
- API gateway auth routing and JWT enforcement for protected paths
- Knowledge document ingestion in `knowledge-base-service` (`POST/GET/DELETE /documents` with async processing status)
- AI chat orchestration in `ai-orchestrator-service` (`POST /chat/ask` with vector retrieval, confidence, and citations)

## Structure

- `frontend/` React app shell with required routes
- `services/` Spring Boot microservice skeletons
- `infra/` Docker/Kubernetes/Helm/Jenkins/Terraform placeholders
- `docs/` architecture/api/diagram placeholders
- `scripts/` helper scripts (empty in Phase 1)

## Local Development

1. Copy defaults:
   - `cp .env.example .env`
2. Start current local stack (recommended):
   - `./scripts/start-local.sh .env`
   - This loads env vars, starts required Docker infra/stubs, starts real Phase 3 backend services (`api-gateway`, `auth-service`, `tenant-service`, `knowledge-base-service`), and starts frontend dev server.
3. Manual compose options (infra/stubs only):
   - `docker compose --env-file .env.example up -d`
   - `docker compose --env-file .env.example --profile apps up -d`

Default local ports:
- Infra: `5432` (Postgres), `6379` (Redis), `5672` + `15672` (RabbitMQ), `9000` + `9001` (MinIO)
- App stubs: `5173` (frontend), `8080-8087` (gateway/services)

## Backend

Run all service tests from repo root:

```bash
./gradlew test
```

## Frontend

```bash
cd frontend
npm install
npm run build
```

Phase 2 UI routes:
- `/register` tenant bootstrap registration
- `/login` login for existing users
- `/dashboard` tenant summary
- `/tenant-users` list/create tenant users
- `/tenant-settings` view/update tenant profile/settings
- Logout is available from the app header.

## Notes

- Most services are still skeleton-only; `auth-service`, `tenant-service`, `knowledge-base-service`, `ai-orchestrator-service`, and `api-gateway` now include implemented behavior.
- Business endpoints and cross-service workflows are added in later phases.
- Health endpoint baseline: `/actuator/health`.
- If UI requests fail with `ERR_CONNECTION_REFUSED`, start the local stack first (`./scripts/start-local.sh .env`).
- If browser reports CORS on protected tenant/user routes, ensure `api-gateway` is running latest config and restart it.
