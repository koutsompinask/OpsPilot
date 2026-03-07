# OpsPilot – Implementation Plan (LLM-Friendly)

## Project Overview

**OpsPilot** is a cloud-native AI support assistant platform designed for businesses (e.g., hotels, Airbnb hosts, small companies). It allows administrators to upload knowledge documents and provides an AI-powered assistant that answers user questions using Retrieval-Augmented Generation (RAG). When the AI cannot answer confidently, it creates a support ticket for human staff.

The project is designed to demonstrate modern software engineering practices including:

* Spring Boot microservices
* React frontend
* Retrieval-Augmented Generation (RAG)
* Docker containerization
* Kubernetes orchestration
* CI/CD with Jenkins
* Cloud deployment on AWS
* Observability with Prometheus and Grafana
* Event-driven architecture

The repository should be structured to look like a **production SaaS system**.

---

# 1. Repository Structure

Create a **monorepo** with the following structure:

```
opspilot/
│
├─ frontend/
│
├─ services/
│   ├─ api-gateway/
│   ├─ auth-service/
│   ├─ tenant-service/
│   ├─ knowledge-base-service/
│   ├─ ai-orchestrator-service/
│   ├─ ticket-service/
│   ├─ notification-service/
│   └─ analytics-service/
│
├─ infra/
│   ├─ docker/
│   ├─ k8s/
│   ├─ helm/
│   ├─ jenkins/
│   └─ terraform/
│
├─ docs/
│   ├─ architecture/
│   ├─ api/
│   └─ diagrams/
│
├─ scripts/
│
├─ docker-compose.yml
└─ README.md
```

Guidelines:

* Each service is an independent Spring Boot project.
* Services communicate through REST and messaging.
* Infrastructure configs live under `/infra`.

Backend package convention (implemented services):

* Under `com.opspilot.<service>`, use top-level folders: `config`, `controller`, `service`, `repository`, `domain`, `dto`, `mapper`, `exception`, `security`, `util`.
* Create only relevant folders for real code; avoid placeholder files used only for structure.
* Keep entities under `domain/entity`.
* Keep subcategories nested under their parent layer (examples: `service/storage`, `service/integration`, `util/logging`).
* Do not use legacy ad-hoc top-level folders such as `entity`, `client`, `logging`, `chunking`, `embedding`, `messaging`, `storage`.

---

# 2. Core Architecture

## Frontend

Technology:

* React
* TypeScript
* Vite
* TailwindCSS

Responsibilities:

* Admin dashboard
* Chat interface
* Document upload
* Ticket management
* Analytics visualization

---

## Backend Microservices

### API Gateway

Purpose:

* Single entry point to backend
* Request routing
* Authentication forwarding
* Rate limiting

Technology:

* Spring Cloud Gateway

---

### Auth Service

Purpose:

* User authentication
* JWT token generation
* Role-based access

Endpoints:

```
POST /auth/register
POST /auth/login
POST /auth/refresh
```

Entities:

* User
* Role
* Tenant reference

---

### Tenant Service

Purpose:

* Manage organizations (tenants)
* Manage users within tenant

Endpoints:

```
GET /tenants/me
PUT /tenants/me
GET /users
POST /users
```

Entities:

* Tenant
* UserProfile

---

### Knowledge Base Service

Purpose:

* Handle document ingestion
* Store document metadata
* Create embeddings

Responsibilities:

* File upload
* Text extraction
* Chunking
* Embedding creation
* Vector storage

Endpoints:

```
POST /documents
GET /documents
GET /documents/{id}
DELETE /documents/{id}
```

Storage:

* Document metadata → PostgreSQL
* Raw files → S3/MinIO
* Embeddings → PostgreSQL pgvector

---

### AI Orchestrator Service

Purpose:

* Execute RAG workflow
* Process user questions
* Query vector database
* Call LLM
* Return grounded answers

Workflow:

1. Receive user question
2. Generate embedding
3. Retrieve top-k document chunks
4. Construct prompt
5. Send prompt to LLM
6. Evaluate confidence
7. Return answer and sources
8. Create ticket if confidence low

Endpoint:

```
POST /chat/ask
```

Response Example:

```
{
  "answer": "...",
  "confidence": 0.82,
  "sources": [
    {
      "document": "hotel-policy.pdf",
      "chunkId": "chunk-14"
    }
  ],
  "ticketCreated": false
}
```

---

### Ticket Service

Purpose:

* Manage unresolved AI questions
* Human support workflow

Endpoints:

```
GET /tickets
POST /tickets
PATCH /tickets/{id}/status
```

Entities:

* Ticket
* TicketStatus
* ChatReference

---

### Notification Service

Purpose:

* Send notifications when events occur

Consumes events:

* TicketCreated
* DocumentProcessed

Possible channels:

* email
* webhook
* log output (for MVP)

Uses message queue.

---

### Analytics Service

Purpose:

* Track system usage

Tracks:

* chat queries
* AI confidence
* unresolved questions
* document usage

Endpoints:

```
GET /analytics/dashboard
```

---

# 3. Database Design

