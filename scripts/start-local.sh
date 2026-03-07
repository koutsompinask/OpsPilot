#!/usr/bin/env bash
# Fail fast:
# -e: exit on command failure
# -u: fail on unset variables
# -o pipefail: fail pipeline if any command fails
set -euo pipefail

# Resolve repository root (script lives in scripts/, so root is one level up)
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# Pick env file:
# 1) explicit first argument
# 2) .env if it exists
# 3) fallback to .env.example
ENV_FILE="${1:-}"
if [[ -z "$ENV_FILE" ]]; then
  if [[ -f "$ROOT_DIR/.env" ]]; then
    ENV_FILE="$ROOT_DIR/.env"
  else
    ENV_FILE="$ROOT_DIR/.env.example"
  fi
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "[error] Env file not found: $ENV_FILE" >&2
  exit 1
fi

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[error] Required command not found: $1" >&2
    exit 1
  fi
}

require_cmd docker
require_cmd curl
require_cmd npm

# Ensure Gradle wrapper is executable because backend services are started via bootRun.
if [[ ! -x "$ROOT_DIR/gradlew" ]]; then
  echo "[error] Gradle wrapper not executable: $ROOT_DIR/gradlew" >&2
  exit 1
fi

# Export values from env file so docker/gradle/frontend commands see the same settings.
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

# Store runtime logs here for troubleshooting.
LOG_DIR="$ROOT_DIR/.logs"
mkdir -p "$LOG_DIR"

# PIDs of processes started by this script (used for shutdown cleanup).
PIDS=()

# Cleanup handler:
# stops app processes started by this script when script exits or is interrupted.
cleanup() {
  echo "[info] Stopping local app processes..."
  for pid in "${PIDS[@]:-}"; do
    if kill -0 "$pid" >/dev/null 2>&1; then
      kill "$pid" >/dev/null 2>&1 || true
    fi
  done
  wait || true
  echo "[info] Local app processes stopped."
}

trap cleanup EXIT INT TERM

# Start a process in background and write logs to .logs/<name>.log
start_bg() {
  local name="$1"
  shift
  echo "[info] Starting $name ..."
  "$@" >"$LOG_DIR/$name.log" 2>&1 &
  local pid=$!
  PIDS+=("$pid")
  echo "[info] $name started (pid=$pid, log=$LOG_DIR/$name.log)"
}

# Poll a URL until it responds successfully.
# Used as a readiness gate before printing the final "ready" message.
wait_http_ok() {
  local url="$1"
  local name="$2"
  local max_attempts="${3:-60}"

  local attempt=1
  while (( attempt <= max_attempts )); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "[ok] $name is ready at $url"
      return 0
    fi
    sleep 1
    ((attempt++))
  done

  echo "[error] $name did not become ready: $url" >&2
  return 1
}

is_port_in_use() {
  local port="$1"
  if command -v ss >/dev/null 2>&1; then
    ss -ltn "( sport = :$port )" | grep -q ":$port"
  else
    lsof -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
  fi
}

# Start service only when not already reachable on target health endpoint.
# If the port is occupied by a non-ready process, fail fast with a clear error.
start_or_reuse_service() {
  local name="$1"
  local port="$2"
  local health_url="$3"
  shift 3

  if curl -fsS "$health_url" >/dev/null 2>&1; then
    echo "[info] Reusing existing $name at $health_url"
    return 0
  fi

  if is_port_in_use "$port"; then
    echo "[error] Port $port is already in use but $name is not healthy at $health_url" >&2
    echo "[error] Stop the conflicting process or free port $port, then rerun." >&2
    return 1
  fi

  start_bg "$name" "$@"
}

echo "[info] Using env file: $ENV_FILE"
echo "[info] Starting required Docker services (infra + current backend stubs)..."

# Stop placeholder containers that would conflict with real app processes
# (same ports as the Spring Boot and frontend dev servers).
docker compose --env-file "$ENV_FILE" stop api-gateway auth-service tenant-service knowledge-base-service ai-orchestrator-service frontend >/dev/null 2>&1 || true

# Start infra + still-stubbed backend services.
# Real services (gateway/auth/tenant/knowledge-base/ai-orchestrator) are started below via Gradle.
docker compose --env-file "$ENV_FILE" up -d \
  postgres redis rabbitmq minio \
  ticket-service notification-service analytics-service

