import assert from 'node:assert/strict';
import { execFile } from 'node:child_process';
import { mkdir, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { promisify } from 'node:util';

const root = new URL('../', import.meta.url);
const rootPath = fileURLToPath(root);
const execFileAsync = promisify(execFile);
const activeSourceLineLimit = 500;
const activeSourcePrefixes = [
  'backend/mmmail-server/src/main/',
  'backend/mmmail-server/src/test/',
  'frontend-admin/src/',
  'frontend-admin/tests/',
  'frontend-admin/e2e/',
  'tests/',
  'scripts/'
];
const activeSourceExtensions = new Set([
  '.java',
  '.js',
  '.mjs',
  '.sh',
  '.sql',
  '.ts',
  '.vue',
  '.yml',
  '.yaml'
]);
const generatedSourceExemptions = [
  /^frontend-admin\/src\/service\/api\/__generated__\//,
  /^frontend-admin\/src\/typings\/app\.d\.ts$/,
  /^frontend-admin\/src\/typings\/api\/expanded\.d\.ts$/
];
const legacyOversizedSourceAllowlist = new Set([
  'backend/mmmail-server/src/main/java/com/mmmail/server/controller/SuiteController.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/CalendarService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/ContactService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/DriveCollaborationService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/DriveService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/MailService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/MeetGuestRequestService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/MeetService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/OrgBusinessService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/OrgPolicyService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/OrgService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/PassBusinessService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/PassService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/SheetsService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/StandardNotesService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/SuiteBillingCenterService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/SuiteCollaborationService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/SuiteCommandCenterService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/SuiteInsightService.java',
  'backend/mmmail-server/src/main/java/com/mmmail/server/service/WalletService.java',
  'backend/mmmail-server/src/main/resources/db/baseline/community-v1-schema.sql',
  'backend/mmmail-server/src/main/resources/schema.sql',
  'backend/mmmail-server/src/test/java/com/mmmail/server/MailFeatureIntegrationTest.java',
  'frontend-admin/src/locales/langs/en-us.ts',
  'frontend-admin/src/locales/langs/zh-cn.ts'
]);

async function read(path) {
  return readFile(new URL(path, root), 'utf8');
}

function escaped(path) {
  return new RegExp(path.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
}

function extensionOf(path) {
  const match = path.match(/(\.[^.\/]+(?:\.ts)?)$/);
  return match ? match[1] : '';
}

function isActiveSource(path) {
  return activeSourcePrefixes.some(prefix => path.startsWith(prefix))
    && activeSourceExtensions.has(extensionOf(path));
}

function isGeneratedSource(path) {
  return generatedSourceExemptions.some(pattern => pattern.test(path));
}

function lineCount(source) {
  return source.trimEnd().split('\n').length;
}

async function repositoryFiles() {
  const { stdout } = await execFileAsync('git', ['ls-files', '--cached', '--others', '--exclude-standard'], {
    cwd: rootPath,
    maxBuffer: 10 * 1024 * 1024
  });
  return stdout.split('\n').filter(Boolean);
}

async function runSecretScan(reportDir) {
  return execFileAsync('bash', ['scripts/security-secret-scan.sh'], {
    cwd: rootPath,
    maxBuffer: 10 * 1024 * 1024,
    env: {
      ...process.env,
      MMMAIL_SECURITY_REPORT_DIR: reportDir
    }
  });
}

async function seedLegacyRepo(freezeScript) {
  const repoDir = join(tmpdir(), `mmmail-legacy-freeze-contract-${process.pid}`);
  await mkdir(join(repoDir, 'scripts'), { recursive: true });
  await mkdir(join(repoDir, 'frontend-v2'), { recursive: true });
  await writeFile(join(repoDir, 'scripts/validate-legacy-frontend-v2-freeze.sh'), freezeScript, 'utf8');
  await writeFile(join(repoDir, 'frontend-v2/legacy.txt'), 'legacy contract\n', 'utf8');
  await execFileAsync('git', ['init'], { cwd: repoDir });
  await execFileAsync('git', ['add', '.'], { cwd: repoDir });
  await execFileAsync('git', [
    '-c', 'user.email=contract@example.test',
    '-c', 'user.name=MMMail Contract',
    'commit', '-m', 'seed legacy frontend'
  ], { cwd: repoDir });
  const { stdout } = await execFileAsync('git', ['rev-parse', 'HEAD'], { cwd: repoDir });
  return { repoDir, baseRef: stdout.trim() };
}

async function runLegacyFreeze(repoDir, baseRef) {
  return execFileAsync('bash', ['scripts/validate-legacy-frontend-v2-freeze.sh'], {
    cwd: repoDir,
    env: {
      ...process.env,
      MMMAIL_LEGACY_FRONTEND_BASE_REF: baseRef
    }
  });
}

async function assertLegacyPathBlocked(repoDir, baseRef, path) {
  await assert.rejects(
    runLegacyFreeze(repoDir, baseRef),
    error => {
      assert.match(error.stderr, /new or modified frontend-v2 files are blocked/);
      assert.match(error.stderr, new RegExp(path.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
      return true;
    }
  );
}

test('v2.2 root editor and git attributes make repository formatting explicit', async () => {
  const [editorConfig, gitAttributes, gitIgnore] = await Promise.all([
    read('.editorconfig'),
    read('.gitattributes'),
    read('.gitignore')
  ]);

  assert.match(editorConfig, /root = true/);
  assert.match(editorConfig, /\[\*\][\s\S]+charset = utf-8/);
  assert.match(editorConfig, /\[\*\][\s\S]+end_of_line = lf/);
  assert.match(editorConfig, /\[\*\][\s\S]+insert_final_newline = true/);
  assert.match(editorConfig, /\[\*\][\s\S]+trim_trailing_whitespace = true/);
  assert.match(editorConfig, /\[\*\.\{java,xml\}\][\s\S]+indent_size = 4/);
  assert.match(editorConfig, /\[\*\.\{js,mjs,ts,vue,json,yml,yaml,md,css,scss,html\}\][\s\S]+indent_size = 2/);
  assert.match(gitAttributes, /\* text=auto eol=lf/);
  assert.match(gitAttributes, /\*\.sh text eol=lf/);
  assert.match(gitAttributes, /\*\.png binary/);
  assert.match(gitAttributes, /\*\.jar binary/);
  assert.match(gitAttributes, /pnpm-lock\.yaml text eol=lf/);
  assert.match(gitIgnore, /^\.claude\/$/m);
  assert.match(gitIgnore, /^\.codex-tasks\/$/m);
  assert.match(gitIgnore, /^\.superpowers\/$/m);
  assert.match(gitIgnore, /^\.tmp\/$/m);
  assert.match(gitIgnore, /^\.tools\/dependency-check-data\/$/m);
});

test('v2.2 active source line limit uses an explicit legacy allowlist', async () => {
  const [agents, contributing, pullRequestTemplate] = await Promise.all([
    read('AGENTS.md'),
    read('CONTRIBUTING.md'),
    read('.github/pull_request_template.md')
  ]);
  const oversizedFiles = [];

  for (const path of (await repositoryFiles()).filter(isActiveSource)) {
    if (isGeneratedSource(path) || legacyOversizedSourceAllowlist.has(path)) {
      continue;
    }
    const source = await read(path);
    const lines = lineCount(source);
    if (lines > activeSourceLineLimit) {
      oversizedFiles.push(`${lines} ${path}`);
    }
  }

  assert.deepEqual(oversizedFiles, []);
  assert.match(agents, /活跃源码文件行数由 root governance contract 使用显式 allowlist 约束/);
  assert.match(agents, /本地 agent \/ validation 产物/);
  assert.match(contributing, /新增活跃源码文件默认不超过 500 行/);
  assert.match(pullRequestTemplate, /未新增超过 500 行的活跃源码文件/);
});

test('v2.2 local validation guards repository governance files', async () => {
  const [validateLocal, spec, validateSecurity, secretScan, dependencyScan, ciWorkflow] = await Promise.all([
    read('scripts/validate-local.sh'),
    read('docs/v22-open-source-commercial-spec.md'),
    read('scripts/validate-security.sh'),
    read('scripts/security-secret-scan.sh'),
    read('scripts/security-backend-dependency-scan.sh'),
    read('.github/workflows/ci.yml')
  ]);

  for (const path of [
    'AGENTS.md',
    '.editorconfig',
    '.gitattributes',
    'DCO.md',
    'SUPPORT.md',
    '.github/pull_request_template.md',
    'docs/v22-completion-audit.md',
    'docs/v22-external-evidence-checklist.md',
    'docs/commercial/oidc-live-evidence-template.md',
    'docs/release/image-digest-evidence-template.md',
    'docs/billing/private-billing-evidence-template.md',
    'scripts/validate-v22-external-evidence.sh'
  ]) {
    assert.match(validateLocal, escaped(path));
    assert.match(spec, escaped(path));
  }

  for (const script of [validateSecurity, secretScan, dependencyScan]) {
    assert.match(script, /\$\{TMPDIR:-\/tmp\}\/mmmail-security/);
  }
  assert.match(ciWorkflow, /MMMAIL_SECURITY_REPORT_DIR: artifacts\/security/);
  assert.doesNotMatch(validateLocal, /ROOT_DIR\/artifacts\/security/);
  assert.match(validateLocal, /node frontend-admin\/scripts\/normalize-generated-types\.mjs/);
  assert.match(validateLocal, /git diff --check/);
});

test('v2.2 release gate documents the expanded deployment step count', async () => {
  const [releaseGate, ciWorkflow] = await Promise.all([
    read('scripts/release-gate.sh'),
    read('.github/workflows/ci.yml')
  ]);
  const releaseGateJob = ciWorkflow.slice(ciWorkflow.indexOf('  release-gate:'));

  assert.match(releaseGate, /17 步顺序硬阻断/);
  assert.match(releaseGate, /bash scripts\/release-gate\.sh\s+# 全 17 步/);
  assert.match(releaseGate, /CI must not set MMMAIL_SKIP_BACKEND or MMMAIL_SKIP_E2E/);
  assert.match(releaseGate, /run_step 13 "sbom-license"/);
  assert.match(releaseGate, /run_step 14 "helm-lint"/);
  assert.match(releaseGate, /run_step 15 "image-workflow-contract"/);
  assert.match(releaseGate, /run_step 16 "dsr-inventory"/);
  assert.match(releaseGate, /step_legacy_frontend_freeze/);
  assert.match(releaseGate, /run_step 17 "legacy-frontend-freeze"/);
  assert.match(releaseGateJob, /fetch-depth: 0/);
  assert.doesNotMatch(releaseGate, /全 12 步/);
  assert.doesNotMatch(releaseGate, /全 13 步/);
  assert.doesNotMatch(releaseGate, /全 14 步/);
  assert.doesNotMatch(releaseGate, /全 15 步/);
  assert.doesNotMatch(releaseGate, /全 16 步/);

  await assert.rejects(
    execFileAsync('bash', ['scripts/release-gate.sh'], {
      cwd: rootPath,
      env: { ...process.env, CI: 'true', MMMAIL_SKIP_BACKEND: '1' }
    }),
    error => {
      assert.match(error.stderr, /CI must not set MMMAIL_SKIP_BACKEND or MMMAIL_SKIP_E2E/);
      return true;
    }
  );
});

test('v2.2 open source governance docs keep frontend-admin as the product frontend', async () => {
  const i18nGovernance = await read('docs/open-source/i18n-governance.md');

  assert.match(i18nGovernance, /frontend-admin/);
  assert.match(i18nGovernance, /legacy `frontend-v2`/);
  assert.doesNotMatch(i18nGovernance, /当前治理对象为 `frontend-v2`/);
  assert.doesNotMatch(i18nGovernance, /Frontend-v2 tests/);
  assert.doesNotMatch(i18nGovernance, /pnpm --dir frontend-v2 test/);
});

test('v2.2 runtime governance docs use frontend-admin as the runtime frontend', async () => {
  const [readme, supportBoundaries, install, installEn, deploymentRunbook, deploymentTopology, threatModel] = await Promise.all([
    read('README.md'),
    read('docs/release/v2-support-boundaries.md'),
    read('docs/ops/install.md'),
    read('docs/ops/install.en.md'),
    read('docs/deployment-runbook.md'),
    read('docs/architecture/deployment-topology.md'),
    read('docs/security/threat-model.md')
  ]);

  for (const document of [readme, supportBoundaries, install, installEn, deploymentRunbook, deploymentTopology, threatModel]) {
    assert.match(document, /frontend-admin/);
  }
  for (const document of [readme, supportBoundaries, install, deploymentTopology]) {
    assert.match(document, /冻结 legacy reference/);
    assert.match(document, /只允许删除文件或迁出历史材料/);
  }
  assert.match(installEn, /frozen legacy reference/);
  assert.match(installEn, /only deletion or migration out of historical material is allowed/);
  assert.doesNotMatch(deploymentRunbook, /统一 `frontend-v2`/);
  assert.doesNotMatch(deploymentRunbook, /当前默认自托管运行模型：`frontend-v2 Web/);
  assert.doesNotMatch(deploymentRunbook, /`frontend-v2` tests/);
  assert.doesNotMatch(deploymentTopology, /一个 `frontend-v2` Web 前端/);
  assert.doesNotMatch(threatModel, /浏览器 \/ `frontend-v2` 运行时/);
  assert.doesNotMatch(threatModel, /pnpm --dir frontend-v2 audit/);
});

test('v2.2 legacy frontend governance entries are explicitly migration-scoped', async () => {
  const [codeowners, dependabot, topologyAudit, convergenceDecision] = await Promise.all([
    read('.github/CODEOWNERS'),
    read('.github/dependabot.yml'),
    read('docs/frontend/v22-frontend-topology-audit.md'),
    read('docs/frontend/v22-frontend-convergence-decision.md')
  ]);

  assert.match(codeowners, /Legacy frontend-v2 ownership is limited to deletion or rename-out archival/);
  assert.match(codeowners, /\/frontend-v2\/ @IMG-LTD/);
  assert.match(dependabot, /Legacy frontend-v2 stays on dependency alerts only until contract archival/);
  assert.match(dependabot, /directory: "\/frontend-v2"[\s\S]+open-pull-requests-limit: 0/);
  assert.match(topologyAudit, /v22-frontend-convergence-decision\.md/);
  assert.match(topologyAudit, /validate-legacy-frontend-v2-freeze\.sh/);
  assert.match(topologyAudit, /Selected legacy contracts have moved/);
  assert.match(topologyAudit, /Historical `docs\/superpowers\/`/);
  assert.match(topologyAudit, /not current release gates, runtime topology, or product frontend instructions/);
  assert.match(convergenceDecision, /archival evidence only/);
  assert.match(convergenceDecision, /must not be used as current product gates/);
  assert.doesNotMatch(topologyAudit, /validate-legacy-frontend-v2\.sh/);
});

test('v2.2 legacy frontend freeze gate blocks new frontend-v2 product files', async () => {
  const [freezeScript, validateLocal, spec, convergenceDecision] = await Promise.all([
    read('scripts/validate-legacy-frontend-v2-freeze.sh'),
    read('scripts/validate-local.sh'),
    read('docs/v22-open-source-commercial-spec.md'),
    read('docs/frontend/v22-frontend-convergence-decision.md')
  ]);
  assert.match(validateLocal, /validate-legacy-frontend-v2-freeze\.sh/);
  assert.match(spec, /legacy-frontend-freeze/);
  assert.match(validateLocal, /docs\/frontend\/v22-frontend-convergence-decision\.md/);
  assert.match(convergenceDecision, /Decision: keep `frontend-v2` as a v2\.2 legacy reference only/);
  assert.match(convergenceDecision, /No new feature, bugfix, dependency bump, style change, or migration shim may enter `frontend-v2`/);
  assert.match(convergenceDecision, /only deletion or rename-out archival is allowed/);
  assert.match(freezeScript, /MMMAIL_LEGACY_FRONTEND_BASE_REF/);
  assert.match(freezeScript, /new or modified frontend-v2 files are blocked/);

  const { repoDir, baseRef } = await seedLegacyRepo(freezeScript);
  try {
    await runLegacyFreeze(repoDir, baseRef);
    await writeFile(join(repoDir, 'frontend-v2/new-product.ts'), 'export const product = true;\n', 'utf8');
    await assertLegacyPathBlocked(repoDir, baseRef, 'frontend-v2/new-product.ts');
    await rm(join(repoDir, 'frontend-v2/new-product.ts'), { force: true });

    await writeFile(join(repoDir, 'frontend-v2/committed-product.ts'), 'export const product = true;\n', 'utf8');
    await execFileAsync('git', ['add', '.'], { cwd: repoDir });
    await execFileAsync('git', [
      '-c', 'user.email=contract@example.test',
      '-c', 'user.name=MMMail Contract',
      'commit', '-m', 'add blocked frontend-v2 product file'
    ], { cwd: repoDir });
    await assertLegacyPathBlocked(repoDir, baseRef, 'frontend-v2/committed-product.ts');
  } finally {
    await rm(repoDir, { recursive: true, force: true });
  }
});

test('v2.2 secret scan respects git-ignored generated frontend artifacts', async () => {
  const ignoredDir = join(rootPath, 'frontend-admin/node_modules/__secret_scan_contract__');
  const reportDir = join(tmpdir(), `mmmail-secret-scan-contract-${process.pid}`);
  const fakeAwsKey = `${'AKIA'}${'ABCDEFGHIJKLMNOP'}`;

  await mkdir(ignoredDir, { recursive: true });
  await writeFile(join(ignoredDir, 'ignored-generated.txt'), fakeAwsKey, 'utf8');

  try {
    await runSecretScan(reportDir);
  } finally {
    await rm(ignoredDir, { recursive: true, force: true });
    await rm(reportDir, { recursive: true, force: true });
  }
});

test('v2.2 commercial backend contracts are wired into local and CI gates', async () => {
  const [validateLocal, ciWorkflow] = await Promise.all([
    read('scripts/validate-local.sh'),
    read('.github/workflows/ci.yml')
  ]);
  for (const contract of [
    'BackendV22EditionCoreContractTest',
    'BackendV22LicenseVerifierContractTest',
    'BackendV22BillingWebhookContractTest',
    'BackendV22EntitlementEnforcementContractTest',
    'BackendV22CommercialSurfaceCoverageContractTest',
    'BackendV22LicenseManagementApiContractTest',
    'BackendV22AuditExportContractTest',
    'BackendV22DsrContractTest',
    'BackendV22OpenTelemetryContractTest',
    'BackendV22OidcSsoContractTest'
  ]) {
    assert.match(validateLocal, new RegExp(contract));
    assert.match(ciWorkflow, new RegExp(contract));
  }
  assert.match(validateLocal, /backend v2\.2 commercial regression/);
  assert.match(ciWorkflow, /Backend v2\.2 commercial regression/);
});

test('v2.2 supply chain SBOM and license report are generated and gated', async () => {
  const [script, releaseGate, validateLocal, ciWorkflow] = await Promise.all([
    read('scripts/generate-sbom-license-report.mjs'),
    read('scripts/release-gate.sh'),
    read('scripts/validate-local.sh'),
    read('.github/workflows/ci.yml')
  ]);

  assert.match(script, /CycloneDX/);
  assert.match(script, /dependency-license-report\.json/);
  assert.match(releaseGate, /step_sbom_license/);
  assert.match(releaseGate, /sbom-license/);
  assert.match(validateLocal, /generate-sbom-license-report\.mjs/);
  assert.match(ciWorkflow, /Generate SBOM and dependency license report/);
  assert.match(ciWorkflow, /actions\/upload-artifact@v4/);
});

test('v2.2 supply chain report script writes CycloneDX and dependency license artifacts', async () => {
  const reportDir = join(tmpdir(), `mmmail-supply-chain-contract-${process.pid}`);

  try {
    await execFileAsync('node', ['scripts/generate-sbom-license-report.mjs'], {
      cwd: rootPath,
      maxBuffer: 10 * 1024 * 1024,
      env: { ...process.env, MMMAIL_SUPPLY_CHAIN_REPORT_DIR: reportDir }
    });
    const [sbom, licenseReport] = await Promise.all([
      readFile(join(reportDir, 'mmmail-sbom.cdx.json'), 'utf8'),
      readFile(join(reportDir, 'dependency-license-report.json'), 'utf8')
    ]);
    const parsedSbom = JSON.parse(sbom);
    const parsedLicenseReport = JSON.parse(licenseReport);

    assert.equal(parsedSbom.bomFormat, 'CycloneDX');
    assert.equal(parsedSbom.specVersion, '1.5');
    assert.ok(parsedSbom.components.some(component => component.name === 'mmmail-frontend-admin'));
    assert.ok(parsedLicenseReport.components.length > 0);
  } finally {
    await rm(reportDir, { recursive: true, force: true });
  }
});
