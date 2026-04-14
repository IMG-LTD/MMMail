#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

source "$ROOT_DIR/scripts/lib/db-common.sh"
source "$ROOT_DIR/scripts/lib/java-common.sh"

MVN_BIN="$(resolve_maven_bin "$ROOT_DIR")"
BACKEND_AUTH_RBAC_TESTS="AuthFlowIntegrationTest,OrgAuthenticationSecurityIntegrationTest,OrgAdminConsoleIntegrationTest,OrgMemberGovernanceIntegrationTest"
BACKEND_DOCS_TESTS="DocsCollaborationIntegrationTest,DocsSuggestionWorkflowIntegrationTest,DocsOrgAccessIntegrationTest"
FRONTEND_DOCS_TESTS="tests/docs-smoke.spec.ts tests/docs-panels.smoke.spec.ts tests/docs-comments.smoke.spec.ts tests/docs-presentation.spec.ts tests/docs-transfer.spec.ts tests/docs-draft.spec.ts tests/docs-route.spec.ts tests/docs-leave-guard.spec.ts"
BACKEND_MAIL_GA_TESTS="MailGaIntegrationTest,MailAttachmentIntegrationTest,MailReleaseBlockingIntegrationTest"
FRONTEND_MAIL_GA_TESTS="tests/mail-compose.spec.ts tests/mail-attachments.spec.ts tests/mail-smoke.spec.ts"
BACKEND_CALENDAR_GA_TESTS="CalendarSharingAvailabilityIntegrationTest,CalendarReleaseBlockingIntegrationTest,CalendarIcsImportIntegrationTest"
FRONTEND_CALENDAR_GA_TESTS="tests/calendar-availability.spec.ts tests/calendar-workspace.spec.ts tests/calendar-smoke.spec.ts"
BACKEND_DRIVE_GA_TESTS="DriveReleaseBlockingIntegrationTest,DriveCollaboratorShareIntegrationTest,DriveSharedWithMeIntegrationTest,DriveSecureShareIntegrationTest,DrivePublicFolderShareIntegrationTest"
FRONTEND_DRIVE_GA_TESTS="tests/drive-smoke.spec.ts tests/drive-batch-share.spec.ts tests/drive-collaborator-sharing.spec.ts"
BACKEND_OBSERVABILITY_TESTS="ObservabilityIntegrationTest,JobRunMonitorServiceTest,GlobalExceptionHandlerUnitTest"
FRONTEND_OBSERVABILITY_TESTS="tests/system-health.spec.ts tests/error-tracking.spec.ts"
FRONTEND_COMMUNITY_BOUNDARY_TESTS="tests/community-navigation.spec.ts tests/community-boundary.spec.ts"
FRONTEND_PWA_TESTS="tests/pwa-install.spec.ts tests/pwa-settings-panel.spec.ts"
BACKEND_SHEETS_TESTS="SheetsWorkbookIntegrationTest,SheetsWorkbookDataManagementIntegrationTest,SheetsSharingVersionIntegrationTest,SheetsWorkbookMultiSheetIntegrationTest"
FRONTEND_SHEETS_TESTS="tests/sheets-business.spec.ts tests/sheets-sharing-version.spec.ts tests/sheets-refresh-regression.spec.ts tests/sheets-sidebar.spec.ts tests/sheets-workspace-route.spec.ts tests/sheets-workspace.spec.ts tests/sheets-mutation-state.spec.ts tests/sheets-collaboration-state.spec.ts tests/sheets-visible-workbooks-state.spec.ts tests/sheets-panels.smoke.spec.ts tests/sheets-trade-collaboration.smoke.spec.ts tests/sheets-structure.smoke.spec.ts tests/sheets-tools-formula.smoke.spec.ts tests/sheets-grid.smoke.spec.ts tests/sheets-state-boundary.smoke.spec.ts tests/sheets-toolbar-empty.smoke.spec.ts tests/sheets-sharing-boundary.smoke.spec.ts tests/sheets-trade-boundary.smoke.spec.ts tests/sheets-panel-safety.smoke.spec.ts tests/sheets-incoming-boundary.smoke.spec.ts tests/sheets-insight-boundary.smoke.spec.ts"