# Shared environment for real backend services launched via Gradle bootRun.
COMMON_ENV=(
  "JWT_SECRET=${JWT_SECRET}"
  "INTERNAL_SERVICE_TOKEN=${INTERNAL_SERVICE_TOKEN}"
  "AUTH_SERVICE_BASE_URL=${AUTH_SERVICE_BASE_URL:-http://localhost:${AUTH_SERVICE_PORT:-8081}}"
  "TENANT_SERVICE_BASE_URL=${TENANT_SERVICE_BASE_URL:-http://localhost:${TENANT_SERVICE_PORT:-8082}}"
  "KNOWLEDGE_BASE_SERVICE_URL=${KNOWLEDGE_BASE_SERVICE_URL:-http://localhost:${KNOWLEDGE_BASE_SERVICE_PORT:-8083}}"
  "AI_ORCHESTRATOR_SERVICE_URL=${AI_ORCHESTRATOR_SERVICE_URL:-http://localhost:${AI_ORCHESTRATOR_SERVICE_PORT:-8084}}"
  "POSTGRES_HOST=${POSTGRES_HOST:-localhost}"
  "POSTGRES_PORT=${POSTGRES_PORT:-5432}"
  "POSTGRES_DB=${POSTGRES_DB:-opspilot}"
  "POSTGRES_USER=${POSTGRES_USER:-opspilot}"
  "POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-opspilot}"
  "RABBITMQ_HOST=${RABBITMQ_HOST:-localhost}"
  "RABBITMQ_PORT=${RABBITMQ_PORT:-5672}"
  "RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-guest}"
  "RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-guest}"
  "S3_ENDPOINT=${S3_ENDPOINT:-http://localhost:${MINIO_PORT:-9000}}"
  "S3_REGION=${S3_REGION:-us-east-1}"
  "S3_ACCESS_KEY=${S3_ACCESS_KEY:-${MINIO_ROOT_USER:-minioadmin}}"
  "S3_SECRET_KEY=${S3_SECRET_KEY:-${MINIO_ROOT_PASSWORD:-minioadmin}}"
  "S3_BUCKET=${S3_BUCKET:-knowledge-documents}"
  "S3_AUTO_CREATE_BUCKET=${S3_AUTO_CREATE_BUCKET:-true}"
  "KNOWLEDGE_EMBEDDING_PROVIDER=${KNOWLEDGE_EMBEDDING_PROVIDER:-local}"
  "OPENAI_API_KEY=${OPENAI_API_KEY:-}"
  "OPENAI_EMBEDDING_MODEL=${OPENAI_EMBEDDING_MODEL:-text-embedding-3-small}"
  "OPENAI_EMBEDDING_URL=${OPENAI_EMBEDDING_URL:-https://api.openai.com/v1/embeddings}"
  "OPENAI_CHAT_MODEL=${OPENAI_CHAT_MODEL:-gpt-4o-mini}"
  "OPENAI_CHAT_URL=${OPENAI_CHAT_URL:-https://api.openai.com/v1/chat/completions}"
  "AI_EMBEDDING_PROVIDER=${AI_EMBEDDING_PROVIDER:-${KNOWLEDGE_EMBEDDING_PROVIDER:-local}}"
  "AI_CHAT_DEFAULT_TOP_K=${AI_CHAT_DEFAULT_TOP_K:-4}"
  "AI_CHAT_LOW_CONFIDENCE_THRESHOLD=${AI_CHAT_LOW_CONFIDENCE_THRESHOLD:-0.55}"
  "KNOWLEDGE_MESSAGING_ENABLED=${KNOWLEDGE_MESSAGING_ENABLED:-true}"
  "DOCUMENT_PROCESSED_EXCHANGE=${DOCUMENT_PROCESSED_EXCHANGE:-opspilot.events}"
  "DOCUMENT_PROCESSED_ROUTING_KEY=${DOCUMENT_PROCESSED_ROUTING_KEY:-document.processed}"
)

# Start real backend services for current implementation phase.
start_or_reuse_service "tenant-service" "${TENANT_SERVICE_PORT:-8082}" \
  "http://localhost:${TENANT_SERVICE_PORT:-8082}/actuator/health" \
  env "${COMMON_ENV[@]}" ./gradlew --project-cache-dir /tmp/opspilot-gradle-cache :services:tenant-service:bootRun
