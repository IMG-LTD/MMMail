import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const root = new URL('../', import.meta.url);

async function readText(path) {
  return readFile(new URL(path, root), 'utf8');
}

async function readJson(path) {
  return JSON.parse(await readText(path));
}

function assertCoverageScripts(pkg) {
  assert.match(pkg.scripts['test:unit'], /vitest run --coverage/);
  assert.match(pkg.scripts['test:component'], /vitest run --coverage/);
  assert.match(pkg.scripts['test:coverage'], /test:unit/);
  assert.match(pkg.scripts['test:coverage'], /test:component/);
}

function assertCoverageDependencies(pkg, name) {
  for (const dependency of ['vitest', '@vitest/coverage-v8', '@vue/test-utils', 'jsdom']) {
    assert.ok(pkg.devDependencies[dependency], `${name} missing ${dependency}`);
  }
}

test('frontend projects expose Vitest unit and component coverage gates', async () => {
  const frontend = await readJson('frontend-v2/package.json');
  const soybean = await readJson('frontend-admin/package.json');

  assertCoverageDependencies(frontend, 'frontend-v2');
  assertCoverageDependencies(soybean, 'frontend-admin');
  assertCoverageScripts(frontend);
  assertCoverageScripts(soybean);
});

test('coverage thresholds are documented in Vitest configs', async () => {
  const frontendConfig = await readText('frontend-v2/vitest.config.ts');
  const soybeanConfig = await readText('frontend-admin/vitest.config.ts');

  for (const config of [frontendConfig, soybeanConfig]) {
    assert.match(config, /UNIT_COVERAGE_THRESHOLD = 80/);
    assert.match(config, /COMPONENT_COVERAGE_THRESHOLD = (80|100)/);
    assert.match(config, /coverage:/);
    assert.match(config, /provider:\s*['"]v8['"]/);
  }
});

test('backend enforces JaCoCo coverage before CI can pass', async () => {
  const parentPom = await readText('backend/pom.xml');
  const serverPom = await readText('backend/mmmail-server/pom.xml');

  assert.match(parentPom, /jacoco-maven-plugin/);
  assert.match(parentPom, /jacoco.minimum/);
  assert.match(serverPom, /<goal>check<\/goal>/);
});

test('local and CI validation run product coverage gates and keep legacy frontend retired', async () => {
  const validateLocal = await readText('scripts/validate-local.sh');
  const ci = await readText('.github/workflows/ci.yml');

  assert.match(validateLocal, /mmmail admin v2\.1\.2 coverage gates/);
  assert.doesNotMatch(validateLocal, /frontend-v2 coverage gates/);
  assert.doesNotMatch(validateLocal, /validate-legacy-frontend-v2\.sh/);
  assert.match(validateLocal, /backend coverage gate/);
  assert.match(ci, /MMMail admin v2\.1\.2 coverage gates/);
  assert.doesNotMatch(ci, /Legacy frontend-v2 migration signal/);
  assert.doesNotMatch(ci, /validate-legacy-frontend-v2\.sh/);
  assert.match(ci, /Backend coverage gate/);
});

test('local and CI validation run root repository contract gates', async () => {
  const validateLocal = await readText('scripts/validate-local.sh');
  const ci = await readText('.github/workflows/ci.yml');

  assert.match(validateLocal, /root repository contract gates/);
  assert.doesNotMatch(validateLocal, /root v2\.1\.2 contract gates/);
  assert.match(validateLocal, /node --test tests\/\*\.test\.mjs/);
  assert.match(ci, /Root repository contract gates/);
  assert.doesNotMatch(ci, /Root v2\.1\.2 contract gates/);
  assert.match(ci, /node --test tests\/\*\.test\.mjs/);
});

test('coverage tooling dependency decision is recorded', async () => {
  const decisionLog = await readText('docs/superpowers/specs/2026-05-15-v212-decision-log.md');

  assert.match(decisionLog, /Coverage Tooling/);
  for (const dependency of ['vitest', '@vitest/coverage-v8', '@vue/test-utils', 'jsdom']) {
    assert.match(decisionLog, new RegExp(dependency.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
  }
  assert.match(decisionLog, /Soybean \/ Naive UI \/ `@sa\/\*`/);
  assert.match(decisionLog, /devDependencies/);
});
