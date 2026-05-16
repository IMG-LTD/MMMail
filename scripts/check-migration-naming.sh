#!/usr/bin/env bash
# scripts/check-migration-naming.sh
# 校验 Flyway SQL 迁移文件命名与首部注释，并校验 SQL / Java migration 版本号全局唯一。
# spec 出处：docs/v212-migration-spec.md §21.1（v1.3 修订）。
# CI 中由 scripts/release-gate.sh 调起；任意失败退出码 1。

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
MIGRATION_DIR="$ROOT_DIR/backend/mmmail-server/src/main/resources/db/migration"
JAVA_MIGRATION_DIR="$ROOT_DIR/backend/mmmail-server/src/main/java/db/migration"
ALLOWLIST_FILE="$ROOT_DIR/scripts/.migration-naming-allowlist"

declare -A FILE_ALLOWLIST=()
declare -A GAP_ALLOWLIST=()
if [[ -f "$ALLOWLIST_FILE" ]]; then
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ -z "$line" || "${line#\#}" != "$line" ]] && continue
    if [[ "$line" =~ ^GAP[[:space:]]+V([0-9]+)[[:space:]]+V([0-9]+)$ ]]; then
      GAP_ALLOWLIST["${BASH_REMATCH[1]}-${BASH_REMATCH[2]}"]=1
    else
      FILE_ALLOWLIST["$line"]=1
    fi
  done < "$ALLOWLIST_FILE"
fi

if [[ ! -d "$MIGRATION_DIR" ]]; then
  echo "[check-migration-naming] FATAL: 迁移目录不存在: $MIGRATION_DIR" >&2
  exit 1
fi

cd "$MIGRATION_DIR"

errors=0
versions=()
naming_re='^V[0-9]+__[a-z0-9_]+\.sql$'

for file in V*.sql; do
  [[ -f "$file" ]] || continue

  # 1. 命名规约（无豁免）
  if ! [[ "$file" =~ $naming_re ]]; then
    echo "[check-migration-naming] FAIL 命名: $file 不符合 V{N}__{module_purpose}.sql（小写 + 下划线）" >&2
    errors=$((errors + 1))
    continue
  fi

  # 2. 提取版本号
  version="${file#V}"
  version="${version%%__*}"
  if ! [[ "$version" =~ ^[0-9]+$ ]]; then
    echo "[check-migration-naming] FAIL 版本: $file 版本号非纯整数" >&2
    errors=$((errors + 1))
    continue
  fi
  versions+=("$version")

  # 3 + 4 注释检查（允许 allowlist 豁免）
  if [[ -n "${FILE_ALLOWLIST[$file]:-}" ]]; then
    continue
  fi

  # 3. 首行 DESCRIPTION
  first_line=$(head -n 1 "$file")
  if ! [[ "$first_line" == "-- DESCRIPTION:"* ]]; then
    echo "[check-migration-naming] FAIL DESCRIPTION: $file 首行必须以 '-- DESCRIPTION:' 开头" >&2
    errors=$((errors + 1))
  fi

  # 4. 前 20 行内 ROLLBACK
  if ! head -n 20 "$file" | grep -q '^-- ROLLBACK:'; then
    echo "[check-migration-naming] FAIL ROLLBACK: $file 前 20 行内必须出现 '-- ROLLBACK:' 注释" >&2
    errors=$((errors + 1))
  fi
done

# 5. SQL / Java migration 全局版本唯一性与连续性
declare -A GLOBAL_VERSION_LOCATIONS=()
global_versions=()
while IFS= read -r migration_file || [[ -n "$migration_file" ]]; do
  filename="$(basename "$migration_file")"
  if ! [[ "$filename" =~ ^V([0-9]+)__.+\.(sql|java)$ ]]; then
    continue
  fi
  version="${BASH_REMATCH[1]}"
  global_versions+=("$version")
  if [[ -n "${GLOBAL_VERSION_LOCATIONS[$version]:-}" ]]; then
    echo "[check-migration-naming] FAIL 重复版本号: V${version} 同时存在于 ${GLOBAL_VERSION_LOCATIONS[$version]} 和 $migration_file" >&2
    errors=$((errors + 1))
    continue
  fi
  GLOBAL_VERSION_LOCATIONS["$version"]="$migration_file"
done < <(
  find "$MIGRATION_DIR" "$JAVA_MIGRATION_DIR" -maxdepth 1 -type f \( -name 'V*.sql' -o -name 'V*.java' \) -print | sort
)

if [[ ${#global_versions[@]} -ge 2 ]]; then
  IFS=$'\n' sorted_global=($(printf '%s\n' "${global_versions[@]}" | sort -n -u))
  unset IFS
  prev=""
  for v in "${sorted_global[@]}"; do
    if [[ -n "$prev" ]]; then
      gap=$((v - prev))
      if [[ $gap -gt 1 ]]; then
        if [[ -z "${GAP_ALLOWLIST[${prev}-${v}]:-}" ]]; then
          echo "[check-migration-naming] FAIL 跳号: V${prev} 之后直接出现 V${v}（跳了 $((gap - 1)) 号）。如属历史豁免请加到 scripts/.migration-naming-allowlist" >&2
          errors=$((errors + 1))
        fi
      fi
    fi
    prev="$v"
  done
fi

if [[ $errors -gt 0 ]]; then
  echo "[check-migration-naming] 共 $errors 项不合规，详见上方日志" >&2
  exit 1
fi

echo "[check-migration-naming] OK — 校验通过 ${#versions[@]} 个 SQL 迁移文件，${#GLOBAL_VERSION_LOCATIONS[@]} 个 SQL/Java 迁移版本全局唯一且连续"
