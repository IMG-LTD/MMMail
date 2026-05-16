import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 bundle budget gate uses analyzer output and size-limit', async () => {
  const [packageJsonSource, pluginSource, budgetScript, ciWorkflow] = await Promise.all([
    read('package.json'),
    read('build/plugins/bundle-analyzer.ts'),
    read('scripts/check-bundle-budget.mjs'),
    read('../.github/workflows/ci.yml')
  ]);
  const packageJson = JSON.parse(packageJsonSource);

  assert.match(packageJson.scripts['check:bundle-budget'], /check-bundle-budget\.mjs/);
  assert.match(packageJson.scripts['check:bundle-budget'], /size-limit/);
  assert.match(packageJson.devDependencies['size-limit'], /\d/);
  assert.match(packageJson.devDependencies['@size-limit/file'], /\d/);
  assert.match(packageJson.devDependencies['vite-plugin-bundle-analyzer'], /\d/);
  assert.match(packageJsonSource, /"size-limit": \[/);
  assert.match(pluginSource, /name: 'vite-plugin-bundle-analyzer'/);
  assert.match(pluginSource, /bundle-analyzer\.json/);
  assert.match(budgetScript, /workspace-first-screen/);
  assert.match(budgetScript, /limitKb: 500/);
  assert.match(ciWorkflow, /pnpm --dir frontend-admin check:bundle-budget/);
});
