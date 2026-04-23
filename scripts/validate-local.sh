#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

source "$ROOT_DIR/scripts/lib/db-common.sh"
source "$ROOT_DIR/scripts/lib/java-common.sh"

MVN_BIN="$(resolve_maven_bin "$ROOT_DIR")"
BACKEND_AUTH_RBAC_TESTS="AuthFlowIntegrationTest,OrgAuthenticationSecurityIntegrationTest,OrgAdminConsoleIntegrationTest,OrgMemberGovernanceIntegrationTest"
BACKEND_DOCS_TESTS="DocsCollaborationIntegrationTest,DocsSuggestionWorkflowIntegrationTest,DocsOrgAccessIntegrationTest"
BACKEND_MAIL_GA_TESTS="MailGaIntegrationTest,MailAttachmentIntegrationTest,MailReleaseBlockingIntegrationTest"
BACKEND_CALENDAR_GA_TESTS="CalendarSharingAvailabilityIntegrationTest,CalendarReleaseBlockingIntegrationTest,CalendarIcsImportIntegrationTest"
BACKEND_DRIVE_GA_TESTS="DriveReleaseBlockingIntegrationTest,DriveCollaboratorShareIntegrationTest,DriveSharedWithMeIntegrationTest,DriveSecureShareIntegrationTest,DrivePublicFolderShareIntegrationTest"
BACKEND_OBSERVABILITY_TESTS="JobRunMonitorServiceTest,GlobalExceptionHandlerUnitTest"
BACKEND_SHEETS_TESTS="SheetsWorkbookIntegrationTest,SheetsWorkbookDataManagementIntegrationTest,SheetsSharingVersionIntegrationTest,SheetsWorkbookMultiSheetIntegrationTest"
FRONTEND_V2_CONTRACT_TESTS="tests/foundation-route-contract.test.mjs tests/redirect-contract.test.mjs tests/auth-scope-contract.test.mjs tests/public-share-contract.test.mjs tests/public-share-runtime-contract.test.mjs tests/public-share-view-contract.test.mjs tests/mail-workspace-contract.test.mjs tests/calendar-workspace-contract.test.mjs tests/drive-workspace-contract.test.mjs tests/pass-workspace-contract.test.mjs tests/docs-sheets-runtime-contract.test.mjs tests/workspace-aggregation-contract.test.mjs tests/settings-panel-contract.test.mjs tests/system-health-contract.test.mjs tests/command-center-query-contract.test.mjs"
BACKEND_V2_CONTRACT_TESTS="PlatformCapabilityIntegrationTest,PublicShareCapabilityIntegrationTest,WorkspaceAggregationIntegrationTest,AiMcpCapabilityIntegrationTest,RequestHeaderContractIntegrationTest,ObservabilityIntegrationTest,BillingReadinessIntegrationTest,ContractCatalogRegressionTest,TenantScopeFoundationContractTest,BackendModuleExtractionContractTest,PublicShareTokenHashMigrationIntegrationTest,MailPublicShareTokenHashContractTest,PassPublicShareTokenHashContractTest,DrivePublicShareTokenHashContractTest"

echo "[validate-local] frontend-v2 tests"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend-v2 test >/tmp/mmmail-frontend-v2-test.log 2>&1

echo "[validate-local] frontend-v2 contract regression"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend-v2 exec node --test $FRONTEND_V2_CONTRACT_TESTS >/tmp/mmmail-frontend-v2-contract.log 2>&1

echo "[validate-local] frontend-v2 typecheck"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend-v2 typecheck >/tmp/mmmail-frontend-v2-typecheck.log 2>&1

echo "[validate-local] frontend-v2 dependency audit (high)"
env -u http_proxy -u https_proxy -u HTTP_PROXY -u HTTPS_PROXY -u ALL_PROXY -u all_proxy \
  pnpm --dir frontend-v2 audit --prod --audit-level=high --ignore-registry-errors >/tmp/mmmail-frontend-v2-audit.log 2>&1

echo "[validate-local] security gates"
bash scripts/validate-security.sh >/tmp/mmmail-security.log 2>&1

echo "[validate-local] required files"
required=(
  README.md
  LICENSE
  CONTRIBUTING.md
  SECURITY.md
  .github/ISSUE_TEMPLATE/security-contact-request.md
  .github/ISSUE_TEMPLATE/config.yml
  .github/ISSUE_TEMPLATE/bug-report.md
  .github/ISSUE_TEMPLATE/release-blocking-regression.md
  .github/ISSUE_TEMPLATE/self-hosting-feedback.md
  .github/ISSUE_TEMPLATE/feature-request.md
  docs/ops/install.md
  docs/ops/upgrade.md
  docs/ops/backup-restore.md
  docs/ops/runbook.md
  docs/release/v2-support-boundaries.md
  docs/release/v2-feedback-intake.md
  docs/release/v2.0.3-release-notes.md
  docs/open-source/module-maturity-matrix.md
  docs/open-source/README.en.md
  docs/ops/install.en.md
  docs/architecture/deployment-topology.md
  docs/architecture/database-migration-strategy.md
  docs/security/threat-model.md
  docker-compose.yml
  docker-compose.minimal.yml
  backend/Dockerfile
  backend/pom.xml
  frontend-v2/Dockerfile
  frontend-v2/package.json
  frontend-v2/src/app/router/routes.ts
  frontend-v2/src/layouts/modules/shell-nav.ts
  frontend-v2/src/shared/content/route-surfaces.ts
  .env.example
  config/backend.env.example
  config/backend.test.env.example
  scripts/security-secret-scan.sh
  scripts/security-backend-dependency-scan.sh
  scripts/validate-security.sh
  scripts/validate-runtime-env.sh
  scripts/validate-backend-test-env.sh
  scripts/validate-batch3.sh
  scripts/validate-local.sh
  scripts/validate-ci.sh
  scripts/db-upgrade.sh
  scripts/db-backup.sh
  scripts/db-restore.sh
  scripts/db-rollback.sh
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
  ".env.example|VITE_API_BASE_URL=http://localhost:8080"
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
  VITE_API_BASE_URL
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

echo "[validate-local] backend v2 contract regression"
timeout 60s "$MVN_BIN" -f backend/pom.xml -pl mmmail-server -am \
  -Dtest="$BACKEND_V2_CONTRACT_TESTS" -Dsurefire.failIfNoSpecifiedTests=false test \
  >/tmp/mmmail-backend-v2-contract.log 2>&1

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
