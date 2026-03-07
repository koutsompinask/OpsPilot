## Pending
- [ ] Phase 3: Knowledge ingestion (upload, extraction, chunking, embeddings, pgvector)
- [ ] Phase 4: AI chat workflow (RAG retrieval, citations, confidence)
- [ ] Phase 5: Support workflow (ticket creation, RabbitMQ events, notifications)

## To-Do
- [ ] Phase 5: Support workflow (ticket creation, RabbitMQ events, notifications)

## Done
- [x] Session initialized with `plan.md` + `AGENTS.md` review
- [x] Created monorepo Phase 1 structure (`frontend`, `services/*`, `infra/*`, `docs/*`, `scripts`)
- [x] Added root scaffold files: `README.md`, `settings.gradle`, `build.gradle`, `gradle.properties`, `.env.example`, `docker-compose.yml`
- [x] Scaffolded 8 Spring Boot services with app entrypoint, `application.yml`, context-load test, and `Dockerfile`
- [x] Scaffolded frontend React + Vite + TypeScript + Tailwind shell with required routes and shared layout
- [x] Added infra/docs placeholder READMEs under `infra/*` and `docs/*`
- [x] Verified Gradle wrapper artifacts exist (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`)
- [x] Restored compose defaults and `.env.example` to standard local ports (`5432`, `6379`, `5672`, `15672`, `9000`, `9001`, app ports `5173`/`8080-8087`)
- [x] Fixed backend build break in `services/api-gateway/build.gradle` (use `spring-cloud-starter-gateway`)
- [x] Verified backend tests pass (`./gradlew --project-cache-dir /tmp/opspilot-gradle-cache test`)
- [x] Verified frontend build passes (`cd frontend && npm install && npm run build`)
- [x] Verified compose config renders cleanly (`docker compose config`)
- [x] Verified compose smoke startup for infra and apps profiles (`docker compose --env-file .env.example up -d` and `docker compose --env-file .env.example --profile apps up -d`)
- [x] Start Phase 2 implementation planning (auth, tenant, user management)
- [x] Implemented auth-service persistence + Flyway + JWT token issuance (`auth_users`, `refresh_sessions`, `/auth/register|login|refresh`, refresh rotation)
- [x] Implemented tenant-service persistence + Flyway + tenant/user APIs (`tenants`, `user_profiles`, `/tenants/me`, `/users`)
- [x] Implemented auth/tenant internal service-token protected endpoints (`/internal/tenants/bootstrap`, `/internal/auth/users`)
- [x] Implemented API gateway JWT enforcement and auth/tenant route config (`/auth/**` public; `/tenants/**` and `/users/**` protected)
- [x] Wired minimal frontend login to `/auth/login` with token storage + redirect to dashboard
- [x] Verified Phase 2 backend and frontend checks (`./gradlew --project-cache-dir /tmp/opspilot-gradle-cache test`, `cd frontend && npm run build`)
- [x] Ran Phase 2 gateway smoke flow (`register -> login -> tenants/me -> users create/list`) against live local services
- [x] Added Phase 2 documentation ledger (`docs/api/phase-2-flow-ledger.md`) and linked it from `docs/api/README.md`
- [x] Added `scripts/start-local.sh` to run current local stack (env + docker deps + backend + frontend)
- [x] Updated `AGENTS.md` with rule to keep local startup script current as phases advance
- [x] Fixed gateway CORS for frontend origin (`http://localhost:5173`) on auth endpoints
- [x] Added minimal frontend register page and route for Phase 2 auth flow testing
- [x] Guarded frontend private routes so unauthenticated users cannot access app pages
- [x] Implemented complete minimal Phase 2 frontend UI flow (login/register/logout + tenant summary/settings/users list/create)
- [x] Added frontend auth/API lifecycle helpers (claim decoding, token validity checks, clear session, auto refresh on `401`)
- [x] Updated frontend navigation to prioritize Phase 2 pages and mark later-phase pages as upcoming
- [x] Fixed gateway security preflight handling for protected Phase 2 routes (`OPTIONS /**` permit + security CORS enabled)
- [x] Implemented structured JSON logging baseline (`logback-spring.xml`) across all backend services
- [x] Added `X-Request-Id` generation/propagation for gateway/auth/tenant request paths and internal service calls
- [x] Added operational lifecycle logs in gateway/auth/tenant services plus centralized exception logging
- [x] Updated `AGENTS.md` and `plan.md` with explicit logging and correlation requirements for future phases
- [x] Added correlation-ID tests for gateway/auth/tenant and verified backend service tests pass

### Review
- `docker compose --env-file .env.example up -d` and `docker compose --env-file .env.example --profile apps up -d` both start successfully with standard port mappings (`5173`, `8080-8087`, `5432`, `6379`, `5672`, `15672`, `9000`, `9001`).
- `./gradlew test` passes after dependency correction; prior failures were caused by stale ownership/network environment issues and an invalid gateway starter artifact.
- `npm run build` completes successfully with Vite output under `frontend/dist`.
- `README.md` updated to reflect standard ports and explicit `--env-file .env.example` compose commands.
- `./gradlew --project-cache-dir /tmp/opspilot-gradle-cache test` succeeds after Phase 2 implementation (all service test tasks passed, including `auth-service`, `tenant-service`, and `api-gateway`).
- `cd frontend && npm run build` succeeds after login wiring; Vite build emits production assets in `frontend/dist`.
- Live smoke on 2026-03-06 through `http://localhost:8080` succeeded: register/login/tenant lookup/user create/user list; protected route check without JWT returned `401`.
- `bash -n scripts/start-local.sh` passes and timed run `timeout 120s ./scripts/start-local.sh .env.example` reaches `[ready]` for tenant/auth/gateway/frontend before timeout cleanup.
- CORS preflight verification passed: `OPTIONS /auth/login` via gateway returned `200` with `Access-Control-Allow-Origin: http://localhost:5173` and allowed methods/headers.
- Frontend register route verification: `cd frontend && npm run build` succeeds after adding `/register` page and auth client `register(...)` call.
- Frontend route guard verification: `cd frontend && npm run build` succeeds after adding `RequireAuth`/`PublicOnly` guards around private/public routes.
- `cd frontend && npm run build` succeeds after complete Phase 2 frontend UI implementation (`tsc -b` + Vite build on 2026-03-06).
- `./gradlew --project-cache-dir /tmp/opspilot-gradle-cache :services:api-gateway:compileJava` succeeds after gateway preflight/CORS security fix.
- `GRADLE_USER_HOME=/tmp/opspilot-gradle-home ./gradlew --project-cache-dir /tmp/opspilot-gradle-cache :services:api-gateway:test :services:auth-service:test :services:tenant-service:test` succeeded after adding correlation-ID filters/tests and logging updates.
- `GRADLE_USER_HOME=/tmp/opspilot-gradle-home ./gradlew --project-cache-dir /tmp/opspilot-gradle-cache :services:knowledge-base-service:test :services:ai-orchestrator-service:test :services:ticket-service:test :services:notification-service:test :services:analytics-service:test` succeeded after adding shared JSON logging baseline.
