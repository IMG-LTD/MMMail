#!/usr/bin/env bash
# scripts/release-gate.sh
# v2.1.3 release-gate 自动化校验（spec §9）。
# 12 步顺序硬阻断；任意一步非零退出即终止。
#
# 用法:
#   bash scripts/release-gate.sh             # 全 12 步
#   bash scripts/release-gate.sh --skip 5,8  # 跳过指定步骤号（仅本地排查用，CI 不允许）
#   bash scripts/release-gate.sh --only 1,2  # 仅执行指定步骤号
#
# 环境变量:
#   MMMAIL_RELEASE_GATE_LOG_DIR  日志目录，默认 /tmp
#   MMMAIL_SKIP_BACKEND          1 = 跳过后端 mvn test（5）
#   MMMAIL_SKIP_E2E              1 = 跳过前端 e2e（8，本地无 docker 时使用）

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

LOG_DIR="${MMMAIL_RELEASE_GATE_LOG_DIR:-/tmp}"
mkdir -p "$LOG_DIR"

SKIP_LIST=""
ONLY_LIST=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip) SKIP_LIST="$2"; shift 2 ;;
    --only) ONLY_LIST="$2"; shift 2 ;;
    -h|--help) grep -E '^# ' "$0" | sed 's/^# //'; exit 0 ;;
    *) echo "[release-gate] 未知参数: $1" >&2; exit 1 ;;
  esac
done

is_skipped() {
  local step="$1"
  if [[ -n "$ONLY_LIST" ]]; then
    [[ ",$ONLY_LIST," != *",$step,"* ]]
    return $?
  fi
  [[ ",$SKIP_LIST," == *",$step,"* ]]
}

start_ts=$(date +%s)
declare -a PASSED=()
declare -a SKIPPED=()

run_step() {
  local step_no="$1" label="$2" log_name="$3"; shift 3
  if is_skipped "$step_no"; then
    echo "[release-gate] step $step_no [$label] SKIPPED"
    SKIPPED+=("$step_no:$label")
    return 0
  fi
  local log_path="$LOG_DIR/release-gate-$log_name.log"
  echo "[release-gate] step $step_no [$label] -> $log_path"
  if ! ( "$@" ) >"$log_path" 2>&1; then
    echo "[release-gate] step $step_no [$label] FAILED. Tail of log:" >&2
    tail -40 "$log_path" >&2 || true
    exit 1
  fi
  PASSED+=("$step_no:$label")
}

step_docker_group() {
  bash "$ROOT_DIR/scripts/check-docker-group.sh"
}

step_typecheck() {
  echo "[typecheck] frontend-v2"
  pnpm --dir frontend-v2 typecheck
  echo "[typecheck] frontend-admin"
  pnpm --dir frontend-admin typecheck
  echo "[typecheck] backend compile"
  if [[ -x "$(command -v mvn)" ]]; then
    mvn -B -ntp -f backend/pom.xml -pl mmmail-server -am -DskipTests compile
  else
    echo "[typecheck] mvn not on PATH; skipping backend compile" >&2
  fi
}

step_lint() {
  echo "[lint] frontend-v2 oxlint + eslint (check-only)"
  pnpm --dir frontend-v2 exec oxlint
  pnpm --dir frontend-v2 exec eslint .
  echo "[lint] frontend-admin oxlint + eslint (check-only)"
  pnpm --dir frontend-admin exec oxlint
  pnpm --dir frontend-admin exec eslint .
}

step_fmt_clean() {
  pnpm --dir frontend-v2 exec oxfmt --check
  pnpm --dir frontend-admin exec oxfmt --check
}

step_backend_tests() {
  if [[ "${MMMAIL_SKIP_BACKEND:-0}" == "1" ]]; then
    echo "[backend-tests] MMMAIL_SKIP_BACKEND=1; skipped"
    return 0
  fi
  bash "$ROOT_DIR/scripts/run-tests-docker.sh" backend
}

step_frontend_v212() {
  pnpm --dir frontend-admin test:v212
}

step_frontend_coverage() {
  pnpm --dir frontend-admin test:coverage
}

step_frontend_e2e() {
  if [[ "${MMMAIL_SKIP_E2E:-0}" == "1" ]]; then
    echo "[frontend-e2e] MMMAIL_SKIP_E2E=1; skipped (skip 占位 spec 仍然需 docker 栈接入)"
    return 0
  fi
  bash "$ROOT_DIR/scripts/run-tests-docker.sh" e2e
}

step_style_discipline() {
  pnpm --dir frontend-admin check:style-discipline
}

step_bundle_budget() {
  pnpm --dir frontend-admin check:bundle-budget
}

step_i18n_keys() {
  pnpm --dir frontend-admin check:i18n
}

step_migration_naming() {
  bash "$ROOT_DIR/scripts/check-migration-naming.sh"
}

run_step  1 "docker-group"        docker-group        step_docker_group
run_step  2 "typecheck"           typecheck           step_typecheck
run_step  3 "lint"                lint                step_lint
run_step  4 "fmt-clean-diff"      fmt-clean           step_fmt_clean
run_step  5 "backend-tests"       backend-tests       step_backend_tests
run_step  6 "frontend-v212"       frontend-v212       step_frontend_v212
run_step  7 "frontend-coverage"   frontend-coverage   step_frontend_coverage
run_step  8 "frontend-e2e"        frontend-e2e        step_frontend_e2e
run_step  9 "style-discipline"    style-discipline    step_style_discipline
run_step 10 "bundle-budget"       bundle-budget       step_bundle_budget
run_step 11 "i18n-keys"           i18n-keys           step_i18n_keys
run_step 12 "migration-naming"    migration-naming    step_migration_naming

clean_diff_log="$LOG_DIR/release-gate-final-clean-diff.log"
echo "[release-gate] final clean diff check -> $clean_diff_log"
if ! git diff --exit-code >"$clean_diff_log" 2>&1; then
  echo "[release-gate] final clean diff check FAILED. Tail of log:" >&2
  tail -40 "$clean_diff_log" >&2 || true
  exit 1
fi

elapsed=$(( $(date +%s) - start_ts ))
echo
echo "[release-gate] all gates green (${#PASSED[@]} passed, ${#SKIPPED[@]} skipped) in ${elapsed}s"
if [[ ${#SKIPPED[@]} -gt 0 ]]; then
  echo "[release-gate] skipped: ${SKIPPED[*]}"
fi
