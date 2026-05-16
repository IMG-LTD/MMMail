import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import test from 'node:test';

const read = path => readFileSync(path, 'utf8');

test('v2.2 DSR backend and compliance documentation surfaces exist', () => {
  [
    'backend/mmmail-server/src/main/java/com/mmmail/server/controller/DsrRequestController.java',
    'backend/mmmail-server/src/main/java/com/mmmail/server/compliance/DsrRequestService.java',
    'backend/mmmail-server/src/main/java/com/mmmail/server/compliance/DsrExecutionService.java',
    'backend/mmmail-server/src/test/java/com/mmmail/server/BackendV22DsrContractTest.java',
    'docs/compliance/dsr.md',
    'docs/compliance/data-inventory.yaml',
    'scripts/validate-dsr-inventory.mjs'
  ].forEach(path => assert.equal(existsSync(path), true, `${path} should exist`));
});

test('v2.2 DSR requests are guarded by Business dsr.requests entitlement', () => {
  const controller = read('backend/mmmail-server/src/main/java/com/mmmail/server/controller/DsrRequestController.java');
  const featureCode = read('backend/mmmail-server/src/main/java/com/mmmail/server/commercial/FeatureCode.java');
  const licenseTest = read('backend/mmmail-server/src/test/java/com/mmmail/server/BackendV22LicenseVerifierContractTest.java');

  assert.match(controller, /enforceFeature\(request,\s*orgId,\s*FeatureCode\.DSR_REQUESTS\)/);
  assert.match(featureCode, /DSR_REQUESTS\("dsr\.requests",\s*Edition\.BUSINESS\)/);
  assert.match(licenseTest, /dsr\.requests/);
});

test('v2.2 data inventory gate covers every declared table with explicit DSR metadata', () => {
  const result = spawnSync('node', ['scripts/validate-dsr-inventory.mjs'], {
    encoding: 'utf8',
    env: { ...process.env, NO_COLOR: '1' }
  });

  assert.equal(result.status, 0, result.stderr || result.stdout);
  assert.match(result.stdout, /validated \d+ data inventory entries/);
});

test('v2.2 DSR gate is wired into local, release, CI, and spec', () => {
  const validateLocal = read('scripts/validate-local.sh');
  const releaseGate = read('scripts/release-gate.sh');
  const ci = read('.github/workflows/ci.yml');
  const spec = read('docs/v22-open-source-commercial-spec.md');
  const governanceTests = [
    'tests/v22-repository-governance-contract.test.mjs',
    'tests/v22-repository-governance-validation-contract.test.mjs'
  ].map(read).join('\n');

  assert.match(validateLocal, /validate-dsr-inventory\.mjs/);
  assert.match(validateLocal, /BackendV22DsrContractTest/);
  assert.match(releaseGate, /dsr-inventory/);
  assert.match(ci, /BackendV22DsrContractTest/);
  assert.match(spec, /\| BUS-03 \| done \|/);
  assert.match(governanceTests, /BackendV22DsrContractTest/);
});
