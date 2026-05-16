import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import assert from 'node:assert/strict';

const readText = path => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
const packageJson = JSON.parse(readText('package.json'));
const ciWorkflow = readFileSync(new URL('../../.github/workflows/ci.yml', import.meta.url), 'utf8');

test('v2.1.2 API type generation script is wired to openapi-typescript', () => {
  assert.equal(packageJson.scripts['gen:api'], 'node scripts/gen-api.mjs');
  assert.match(packageJson.devDependencies['openapi-typescript'], /\d/);

  const script = readText('scripts/gen-api.mjs');
  assert.match(script, /openapi-typescript/);
  assert.match(script, /src\/service\/api\/__generated__\/openapi\.d\.ts/);
  assert.match(script, /MMMAIL_OPENAPI_SOURCE/);
});

test('v2.1.2 CI rejects stale generated API types', () => {
  assert.match(ciWorkflow, /pnpm --dir frontend-admin install --frozen-lockfile/);
  assert.match(ciWorkflow, /pnpm --dir frontend-admin gen:api/);
  assert.match(ciWorkflow, /git diff --exit-code -- frontend-admin\/src\/service\/api\/__generated__/);
});
