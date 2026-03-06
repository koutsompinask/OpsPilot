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