echo "[validate-local] frontend tests"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend test >/tmp/mmmail-frontend-test.log 2>&1

echo "[validate-local] frontend docs regression"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend exec vitest run $FRONTEND_DOCS_TESTS >/tmp/mmmail-frontend-docs.log 2>&1

echo "[validate-local] frontend mail ga regression"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend exec vitest run $FRONTEND_MAIL_GA_TESTS >/tmp/mmmail-frontend-mail-ga.log 2>&1

echo "[validate-local] frontend calendar ga regression"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend exec vitest run $FRONTEND_CALENDAR_GA_TESTS >/tmp/mmmail-frontend-calendar-ga.log 2>&1

echo "[validate-local] frontend drive ga regression"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend exec vitest run $FRONTEND_DRIVE_GA_TESTS >/tmp/mmmail-frontend-drive-ga.log 2>&1

echo "[validate-local] frontend observability regression"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend exec vitest run $FRONTEND_OBSERVABILITY_TESTS >/tmp/mmmail-frontend-observability.log 2>&1

echo "[validate-local] frontend community boundary regression"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend exec vitest run $FRONTEND_COMMUNITY_BOUNDARY_TESTS >/tmp/mmmail-frontend-community-boundary.log 2>&1

echo "[validate-local] frontend pwa regression"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend exec vitest run $FRONTEND_PWA_TESTS >/tmp/mmmail-frontend-pwa.log 2>&1

echo "[validate-local] frontend sheets regression"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend exec vitest run $FRONTEND_SHEETS_TESTS >/tmp/mmmail-frontend-sheets.log 2>&1

echo "[validate-local] frontend i18n governance"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend i18n:test >/tmp/mmmail-frontend-i18n.log 2>&1

echo "[validate-local] i18n consistency report"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend i18n:catalog >/tmp/mmmail-i18n-report.log 2>&1

echo "[validate-local] i18n page coverage report"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend i18n:coverage >/tmp/mmmail-i18n-coverage.log 2>&1

echo "[validate-local] security gates"
bash scripts/validate-security.sh >/tmp/mmmail-security.log 2>&1

echo "[validate-local] frontend typecheck"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend typecheck >/tmp/mmmail-frontend-typecheck.log 2>&1

echo "[validate-local] frontend dependency audit (high)"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend audit --prod --audit-level=high >/tmp/mmmail-frontend-audit.log 2>&1

echo "[validate-local] required files"
required=(
  docs/requirements.md
  docs/prd.md
  docs/ui-design.md
  docs/review-minutes.md
  docs/frontend-architecture.md
  docs/backend-architecture.md
  docs/frontend-test-report.md
  docs/backend-test-report.md
  docs/integration-report.md
  docs/security-audit.md
  docs/uat-report.md
  docs/deployment-runbook.md
  docs/ops/install.md
  docs/ops/upgrade.md
  docs/ops/backup-restore.md
  docs/ops/runbook.md
  docs/architecture/deployment-topology.md
  docs/architecture/database-migration-strategy.md
  docs/security/threat-model.md
  docs/final-summary.md
  docs/release/community-v1-gate.md
  docs/release/community-v1-roadmap.md
  docs/release/community-v1-rc-checklist.md
  docs/release/community-v1-rc1-notes.md
  docs/release/community-v1-known-issues.md
  docs/release/community-v1-support-boundaries.md
  docs/release/community-v1-pre-release-checklist.md
  docs/open-source/i18n-governance.md
  docs/release/external-ci-handoff.md
  docs/release/external-execution-checklist.md
  docs/release/community-v1-release-manager-brief.md
  docs/release/external-failure-triage.md
  docs/release/freeze-exception-template.md
  docs/release/post-external-receipt-checklist.md
  docs/release/gate-backfill-template.md
  docs/release/community-v1-external-receipt-log.md
  docs/release/community-v1-rc-status.md
  docs/release/community-v1-final-signoff.md
  docs/release/release-notes-template.md
  docs/release/community-v1-scope.md
  docs/open-source/module-maturity-matrix.md
  README.md
  LICENSE
  CONTRIBUTING.md
  docker-compose.yml
  docker-compose.minimal.yml
  backend/Dockerfile
  frontend/Dockerfile
  backend/pom.xml
  frontend/pages/inbox.vue
  frontend/pages/labs.vue
  .env.example
  config/backend.env.example
  SECURITY.md
  scripts/security-secret-scan.sh
  scripts/security-backend-dependency-scan.sh
  scripts/validate-security.sh
  scripts/validate-runtime-env.sh
  scripts/validate-backend-test-env.sh
  scripts/validate-batch3.sh
  scripts/validate-local.sh
  scripts/validate-ci.sh
  scripts/validate-rc1-local.sh
  scripts/validate-rc1-container.sh
  scripts/db-upgrade.sh
  scripts/db-backup.sh
  scripts/db-restore.sh
  scripts/db-rollback.sh
  config/backend.test.env.example
)
for file in "${required[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "missing required file: $file" >&2
    exit 1
  fi
