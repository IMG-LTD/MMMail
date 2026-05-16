import assert from 'node:assert/strict';
import { execFile } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { promisify } from 'node:util';

const root = new URL('../', import.meta.url);
const rootPath = fileURLToPath(root);
const execFileAsync = promisify(execFile);
const requiredTrackedDocs = [
  'docs/superpowers/specs/2026-05-15-v212-decision-log.md',
  'docs/superpowers/specs/2026-05-15-v212-module-design-coverage.md',
  'docs/superpowers/specs/2026-05-15-collab-sheets-board-design.md'
];
const requiredTrackedFrontendEnv = ['frontend-admin/.env', 'frontend-admin/.env.test'];

async function read(path) {
  return readFile(new URL(path, root), 'utf8');
}

async function trackedFiles(paths) {
  const { stdout } = await execFileAsync('git', ['ls-files', ...paths], {
    cwd: rootPath,
    maxBuffer: 1024 * 1024
  });
  return new Set(stdout.split('\n').filter(Boolean));
}

test('v2.2 CI toolchain uses frontend-admin engine-compatible pnpm', async () => {
  const [ciWorkflow, packageJson] = await Promise.all([
    read('.github/workflows/ci.yml'),
    read('frontend-admin/package.json').then(JSON.parse)
  ]);

  assert.equal(packageJson.engines.pnpm, '>=10.5.0');
  assert.match(ciWorkflow, /MMMAIL_PNPM_VERSION: "10\.5\.0"/);
  assert.match(ciWorkflow, /pnpm\/action-setup@v6/);
  assert.doesNotMatch(ciWorkflow, /version:\s*9\b/);
});

test('v2.2 workflows use Node 24-compatible action majors', async () => {
  const workflows = [
    await read('.github/workflows/ci.yml'),
    await read('.github/workflows/images.yml'),
    await read('.github/workflows/dco.yml')
  ].join('\n');

  for (const action of [
    'actions/checkout@v4',
    'actions/setup-node@v4',
    'actions/setup-java@v4',
    'actions/upload-artifact@v4',
    'actions/cache/restore@v4',
    'actions/cache/save@v4',
    'actions/github-script@v7',
    'pnpm/action-setup@v4',
    'docker/setup-qemu-action@v3',
    'docker/setup-buildx-action@v3',
    'docker/login-action@v3',
    'docker/metadata-action@v5',
    'docker/build-push-action@v6',
    'azure/setup-helm@v4'
  ]) {
    assert.doesNotMatch(workflows, new RegExp(action.replaceAll('/', '\\/')));
  }
});

test('frontend CI job declares Java 21 before Docker-backed e2e', async () => {
  const ciWorkflow = await read('.github/workflows/ci.yml');
  const javaSetupIndex = ciWorkflow.indexOf('Setup Java for frontend e2e');
  const frontendE2eIndex = ciWorkflow.indexOf('pnpm --dir frontend-admin test:e2e');

  assert.ok(javaSetupIndex > -1, 'frontend job must install Java 21 before e2e');
  assert.ok(frontendE2eIndex > javaSetupIndex, 'frontend e2e must run after Java setup');
  assert.match(
    ciWorkflow.slice(javaSetupIndex, frontendE2eIndex),
    /actions\/setup-java@v5[\s\S]*distribution: temurin[\s\S]*java-version: 21/
  );
});

test('v2.1.2 contract fixture docs are tracked with the contracts that read them', async () => {
  const tracked = await trackedFiles(requiredTrackedDocs);

  for (const path of requiredTrackedDocs) {
    assert.equal(tracked.has(path), true, `${path} must be tracked`);
  }
});

test('frontend CI declares direct toolchain dependencies', async () => {
  const packageJson = JSON.parse(await read('frontend-admin/package.json'));

  assert.equal(packageJson.devDependencies['@iconify/utils'], '3.1.3');
  assert.equal(packageJson.devDependencies['chrome-launcher'], '1.2.1');
});

test('frontend API type generation formats generated output before CI diff guard', async () => {
  const [packageJson, script] = await Promise.all([
    read('frontend-admin/package.json').then(JSON.parse),
    read('frontend-admin/scripts/gen-api.mjs')
  ]);

  assert.equal(packageJson.devDependencies.oxfmt, '^0.49.0');
  assert.match(script, /openapi-typescript/);
  assert.match(script, /oxfmt/);
  assert.match(script, /src\/service\/api\/__generated__\/openapi\.d\.ts/);
});

test('frontend Docker build tracks non-secret Vite env defaults', async () => {
  const [gitIgnore, dockerIgnore] = await Promise.all([read('.gitignore'), read('.dockerignore')]);
  const tracked = await trackedFiles(requiredTrackedFrontendEnv);

  for (const path of requiredTrackedFrontendEnv) {
    assert.equal(tracked.has(path), true, `${path} must be tracked`);
    assert.match(gitIgnore, new RegExp(`^!${path.replaceAll('.', '\\.')}$`, 'm'));
  }

  assert.match(dockerIgnore, /^\/\.env$/m);
  assert.match(dockerIgnore, /^\/\.env\.test$/m);
  assert.doesNotMatch(dockerIgnore, /^frontend-admin\/\.env$/m);
  assert.doesNotMatch(dockerIgnore, /^frontend-admin\/\.env\.test$/m);
});

test('Docker build context excludes generated and local agent artifacts', async () => {
  const dockerIgnore = await read('.dockerignore');

  for (const pattern of [
    'frontend-admin/node_modules',
    'frontend-admin/dist',
    'frontend-admin/coverage',
    '.claude',
    '.tools',
    'artifacts',
    'docs/assets'
  ]) {
    assert.match(dockerIgnore, new RegExp(`^${pattern.replaceAll('.', '\\.')}$`, 'm'));
  }
});
