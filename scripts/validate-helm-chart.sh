#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CHART_DIR="$ROOT_DIR/helm/mmmail"

if ! command -v helm >/dev/null 2>&1; then
  echo "helm is required for DEP-01 validation. Install Helm 3 before running this gate." >&2
  exit 1
fi

rendered="$(mktemp)"
trap 'rm -f "$rendered"' EXIT

helm lint "$CHART_DIR"
helm template mmmail "$CHART_DIR" >"$rendered"

grep -Fq 'app.kubernetes.io/component: backend' "$rendered"
grep -Fq 'app.kubernetes.io/component: frontend-admin' "$rendered"
grep -Fq 'kind: Deployment' "$rendered"

if grep -Fq 'frontend-v2' "$rendered"; then
  echo "helm template must not include frontend-v2" >&2
  exit 1
fi
