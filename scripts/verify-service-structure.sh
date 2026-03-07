#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

SERVICES=(
  "api-gateway:com/opspilot/apigateway"
  "auth-service:com/opspilot/auth"
  "tenant-service:com/opspilot/tenant"
  "knowledge-base-service:com/opspilot/knowledgebase"
  "ai-orchestrator-service:com/opspilot/aiorchestrator"
)

ALLOWED_TOP=(config controller service repository domain dto mapper exception security util)
LEGACY_TOP=(entity client logging chunking embedding messaging storage)

errors=0

for entry in "${SERVICES[@]}"; do
  service="${entry%%:*}"
  pkg_path="${entry##*:}"
  base="$ROOT_DIR/services/$service/src/main/java/$pkg_path"

  if [[ ! -d "$base" ]]; then
    echo "[error] missing base package directory: $base"
    errors=$((errors + 1))
    continue
  fi

  # Validate declared top-level packages in Java sources.
  while IFS= read -r top; do
    [[ -z "$top" ]] && continue
    allowed=0
    for candidate in "${ALLOWED_TOP[@]}"; do
      if [[ "$candidate" == "$top" ]]; then
        allowed=1
        break
      fi
    done
    if [[ $allowed -eq 0 ]]; then
      echo "[error] $service uses unsupported top-level package: $top"
      errors=$((errors + 1))
    fi
  done < <(
    find "$base" -type f -name '*.java' \
      | xargs -r sed -n "s#^package ${pkg_path//\//\\.}\\.\\([a-zA-Z0-9_]*\\)\\..*#\\1#p" \
      | sort -u
  )

  # Services with repositories must declare at least one domain.entity package.
  has_repository=0
  if find "$base/repository" -type f -name '*.java' 2>/dev/null | grep -q .; then
    has_repository=1
  fi
  if [[ $has_repository -eq 1 ]]; then
    if ! grep -Rqs "^package ${pkg_path//\//\\.}\\.domain\\.entity;" "$base"; then
      echo "[error] $service defines repositories but no domain.entity package declarations found"
      errors=$((errors + 1))
    fi
  fi

  # Legacy folders should not exist at top level.
  for legacy in "${LEGACY_TOP[@]}"; do
    if [[ -d "$base/$legacy" ]]; then
      echo "[error] $service uses legacy top-level package folder: $legacy"
      errors=$((errors + 1))
    fi
  done

done

if [[ $errors -gt 0 ]]; then
  echo "[fail] service package structure verification failed with $errors issue(s)."
  exit 1
fi

echo "[ok] service package structure verification passed for implemented services."