done

echo "[validate-local] sanitized secrets in config templates"
placeholder_checks=(
  ".env.example|MMMAIL_NACOS_ENABLED=false"
  ".env.example|NUXT_PUBLIC_ENABLE_PREVIEW_MODULES=false"
  ".env.example|MMMAIL_JWT_SECRET=replace-with-32-plus-char-random-secret"
  "config/backend.env.example|MMMAIL_NACOS_ENABLED=true"
  "config/backend.env.example|SPRING_DATASOURCE_PASSWORD=replace-with-db-password"
  "config/backend.env.example|SPRING_REDIS_PASSWORD=replace-with-redis-password"
  "config/backend.env.example|NACOS_PASSWORD=replace-with-nacos-password"
  "config/backend.env.example|MMMAIL_JWT_SECRET=replace-with-32-plus-char-random-secret"
  "backend/mmmail-server/src/main/resources/application-local.yml|      enabled: \${MMMAIL_NACOS_ENABLED:true}"
  "backend/mmmail-server/src/main/resources/application-local.yml|    password: \${SPRING_DATASOURCE_PASSWORD:replace-with-db-password}"
  "backend/mmmail-server/src/main/resources/application-local.yml|      password: \${SPRING_REDIS_PASSWORD:replace-with-redis-password}"
  "backend/mmmail-server/src/main/resources/application-local.yml|    bootstrap-servers: \${SPRING_KAFKA_BOOTSTRAP_SERVERS:127.0.0.1:9092}"
  "backend/mmmail-server/src/main/resources/application-local.yml|      username: \${NACOS_USERNAME:replace-with-nacos-user}"
  "backend/mmmail-server/src/main/resources/application-local.yml|      password: \${NACOS_PASSWORD:replace-with-nacos-password}"
  "backend/mmmail-server/src/main/resources/application-local.yml|  jwt-secret: \${MMMAIL_JWT_SECRET:replace-with-32-plus-char-random-secret}"
)
for entry in "${placeholder_checks[@]}"; do
  file="${entry%%|*}"
  expected_line="${entry#*|}"
  if ! grep -Fqx -- "$expected_line" "$file"; then
    echo "missing sanitized placeholder in $file: $expected_line" >&2
    exit 1
  fi
done

echo "[validate-local] env templates include required keys"
required_env_keys=(
  MMMAIL_NACOS_ENABLED
  MMMAIL_JWT_SECRET
  MMMAIL_CORS_ALLOWED_ORIGINS
  SPRING_DATASOURCE_URL
  SPRING_DATASOURCE_USERNAME
  SPRING_DATASOURCE_PASSWORD
  MYSQL_ROOT_PASSWORD
  NACOS_USERNAME
  NACOS_PASSWORD
)
for key in "${required_env_keys[@]}"; do
  if ! grep -Eq "^${key}=" .env.example config/backend.env.example; then
    echo "missing required env key in templates: $key" >&2
    exit 1
  fi
done

