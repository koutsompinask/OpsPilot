## Pending
- [ ] Phase 2: Authentication system (JWT auth, tenant creation, user management)
- [ ] Phase 3: Knowledge ingestion (upload, extraction, chunking, embeddings, pgvector)
- [ ] Phase 4: AI chat workflow (RAG retrieval, citations, confidence)

## To-Do
- [ ] Start Phase 2 implementation planning (auth, tenant, user management)

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

### Review
- `docker compose --env-file .env.example up -d` and `docker compose --env-file .env.example --profile apps up -d` both start successfully with standard port mappings (`5173`, `8080-8087`, `5432`, `6379`, `5672`, `15672`, `9000`, `9001`).
- `./gradlew test` passes after dependency correction; prior failures were caused by stale ownership/network environment issues and an invalid gateway starter artifact.
- `npm run build` completes successfully with Vite output under `frontend/dist`.
- `README.md` updated to reflect standard ports and explicit `--env-file .env.example` compose commands.
