# Phase 3 Flow Ledger

Date: 2026-03-07
Environment: local (`api-gateway` on `8080`, `knowledge-base-service` on `8083`, Postgres `5432`, MinIO `9000`, RabbitMQ `5672`)

## Scope
Validated Phase 3 runtime flow through API Gateway:
1. `POST /documents` (multipart upload, async acceptance)
2. `GET /documents` (tenant-scoped listing)
3. `GET /documents/{id}` (tenant-scoped details/status)
4. `DELETE /documents/{id}` (tenant admin only)

Also validates:
- async status transition (`PROCESSING` -> `READY` or `FAILED`)
- text-only file acceptance (`.txt`, `.md`)
- unsupported file rejection (`400`)
- protected endpoint denial without JWT (`401`)

## Repro Commands
```bash
BASE_URL=http://localhost:8080
EMAIL="admin.$(date +%s)@example.com"
PASSWORD="password123"

REGISTER_PAYLOAD="{\"tenantName\":\"Acme Hotel\",\"adminName\":\"Alice Admin\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"
REGISTER_RESP=$(curl -sS -X POST "$BASE_URL/auth/register" -H 'Content-Type: application/json' -d "$REGISTER_PAYLOAD")
ACCESS_TOKEN=$(python3 -c 'import json,sys; print(json.loads(sys.stdin.read())["accessToken"])' <<< "$REGISTER_RESP")

printf 'Hotel check-in is at 15:00. Breakfast starts at 07:00.' > /tmp/opspilot-phase3.txt
UPLOAD_RESP=$(curl -sS -X POST "$BASE_URL/documents" -H "Authorization: Bearer $ACCESS_TOKEN" -F file=@/tmp/opspilot-phase3.txt)
DOC_ID=$(python3 -c 'import json,sys; print(json.loads(sys.stdin.read())["id"])' <<< "$UPLOAD_RESP")

curl -sS "$BASE_URL/documents" -H "Authorization: Bearer $ACCESS_TOKEN"
curl -sS "$BASE_URL/documents/$DOC_ID" -H "Authorization: Bearer $ACCESS_TOKEN"

curl -sS -X DELETE "$BASE_URL/documents/$DOC_ID" -H "Authorization: Bearer $ACCESS_TOKEN" -i

curl -s -o /tmp/noauth-documents.txt -w '%{http_code}' "$BASE_URL/documents"
curl -sS -X POST "$BASE_URL/documents" -H "Authorization: Bearer $ACCESS_TOKEN" -F file=@/etc/hosts
```

## Observed Result Snapshot
Run timestamp: 2026-03-07 17:11 (Europe/Athens)

- Upload acceptance: `POST /documents` returned `202` with initial `status=PROCESSING`
- Async completion: `GET /documents/{id}` transitioned to `status=READY` within poll window
- List API: `GET /documents` returned tenant-scoped array with uploaded document (`count=1`)
- Delete API: `DELETE /documents/{id}` returned `204`
- Unauthorized check: `GET /documents` without JWT returned `401`
- Validation check: uploading unsupported `.csv` returned `400`
- Event publishing check: knowledge-service log contains `knowledge_document_processed_event_published` for processed document id

## Notes
- Phase 3 currently supports `.txt` and `.md` only.
- Ingestion runs asynchronously in service worker threads and surfaces outcome via document status fields.
- `DocumentProcessed` event publication is enabled by default and logged with request/tenant/document correlation fields.
