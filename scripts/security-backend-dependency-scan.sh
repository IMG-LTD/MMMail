#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

source "$ROOT_DIR/scripts/lib/java-common.sh"

MVN_BIN="$(resolve_maven_bin "$ROOT_DIR")"
REPORT_DIR="${MMMAIL_SECURITY_REPORT_DIR:-$ROOT_DIR/artifacts/security}/dependency-check"
DATA_DIR="${MMMAIL_DEPENDENCY_CHECK_DATA_DIR:-$ROOT_DIR/.tools/dependency-check-data}"
FAIL_ON_CVSS="${MMMAIL_DEPENDENCY_SCAN_FAIL_ON_CVSS:-7}"
mkdir -p "$REPORT_DIR" "$DATA_DIR"

if [[ -z "${MMMAIL_NVD_API_KEY:-}" ]]; then
  echo "Missing MMMAIL_NVD_API_KEY for OWASP dependency-check." >&2
  echo "The NVD bootstrap is too slow for CI without an API key; configure MMMAIL_NVD_API_KEY and re-run." >&2
  exit 1
fi

maven_args=(
  -f backend/pom.xml
  org.owasp:dependency-check-maven:aggregate
  -Dformats=HTML,JSON
  -Dodc.outputDirectory="$REPORT_DIR"
  -DdataDirectory="$DATA_DIR"
  -DfailBuildOnCVSS="$FAIL_ON_CVSS"
)

if [[ -n "${MMMAIL_NVD_API_KEY:-}" ]]; then
  maven_args+=("-DnvdApiKey=${MMMAIL_NVD_API_KEY}")
fi

timeout 900s "$MVN_BIN" "${maven_args[@]}"
