#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${1:-.env}"
BACKUP_DIR="${2:-}"

if [[ -z "$BACKUP_DIR" ]]; then
  echo "rollback requires a backup directory created before upgrade" >&2
  exit 1
fi

echo "[db-rollback] automatic down migration is not supported; restoring from backup instead"
"$ROOT_DIR/scripts/db-restore.sh" "$ENV_FILE" "$BACKUP_DIR"