start_or_reuse_service "auth-service" "${AUTH_SERVICE_PORT:-8081}" \
  "http://localhost:${AUTH_SERVICE_PORT:-8081}/actuator/health" \
  env "${COMMON_ENV[@]}" ./gradlew --project-cache-dir /tmp/opspilot-gradle-cache :services:auth-service:bootRun
start_or_reuse_service "knowledge-base-service" "${KNOWLEDGE_BASE_SERVICE_PORT:-8083}" \
  "http://localhost:${KNOWLEDGE_BASE_SERVICE_PORT:-8083}/actuator/health" \
  env "${COMMON_ENV[@]}" ./gradlew --project-cache-dir /tmp/opspilot-gradle-cache :services:knowledge-base-service:bootRun
start_or_reuse_service "ai-orchestrator-service" "${AI_ORCHESTRATOR_SERVICE_PORT:-8084}" \
  "http://localhost:${AI_ORCHESTRATOR_SERVICE_PORT:-8084}/actuator/health" \
  env "${COMMON_ENV[@]}" ./gradlew --project-cache-dir /tmp/opspilot-gradle-cache :services:ai-orchestrator-service:bootRun
start_or_reuse_service "api-gateway" "${API_GATEWAY_PORT:-8080}" \
  "http://localhost:${API_GATEWAY_PORT:-8080}/actuator/health" \
  env "${COMMON_ENV[@]}" ./gradlew --project-cache-dir /tmp/opspilot-gradle-cache :services:api-gateway:bootRun

# Install frontend dependencies automatically when missing (first run convenience).
if [[ ! -d "$ROOT_DIR/frontend/node_modules" ]]; then
  echo "[info] frontend/node_modules missing; running npm install ..."
  (cd "$ROOT_DIR/frontend" && npm install)
fi

# Start Vite dev server bound on all interfaces (useful for local/container networking).
if curl -fsS "http://localhost:${FRONTEND_PORT:-5173}" >/dev/null 2>&1; then
  echo "[info] Reusing existing frontend at http://localhost:${FRONTEND_PORT:-5173}"
elif is_port_in_use "${FRONTEND_PORT:-5173}"; then
  echo "[error] Port ${FRONTEND_PORT:-5173} is already in use but frontend is not responding" >&2
  echo "[error] Stop the conflicting process or free the frontend port, then rerun." >&2
  exit 1
else
  start_bg "frontend" bash -lc "cd '$ROOT_DIR/frontend' && npm run dev -- --host 0.0.0.0 --port ${FRONTEND_PORT:-5173}"
fi

# Wait until services are actually reachable before announcing success.
wait_http_ok "http://localhost:${TENANT_SERVICE_PORT:-8082}/actuator/health" "tenant-service"
wait_http_ok "http://localhost:${AUTH_SERVICE_PORT:-8081}/actuator/health" "auth-service"
wait_http_ok "http://localhost:${KNOWLEDGE_BASE_SERVICE_PORT:-8083}/actuator/health" "knowledge-base-service"
wait_http_ok "http://localhost:${AI_ORCHESTRATOR_SERVICE_PORT:-8084}/actuator/health" "ai-orchestrator-service"
wait_http_ok "http://localhost:${API_GATEWAY_PORT:-8080}/actuator/health" "api-gateway"

# Frontend readiness check (Vite index page).
wait_http_ok "http://localhost:${FRONTEND_PORT:-5173}" "frontend" 90

echo
echo "[ready] OpsPilot local stack is up"
echo "  frontend:    http://localhost:${FRONTEND_PORT:-5173}"
echo "  api-gateway: http://localhost:${API_GATEWAY_PORT:-8080}"
echo "  auth:        http://localhost:${AUTH_SERVICE_PORT:-8081}"
echo "  tenant:      http://localhost:${TENANT_SERVICE_PORT:-8082}"
echo "  knowledge:   http://localhost:${KNOWLEDGE_BASE_SERVICE_PORT:-8083}"
echo "  ai-chat:     http://localhost:${AI_ORCHESTRATOR_SERVICE_PORT:-8084}"
echo
echo "[info] Press Ctrl+C to stop local app processes started by this script."
echo "[info] Docker containers remain running."

# Keep script alive while background services run.
wait