Use **PostgreSQL** with **pgvector extension**.

Core tables:

```
tenants
users
documents
document_chunks
chat_sessions
chat_messages
tickets
analytics_events
```

Vector storage:

```
document_chunks
- id
- document_id
- chunk_text
- embedding (vector)
```

Multi-tenancy rule:

All records must include:

```
tenant_id
```

---

# 4. RAG Pipeline

The AI system should implement Retrieval-Augmented Generation.

Steps:

1. Document upload
2. Extract text
3. Split text into chunks
4. Generate embeddings
5. Store embeddings
6. Query embeddings during chat
7. Retrieve top K chunks
8. Inject chunks into prompt
9. Generate answer
10. Return citations

Embedding options:

Preferred:

* OpenAI embeddings (development)

Optional later:

* AWS Bedrock embeddings

---

# 5. Messaging Architecture

Use **RabbitMQ** for asynchronous communication.

Example events:

```
DocumentProcessed
TicketCreated
ChatCompleted
NotificationRequested
```

Services publish and consume events.

Example flow:

User Question → AI Confidence Low → TicketCreated Event → Notification Service → Notify staff

---

# 6. Frontend Features

Pages required:

Login Page

Admin Dashboard

Document Upload Page

Chat Interface

Ticket Management Page

Analytics Page

Tenant Settings Page

---

## Chat UI

Features:

* message history
* streaming answer
* display document sources
* show fallback when AI unsure

---

# 7. Local Development Environment

Use **Docker Compose**.

Services included:

* PostgreSQL
* Redis
* RabbitMQ
* MinIO
* All backend services
* Frontend

Command:

```
docker compose up
```

The entire system should run locally.

---

# 8. Containerization

Each service requires:

Dockerfile

Best practices:

* multi-stage build
* small runtime images
* environment variable configuration

Example:

```
FROM eclipse-temurin:21-jdk
COPY target/app.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

---

# 9. Kubernetes Deployment

Deploy services to Kubernetes.

Resources required:

* Deployments
* Services
* Ingress
* ConfigMaps
* Secrets
* Horizontal Pod Autoscaler

Services deployed:

```
frontend
api-gateway
auth-service
tenant-service
knowledge-base-service
ai-orchestrator-service
ticket-service
notification-service
analytics-service
```

---

# 10. CI/CD Pipeline

Use **Jenkins**.

Pipeline stages:

1. checkout code
2. run backend tests
3. run frontend build
4. build JAR files
5. build Docker images
6. push images to registry
7. deploy to Kubernetes
8. smoke tests

Jenkinsfile must exist in repository.

---

# 11. AWS Deployment

Target AWS services:

```
EKS → Kubernetes cluster
ECR → Docker registry
RDS → PostgreSQL
ElastiCache → Redis
S3 → document storage
CloudWatch → logs
ALB → ingress
```

Goal:

Production-like cloud deployment.

---

# 12. Observability

Use:

* Prometheus
* Grafana
* Spring Boot Actuator

Metrics:

* request latency
* error rates
* chat usage
* AI confidence
* ticket creation rate

Logs should include correlation IDs.

Logging baseline for all phases:

* structured JSON logs for backend services
* request lifecycle logs at API boundaries (method/path/status/duration)
* business outcome logs for major domain actions (success/failure)
* centralized exception logging with clear severity (`WARN` for expected 4xx, `ERROR` for unexpected failures)
* correlation via `X-Request-Id` generated at ingress and propagated across internal service calls
* no logging of secrets (passwords, tokens, raw JWTs, sensitive payload bodies)

---

# 13. Development Phases

## Phase 1

Project setup

* repository structure
* docker compose
* service skeletons

---

## Phase 2

Authentication system

* JWT auth
* tenant creation
* user management

---

## Phase 3

Knowledge ingestion

* upload documents
* extract text
* chunking
* embeddings
* pgvector storage

---

## Phase 4

AI chat

* RAG pipeline
* vector search
* LLM integration
* source citations

---

## Phase 5

Support workflow

* ticket creation
* RabbitMQ events
* notification system

---

## Phase 6

Frontend UI

* login
* document upload
* chat interface
* tickets dashboard
* analytics dashboard

---

## Phase 7

Dockerization

* containerize services
* docker compose orchestration

---

## Phase 8

Kubernetes

* deploy microservices
* configure ingress
* add autoscaling

---

## Phase 9

CI/CD

* Jenkins pipeline
* automated builds
* Kubernetes deployment

---

## Phase 10

Cloud deployment

* AWS infrastructure
* EKS cluster
* RDS database
* S3 storage

---

# 14. MVP Definition

The minimum viable system must support:

1. tenant registration
2. admin login
3. document upload
4. document embedding
5. AI chat answering questions
6. answer citations
7. ticket creation when confidence low

Everything else is an enhancement.

---

# 15. Success Criteria

The system is complete when the following demo works:

1. Admin registers tenant
2. Admin uploads policy document
3. User asks a question in chat
4. AI answers using document context
5. AI provides citation
6. Unsupported question creates ticket
7. Staff sees ticket in dashboard

---

# End of Plan
