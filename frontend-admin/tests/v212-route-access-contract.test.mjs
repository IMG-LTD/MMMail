import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 route meta patches mirror the EntitlementGate default table', async () => {
  const [accessMeta, routes] = await Promise.all([
    read('src/router/routes/access-meta.ts'),
    read('src/router/routes/custom-routes.ts')
  ]);

  assert.match(accessMeta, /wallet:\s*\{[\s\S]*orgRequired:\s*true[\s\S]*premiumOnly:\s*true/);
  assert.match(accessMeta, /wallet:\s*\{[\s\S]*featureFlag:\s*'feat\.wallet\.enabled'[\s\S]*requires:\s*\['WALLET'\]/);
  assert.match(accessMeta, /wallet:\s*\{[\s\S]*fallback:\s*'upgrade'/);

  assert.match(accessMeta, /vpn:\s*\{[\s\S]*orgRequired:\s*false[\s\S]*premiumOnly:\s*true/);
  assert.match(accessMeta, /vpn:\s*\{[\s\S]*featureFlag:\s*'feat\.vpn\.enabled'[\s\S]*requires:\s*\['VPN'\]/);

  assert.match(accessMeta, /meet:\s*\{[\s\S]*orgRequired:\s*false[\s\S]*premiumOnly:\s*true/);
  assert.match(accessMeta, /meet:\s*\{[\s\S]*featureFlag:\s*'feat\.meet\.enabled'[\s\S]*requires:\s*\['MEET'\]/);

  assert.match(accessMeta, /contacts:\s*\{[\s\S]*orgRequired:\s*false[\s\S]*requires:\s*\[\]/);
  assert.match(
    accessMeta,
    /community:\s*\{[\s\S]*orgRequired:\s*false[\s\S]*featureFlag:\s*'feat\.community\.enabled'/
  );
  assert.match(
    accessMeta,
    /integrations_simplelogin:\s*\{[\s\S]*orgRequired:\s*true[\s\S]*requires:\s*\['SIMPLE_LOGIN'\]/
  );
  assert.match(routes, /name:\s*'admin_billing'[\s\S]*roles:\s*\['BILLING_ADMIN'\][\s\S]*fallback:\s*'forbidden'/);
});

test('v2.1.2 access checks re-evaluate when the active organization changes', async () => {
  const [authStore, orgStore] = await Promise.all([
    read('src/store/modules/auth/index.ts'),
    read('src/store/modules/org/index.ts')
  ]);

  assert.match(authStore, /const currentOrgId = computed\(\(\) => orgStore\.currentOrgId\)/);
  assert.doesNotMatch(authStore, /const currentOrgId = ref\(''\)/);
  assert.match(authStore, /function hasOrg[\s\S]*currentOrgId\.value/);
  assert.match(orgStore, /function setCurrentOrgId\(orgId: string\)/);
});

test('v2.1.2 custom underscore routes can still use layout-view single level components', async () => {
  const routes = await read('src/router/routes/index.ts');

  assert.match(routes, /function isCustomSingleLevelRoute/);
  assert.match(routes, /component\.includes\('\$'\)/);
  assert.match(routes, /function transformCustomSingleLevelRoute/);
});
