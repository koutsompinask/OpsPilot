# OpsPilot Implementation Plan

Last updated: 2026-03-16
Status: backend phases 1-5 are implemented; frontend core flows plus tickets workspace are implemented; analytics, production infra, CI/CD, and cloud deployment remain open.

## Project Intent

OpsPilot is a multi-tenant AI support assistant platform for small and mid-sized businesses. Tenant admins can upload knowledge documents, tenant users can ask grounded questions in chat, and low-confidence answers can escalate into support tickets for human follow-up.

This repository is no longer in greenfield planning mode. The plan below reflects the codebase and commit history through:
- `refactor(backend): consolidate rag services into assistant-service` on 2026-03-16
- the Phase 5 backend support workflow delivered on 2026-03-13
- the Phase 6A tickets workspace delivered on 2026-03-13
- the follow-up assistant-service review fixes completed on 2026-03-16

## Current Repository Shape

Top-level structure:

```text
opspilot/
├─ frontend/
├─ services/
│  ├─ analytics-service/
│  ├─ api-gateway/
│  ├─ assistant-service/
│  ├─ auth-service/
│  ├─ notification-service/
│  ├─ tenant-service/
│  └─ ticket-service/
├─ infra/
│  ├─ docker/
│  ├─ helm/
│  ├─ jenkins/
│  ├─ k8s/
│  └─ terraform/
├─ docs/
├─ scripts/
├─ docker-compose.yml
├─ plan.md
└─ tasks.md
```

Notes:
- `knowledge-base-service` and `ai-orchestrator-service` were intentionally consolidated into `assistant-service`.
- `analytics-service` still exists as a scaffold only.
- `infra/*` directories are still mostly placeholders for later phases.

Backend package convention for implemented services remains mandatory:
- use `config`, `controller`, `service`, `repository`, `domain`, `dto`, `mapper`, `exception`, `security`, `util`
- place entities under `domain/entity`
- nest subcategories under their parent layer
- validate with `bash scripts/verify-service-structure.sh`

## Current Architecture

### Frontend

Technology:
- React
- TypeScript
- Vite
- Tailwind CSS

Implemented routes:
- `/register`
- `/login`
- `/dashboard`
- `/tenant-users`
- `/tenant-settings`
- `/documents`
- `/chat`
- `/tickets`

Current frontend status:
- auth, tenant management, document management, chat, and tickets are live
- `/analytics` is intentionally placeholder
- Phase 6 visual redesign and motion polish are in place

### Backend Services

#### API Gateway

Purpose:
- single ingress for frontend traffic
- route forwarding to implemented backend services
- auth forwarding and protected-route enforcement
- request correlation propagation

Implemented route groups:
- `/auth/**`
- `/tenants/**`
- `/users/**`
- `/documents/**`
- `/chat/**`
- `/tickets/**`

#### Auth Service

Purpose:
- tenant bootstrap registration
- login and refresh token flow
- JWT issuance

Implemented endpoints:

```text
POST /auth/register
POST /auth/login
POST /auth/refresh
```

#### Tenant Service

Purpose:
- tenant profile lookup/update
- tenant-scoped user management

Implemented endpoints:

```text
GET /tenants/me
PUT /tenants/me
GET /users
POST /users
```

#### Assistant Service

Purpose:
- document ingestion
- text chunking and embedding generation
- document metadata and chunk persistence
- vector search
- grounded answer generation
- low-confidence ticket escalation

Implemented endpoints:

```text
POST /documents
GET /documents
GET /documents/{id}
DELETE /documents/{id}
POST /chat/ask
```

Key implementation notes:
- owns the former knowledge and chat workflows behind one runtime
- uses PostgreSQL plus pgvector and MinIO-backed document storage
- publishes `document.processed`
- calls `ticket-service` internally when low confidence requires escalation

#### Ticket Service

Purpose:
- ticket persistence and support workflow
- tenant-scoped queue visibility
- admin-only status transitions
- `ticket.created` event publishing

Implemented endpoints:

```text
GET /tickets
POST /tickets
PATCH /tickets/{id}/status
POST /internal/tickets
```

#### Notification Service

Purpose:
- consume support and document events
- forward best-effort webhook notifications
- log delivery outcomes with correlation context

Current event coverage:
- `ticket.created`
- `document.processed`

#### Analytics Service

Purpose:
- reserved for future analytics dashboards and event aggregation

Status:
- scaffold only; no business endpoints implemented yet

## Data and Integration Model

Current persistence and infrastructure choices:
- PostgreSQL for relational data
- pgvector for document embeddings
- MinIO for document object storage
- RabbitMQ for asynchronous events
- local webhook receiver for notification smoke testing

Current core domains:
- tenants and users
- documents and document chunks
- chat answer requests and citations
- support tickets

Important architecture decision:
- the RAG domain is now centered in `assistant-service`; do not reintroduce a separate knowledge/chat split unless a real runtime boundary appears again

## Logging and Correlation Baseline

