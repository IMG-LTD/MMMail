import assert from 'node:assert/strict';
import { existsSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const root = new URL('../', import.meta.url);

async function read(path) {
  return readFile(new URL(path, root), 'utf8');
}

function exists(path) {
  return existsSync(new URL(path, root));
}

function lineCount(source) {
  return source.trimEnd().split('\n').length;
}

function idsBetween(source, startMarker, endMarker) {
  const start = source.indexOf(startMarker);
  const end = source.indexOf(endMarker, start);
  assert.notEqual(start, -1, `${startMarker} must exist`);
  assert.notEqual(end, -1, `${endMarker} must exist`);
  return [...source.slice(start, end).matchAll(/^\| ((?:GOV|FE|COMM|BILL|BUS|DEP|OBS|GATE|GTM)-\d+) \|/gm)]
    .map(match => match[1]);
}

test('v2.2 repository community health files are present and discoverable', async () => {
  for (const path of [
    '.editorconfig',
    '.gitattributes',
    'AGENTS.md',
    'CODE_OF_CONDUCT.md',
    'CONTRIBUTING.md',
    'DCO.md',
    'GOVERNANCE.md',
    'MAINTAINERS.md',
    'NOTICE',
    'ROADMAP.md',
    'SECURITY.md',
    'SUPPORT.md',
    '.github/CODEOWNERS',
    '.github/dependabot.yml',
    '.github/pull_request_template.md',
    '.github/workflows/dco.yml',
    '.github/ISSUE_TEMPLATE/security-contact-request.md',
    'docs/commercial/oidc-live-evidence-template.md',
    'docs/release/image-digest-evidence-template.md',
    'docs/billing/private-billing-evidence-template.md',
    'docs/v22-completion-audit.md',
    'docs/v22-external-evidence-checklist.md',
    'scripts/validate-v22-external-evidence.sh'
  ]) {
    assert.equal(exists(path), true, `${path} must exist`);
  }

  const [readme, contributing] = await Promise.all([
    read('README.md'),
    read('CONTRIBUTING.md')
  ]);
  assert.match(readme, /SUPPORT\.md/);
  assert.match(readme, /DCO\.md/);
  assert.match(contributing, /SUPPORT\.md/);
  assert.match(contributing, /DCO\.md/);
});

test('v2.2 DCO policy is documented and enforced by workflow', async () => {
  const [dco, workflow, contributing, pullRequestTemplate] = await Promise.all([
    read('DCO.md'),
    read('.github/workflows/dco.yml'),
    read('CONTRIBUTING.md'),
    read('.github/pull_request_template.md')
  ]);

  for (const required of [
    /Developer Certificate of Origin/,
    /instead of a Contributor License Agreement/,
    /Signed-off-by: Name <email@example\.com>/,
    /git commit -s/,
    /git commit --amend -s --no-edit/,
    /git rebase --signoff <base-ref>/,
    /\.github\/workflows\/dco\.yml/,
    /CONTRIBUTING\.md/
  ]) {
    assert.match(dco, required);
  }
  assert.match(workflow, /Verify Signed-off-by/);
  assert.match(workflow, /pull-requests: read/);
  assert.match(workflow, /actions\/github-script@v9/);
  assert.match(workflow, /github\.rest\.pulls\.listCommits/);
  assert.match(workflow, /context\.payload\.pull_request\.number/);
  assert.doesNotMatch(workflow, /git rev-list "origin\/\$BASE_REF"\.\.HEAD/);
  assert.match(contributing, /DCO\.md/);
  assert.match(pullRequestTemplate, /Signed-off-by/);
});

test('v2.2 spec status table covers every P0 work package', async () => {
  const spec = await read('docs/v22-open-source-commercial-spec.md');
  const p0Ids = idsBetween(spec, '## 4. P0 工作包总表', '### 4.1 当前执行状态');
  const statusIds = idsBetween(spec, '### 4.1 当前执行状态', '## 5. 工作包详细拆分');

  assert.deepEqual([...statusIds].sort(), [...p0Ids].sort());
  assert.equal(statusIds.length, p0Ids.length);
});

test('v2.2 repository agent instructions describe MMMail boundaries', async () => {
  const agents = await read('AGENTS.md');

  assert.match(agents, /当前仓库：`MMMail`/);
  assert.match(agents, /frontend-admin/);
  assert.match(agents, /legacy `frontend-v2`/);
  assert.match(agents, /新增或修改 `frontend-v2` 文件/);
  assert.match(agents, /scripts\/validate-local\.sh/);
  assert.match(agents, /scripts\/validate-legacy-frontend-v2-freeze\.sh/);
  assert.doesNotMatch(agents, /scripts\/validate-legacy-frontend-v2\.sh/);
  assert.match(agents, /docs\/v22-open-source-commercial-spec\.md/);
  assert.match(agents, /docs\/v22-completion-audit\.md/);
  assert.match(agents, /docs\/v22-external-evidence-checklist\.md/);
  assert.match(agents, /validate-v22-external-evidence\.sh/);
  assert.match(agents, /不进入默认绿色门禁/);
  assert.match(agents, /不得通过 root tests 间接执行/);
  assert.match(agents, /frontend-admin\/\.env/);
  assert.match(agents, /非敏感 Vite 构建默认值/);
  assert.match(agents, /OpenAPI 类型生成必须在同一脚本内完成格式化/);
  assert.match(agents, /frontend-admin test:e2e/);
  assert.match(agents, /CI frontend job 必须在 e2e 前显式安装 Java 21/);
  assert.match(agents, /版本化 spec、release audit、验收矩阵类文档允许超过常规文件行数建议/);
  assert.match(agents, /新代码质量上限/);
  assert.match(agents, /函数.*50 行/);
  assert.match(agents, /常规源码文件.*500 行/);
  assert.match(agents, /不得继续扩大已超限文件/);
  assert.match(agents, /Spring 管理组件优先使用单一构造器注入/);
  assert.match(agents, /运行时构造器必须显式 `@Autowired`/);
  assert.match(agents, /历史文档边界/);
  assert.match(agents, /docs\/superpowers\//);
  assert.match(agents, /旧 `docs\/v21\*` \/ `docs\/v212\*` spec/);
  assert.match(agents, /不代表当前产品入口、当前 CI 门禁或当前发布流程/);
  assert.match(agents, /BackendV22CommercialSurfaceCoverageContractTest/);
  assert.doesNotMatch(agents, /full-stack-skills/);
  assert.doesNotMatch(agents, /docs\/skill-group-mapping\.md/);
  assert.doesNotMatch(agents, /docs\/pipeline-stage-to-skills\.md/);
});

test('v2.2 touched oversized files keep new route, locale, and limiter logic split out', async () => {
  const [
    agents,
    routeIndex,
    customRoutes,
    publicShareRoutes,
    enLocale,
    zhLocale,
    enCommercialLocale,
    zhCommercialLocale,
    driveService,
    rateLimiter
  ] = await Promise.all([
    read('AGENTS.md'),
    read('frontend-admin/src/router/routes/index.ts'),
    read('frontend-admin/src/router/routes/custom-routes.ts'),
    read('frontend-admin/src/router/routes/public-share-routes.ts'),
    read('frontend-admin/src/locales/langs/en-us.ts'),
    read('frontend-admin/src/locales/langs/zh-cn.ts'),
    read('frontend-admin/src/locales/langs/v22-commercial/en-us.ts'),
    read('frontend-admin/src/locales/langs/v22-commercial/zh-cn.ts'),
    read('backend/mmmail-server/src/main/java/com/mmmail/server/service/DriveService.java'),
    read('backend/mmmail-server/src/main/java/com/mmmail/server/service/DrivePublicShareRateLimiter.java')
  ]);

  assert.match(agents, /不得继续扩大已超限文件/);
  assert.match(agents, /新增逻辑落到更小的模块/);
  assert.ok(lineCount(routeIndex) <= 500, 'router index should stay below the regular source file limit');
  assert.ok(lineCount(customRoutes) <= 500, 'custom route registry should stay below the regular source file limit');
  assert.ok(lineCount(publicShareRoutes) <= 500, 'public share route registry should stay below the regular source file limit');
  assert.ok(lineCount(enCommercialLocale) <= 500, 'English commercial locale module should stay below the limit');
  assert.ok(lineCount(zhCommercialLocale) <= 500, 'Chinese commercial locale module should stay below the limit');
  assert.ok(lineCount(rateLimiter) <= 500, 'public share rate limiter should stay below the regular source file limit');
  assert.match(routeIndex, /customRoutes/);
  assert.doesNotMatch(routeIndex, /name:\s*'admin_billing'/);
  assert.match(customRoutes, /publicShareRoutes/);
  assert.match(publicShareRoutes, /public_mail_share/);
  assert.match(enLocale, /v22CommercialPageLocale/);
  assert.match(zhLocale, /v22CommercialPageLocale/);
  assert.doesNotMatch(enLocale, /licenseKeyPlaceholder: 'Paste the license key'/);
  assert.doesNotMatch(zhLocale, /licenseKeyPlaceholder: '粘贴许可证密钥'/);
  assert.match(enCommercialLocale, /licenseKeyPlaceholder: 'Paste the license key'/);
  assert.match(zhCommercialLocale, /licenseKeyPlaceholder: '粘贴许可证密钥'/);
  assert.doesNotMatch(driveService, /public_share_rate_limit/);
  assert.match(rateLimiter, /public_share_rate_limit/);
});

test('v2.2 support policy routes help and security reports without public vulnerability disclosure', async () => {
  const [support, security] = await Promise.all([
    read('SUPPORT.md'),
    read('SECURITY.md')
  ]);

  assert.match(support, /docs\/release\/v2-support-boundaries\.md/);
  assert.match(support, /docs\/release\/v2-feedback-intake\.md/);
  assert.match(support, /\.github\/ISSUE_TEMPLATE\/release-blocking-regression\.md/);
  assert.match(support, /SECURITY\.md/);
  assert.match(support, /\.github\/ISSUE_TEMPLATE\/security-contact-request\.md/);
  assert.match(support, /Do not open a public issue for a live security vulnerability/);
  assert.match(support, /Use GitHub private vulnerability reporting whenever possible/);
  assert.match(security, /GitHub private vulnerability reporting is enabled for this repository/);
  assert.doesNotMatch(security, /currently \*\*not enabled\*\*/);
});

test('v2.2 contribution templates protect commercial and deployment boundaries', async () => {
  const [contributing, pullRequestTemplate] = await Promise.all([
    read('CONTRIBUTING.md'),
    read('.github/pull_request_template.md')
  ]);

  for (const required of [
    /docs\/commercial\/pricing-boundaries\.md/,
    /docs\/observability\/sli-slo\.md/,
    /license signing private keys stay outside/i,
    /real payment processing is not live/i,
    /Business \/ Hosted \/ payment \/ SLA \/ license/i,
    /Helm \/ image \/ env \/ deployment/i,
    /docs\/v22-external-evidence-checklist\.md/,
    /validate-v22-external-evidence\.sh/,
    /dry run、mock billing/,
    /未新增或修改 .*frontend-v2.*文件/,
    /Spring 管理组件如保留多个构造器/
  ]) {
    assert.match(`${contributing}\n${pullRequestTemplate}`, required);
  }
});

test('v2.2 governance status does not list delivered deployment docs as missing', async () => {
  const [agents, spec] = await Promise.all([
    read('AGENTS.md'),
    read('docs/v22-open-source-commercial-spec.md')
  ]);
  const remainingSectionStart = spec.indexOf('仍未落地：');
  const remainingSectionEnd = spec.indexOf('---', remainingSectionStart);
  const remainingSection = spec.slice(remainingSectionStart, remainingSectionEnd);

  assert.match(agents, /Helm chart \/ image publishing \/ SLI\/SLO docs \/ audit export \/ DSR inventory \/ OpenTelemetry runtime tracing \/ OIDC token\/session 主仓基线 \/ OIDC callback 专项 trace 已有代码、文档和门禁/);
  assert.match(remainingSection, /live Keycloak login \/ callback \/ session \/ logout \/ token refresh e2e/);
  assert.match(spec, /Helm \/ image gates 已完成，后续只补真实 tag digest/);
  assert.match(spec, /SLI\/SLO docs 与 OBS-01 runtime trace evidence 已接入主仓门禁/);
  assert.match(spec, /OIDC token\/session 主仓基线已落地，live Keycloak e2e 证据仍跟随 BUS-01 验收/);
  assert.match(spec, /OIDC callback 输出 `mmmail\.oidc\.callback`/);
  assert.doesNotMatch(spec, /企业准入、Helm、多架构镜像、OpenTelemetry 和 SLI\/SLO 仍是后续 P0/);
  assert.doesNotMatch(spec, /后续仍需把 Helm \/ image \/ OIDC \/ audit \/ DSR gate 补齐/);
  assert.doesNotMatch(spec, /企业准入 OIDC 和 OpenTelemetry 仍是后续 P0/);
  assert.doesNotMatch(spec, /后续只补真实 tag digest、OIDC gate/);
  assert.doesNotMatch(spec, /OIDC 配置后端仍不在主仓本阶段/);
  assert.doesNotMatch(spec, /真实 token exchange \/ session 仍跟随 BUS-01 live IdP 验收/);
  assert.doesNotMatch(spec, /OIDC callback 专项 span 随 BUS-01 OIDC 后端补齐/);
  assert.doesNotMatch(remainingSection, /OIDC callback 专项 span/);
  assert.doesNotMatch(agents, /Helm、镜像发布、OTEL、SLO、支付 provider、企业访问仍按 spec 拆分推进/);
  assert.doesNotMatch(agents, /OIDC、OIDC callback 专项 trace、真实支付 provider/);
});

test('v2.2 completion audit maps the objective to concrete evidence and external gaps', async () => {
  const [audit, externalEvidence, externalVerifier, validateLocal, releaseGate, spec] = await Promise.all([
    read('docs/v22-completion-audit.md'),
    read('docs/v22-external-evidence-checklist.md'),
    read('scripts/validate-v22-external-evidence.sh'),
    read('scripts/validate-local.sh'),
    read('scripts/release-gate.sh'),
    read('docs/v22-open-source-commercial-spec.md')
  ]);

  assert.match(audit, /Objective Restatement/);
  assert.match(audit, /Prompt requirements and spec gates to concrete repository artifacts/i);
  assert.match(audit, /docs\/v22-open-source-commercial-spec\.md/);
  assert.match(audit, /docs\/v22-external-evidence-checklist\.md/);
  assert.match(audit, /scripts\/validate-v22-external-evidence\.sh/);
  assert.match(audit, /BackendV22EditionCoreContractTest/);
  assert.match(audit, /BackendV22CommercialSurfaceCoverageContractTest/);
  assert.match(audit, /node --test tests\/\*\.test\.mjs/);
  assert.match(audit, /GitHub API-based PR commit enforcement that avoids merge-commit false positives/);
  assert.match(audit, /legacy `frontend-v2` Dependabot version PRs are disabled with `open-pull-requests-limit: 0`/);
  assert.match(audit, /Docker daemon access denied in the normal shell/);
  assert.match(audit, /No matching environment variable names in the current shell/);
  assert.match(audit, /`MMMail CI`, `DCO`, `MMMail Images`, and Dependabot workflows are visible/);
  assert.match(audit, /Remote `main` CI for `f29a1510` completed with failure/);
  assert.match(audit, /workflow pnpm was 9 while `frontend-admin` requires pnpm `>=10\.5\.0`/);
  assert.match(audit, /Frontend-admin Docker build failed with `ERR_MODULE_NOT_FOUND` for `@iconify\/utils`/);
  assert.match(audit, /HTTP 403: GitHub API requires `read:packages` scope/);
  assert.match(audit, /41 passed, build success/);
  assert.match(audit, /performance=86/);
  assert.match(audit, /refs\/tags\/v2\.1\.2-shipping-clean/);
  assert.match(audit, /docs\/commercial\/oidc-live-evidence-template\.md/);
  assert.match(audit, /does not replace a real IdP run/);
  assert.match(audit, /GitHub private vulnerability reporting/);
  assert.match(audit, /live Keycloak login, callback, MMMail session, logout, and token refresh e2e evidence/);
  assert.match(audit, /GATE-01 requires the live Keycloak e2e gate evidence/);
  assert.match(audit, /successful tag-baseline Images workflows through `v2\.2\.0-rc\.13`[\s\S]*completed image digest evidence/);
  assert.match(audit, /independent billing repository/);
  assert.match(audit, /Generated type and diff hygiene/);
  assert.match(audit, /`gen:api` runs `openapi-typescript` and then `oxfmt`/);
  assert.match(audit, /Frontend build env defaults/);
  assert.match(audit, /tracked non-secret Vite defaults/);
  assert.match(audit, /Frontend e2e Java toolchain/);
  assert.match(audit, /installs Temurin Java 21 before `pnpm --dir frontend-admin test:e2e`/);
  assert.match(audit, /Frontend Lighthouse and auth first screen/);
  assert.match(audit, /declares `chrome-launcher@1\.2\.1` as a direct devDependency/);
  assert.match(audit, /Backend dependency security baseline/);
  assert.match(audit, /OWASP dependency-check failures/);
  assert.match(audit, /dependency-check-maven:aggregate/);
  assert.match(audit, /Spring Boot 3\.5\.14/);
  assert.match(audit, /OpenTelemetry-Go CVEs/);
  assert.match(audit, /config\/dependency-check-suppressions\.xml/);
  assert.match(audit, /Kotlin stdlib 2\.3\.21/);
  assert.match(audit, /git diff --check/);
  assert.match(audit, /Only `mmmail-nacos` and `mmmail-kafka` are running/);
  assert.match(audit, /rechecks GitHub private vulnerability reporting via GitHub API/);
  assert.match(audit, /reports current read-only evidence gaps/);
  assert.match(audit, /requires completed evidence files with non-empty metadata fields, not templates/);
  assert.match(audit, /successful tag-push image workflow/);
  assert.match(audit, /Remote Publication Status/);
  assert.match(audit, /tag `v2\.2\.0-rc\.1` was pushed/);
  assert.match(audit, /tag `v2\.2\.0-rc\.2`/);
  assert.match(audit, /MMMail admin API type generation guard/);
  assert.match(audit, /VITE_ICON_LOCAL_PREFIX/);
  assert.match(audit, /tag `v2\.2\.0-rc\.3`/);
  assert.match(audit, /Fatal error compiling: error: release version 21 not supported/);
  assert.match(audit, /tag `v2\.2\.0-rc\.4`/);
  assert.match(audit, /ERR_MODULE_NOT_FOUND/);
  assert.match(audit, /tag `v2\.2\.0-rc\.5`/);
  assert.match(audit, /MMMail Images` succeeded for backend and frontend-admin/);
  assert.match(audit, /opentelemetry-semconv/);
  assert.match(audit, /tag `v2\.2\.0-rc\.6`/);
  assert.match(audit, /explicitly use the historical desktop Lighthouse preset[\s\S]*tag `v2\.2\.0-rc\.7`/);
  assert.match(audit, /not acceptable release evidence/);
  assert.match(audit, /25977701508[\s\S]*25977702756[\s\S]*rc10 provided the first clean main-repo remote release evidence baseline[\s\S]*25979632379[\s\S]*v2\.2\.0-rc\.13[\s\S]*historical evidence/);
  assert.match(audit, /same Public MMMail commit/);
  assert.match(audit, /not complete until the external evidence gaps above are resolved/);
  assert.match(spec, /status: main-repo-implemented-external-evidence-required/);
  assert.match(spec, /spec_version: oss-comm-v1\.94/);
  for (const requiredPass of [
    /pass-51 外部状态核查/, /pass-52 OTel 文档复查/, /pass-53 live OIDC 证据模板复查/,
    /pass-54 DEP-02 \/ billing 外部证据模板复查/, /pass-55 远端 CI 状态复查/, /pass-56 后端 v2\.2 contract 新鲜验证/,
    /pass-57 前端 commercial surface 新鲜验证/, /pass-58 仓库规范冻结口径复查/, /pass-59 仓库规范 Spring 注入与 auth shell 性能复查/,
    /pass-60 仓库规范安全产物复查/, /pass-61 auth shell 首屏性能复查/, /pass-62 完整本地门禁复查/,
    /pass-63 生成文件 hygiene 复查/, /pass-64 外部状态再核查/, /pass-65 外部 verifier 完成态加固/,
    /pass-66 completed external evidence 复验/, /pass-67 外部 evidence 文件防模板误判/, /pass-68 image digest 完成态门禁收紧/,
    /pass-69 外部 evidence 字段级校验/, /pass-70 外部 verifier 当前态实证输出/, /pass-71 外部 verifier 失败口径复查/,
    /pass-72 仓库规范 release-gate 复查/, /pass-73 外部 verifier 发布前置条件复查/, /pass-74 默认门禁外部 verifier 隔离复查/,
    /pass-75 完成态发布前置条件加固复查/, /pass-76 仓库规范超大文件复查/, /pass-77 仓库规范源码行数与本地产物复查/,
    /pass-81 Docker context 复查/, /pass-82 API 生成 clean-diff 复查/, /pass-83 frontend-admin env 复查/,
    /pass-84 frontend e2e Java 工具链复查/, /pass-85 Lighthouse 直接依赖复查/, /pass-86 后端依赖安全基线复查/,
    /pass-87 Lighthouse desktop preset 复查/, /pass-88 密码登录首屏性能复查/,
    /pass-89 依赖扫描误报治理复查/, /pass-90 安全报告路径复查/, /pass-91 rc10 远端主仓发布证据复查/, /pass-92 release evidence 文案稳定性复查/, /pass-93 供应链安全规范复查/
  ]) {
    assert.match(spec, requiredPass);
  }
  assert.match(spec, /frontend-admin\/scripts\/gen-api\.mjs` 在 `openapi-typescript` 后立即执行 `oxfmt`/);
  assert.match(spec, /frontend-admin\/\.env` 与 `frontend-admin\/\.env\.test` 仅包含非敏感 Vite 构建默认值/);
  assert.match(spec, /frontend job 在 `pnpm --dir frontend-admin test:e2e` 前安装 Temurin Java 21/);
  assert.match(spec, /frontend-admin\/package\.json` 显式声明 `chrome-launcher@1\.2\.1`/);
  assert.match(spec, /frontend-admin\/scripts\/run-lighthouse\.mjs` 显式使用 Lighthouse `preset: 'desktop'`[\s\S]*pwd-login\.vue` 的密码登录首屏使用原生 `auth-native-form`/);
  assert.match(spec, /backend\/pom\.xml` 固定 Spring Boot 3\.5\.14/);
  assert.match(spec, /DependencyVersionGuardTest` 覆盖这些运行时版本下限/);
  assert.match(spec, /OWASP dependency-check 漂移/);
  assert.match(spec, /active-source-size-guardrail/);
  assert.match(spec, /7 个 status markers 加 8 个 read-only evidence gaps/);
  assert.match(spec, /all checks passed/);
  assert.match(spec, /pnpm --dir frontend-admin test:v212` 通过，125 tests/);
  assert.match(spec, /41 tests \/ build success/);
  assert.match(spec, /历史 `frontend-v2` 文档归类为 archival evidence/);
  assert.match(spec, /不是当前产品入口、CI 门禁或发布流程/);
  assert.match(spec, /\| GOV-01 \| done \|/);
  assert.match(spec, /\| GOV-02 \| done \|/);
  assert.match(spec, /\| BILL-01 \| partial done \|/);
  assert.match(spec, /\| DEP-03 \| done \|/);
  assert.match(spec, /private-vulnerability-reporting` 已返回 `\{"enabled":true\}`/);
  assert.doesNotMatch(spec, /GitHub private vulnerability reporting 仍需在 repository settings 外部启用/);
  assert.match(spec, /独立 `mmmail-billing-gateway` 仍需实现 Adapay adapter/);
  assert.match(spec, /## 10\. 决策复查与剩余边界/);
  assert.match(spec, /原 Sprint 0 决策点已经随主仓实现和仓库规范复查收敛为下表/);
  assert.match(spec, /主仓可以发布 OSS launch \+ commercial-ready adapter，不声称 payment-ready/);
  assert.doesNotMatch(spec, /status: implementation-started/);
  assert.doesNotMatch(spec, /进入 v2\.2 Sprint 0 前只剩以下决策需要拍板/);
  assert.doesNotMatch(spec, /README 现在把两者都写成/);
  assert.doesNotMatch(spec, /CI 和 release-gate 同时检查两套前端/);
  assert.doesNotMatch(spec, /仍需外部完成的 GitHub private vulnerability reporting/);
  assert.doesNotMatch(spec, /失败输出必须包含 completion audit、external checklist、GitHub private vulnerability reporting/);
  assert.doesNotMatch(spec, /completion audit、checklist、GitHub、BUS-01/);
  for (const required of [
    /GitHub private vulnerability reporting/,
    /Completed External Evidence/,
    /returned `\{"enabled":true\}`/,
    /docs\/commercial\/oidc-live-evidence-template\.md/,
    /docs\/release\/image-digest-evidence-template\.md/,
    /docs\/billing\/private-billing-evidence-template\.md/,
    /Real Keycloak or approved OIDC IdP/,
    /GATE-01 live Keycloak e2e gate \| Real release or CI gate run against Keycloak\/OIDC/,
    /Private billing repository/,
    /Current Incomplete Markers/,
    /completion audit is still marked not-complete-external-evidence-required/,
    /GATE-01 live Keycloak e2e gate remains partial/,
    /private billing repository and real payment evidence remain external/,
    /`workflow_dispatch` dry runs without tag-published image digests/,
    /Mock or `none` billing provider events/,
    /validate-v22-external-evidence\.sh/,
    /MMMAIL_OIDC_LIVE_EVIDENCE_FILE/,
    /MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE/,
    /MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE/,
    /Evidence status: completed-external-evidence/,
    /template files themselves and unfilled template copies are rejected/,
    /required metadata fields in each evidence package must be filled with non-empty values/,
    /Publication Preconditions/,
    /local uncommitted worktree/,
    /remote default branch or release branch/,
    /release tag must point to the same commit/,
    /sha256:\*/,
    /Workflow event: push/,
    /Workflow conclusion: success/,
    /successful `push` event run for the `MMMail Images` workflow/,
    /Default root governance tests may statically assert/,
    /must not execute `bash scripts\/validate-v22-external-evidence\.sh`/,
    /same Public MMMail commit/,
    /Public MMMail repository commit SHA/,
    /release tag named by the image digest evidence must be visible on `origin`/,
    /origin\/main` or `origin\/release\/\*`/,
    /live OIDC evidence file is not provided/,
    /visible rc10 through rc13 `MMMail Images` successes are historical baseline evidence only/,
    /final image digest acceptance must come from a tag and workflow tied to the same Public MMMail commit/,
    /backend GHCR package versions are not visible/,
    /private billing repository is not accessible/,
    /must not be executed by default `scripts\/validate-local\.sh`, CI, or release-gate paths/,
    /not-complete-external-evidence-required/
  ]) {
    assert.match(externalEvidence, required);
  }
  assert.match(externalVerifier, /verify_completed_external_evidence/);
  assert.match(externalVerifier, /verify_private_vulnerability_reporting/);
  assert.match(externalVerifier, /gh api --method GET repos\/IMG-LTD\/MMMail\/private-vulnerability-reporting/);
  assert.match(externalVerifier, /MMMAIL_OIDC_LIVE_EVIDENCE_FILE/);
  assert.match(externalVerifier, /MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE/);
  assert.match(externalVerifier, /MMMAIL_PRIVATE_BILLING_EVIDENCE_FILE/);
  assert.match(externalVerifier, /require_completed_evidence_file/);
  assert.match(externalVerifier, /require_nonempty_field/);
  assert.match(externalVerifier, /record_if_env_file_missing/);
  assert.match(externalVerifier, /record_if_command_fails/);
  assert.match(externalVerifier, /verify_publication_preconditions/);
  assert.match(externalVerifier, /remote_tag_commit/);
  assert.match(externalVerifier, /"Backend commit SHA" "OIDC backend commit SHA"/);
  assert.match(externalVerifier, /"GitHub workflow run URL" "image workflow run URL"/);
  assert.match(externalVerifier, /"Billing repository commit SHA" "billing repository commit SHA"/);
  assert.match(externalVerifier, /"Public MMMail repository commit SHA" "billing public MMMail repository commit SHA"/);
  assert.match(externalVerifier, /git ls-remote --tags origin "refs\/tags\/\$tag"/);
  assert.match(externalVerifier, /release tag must point to the image commit/);
  assert.match(externalVerifier, /origin\/\(main\|release\/\.\+\)/);
  assert.match(externalVerifier, /Evidence status: completed-external-evidence/);
  assert.match(externalVerifier, /reject_contains "\$MMMAIL_IMAGE_DIGEST_EVIDENCE_FILE" "sha256:\*"/);
  assert.match(externalVerifier, /gh run list --repo IMG-LTD\/MMMail --workflow "MMMail Images" --event push --status success/);
  assert.match(externalVerifier, /record_if_ghcr_versions_unavailable "mmmail-backend" "backend"/);
  assert.match(externalVerifier, /record_if_ghcr_versions_unavailable "mmmail-frontend-admin" "frontend-admin"/);
  assert.match(externalVerifier, /gh repo view IMG-LTD\/mmmail-billing-gateway/);
  assert.match(validateLocal, /docs\/v22-completion-audit\.md/);
  assert.match(validateLocal, /docs\/v22-external-evidence-checklist\.md/);
  assert.match(validateLocal, /scripts\/validate-v22-external-evidence\.sh/);
  assert.doesNotMatch(validateLocal, /bash scripts\/validate-v22-external-evidence\.sh/);
  assert.doesNotMatch(validateLocal, /\.\/scripts\/validate-v22-external-evidence\.sh/);
  assert.doesNotMatch(releaseGate, /validate-v22-external-evidence\.sh/);
  assert.match(spec, /docs\/v22-completion-audit\.md/);
  assert.match(spec, /docs\/v22-external-evidence-checklist\.md/);
  assert.match(spec, /scripts\/validate-v22-external-evidence\.sh/);
  assert.match(spec, /不得进入默认 validate-local、CI 或 release-gate 绿色路径/);
});

test('v2.2 external evidence verifier remains manual-only in default governance gates', async () => {
  const [externalVerifier, governanceContract, validationContract] = await Promise.all([
    read('scripts/validate-v22-external-evidence.sh'),
    read('tests/v22-repository-governance-contract.test.mjs'),
    read('tests/v22-repository-governance-validation-contract.test.mjs')
  ]);
  const runtimeVerifierCall = /execFileAsync\('bash', \['scripts\/validate-v22-external-evidence\.sh'\]/;

  for (const required of [
    /v2\.2 external evidence is incomplete:/,
    /completion audit is still marked not-complete-external-evidence-required/,
    /external evidence checklist is still pending/,
    /BUS-01 live Keycloak SSO remains partial/,
    /DEP-02 image digest evidence remains partial/,
    /OBS-01 live OIDC trace evidence remains partial/,
    /GATE-01 live Keycloak e2e gate remains partial/,
    /private billing repository and real payment evidence remain external/,
    /live OIDC evidence file is not provided/,
    /image digest evidence file is not provided/,
    /private billing evidence file is not provided/,
    /current v2\.2 implementation is not published to a remote commit\/tag/,
    /successful tag-triggered MMMail Images workflow run is not visible/,
    /backend GHCR package versions are not visible/,
    /frontend-admin GHCR package versions are not visible/,
    /private billing repository is not accessible/
  ]) {
    assert.match(externalVerifier, required);
  }
  assert.doesNotMatch(governanceContract, runtimeVerifierCall);
  assert.doesNotMatch(validationContract, runtimeVerifierCall);
});
