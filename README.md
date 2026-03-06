# OpsPilot Monorepo (Phase 1)

Phase 1 provides the baseline monorepo scaffold:
- Gradle multi-project Spring Boot service skeletons
- React + Vite + TypeScript + Tailwind frontend shell
- Local infrastructure with Docker Compose
- Placeholder infra/docs directories for later phases

## Structure

- `frontend/` React app shell with required routes
- `services/` Spring Boot microservice skeletons
- `infra/` Docker/Kubernetes/Helm/Jenkins/Terraform placeholders
- `docs/` architecture/api/diagram placeholders
- `scripts/` helper scripts (empty in Phase 1)

## Local Development

1. Copy defaults:
   - `cp .env.example .env`
2. Start infra only:
   - `docker compose --env-file .env.example up -d`
3. Start infra + app stubs:
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

## Notes

- Services are skeleton-only in Phase 1.
- Business endpoints and cross-service workflows are added in later phases.
- Health endpoint baseline: `/actuator/health`.
