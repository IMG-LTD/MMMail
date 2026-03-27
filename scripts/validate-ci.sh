#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

export MMMAIL_VALIDATE_CONTAINER_TESTS="${MMMAIL_VALIDATE_CONTAINER_TESTS:-true}"
export MMMAIL_RUN_BACKEND_DEPENDENCY_SCAN="${MMMAIL_RUN_BACKEND_DEPENDENCY_SCAN:-true}"
source "$ROOT_DIR/scripts/lib/db-common.sh"

if [[ "$MMMAIL_RUN_BACKEND_DEPENDENCY_SCAN" == "true" ]] && [[ -z "${MMMAIL_NVD_API_KEY:-}" ]]; then
  echo "validate-ci requires MMMAIL_NVD_API_KEY when MMMAIL_RUN_BACKEND_DEPENDENCY_SCAN=true." >&2
  echo "Set the repository or organization secret MMMAIL_NVD_API_KEY before re-running the validate job." >&2
  exit 1
fi

if [[ "$MMMAIL_VALIDATE_CONTAINER_TESTS" != "true" ]]; then
  echo "CI validation requires MMMAIL_VALIDATE_CONTAINER_TESTS=true" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1 || ! docker_client_available; then
  echo "CI validation requires an available docker daemon for container migration gates" >&2
  exit 1
fi

bash scripts/validate-local.sh
bash scripts/validate-rc1-container.sh
