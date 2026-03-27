#!/usr/bin/env bash
set -euo pipefail

resolve_maven_bin() {
  local root_dir="$1"
  if [[ -x "$root_dir/.tools/maven/bin/mvn" ]]; then
    printf '%s\n' "$root_dir/.tools/maven/bin/mvn"
    return
  fi
  if command -v mvn >/dev/null 2>&1; then
    command -v mvn
    return
  fi
  echo "maven executable not found" >&2
  exit 1
}
