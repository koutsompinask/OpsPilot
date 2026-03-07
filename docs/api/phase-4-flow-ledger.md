# Phase 4 Flow Ledger

Date: 2026-03-07
Environment: isolated local alt-port stack (`api-gateway` on `18080`, `ai-orchestrator-service` on `18084`, `auth-service` on `18081`, `tenant-service` on `18082`, `knowledge-base-service` on `18083`)

## Scope
Validated Phase 4 runtime flow through API Gateway:
1. `POST /chat/ask` (authenticated RAG response)
2. source citations in chat response
3. confidence score in chat response
4. low-confidence behavior with `ticketCreated=false`

Also validates:
- gateway route wiring for `/chat/**`
- protected endpoint denial without JWT (`401`)
- chat request execution against ingested tenant document chunks

## Repro Commands
```bash
# Full isolated Phase 4 smoke stack and flow:
/tmp/opspilot-phase4-smoke-full-alt.sh
```

## Observed Result Snapshot
Run timestamp: 2026-03-07 21:30 (Europe/Athens)

- `POST /chat/ask` returned `200` with answer payload including:
  - `confidence=0.503`
  - `sources` array length `1`
  - `ticketCreated=false`
- Unauthorized check: `POST /chat/ask` without JWT returned `401`

## Notes
- Phase 4 returns low-confidence responses as normal `200` payloads; ticket creation remains disabled until Phase 5.
- Response citations use document filename + chunk identifier (`chunk-<index>`).
- OpenAI answer generation is configured to fallback to deterministic local responder when unavailable/failing.
