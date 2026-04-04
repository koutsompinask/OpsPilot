# OpsPilot Backend — Code Review

> Reviewed against the documented codebase (all 6 active services).  
> Findings are grouped by category and ordered by severity within each group.  
> Severities: **Critical** · **High** · **Medium** · **Low**

---

## Table of Contents
1. [Security](#1-security)
2. [Error Handling](#2-error-handling)
3. [Data Integrity & Consistency](#3-data-integrity--consistency)
4. [Performance](#4-performance)
5. [Reliability & Messaging](#5-reliability--messaging)
6. [Architecture & Coupling](#6-architecture--coupling)
7. [Missing Validation & Edge Cases](#7-missing-validation--edge-cases)
8. [Configuration & Operations](#8-configuration--operations)
9. [Summary Table](#9-summary-table)
10. [Priority Action List](#10-priority-action-list)

---

## 1. Security

### 1.1 JWT Issuer Not Validated on Any Resource Server
**Severity: Critical**  
**Services: api-gateway, assistant-service, auth-service, tenant-service, ticket-service**

Every `NimbusJwtDecoder` in the system is built from a raw symmetric key with no issuer constraint. Spring Security's OAuth2 resource server will validate the signature and expiry, but it will accept a token with *any* `iss` claim — including one forged by a compromised internal service or a test key that leaked.

Affected files:
- `api-gateway/.../config/JwtConfig.java`
- `assistant-service/.../config/JwtConfig.java`
- `auth-service/.../config/JwtConfig.java`
- `tenant-service/.../config/JwtConfig.java`
- `ticket-service/.../config/JwtConfig.java`

**Fix:** Add a `JwtValidators.createDefaultWithIssuer(issuer)` validator to each decoder:

```java
NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("opspilot-auth"));
return decoder;
```

The issuer string must match the value set in `auth-service`'s `JwtService` when tokens are minted.

---

### 1.2 `/internal/**` Endpoints Not Blocked at the Gateway
**Severity: Critical**  
**File: `api-gateway/.../config/SecurityConfig.java`**

The architecture relies on internal endpoints (`/internal/tickets`, `/internal/users`, `/internal/tenants/bootstrap`) never being reachable from the public internet — they are protected only by the `INTERNAL_SERVICE_TOKEN` header. However, the gateway's security config has **no rule that denies or blocks `/internal/**` paths**. If a route exists (or is accidentally added) for an internal path, it will be forwarded to the downstream service with no JWT requirement.

**Fix:** Add an explicit deny as the first rule in the filter chain:

```java
.pathMatchers("/internal/**").denyAll()
```

This is a zero-cost, zero-risk change that closes the gap permanently regardless of future routing changes.

---

### 1.3 Service Token Comparison Vulnerable to Timing Attacks
**Severity: Medium**  
**Files: `ticket-service/.../InternalTicketController.java`, `auth-service/.../InternalAuthController.java`, `tenant-service/.../InternalTenantController.java`**

All three internal controllers validate the `INTERNAL_SERVICE_TOKEN` header using plain `.equals()`. This leaks timing information that, over many requests, can allow an attacker to infer the correct token character-by-character.

**Fix:** Use constant-time comparison:

```java
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

boolean valid = MessageDigest.isEqual(
    serviceToken.getBytes(StandardCharsets.UTF_8),
    (provided == null ? "" : provided).getBytes(StandardCharsets.UTF_8)
);
if (!valid) throw new UnauthorizedException("Invalid service token");
```

---

### 1.4 Tenant Isolation
**Severity: None (good)**

All repository methods are correctly scoped by `tenantId` (`findByIdAndTenantId`, `findByTenantIdOrderBy...`, etc.). Controllers pass `currentUser.tenantId()` to every service call. No cross-tenant data leak risk was found.

---

### 1.5 Password Hashing
**Severity: None (good)**

`PasswordHashService` uses BCrypt with an additional application-managed 24-byte `SecureRandom` salt stored separately. Passwords and hashes are never logged. No issues.

---

### 1.6 Input Validation
**Severity: None (good)**

All public-facing request DTOs use Bean Validation (`@NotBlank`, `@Email`, `@Size`), and all controller methods use `@Valid`. Internal endpoint DTOs are also validated. No gaps found.

---

## 2. Error Handling

### 2.1 GlobalExceptionHandler Coverage
**Severity: None (good)**

All six services have a `@RestControllerAdvice` that catches custom exceptions, Bean Validation failures, and a final `Exception` fallback that returns a generic message without leaking stack traces. Coverage is consistent and complete.

---

### 2.2 Inter-Service RestTemplate/RestClient Error Handling
**Severity: None (good)**

All REST clients (`TicketClient`, `AuthClient`, `TenantClient`) use `.onStatus(HttpStatusCode::isError, ...)` to convert non-2xx responses into typed exceptions before they propagate. No silent swallowing of upstream errors.

---

### 2.3 Async Exception Propagation in Ingestion
**Severity: Low**

`DocumentIngestionProcessor` catches exceptions and marks documents `FAILED`, which is correct. However, the event-publishing step after a successful ingestion is caught in a separate try-catch that swallows the exception and lets the document remain `READY`. If `eventPublisher.publish()` fails, the document is ready but no downstream notification is sent. This is intentional (don't fail the document because the event failed), but it means notification-service and any webhooks are silently skipped with no retry path.

**File:** `assistant-service/.../service/DocumentIngestionProcessor.java`

**Recommendation:** At minimum, emit a `WARN` log with enough context to manually trigger re-notification, or add the event to a retry queue.

---

## 3. Data Integrity & Consistency

### 3.1 Documents Can Get Stuck in `PROCESSING` State
**Severity: Medium**  
**File: `assistant-service/.../service/DocumentIngestionProcessor.java`**

The failure catch block correctly marks documents `FAILED`. However, if a JVM crash or an OOM error occurs *after* the document is saved as `PROCESSING` but *before* the catch block can run, the document stays `PROCESSING` indefinitely with no way to recover.

There is also no timeout or watchdog — a document stuck in `PROCESSING` is invisible to the system.

**Recommendation:**
- Add a scheduled cleanup job that moves documents stuck in `PROCESSING` for more than N minutes to `FAILED`.
- Or expose a manual admin endpoint to reset document state.

---

### 3.2 No Retry Endpoint for Failed Documents
**Severity: Medium**  
**File: `assistant-service/.../controller/DocumentController.java`**

Once a document is in `FAILED` state (e.g., because MinIO was down), there is no way to retry ingestion without deleting and re-uploading the file. The file is still in object storage, but the document must be deleted and re-uploaded.

**Recommendation:** Add a `POST /documents/{id}/retry` endpoint (admin-only) that re-queues ingestion for documents in `FAILED` state.

---

### 3.3 Transactional Boundaries
**Severity: None (good)**

Multi-step operations in `AuthService`, `TenantService`, and `TicketService` are correctly marked `@Transactional`. Document deletion in `DocumentService` deletes chunks, then storage, then the record — in the right dependency order. No orphan-data risks found.

---

### 3.4 Database Constraints
**Severity: None (good)**

Email uniqueness, FK constraints, and composite indexes are all present in the Flyway migrations. The pgvector extension is properly used for embedding storage. Schema design is solid.

---

## 4. Performance

### 4.1 No Pagination on List Endpoints
**Severity: Medium**  
**Files: `assistant-service/.../DocumentController.java`, `ticket-service/.../TicketController.java`, `tenant-service/.../UserController.java`**

All three list endpoints return the entire result set with no `Pageable` support. A tenant with thousands of documents or tickets will fetch every row into memory on every list call.

**Recommendation:** Implement Spring Data pagination:

```java
// Repository
Page<Document> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

// Controller
@GetMapping
ResponseEntity<Page<DocumentResponse>> list(
    @AuthenticationPrincipal ... currentUser,
    @PageableDefault(size = 20, sort = "createdAt", direction = DESC) Pageable pageable
)
```

---

### 4.2 N+1 Queries
**Severity: None (good)**

All repositories use method-derived queries that fetch in a single SQL call. No `@OneToMany` or `@ManyToMany` lazy-loaded relationships that could trigger N+1 patterns were found. Chunk bulk-insert and bulk-delete operations use `@Query` with `IN` clauses.

---

### 4.3 Unindexed Queries
**Severity: None (good)**

Tenant-scoped queries on `documents`, `tickets`, `user_profiles`, and `auth_users` all use indexed columns. The pgvector similarity search on `document_chunks` is handled by the extension's own index type. No obvious full-table scan risks.

---

## 5. Reliability & Messaging

### 5.1 No Dead-Letter Queue Configuration
**Severity: Medium**  
**Files: `ticket-service/.../messaging/MessagingConfig.java`, `assistant-service/.../messaging/MessagingConfig.java`, `notification-service/.../config/NotificationMessagingConfig.java`**

RabbitMQ queues are declared as durable but with no dead-letter exchange (DLX) bindings. If a consumer throws an exception while processing a message (e.g., notification-service is down, or a transient DB error hits ticket-service during message consumption), the message is either re-queued indefinitely or dropped depending on the broker config — there is no DLQ to catch poison messages.

**Recommendation:** Add DLX/DLQ bindings to all queue declarations:

```java
Queue queue = QueueBuilder.durable("notification.ticket.created")
    .withArgument("x-dead-letter-exchange", "opspilot.dlx")
    .withArgument("x-dead-letter-routing-key", "notification.ticket.created.dead")
    .build();
```

---

### 5.2 Webhook Delivery Has No Retry
**Severity: Medium**  
**File: `notification-service/.../service/WebhookDeliveryService.java`**

Webhook delivery is deliberately best-effort (documented as such), but with zero retry on transient failures (5xx from the webhook endpoint, connect timeout, etc.), any temporary outage on the receiving side silently drops the notification. For a support platform, missed ticket-created notifications could cause SLA breaches.

**Recommendation:** Add a simple retry with Spring Retry or a manual loop (max 3 attempts, exponential backoff). Messages that exhaust retries should be sent to a DLQ for manual inspection, not dropped silently.

---

### 5.3 Event Publisher Failures are Fire-and-Forget
**Severity: Low**  
**Files: `ticket-service/.../TicketCreatedEventPublisher.java`, `assistant-service/.../DocumentProcessedEventPublisher.java`**

`RabbitTemplate.convertAndSend()` can fail silently if the broker is temporarily unreachable and publisher confirms are not enabled. The current code catches and logs errors, which is good, but the event is simply lost.

**Recommendation:** Enable publisher confirms on the `RabbitTemplate` and implement a transactional outbox pattern for critical events, or at minimum alert (not just log) on publish failures.

---

### 5.4 Message Idempotency
**Severity: Low**

Event payloads carry deterministic entity IDs (`ticketId`, `documentId`) which could be used as idempotency keys on the consumer side, but the consumers (`EventNotificationListener`) do not deduplicate. If RabbitMQ re-delivers a message (e.g., after a consumer crash), the webhook will be called twice. For the current best-effort design this is acceptable, but worth noting if reliability requirements increase.

---

## 6. Architecture & Coupling

### 6.1 Auth-Service ↔ Tenant-Service Circular Dependency
**Severity: Low**

At registration: `auth-service` calls `tenant-service` (bootstrap).  
At user creation: `tenant-service` calls `auth-service` (create auth account).  

These two calls never happen in the same transaction, so there is no deadlock or circular call risk at runtime. However, the coupling means neither service can start in isolation without the other being healthy, which complicates independent deployments and testing.

**Recommendation:** Consider extracting the user provisioning flow into a choreography pattern via RabbitMQ events, removing the synchronous bi-directional dependency.

---

### 6.2 Business Logic in Controllers
**Severity: None (good)**

Controllers are thin — they validate input, extract the current user, delegate to a service, and map the result to a response. No business logic leaks into the controller layer.

---

### 6.3 DTO vs Entity Separation
**Severity: None (good)**

Entities are never serialised directly to clients. Mappers or inline constructor calls convert between the domain layer and DTOs at service/controller boundaries. Clean separation.

---

## 7. Missing Validation & Edge Cases

### 7.1 No Graceful Degradation for LLM Failures
**Severity: Medium**  
**File: `assistant-service/.../service/ChatService.java`**

If the configured answer generator (OpenAI, Ollama) is unreachable or returns an error, the exception propagates unhandled through `ChatService` → `ChatController` → `GlobalExceptionHandler`, returning a generic 500 to the user. There is no user-friendly message explaining that the assistant is temporarily unavailable.

**Recommendation:** Catch `Exception` from `answerService.generate()` specifically and return a structured error response (400 or 503) with a message like `"The assistant is temporarily unavailable. Please try again shortly."` rather than a generic server error.

---

### 7.2 No Graceful Degradation for Embedding Service Failures During Chat
**Severity: Medium**  
**File: `assistant-service/.../service/ChatService.java`**

If `embeddingService.provider().embed()` fails during a chat query (as opposed to document ingestion), the exception propagates as a 500. Same issue as 7.1 — no user-facing message explains the failure.

**Recommendation:** Same approach as 7.1. Wrap the retrieval step in a try-catch and return a 503 with an informative message.

---

### 7.3 Empty/Malformed Document Upload
**Severity: None (good)**

`DocumentService` validates that the multipart file is non-empty and has a non-blank filename before saving. If the document has content but produces no parseable text, the ingestion processor marks it `FAILED` with a descriptive error. Edge case is handled.

---

### 7.4 MinIO Unavailability During Document Upload
**Severity: Low**

If MinIO is down when a user uploads a document, `DocumentStorageService.store()` will throw a `StorageException`, which is caught by the `GlobalExceptionHandler` and returned as a 500. The document record is **not** created (upload fails cleanly), so no orphan record is left. Behaviour is correct but the 500 response could be a more specific 503.

---

## 8. Configuration & Operations

### 8.1 Weak Default Secrets in `application.yml`
**Severity: Medium**  
**Files: all `application.yml` files**

Default values like `opspilot-internal-dev-token` and `opspilot-super-secret-jwt-key-for-dev` are present in application config. These are appropriate for local development but create risk if an environment is accidentally deployed without overriding them.

**Recommendation:**
- Remove defaults from `application.yml` entirely (let startup fail fast if the env var is missing).
- Or add a startup validator bean that refuses to start in non-local profiles if secrets match the known dev values.

---

### 8.2 No Actuator Endpoint Security on Downstream Services
**Severity: Low**  
**Files: all `application.yml` files (downstream services)**

Actuator is enabled on all services. The gateway exposes `/actuator/health` publicly, which is intentional. However, the downstream services expose actuator on their own ports (8081-8086) with no authentication, relying entirely on network-level access control (not running those ports in the public network). This is fine for local/container deployments, but should be documented as a hard network requirement.

**Recommendation:** Document in the `README` or deployment guide that downstream service ports must not be publicly routable. Alternatively, restrict actuator exposure to `management.endpoints.web.exposure.include=health` only.

---

### 8.3 No Document Retry Mechanism (Duplicate of 3.2)
*See [3.2](#32-no-retry-endpoint-for-failed-documents) above.*

---

## 9. Summary Table

| # | Area | Severity | Finding |
|---|------|----------|---------|
| 1.1 | Security | **Critical** | JWT issuer not validated — all resource servers accept tokens from any issuer |
| 1.2 | Security | **Critical** | API Gateway has no rule blocking `/internal/**` — internal endpoints may be reachable externally |
| 1.3 | Security | **Medium** | Service token compared with `.equals()` — vulnerable to timing attacks |
| 2.3 | Error Handling | **Low** | Failed event publish after ingestion is silently dropped with no retry path |
| 3.1 | Data Integrity | **Medium** | Documents can get stuck in `PROCESSING` forever on JVM crash; no watchdog or timeout |
| 3.2 | Data Integrity | **Medium** | No retry endpoint for `FAILED` documents — users must delete and re-upload |
| 4.1 | Performance | **Medium** | No pagination on `/documents`, `/tickets`, `/users` list endpoints |
| 5.1 | Reliability | **Medium** | No dead-letter queue on any RabbitMQ queue — failed/poisoned messages are dropped |
| 5.2 | Reliability | **Medium** | Webhook delivery has no retry — transient outages silently drop notifications |
| 5.3 | Reliability | **Low** | Event publisher failures are fire-and-forget — events can be silently lost |
| 5.4 | Reliability | **Low** | No consumer-side deduplication — duplicate event delivery possible on RabbitMQ re-delivery |
| 6.1 | Architecture | **Low** | Auth ↔ Tenant synchronous circular dependency complicates independent deployment |
| 7.1 | Edge Cases | **Medium** | No graceful degradation when LLM is unreachable — returns generic 500 |
| 7.2 | Edge Cases | **Medium** | No graceful degradation when embedding service is unreachable during chat |
| 8.1 | Config/Ops | **Medium** | Weak default secrets in `application.yml` — risk of dev secrets in production |
| 8.2 | Config/Ops | **Low** | Downstream actuator ports unprotected — relies entirely on network isolation |

---

## 10. Priority Action List

Ordered by risk × effort:

1. **[Critical]** Add JWT issuer validation to all five `JwtConfig` beans (< 1 hour, zero risk)
2. **[Critical]** Add `.pathMatchers("/internal/**").denyAll()` to the gateway `SecurityConfig` (< 15 min, zero risk)
3. **[Medium]** Replace `.equals()` service token checks with `MessageDigest.isEqual()` in all three internal controllers
4. **[Medium]** Add pagination (`Pageable`) to document, ticket, and user list endpoints
5. **[Medium]** Add a scheduled job (or admin endpoint) to recover documents stuck in `PROCESSING`
6. **[Medium]** Add retry logic to webhook delivery in `notification-service` (Spring Retry, 3 attempts, exponential backoff)
7. **[Medium]** Configure dead-letter exchanges/queues for all RabbitMQ queues
8. **[Medium]** Wrap LLM and embedding calls in `ChatService` to return a 503 with a user-facing message on failure
9. **[Medium]** Remove default secret values from `application.yml` (fail-fast on missing env vars)
10. **[Low]** Add a `POST /documents/{id}/retry` admin endpoint for `FAILED` documents
11. **[Low]** Consider moving the auth ↔ tenant provisioning to an event-driven flow to break synchronous coupling
