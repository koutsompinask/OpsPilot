# Service Boundary Review

## RAG Backend

`knowledge-base-service` and `ai-orchestrator-service` were consolidated into `assistant-service`.

Reasoning:
- both previous services depended on the same `knowledge` Postgres schema
- chat retrieval already queried knowledge tables directly, so the runtime boundary was not real
- security, correlation/logging, and embedding provider implementations were duplicated across both services

Result:
- one service now owns document ingestion, embeddings, retrieval, grounded answer generation, and low-confidence ticket creation
- gateway routing for `/documents/**` and `/chat/**` points to the same backend runtime
- duplication is reduced without introducing a shared module for code that no longer needs a service boundary

## Remaining Services

Current recommendation for the rest of the backend:
- keep `auth-service`, `tenant-service`, `ticket-service`, and `notification-service` separate for now because they still map to distinct business capabilities and integration boundaries
- revisit consolidation only if direct database coupling or repeated domain logic appears between those services, not just shared boilerplate
- extract shared platform code into a common module only when duplication persists across still-valid service boundaries
