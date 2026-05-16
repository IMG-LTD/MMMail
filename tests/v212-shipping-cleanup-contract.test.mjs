import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';

const rootUrl = new URL('../', import.meta.url);
const root = fileURLToPath(rootUrl);
const legacyAdminPath = ['soybean', 'admin', 'main'].join('-');

function pathUrl(path) {
  return new URL(path, rootUrl);
}

async function read(path) {
  return readFile(pathUrl(path), 'utf8');
}

function exists(path) {
  return existsSync(pathUrl(path));
}

function gitLines(args) {
  return execFileSync('git', args, { cwd: root, encoding: 'utf8' })
    .trim()
    .split('\n')
    .filter(Boolean);
}

function checkIgnored(path) {
  execFileSync('git', ['check-ignore', path], { cwd: root, encoding: 'utf8' });
}

function rgLegacyAdminRefs() {
  try {
    return execFileSync(
      'rg',
      [
        '-n',
        legacyAdminPath,
        'docs',
        'scripts',
        'tests',
        '.github',
        'frontend-v2',
        'README.md',
        'CONTRIBUTING.md',
        '-g',
        '!docs/v212-shipping-cleanup-spec.md',
        '-g',
        '!node_modules/**',
        '-g',
        '!.git/**',
        '-g',
        '!.claude/**',
        '-g',
        '!.worktrees/**'
      ],
      { cwd: root, encoding: 'utf8' }
    );
  } catch (error) {
    if (error.status === 1) return '';
    throw error;
  }
}

test('v2.1.2 shipping cleanup renames the admin frontend and removes legacy residues', () => {
  assert.equal(exists('frontend-admin/package.json'), true);
  assert.equal(exists(`${legacyAdminPath}/package.json`), false);

  for (const path of [
    '.tmp',
    '.superpowers',
    '.codex-tasks',
    '.gstack',
    'artifacts',
    'frontend',
    'docs/MMMail',
    'docs/MMMail.zip'
  ]) {
    assert.equal(exists(path), false, `${path} should be archived outside the repo`);
  }
});

test('v2.1.2 shipping cleanup keeps generated admin artifacts ignored', async () => {
  const gitignore = await read('.gitignore');

  for (const pattern of [
    'frontend-admin/node_modules/',
    'frontend-admin/dist/',
    'frontend-admin/coverage/',
    'frontend-admin/test-results/',
    'frontend-admin/playwright-report/',
    '.codex-tasks/',
    '.tmp/',
    '.superpowers/',
    '.gstack/'
  ]) {
    assert.match(gitignore, new RegExp(pattern.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
  }

  checkIgnored('frontend-admin/node_modules/.keep');
  checkIgnored('frontend-admin/dist/.keep');
  checkIgnored('.codex-tasks/.keep');
});

test('v2.1.2 shipping cleanup updates active repository references to frontend-admin', async () => {
  const files = [
    '.github/workflows/ci.yml',
    'scripts/release-gate.sh',
    'scripts/run-tests-docker.sh',
    'scripts/validate-local.sh',
    'tests/v213-closure-contract.test.mjs',
    'tests/v212-coverage-gates-contract.test.mjs'
  ];

  for (const file of files) {
    const content = await read(file);
    assert.doesNotMatch(content, new RegExp(legacyAdminPath), file);
    assert.match(content, /frontend-admin/, file);
  }

  assert.equal(rgLegacyAdminRefs(), '');
});

test('v2.1.2 shipping cleanup scopes imported admin git hooks to frontend-admin', async () => {
  const packageJson = JSON.parse(await read('frontend-admin/package.json'));
  const hooks = packageJson['simple-git-hooks'];

  assert.match(hooks['pre-commit'], /pnpm --dir frontend-admin typecheck/);
  assert.match(hooks['pre-commit'], /pnpm --dir frontend-admin exec oxfmt --check/);
  assert.match(hooks['commit-msg'], /cd frontend-admin && pnpm sa git-commit-verify/);
  assert.doesNotMatch(hooks['pre-commit'], /^pnpm typecheck/);
});

test('v2.1.2 shipping cleanup keeps release gate from leaving generated route type diffs', async () => {
  const [packageJson, releaseGate, normalizeScript] = await Promise.all([
    read('frontend-admin/package.json').then(JSON.parse),
    read('scripts/release-gate.sh'),
    read('frontend-admin/scripts/normalize-generated-types.mjs')
  ]);

  assert.match(packageJson.scripts.build, /node scripts\/normalize-generated-types\.mjs/);
  assert.match(packageJson.scripts['build:test'], /node scripts\/normalize-generated-types\.mjs/);
  assert.match(releaseGate, /release-gate-final-clean-diff\.log/);
  assert.match(releaseGate, /git diff --exit-code/);
  assert.match(normalizeScript, /elegant-router\.d\.ts/);
  assert.match(normalizeScript, /\[ \\t\]\+\$/);
});

test('v2.1.2 shipping cleanup keeps docker test runtime outside the repository tmp root', async () => {
  const dockerRunner = await read('scripts/run-tests-docker.sh');

  assert.match(dockerRunner, /MMMAIL_TEST_RUNTIME_DIR/);
  assert.match(dockerRunner, /mmmail-test-runtime/);
  assert.doesNotMatch(dockerRunner, /\$ROOT_DIR\/\.tmp/);
});

test('v2.1.2 shipping cleanup ingests first-time tracked code without dependency caches', () => {
  assert.ok(gitLines(['ls-files', 'frontend-admin']).length >= 200);
  assert.ok(gitLines(['ls-files', 'tests']).length >= 1);
  assert.ok(gitLines(['ls-files', 'ops']).length >= 1);

  assert.deepEqual(gitLines(['ls-files', 'frontend-admin/node_modules']), []);
  assert.deepEqual(gitLines(['ls-files', 'frontend-admin/dist']), []);
  assert.deepEqual(gitLines(['ls-files', 'frontend-admin/coverage']), []);
});

test('v2.1.2 shipping cleanup records completion in docs and changelog', async () => {
  const [spec, progress, changelog, readme] = await Promise.all([
    read('docs/v212-shipping-cleanup-spec.md'),
    read('docs/v212-progress-report.md'),
    read('CHANGELOG.md'),
    read('README.md')
  ]);

  assert.match(spec, /status: implemented/);
  assert.match(spec, /implemented_commit:/);
  assert.match(progress, /v2\.1\.2 上线清理完成/);
  assert.match(
    changelog,
    /Renamed legacy admin frontend to frontend-admin; moved dev-only artifacts to a local filesystem archive/
  );
  assert.match(readme, /frontend-admin/);
});
