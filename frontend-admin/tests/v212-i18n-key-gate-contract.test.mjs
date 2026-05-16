import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 i18n key parity is enforced for all shipped locales', async () => {
  const [packageJsonSource, script, ciWorkflow] = await Promise.all([
    read('package.json'),
    read('scripts/check-i18n-keys.ts'),
    read('../.github/workflows/ci.yml')
  ]);
  const packageJson = JSON.parse(packageJsonSource);

  assert.equal(packageJson.scripts['check:i18n'], 'tsx scripts/check-i18n-keys.ts');
  assert.match(script, /zhCN/);
  assert.match(script, /enUS/);
  assert.match(script, /zhTW/);
  assert.match(script, /flatten/);
  assert.match(ciWorkflow, /pnpm --dir frontend-admin check:i18n/);
});
