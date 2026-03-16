# Lessons Learned

## 2026-03-06

### 1) Missing frontend registration path initially
- What went wrong: Phase 2 auth flow was delivered without a register page wired in the UI.
- Preventive rule: For each phase endpoint in `plan.md`, ensure there is a matching minimal UI path when user requests complete frontend flow.
- Early detection: Compare route map against Phase 2 endpoint ledger before handoff.

### 2) Protected-route CORS preflight gap in gateway security
- What went wrong: CORS worked for auth route checks but failed on protected tenant/user preflight via security chain.
- Preventive rule: In Spring Security gateway config, explicitly enable CORS and permit `OPTIONS /**` requests.
- Early detection: Run explicit preflight checks for both public and protected routes (`/auth/login`, `/tenants/me`) before handoff.

## 2026-03-07

### 3) Typing animation restarted on rerenders
- What went wrong: `TypingText` effect depended on `onComplete`; inline callback identity changed on rerender and retriggered typing.
- Preventive rule: Do not include unstable callback props in animation effect dependencies; store callback in a ref and trigger from the ref.
- Early detection: In dev/StrictMode, verify typing plays once through sequential chains (badge -> title -> copy) without restart flicker.

## 2026-03-16

### 4) Bulk rename accidentally changed persisted schema ownership
- What went wrong: During the RAG service merge, a broad rename changed SQL migration and JDBC references from the existing `knowledge` schema to `assistant`, which broke Flyway validation and local startup against the already-initialized database.
- Preventive rule: When renaming a service/package, treat database schema names and migration contents as separate compatibility decisions; never mass-rename them unless the migration plan explicitly includes schema migration/repair.
- Early detection: Diff migration files and grep SQL/JDBC references for schema names before running startup; if Flyway-managed history already exists, validate checksum compatibility before handoff.

### 5) Startup readiness window was too short for concurrent Gradle boot runs
- What went wrong: `scripts/start-local.sh` launched several backend services in parallel, but the readiness poll budget was too small, so healthy services missed the cutoff and the smoke run failed.
- Preventive rule: Size readiness timeouts for worst-case local startup under concurrent `bootRun`, not just ideal warm-cache starts.
- Early detection: Always run one practical `start-local.sh` smoke after topology changes and inspect whether failures are actual boot errors or timeout-budget issues.
