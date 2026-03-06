# Phase 2 Flow Ledger

Date: 2026-03-06
Environment: local (`api-gateway` on `8080`, `auth-service` on `8081`, `tenant-service` on `8082`, Postgres on `5432`)

## Scope
Validated Phase 2 runtime flow through API Gateway:
1. `POST /auth/register`
2. `POST /auth/login`
3. `GET /tenants/me` (JWT required)
4. `POST /users` (JWT + admin role)
5. `GET /users` (JWT + admin role)

Also validated protected route denial without JWT:
- `GET /tenants/me` returns `401` without `Authorization` header.

## Repro Commands
```bash
BASE_URL=http://localhost:8080
EMAIL="admin.$(date +%s)@example.com"
PASSWORD="password123"

REGISTER_PAYLOAD="{\"tenantName\":\"Acme Hotel\",\"adminName\":\"Alice Admin\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}"
REGISTER_RESP=$(curl -sS -X POST "$BASE_URL/auth/register" -H 'Content-Type: application/json' -d "$REGISTER_PAYLOAD")
ACCESS_TOKEN=$(python3 -c 'import json,sys; print(json.loads(sys.stdin.read())["accessToken"])' <<< "$REGISTER_RESP")

LOGIN_RESP=$(curl -sS -X POST "$BASE_URL/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
LOGIN_ACCESS=$(python3 -c 'import json,sys; print(json.loads(sys.stdin.read())["accessToken"])' <<< "$LOGIN_RESP")

curl -sS "$BASE_URL/tenants/me" -H "Authorization: Bearer $LOGIN_ACCESS"

curl -sS -X POST "$BASE_URL/users" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $LOGIN_ACCESS" \
  -d '{"displayName":"Bob Member","email":"bob.member@example.com","password":"memberpass123","role":"TENANT_MEMBER"}'

curl -sS "$BASE_URL/users" -H "Authorization: Bearer $LOGIN_ACCESS"

curl -s -o /tmp/noauth.txt -w '%{http_code}' "$BASE_URL/tenants/me"
```

## Observed Result Snapshot
Run timestamp: 2026-03-06 12:50 (Europe/Athens)

- Register: success, returned token pair (`expiresIn=900`, `tokenType=Bearer`)
  - created admin email: `admin.1772794227@example.com`
- Login: success, returned token pair
- Tenant lookup: success
  - `tenant_id = cd7d66f2-6ee6-4458-80a2-2210d00790be`
  - `name = Acme Hotel`
- Create user: success
  - created member `bob.member@example.com` role `TENANT_MEMBER`
- List users: success
  - returned 2 users (`TENANT_ADMIN`, `TENANT_MEMBER`)
- Unauthorized check: `GET /tenants/me` without JWT returned HTTP `401`

## Notes
- The `api-gateway` enforces JWT for `/tenants/**` and `/users/**`.
- `auth-service` and `tenant-service` internal cross-service calls were successful during registration and user creation.
- Browser/UI flow is now available for the same Phase 2 scope (`register -> login -> tenant dashboard -> tenant users/settings`).
- Gateway security permits CORS preflight (`OPTIONS`) for protected tenant/user routes, enabling frontend calls from `http://localhost:5173`.
