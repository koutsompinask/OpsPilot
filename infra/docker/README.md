# Docker Infrastructure

Local Docker infrastructure is now an active part of the implemented development workflow rather than a placeholder.

## Current Local Workflow

1. Build backend runtime artifacts on the host:
   `./gradlew :services:api-gateway:bootJar :services:auth-service:bootJar :services:tenant-service:bootJar :services:assistant-service:bootJar :services:ticket-service:bootJar :services:notification-service:bootJar`
2. Start the local stack:
   `./scripts/start-local.sh .env.example`
3. Stop the local stack:
   `docker compose --env-file .env.example down`

## Current Runtime Shape

- `docker-compose.yml` runs the implemented stack: `frontend`, `api-gateway`, `auth-service`, `tenant-service`, `assistant-service`, `ticket-service`, `notification-service`
- Shared infra services: `postgres`, `redis`, `rabbitmq`, `minio`, `webhook-receiver`
- `analytics-service` is intentionally excluded from the active local stack until a later analytics phase

## Image Strategy

- Backend service images are runtime-only images
- Backend jars are built outside Docker and copied from `services/*/build/libs/*.jar`
- Frontend still uses a multi-stage build to produce static assets served by `nginx`
- CI/CD can later take over artifact creation and image publication; local development currently relies on manual Gradle `bootJar` commands

## Operational Notes

- `scripts/start-local.sh` now removes orphaned containers and fails early if required host ports are already occupied
- The most common local failure mode after the old host-run workflow is stale Java or Vite processes still holding ports like `5173` or `8080-8086`
- If startup fails on a bound port, stop the conflicting host process or change the corresponding `*_PORT` value in the env file