This remains a non-negotiable cross-cutting requirement:
- structured JSON logs for implemented backend services
- request lifecycle logs at API boundaries
- business outcome logs for important domain actions
- centralized exception logging with sensible severity
- `X-Request-Id` generation and propagation across service boundaries
- no logging of passwords, tokens, raw JWTs, or sensitive payload bodies

## Verified Delivery Ledger

### Implemented and verified

#### Phase 1
- monorepo scaffold
- service skeletons
- frontend shell
- Docker Compose baseline

#### Phase 2
- auth flow
- tenant management
- user management
- gateway protection and CORS handling
- frontend register, login, dashboard, tenant users, tenant settings

#### Phase 3
- document upload and listing
- async ingestion flow
- chunking and embedding
- pgvector persistence
- document delete and detail flow

#### Phase 4
- authenticated chat ask flow
- vector retrieval and grounded answers
- confidence score and source citations
- gateway `/chat/**` routing

Verification reference:
- `docs/api/phase-4-flow-ledger.md`

#### Phase 5
- low-confidence auto-ticket creation
- `ticket-service` APIs and persistence
- `notification-service` event consumption and webhook delivery
- gateway and startup wiring for the support workflow

Verification reference:
- `docs/api/phase-5-flow-ledger.md`

#### Phase 6 completed subset
- frontend redesign system and route refresh
- live `/tickets` workspace with list, detail, filters, and admin status controls

### Post-phase hardening already completed

Recent follow-up work from commit history:
- consolidated `knowledge-base-service` and `ai-orchestrator-service` into `assistant-service`
- documented the new service boundary in `docs/architecture/service-boundary-review.md`
- fixed accidental schema rename regression by restoring the persistence schema to `assistant`
- moved object storage bucket initialization to explicit startup
- removed duplicated pgvector serialization logic
- batched document chunk inserts
- expanded assistant-service workflow test coverage

## Remaining Work

### Priority 1: close the frontend and product gap

1. Implement `/analytics` instead of leaving it as a placeholder.
2. Decide whether Phase 6 needs any additional post-redesign polish beyond the current tickets workspace.
3. Keep frontend docs and route inventory aligned with live behavior.

### Priority 2: harden the merged assistant runtime

1. Continue reviewing `assistant-service` for clean boundaries after the merge.
2. Watch for new duplication that would justify a shared library, but only after repeated concrete evidence.
3. Preserve startup reliability and migration compatibility when renaming packages or services.

### Priority 3: observability and analytics

1. Define the analytics event model and dashboard scope.
2. Implement real `analytics-service` endpoints and data capture.
3. Add Prometheus and Grafana assets under `infra/` when the metrics contract is ready.

### Priority 4: delivery infrastructure

1. Replace placeholder infra docs with deployable Kubernetes manifests or Helm charts.
2. Add Jenkins pipeline assets for build, test, image build, and smoke verification.
3. Document image strategy and deployment flow.

### Priority 5: cloud deployment

1. Define AWS target architecture for EKS, RDS, object storage, and ingress.
2. Add Terraform and environment overlays only after local and container deployment assets are settled.

## Next Recommended Execution Order

1. Finish the analytics slice end to end:
   implement backend event capture, analytics API, and the `/analytics` frontend page together.
2. Convert infrastructure placeholders into a minimal real deployment baseline:
   container/image documentation, Kubernetes manifests or Helm, then Jenkins pipeline.
3. Add cloud deployment assets after local and cluster deployment paths are reproducible.

## Verification Expectations For Future Tasks

Every non-trivial task should include:
- relevant Gradle test targets for changed services
- `cd frontend && npm run build` for frontend changes
- `bash scripts/verify-service-structure.sh` when package layout changes
- `bash -n scripts/start-local.sh` plus a practical smoke run when startup behavior changes
- updates to `tasks.md` with concrete verification evidence before handoff

## Lessons Incorporated Into This Plan

From `tasks/lessons.md`, the plan now explicitly guards against:
- shipping backend phases without the matching minimal frontend route when full flow delivery is expected
- missing gateway CORS preflight coverage on protected routes
- unstable animation callbacks that retrigger on rerender
- broad rename operations that unintentionally alter persisted schema or Flyway-managed artifacts
- startup readiness windows that are too short for concurrent local boot flows

## Definition Of Current MVP

The repository currently satisfies the original MVP:
1. tenant registration
2. admin login
3. document upload
4. document embedding and retrieval
5. AI chat answers with citations
6. low-confidence ticket creation
7. staff visibility and status updates in the ticket dashboard

What is still beyond the MVP:
- analytics implementation
- production-grade infra and CI/CD
- AWS deployment assets

## Success Criteria For The Next Phase

The next meaningful milestone is complete when:
1. `/analytics` is implemented end to end
2. analytics data is captured from live workflows instead of placeholder content
3. local verification includes analytics behavior alongside existing chat and ticket flows
4. planning docs, startup scripts, and infra assets remain aligned with the actual repo state
