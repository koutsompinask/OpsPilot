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