echo "[validate-local] runtime env template"
tmp_env_standard="$(mktemp)"
tmp_env_minimal="$(mktemp)"
trap 'rm -f "$tmp_env_standard" "$tmp_env_minimal"' EXIT
cp .env.example "$tmp_env_standard"
sed -i 's/replace-with-32-plus-char-random-secret/0123456789abcdef0123456789abcdef/' "$tmp_env_standard"
sed -i 's/replace-with-db-password/DbPassword123!/' "$tmp_env_standard"
sed -i 's/replace-with-mysql-root-password/MySqlRoot123!/' "$tmp_env_standard"
sed -i 's/replace-with-redis-password/RedisPassword123!/' "$tmp_env_standard"
sed -i 's/MMMAIL_NACOS_ENABLED=false/MMMAIL_NACOS_ENABLED=true/' "$tmp_env_standard"
sed -i 's/replace-with-nacos-user/nacos/' "$tmp_env_standard"
sed -i 's/replace-with-nacos-password/nacos/' "$tmp_env_standard"
bash scripts/validate-runtime-env.sh "$tmp_env_standard" >/tmp/mmmail-runtime-env.log 2>&1

echo "[validate-local] runtime env template (minimal mode)"
cp .env.example "$tmp_env_minimal"
sed -i 's/replace-with-32-plus-char-random-secret/0123456789abcdef0123456789abcdef/' "$tmp_env_minimal"
sed -i 's/replace-with-db-password/DbPassword123!/' "$tmp_env_minimal"
sed -i 's/replace-with-mysql-root-password/MySqlRoot123!/' "$tmp_env_minimal"
sed -i 's/replace-with-redis-password/RedisPassword123!/' "$tmp_env_minimal"
sed -i 's/MMMAIL_NACOS_ENABLED=true/MMMAIL_NACOS_ENABLED=false/' "$tmp_env_minimal"
bash scripts/validate-runtime-env.sh "$tmp_env_minimal" >/tmp/mmmail-runtime-env-minimal.log 2>&1

echo "[validate-local] compose assets"
if command -v docker >/dev/null 2>&1 && docker_client_available; then
  docker compose --env-file "$tmp_env_standard" -f docker-compose.yml config >/tmp/mmmail-docker-compose.log 2>&1
  docker compose --env-file "$tmp_env_minimal" -f docker-compose.minimal.yml config >/tmp/mmmail-docker-compose-minimal.log 2>&1
else
  echo "[validate-local] skip docker compose config: docker daemon unavailable"
fi

echo "[validate-local] secure defaults"
if grep -nE "change-me|please-change-me-before-production-use" \
  backend/mmmail-server/src/main/resources/application.yml \
  >/tmp/mmmail-insecure-defaults.log 2>&1; then
  echo "insecure default placeholder still exists in backend application.yml" >&2
  cat /tmp/mmmail-insecure-defaults.log >&2
  exit 1
fi

echo "[validate-local] db scripts syntax"
bash -n scripts/db-backup.sh scripts/db-restore.sh scripts/db-rollback.sh >/tmp/mmmail-db-scripts-syntax.log 2>&1

echo "[validate-local] backend compile"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am -DskipTests compile >/tmp/mmmail-backend-compile.log 2>&1

echo "[validate-local] backend auth/rbac regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_AUTH_RBAC_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-auth-rbac.log 2>&1

echo "[validate-local] backend docs regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_DOCS_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-docs.log 2>&1

echo "[validate-local] backend mail ga regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_MAIL_GA_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-mail-ga.log 2>&1

echo "[validate-local] backend calendar ga regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_CALENDAR_GA_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-calendar-ga.log 2>&1

echo "[validate-local] backend drive ga regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_DRIVE_GA_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-drive-ga.log 2>&1

echo "[validate-local] backend observability regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_OBSERVABILITY_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-observability.log 2>&1

echo "[validate-local] backend sheets regression"
SPRING_PROFILES_ACTIVE=test timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_SHEETS_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-sheets.log 2>&1

echo "[validate-local] Batch 3 migration gates"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  bash scripts/validate-batch3.sh >/tmp/mmmail-batch3-validate.log 2>&1

echo "[validate-local] all checks passed"
