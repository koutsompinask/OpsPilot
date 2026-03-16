#!/usr/bin/env bash
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

# Export values from env file so docker compose sees the same settings.
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "[error] Required build artifact not found: $path" >&2
    echo "[error] Build backend jars first with:" >&2
    echo "[error]   ./gradlew :services:api-gateway:bootJar :services:auth-service:bootJar :services:tenant-service:bootJar :services:assistant-service:bootJar :services:ticket-service:bootJar :services:notification-service:bootJar" >&2
    exit 1
  fi
}

require_file "$ROOT_DIR/services/api-gateway/build/libs/api-gateway-0.1.0-SNAPSHOT.jar"
require_file "$ROOT_DIR/services/auth-service/build/libs/auth-service-0.1.0-SNAPSHOT.jar"
require_file "$ROOT_DIR/services/tenant-service/build/libs/tenant-service-0.1.0-SNAPSHOT.jar"
require_file "$ROOT_DIR/services/assistant-service/build/libs/assistant-service-0.1.0-SNAPSHOT.jar"
require_file "$ROOT_DIR/services/ticket-service/build/libs/ticket-service-0.1.0-SNAPSHOT.jar"
require_file "$ROOT_DIR/services/notification-service/build/libs/notification-service-0.1.0-SNAPSHOT.jar"

is_port_in_use() {
  local port="$1"
  if command -v ss >/dev/null 2>&1; then
    ss -ltn "( sport = :$port )" | grep -q ":$port"
  else
    lsof -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
  fi
}

require_port_free() {
  local port="$1"
  local name="$2"
  if is_port_in_use "$port"; then
    echo "[error] Required host port $port for $name is already in use." >&2
    echo "[error] Stop the conflicting process or override ${name^^}_PORT in your env file, then rerun." >&2
    exit 1
  fi
}

require_port_free "${FRONTEND_PORT:-5173}" "frontend"
require_port_free "${API_GATEWAY_PORT:-8080}" "api_gateway"
require_port_free "${AUTH_SERVICE_PORT:-8081}" "auth_service"
require_port_free "${TENANT_SERVICE_PORT:-8082}" "tenant_service"
require_port_free "${ASSISTANT_SERVICE_PORT:-8083}" "assistant_service"
require_port_free "${TICKET_SERVICE_PORT:-8085}" "ticket_service"
require_port_free "${NOTIFICATION_SERVICE_PORT:-8086}" "notification_service"

# Poll a URL until it responds successfully.
wait_http_ok() {
  local url="$1"
  local name="$2"
  local max_attempts="${3:-120}"

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
echo "[info] Using env file: $ENV_FILE"
echo "[info] Starting Docker Compose stack from prebuilt jars..."
docker compose --env-file "$ENV_FILE" up --build -d --remove-orphans

# Wait until services are actually reachable before announcing success.
wait_http_ok "http://localhost:${TENANT_SERVICE_PORT:-8082}/actuator/health" "tenant-service"
wait_http_ok "http://localhost:${AUTH_SERVICE_PORT:-8081}/actuator/health" "auth-service"
wait_http_ok "http://localhost:${ASSISTANT_SERVICE_PORT:-8083}/actuator/health" "assistant-service"
wait_http_ok "http://localhost:${TICKET_SERVICE_PORT:-8085}/actuator/health" "ticket-service"
wait_http_ok "http://localhost:${NOTIFICATION_SERVICE_PORT:-8086}/actuator/health" "notification-service"
wait_http_ok "http://localhost:${API_GATEWAY_PORT:-8080}/actuator/health" "api-gateway"

# Frontend readiness check (Vite index page).
wait_http_ok "http://localhost:${FRONTEND_PORT:-5173}" "frontend" 90

echo
echo "[ready] OpsPilot local stack is up"
echo "  frontend:    http://localhost:${FRONTEND_PORT:-5173}"
echo "  api-gateway: http://localhost:${API_GATEWAY_PORT:-8080}"
echo "  auth:        http://localhost:${AUTH_SERVICE_PORT:-8081}"
echo "  tenant:      http://localhost:${TENANT_SERVICE_PORT:-8082}"
echo "  assistant:   http://localhost:${ASSISTANT_SERVICE_PORT:-8083}"
echo "  tickets:     http://localhost:${TICKET_SERVICE_PORT:-8085}"
echo "  notify:      http://localhost:${NOTIFICATION_SERVICE_PORT:-8086}"
echo
echo "[info] Use 'docker compose --env-file $ENV_FILE logs -f' for logs."
echo "[info] Use 'docker compose --env-file $ENV_FILE down' to stop the stack."

# Keep script alive while background services run.
wait
