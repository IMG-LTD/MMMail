#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "[validate-all] delegating to scripts/validate-local.sh"
bash scripts/validate-local.sh
