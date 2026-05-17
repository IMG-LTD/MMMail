import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

async function read(path) {
  return readFile(new URL(`../${path}`, import.meta.url), 'utf8');
}

test('v2.1.2 browser gates are wired into package scripts and tooling', async () => {
  const packageJson = JSON.parse(await read('package.json'));

  assert.equal(packageJson.scripts['test:e2e'], 'playwright test');
  assert.equal(packageJson.scripts['test:lighthouse'], 'node scripts/run-lighthouse.mjs');
  assert.match(packageJson.devDependencies['@playwright/test'], /\d/);
  assert.match(packageJson.devDependencies.lighthouse, /\d/);
});

test('v2.1.2 browser gates cover auth shell flows and Lighthouse threshold', async () => {
  const [playwrightSpec, lighthouseScript] = await Promise.all([
    read('e2e/v212-auth-shell.spec.ts'),
    read('scripts/run-lighthouse.mjs')
  ]);

  assert.match(playwrightSpec, /fetchLogin/);
  assert.match(playwrightSpec, /fetchRegister/);
  assert.match(playwrightSpec, /api\/v2\/auth\/login/);
  assert.match(playwrightSpec, /api\/v2\/auth\/register/);
  assert.match(lighthouseScript, /MIN_LIGHTHOUSE_SCORE\s*=\s*80/);
  assert.match(lighthouseScript, /score <= MIN_LIGHTHOUSE_SCORE/);
  assert.match(lighthouseScript, /must be greater than/);
  assert.match(lighthouseScript, /preset:\s*'desktop'/);
  assert.match(lighthouseScript, /performance/);
  assert.match(lighthouseScript, /CLEANUP_TIMEOUT_MS/);
  assert.match(lighthouseScript, /Promise\.race/);
  assert.match(lighthouseScript, /process\.exit\(0\)/);
});

test('v2.1.2 browser gates are part of CI and validate-local', async () => {
  const [workflow, validateLocal] = await Promise.all([
    read('../.github/workflows/ci.yml'),
    read('../scripts/validate-local.sh')
  ]);

  assert.match(workflow, /pnpm --dir frontend-admin test:e2e/);
  assert.match(workflow, /pnpm --dir frontend-admin test:lighthouse/);
  assert.match(validateLocal, /pnpm --dir frontend-admin test:e2e/);
  assert.match(validateLocal, /pnpm --dir frontend-admin test:lighthouse/);
});
