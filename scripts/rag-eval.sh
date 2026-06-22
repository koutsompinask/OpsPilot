#!/usr/bin/env bash
# rag-eval.sh — evaluate RAG quality against the sample tenants.
#
# Usage:
#   ./scripts/rag-eval.sh [OPTIONS]
#
# Options:
#   --gateway URL     API gateway base URL (default: http://localhost:8080)
#   --pass-rate N     Minimum required pass rate 0-100 (default: 60)
#   --tenant NAME     Run only this tenant (default: all)
#   --help
#
# Requires a running local stack with at least one registered tenant per sample set.
# The script registers a fresh eval tenant, uploads documents, waits for READY,
# runs questions from samples/<tenant>/testing-questions.txt, then reports results.
#
# Exit code: 0 if pass rate >= threshold, 1 otherwise.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATEWAY="${GATEWAY:-http://localhost:8080}"
PASS_THRESHOLD=60
ONLY_TENANT=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --gateway) GATEWAY="$2"; shift 2 ;;
    --pass-rate) PASS_THRESHOLD="$2"; shift 2 ;;
    --tenant) ONLY_TENANT="$2"; shift 2 ;;
    --help)
      head -20 "$0" | grep "^#" | sed 's/^# \?//'
      exit 0 ;;
    *) echo "[error] Unknown option: $1" >&2; exit 1 ;;
  esac
done

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[error] Required command not found: $1" >&2; exit 1
  fi
}
require_cmd curl
require_cmd jq

# ── helpers ────────────────────────────────────────────────────────────────────

register_tenant() {
  local email="$1" password="$2" name="$3"
  curl -fsS -X POST "$GATEWAY/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"$password\",\"tenantName\":\"$name\"}" \
    2>/dev/null | jq -r '.accessToken // empty'
}

upload_doc() {
  local token="$1" file="$2"
  curl -fsS -X POST "$GATEWAY/documents" \
    -H "Authorization: Bearer $token" \
    -F "file=@$file" \
    2>/dev/null | jq -r '.id // empty'
}

wait_doc_ready() {
  local token="$1" doc_id="$2" attempts="${3:-60}"
  local i=1
  while (( i <= attempts )); do
    local status
    status=$(curl -fsS "$GATEWAY/documents/$doc_id" \
      -H "Authorization: Bearer $token" 2>/dev/null | jq -r '.status // empty')
    [[ "$status" == "READY" ]] && return 0
    [[ "$status" == "FAILED" ]] && { echo "[warn] document $doc_id FAILED"; return 1; }
    sleep 2
    (( i++ ))
  done
  echo "[warn] document $doc_id did not reach READY in time" >&2
  return 1
}

ask_question() {
  local token="$1" question="$2"
  curl -fsS -X POST "$GATEWAY/chat/ask" \
    -H "Authorization: Bearer $token" \
    -H "Content-Type: application/json" \
    -d "{\"question\":$(jq -n --arg q "$question" '$q')}" \
    2>/dev/null
}

# ── main evaluation loop ───────────────────────────────────────────────────────

TOTAL_PASS=0
TOTAL_FAIL=0
TOTAL_SKIP=0

