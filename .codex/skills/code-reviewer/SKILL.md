---
name: code-reviewer
description: Use this skill for code review in pharma-2, prioritizing bugs, security risks, regressions, missing tests, and also performance issues, optimization targets, and bad practices; present findings first ordered by severity.
---

# Code Reviewer

## Use When
- User asks for a review, audit, or risk assessment
- Reviewing a branch, diff, PR, or specific files
- Security/authorization/concurrency correctness checks
- Investigating performance regressions, high CPU/memory usage, or slow endpoints
- Looking for optimization targets (hot paths, heavy DB queries, large bundles)
- Searching for bad practices and code smells that reduce maintainability or cause inefficiency

## Review Output Contract
1. Findings first, ordered by severity.
2. Each finding includes:
  - severity (`High`/`Medium`/`Low`)
  - file reference(s) and line(s)
  - why it matters (including performance impact when relevant)
  - concrete fix direction and, for performance items, a measurable verification step (e.g., expected latency or memory reduction)
3. Keep summary brief and after findings.
4. If no findings: explicitly state no findings and note residual risk/testing gaps (including unprofiled hotspots).

## Review Checklist
- Authorization and tenant isolation:
  - backend role enforcement
  - org data scoping
- API contract safety:
  - DTO changes reflected in consumers
  - status/error code consistency
- Data integrity and concurrency:
  - idempotency behavior
  - race-prone state transitions
- Migration safety:
  - Flyway compatibility and rollback implications
- Test coverage:
  - missing tests for changed critical behavior
- Performance and optimization:
  - identify expensive DB queries, N+1 query patterns, missing indexes
  - highlight heavy in-memory allocations, long-lived collections, and GC pressure risks
  - spot hot CPU paths, synchronous blocking calls in async flows, and unbounded concurrency
  - frontend: large bundle sizes, unnecessary re-renders, expensive renders, large network payloads
- Bad practices and maintainability:
  - duplicated logic, over-complex functions, large classes, unclear ownership
  - unsafe shortcuts (hardcoded secrets, disabled security checks, commented-out rollback code)

## Repo-Specific Hotspots
- Upload endpoints and role restrictions
- Upload confirm idempotency/concurrency
- Merged `assistant-service` document workflow coverage: verify upload/list/get/delete and async ingestion paths are tested, not only `/chat/**`
- Assistant ingestion performance: watch for per-chunk SQL writes in embedding/chunk persistence and require batching for larger document workloads
- Startup side effects in infrastructure clients: flag constructors that perform network initialization (for example storage bucket setup) because they make tests brittle and violate clean construction boundaries
- Storage-provider safety defaults
- Service/schema ownership drift after merges: call out cases where a renamed service still hard-codes legacy schema or domain naming unless clearly documented as intentional compatibility
- Frontend-backend contract alignment
- Performance hotspots: DB query patterns under `backend/src/main/java`, batch processing, background jobs, and upload/processing pipelines
- Frontend hotspots: large pages in `frontend/src/pages`, heavy client-side data transforms, and `api.js` call patterns contributing to repeated fetches

## Validation Suggestions
- Backend monorepo: `GRADLE_USER_HOME=/tmp/opspilot-gradle-home ./gradlew --project-cache-dir /tmp/opspilot-gradle-cache test`
- Focused backend review targets: `GRADLE_USER_HOME=/tmp/opspilot-gradle-home /home/kkout/.gradle/wrapper/dists/gradle-8.11.1-bin/bpt9gzteqjrbo1mjrsomdt32c/gradle-8.11.1/bin/gradle --project-cache-dir /tmp/opspilot-gradle-cache :services:assistant-service:test :services:api-gateway:test`
- Frontend: `cd frontend && npm run build`
- Performance validation: add profiling or lightweight benchmarks (CPU/memory sampling, `jvm` flight recorder, or `async-profiler`), and run a few representative requests under load to ensure fixes have measurable impact
- Frontend validation: run bundle analysis (`source-map-explorer` / `rollup-plugin-visualizer`) and measure render/interaction latency with Chrome DevTools Lighthouse
- Call out when commands were not run
