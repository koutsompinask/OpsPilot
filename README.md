# OpsPilot Monorepo (Phase 5 Backend)

Current state provides the baseline monorepo scaffold plus implemented backend flows through Phase 5, the first Phase 6 frontend ticket workspace, and a Dockerized local infrastructure baseline for implemented services:
- Gradle multi-project Spring Boot service skeletons
- React + Vite + TypeScript + Tailwind frontend app with implemented auth, dashboard, documents, chat, and tickets flows
- Local infrastructure and implemented app runtime with Docker Compose
- Placeholder infra/docs directories for later phases
- JWT authentication with register/login/refresh in `auth-service`
- Tenant and user management APIs in `tenant-service`
- API gateway auth routing and JWT enforcement for protected paths
- RAG document ingestion and chat workflows in `assistant-service` (`POST/GET/DELETE /documents`, `POST /chat/ask`, async ingestion, retrieval, confidence, and citations)
- Support workflow in `ticket-service` (`GET/POST/PATCH /tickets`, internal low-confidence ticket creation, `ticket.created` event publishing)
- Generic webhook notification delivery in `notification-service` for `ticket.created` and `document.processed`

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
   - Before running it, build backend jars once on the host:
     `./gradlew :services:api-gateway:bootJar :services:auth-service:bootJar :services:tenant-service:bootJar :services:assistant-service:bootJar :services:ticket-service:bootJar :services:notification-service:bootJar`
   - Then the script loads env vars, builds runtime images from those jars, and starts the full local Docker stack for `frontend`, `api-gateway`, `auth-service`, `tenant-service`, `assistant-service`, `ticket-service`, `notification-service`, plus shared infra (`postgres`, `redis`, `rabbitmq`, `minio`, `webhook-receiver`).
3. Manual compose option:
   - `docker compose --env-file .env.example up --build -d`
   - This also expects the backend jars to already exist under `services/*/build/libs/`.
4. Stop the stack:
   - `docker compose --env-file .env down`

Default local ports:
- Infra: `5432` (Postgres), `6379` (Redis), `5672` + `15672` (RabbitMQ), `9000` + `9001` (MinIO), `8090` (webhook receiver)
- Apps: `5173` (frontend), `8080` (gateway), `8081` (auth), `8082` (tenant), `8083` (assistant), `8085` (ticket), `8086` (notification)

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

- `auth-service`, `tenant-service`, `assistant-service`, `api-gateway`, `ticket-service`, and `notification-service` are implemented and included in the default Docker stack.
- Backend Docker images are runtime-only; jar compilation happens outside Docker and is expected to be handled manually for local development and later by CI/CD.
- Frontend `/tickets` now exposes the support queue workspace for tenant users, with admin-only status management and answer-context review.
- `analytics-service` is intentionally excluded from the active local stack pending a later analytics phase.
- Business endpoints and cross-service workflows are added in later phases.
- Health endpoint baseline: `/actuator/health`.
- If UI requests fail with `ERR_CONNECTION_REFUSED`, start the local stack first (`./scripts/start-local.sh .env`).
- If browser reports CORS on protected tenant/user routes, ensure `api-gateway` is running latest config and restart it.