eval_tenant() {
  local tenant_dir="$1"
  local tenant_name
  tenant_name=$(basename "$tenant_dir")

  local questions_file="$tenant_dir/testing-questions.txt"
  if [[ ! -f "$questions_file" ]]; then
    echo "[skip] No testing-questions.txt in $tenant_dir"
    return
  fi

  echo
  echo "══════════════════════════════════════════════════════"
  echo "  Tenant: $tenant_name"
  echo "══════════════════════════════════════════════════════"

  # Register a unique eval tenant for this run
  local epoch
  epoch=$(date +%s)
  local email="eval-${tenant_name}-${epoch}@opspilot-eval.local"
  local password="Eval!${epoch}x"
  local display_name="Eval ${tenant_name} ${epoch}"

  echo "[info] Registering eval tenant: $email"
  local token
  token=$(register_tenant "$email" "$password" "$display_name")
  if [[ -z "$token" ]]; then
    echo "[error] Could not register tenant; is the stack running?" >&2
    (( TOTAL_SKIP++ ))
    return
  fi

  # Upload all documents in the tenant sample directory
  local doc_ids=()
  for doc in "$tenant_dir"/*.txt "$tenant_dir"/*.md; do
    [[ -f "$doc" ]] || continue
    # Skip the questions file itself
    [[ "$(basename "$doc")" == "testing-questions.txt" ]] && continue

    echo "[info] Uploading: $(basename "$doc")"
    local doc_id
    doc_id=$(upload_doc "$token" "$doc")
    if [[ -n "$doc_id" ]]; then
      doc_ids+=("$doc_id")
    else
      echo "[warn] Upload failed for $(basename "$doc")"
    fi
  done

  if [[ ${#doc_ids[@]} -eq 0 ]]; then
    echo "[warn] No documents uploaded for $tenant_name; skipping questions"
    (( TOTAL_SKIP++ ))
    return
  fi

  echo "[info] Waiting for ${#doc_ids[@]} documents to reach READY..."
  for doc_id in "${doc_ids[@]}"; do
    wait_doc_ready "$token" "$doc_id" || true
  done

  # Parse and run questions
  local pass=0 fail=0 current_question="" current_expectation=""

  run_question() {
    local q="$1" expect_correct="$2"
    echo
    echo "  Q: $q"
    local response confidence has_sources answer_mode
    response=$(ask_question "$token" "$q" 2>/dev/null || echo "{}")
    confidence=$(echo "$response" | jq -r '.confidence // 0')
    has_sources=$(echo "$response" | jq -r 'if (.evidence // [] | length) > 0 then "yes" else "no" end')
    answer_mode=$(echo "$response" | jq -r '.answerMode // "unknown"')

    local result="FAIL"
    if [[ "$expect_correct" == "yes" ]]; then
      # Pass if confidence >= 0.4 and at least one source cited
      if (( $(echo "$confidence >= 0.40" | bc -l) )) && [[ "$has_sources" == "yes" ]]; then
        result="PASS"
      fi
    else
      # Pass if confidence < 0.55 (low confidence / no answer)
      if (( $(echo "$confidence < 0.55" | bc -l) )); then
        result="PASS"
      fi
    fi

    echo "     confidence=$confidence sources=$has_sources mode=$answer_mode expect_correct=$expect_correct → $result"
    [[ "$result" == "PASS" ]] && (( pass++ )) || (( fail++ ))
  }

  while IFS= read -r line; do
    # Match "Question: ..." lines
    if [[ "$line" =~ ^[[:space:]]*[0-9]+\)[[:space:]]*Question:[[:space:]]*(.*) ]]; then
      current_question="${BASH_REMATCH[1]}"
      current_expectation=""
    elif [[ "$line" =~ Expectation:[[:space:]]*(.*) ]]; then
      current_expectation="${BASH_REMATCH[1]}"
      if [[ -n "$current_question" ]]; then
        local expect_correct="yes"
        if [[ "$current_expectation" == *"should not answer"* || "$current_expectation" == *"low confidence"* ]]; then
          expect_correct="no"
        fi
        run_question "$current_question" "$expect_correct"
        current_question=""
      fi
    fi
  done < "$questions_file"

  local total=$(( pass + fail ))
  local pct=0
  (( total > 0 )) && pct=$(( pass * 100 / total ))
  echo
  echo "  Results for $tenant_name: $pass/$total passed (${pct}%)"
  TOTAL_PASS=$(( TOTAL_PASS + pass ))
  TOTAL_FAIL=$(( TOTAL_FAIL + fail ))
}

# Find tenant directories
if [[ -n "$ONLY_TENANT" ]]; then
  if [[ -d "$ROOT_DIR/samples/$ONLY_TENANT" ]]; then
    eval_tenant "$ROOT_DIR/samples/$ONLY_TENANT"
  else
    echo "[error] Tenant directory not found: $ROOT_DIR/samples/$ONLY_TENANT" >&2
    exit 1
  fi
else
  for dir in "$ROOT_DIR"/samples/*/; do
    [[ -d "$dir" ]] && eval_tenant "$dir"
  done
fi

# ── summary ───────────────────────────────────────────────────────────────────
TOTAL=$(( TOTAL_PASS + TOTAL_FAIL ))
OVERALL_PCT=0
(( TOTAL > 0 )) && OVERALL_PCT=$(( TOTAL_PASS * 100 / TOTAL ))

echo
echo "══════════════════════════════════════════════════════"
echo "  RAG eval summary"
echo "  Total:  $TOTAL questions"
echo "  Passed: $TOTAL_PASS  ($OVERALL_PCT%)"
echo "  Failed: $TOTAL_FAIL"
[[ "$TOTAL_SKIP" -gt 0 ]] && echo "  Skipped tenants: $TOTAL_SKIP"
echo "  Pass threshold: ${PASS_THRESHOLD}%"
echo "══════════════════════════════════════════════════════"

if (( TOTAL == 0 )); then
  echo "[warn] No questions were evaluated"
  exit 1
fi

if (( OVERALL_PCT < PASS_THRESHOLD )); then
  echo "[FAIL] Pass rate ${OVERALL_PCT}% is below threshold ${PASS_THRESHOLD}%"
  exit 1
else
  echo "[PASS] Pass rate ${OVERALL_PCT}% meets threshold ${PASS_THRESHOLD}%"
  exit 0
fi
