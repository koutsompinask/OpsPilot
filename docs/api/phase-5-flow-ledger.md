# Phase 5 Flow Ledger

Date: 2026-03-13
Environment: local backend stack via `scripts/start-local.sh` (`api-gateway` on `8080`, `auth-service` on `8081`, `tenant-service` on `8082`, `assistant-service` on `8083`, `ticket-service` on `8085`, `notification-service` on `8086`, webhook receiver on `8090`)

## Scope
Validated Phase 5 backend support workflow through API Gateway and local event delivery:
1. low-confidence `POST /chat/ask` creates a tenant-scoped ticket and returns `ticketCreated=true`
2. `GET /tickets` lists the new ticket for authenticated tenant users
3. `PATCH /tickets/{id}/status` is allowed for tenant admins and denied for tenant members
4. `ticket.created` is published to RabbitMQ and consumed by `notification-service`
5. `document.processed` is consumed by `notification-service`
6. generic webhook notifications are delivered to the local receiver

## Repro Commands
```bash
# Start the local Phase 5 backend stack
timeout 300s ./scripts/start-local.sh .env.example

# Register admin tenant
curl -sS -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"tenantName":"Phase5 Verify","adminName":"Phase5 Admin","email":"<admin-email>","password":"Adminpass123"}'

# Create member user and log them in
curl -sS -X POST http://localhost:8080/users \
  -H "Authorization: Bearer <admin-token>" \
  -H 'Content-Type: application/json' \
  -d '{"displayName":"Phase5 Member","email":"<member-email>","password":"Memberpass123","role":"TENANT_MEMBER"}'

curl -sS -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"<member-email>","password":"Memberpass123"}'

# Upload a `.txt` document and poll until status becomes READY
curl -sS -X POST http://localhost:8080/documents \
  -H "Authorization: Bearer <admin-token>" \
  -F file=@/tmp/opspilot-phase5.txt

curl -sS http://localhost:8080/documents/<document-id> \
  -H "Authorization: Bearer <admin-token>"

# Ask an unsupported question to trigger low-confidence auto-ticket creation
curl -sS -X POST http://localhost:8080/chat/ask \
  -H "Authorization: Bearer <admin-token>" \
  -H 'Content-Type: application/json' \
  -d '{"question":"How do I configure Okta SCIM provisioning for our HR system?"}'

# Verify ticket list and status authorization
curl -sS http://localhost:8080/tickets \
  -H "Authorization: Bearer <admin-token>"

curl -sS http://localhost:8080/tickets \
  -H "Authorization: Bearer <member-token>"

curl -i -sS -X PATCH http://localhost:8080/tickets/<ticket-id>/status \
  -H "Authorization: Bearer <member-token>" \
  -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS"}'

curl -sS -X PATCH http://localhost:8080/tickets/<ticket-id>/status \
  -H "Authorization: Bearer <admin-token>" \
  -H 'Content-Type: application/json' \
  -d '{"status":"RESOLVED"}'

# Verify `notification-service` consumed both events and delivered webhooks
grep -E 'notification_(ticket_created|document_processed)_received|notification_webhook_delivered' \
  .logs/notification-service.log

docker logs --since 3m opspilot-webhook-receiver-1
```

## Observed Result Snapshot
Run timestamp: 2026-03-13 (Europe/Athens)

- `POST /chat/ask` returned `200` with low-confidence answer payload and `ticketCreated=true`
- `GET /tickets` returned the new ticket for the tenant after the JWT claim fix (`tenant_id`)
- `GET /tickets` returned `200` for both admin and member; member write attempt returned `403`
- `PATCH /tickets/{id}/status` returned `200` for admin and updated the ticket to `RESOLVED`
- webhook receiver captured both `ticket.created` and `document.processed` notifications

## Notes
- This ledger captures the Phase 5 backend verification state on 2026-03-13. The frontend `/tickets` workspace was implemented later in Phase 6A.
- Webhook delivery is best-effort in Phase 5: failures are logged and acknowledged without retry persistence.
