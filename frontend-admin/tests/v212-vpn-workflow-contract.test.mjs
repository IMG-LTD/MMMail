import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 vpn service exposes settings, profiles CRUD, connect, and session history APIs', async () => {
  const service = await read('src/service/api/vpn.ts');

  assert.match(service, /readVpnSettings/);
  assert.match(service, /updateVpnSettings/);
  assert.match(service, /createVpnProfile/);
  assert.match(service, /updateVpnProfile/);
  assert.match(service, /deleteVpnProfile/);
  assert.match(service, /listVpnSessionHistory/);
  assert.match(service, /connectVpnSession/);
  assert.match(service, /\/api\/v1\/vpn\/sessions\/history/);
  assert.match(service, /\/api\/v1\/vpn\/sessions\/connect/);
});

test('v2.1.2 vpn routes expose servers, profiles, sessions, and settings entry points', async () => {
  const routes = await read('src/router/routes/custom-routes.ts');

  for (const routeName of ['vpn_servers', 'vpn_profiles', 'vpn_sessions', 'vpn_settings']) {
    assert.match(routes, new RegExp(`name: '${routeName}'`));
  }

  assert.match(routes, /path: '\/vpn\/servers'/);
  assert.match(routes, /path: '\/vpn\/profiles'/);
  assert.match(routes, /path: '\/vpn\/sessions'/);
  assert.match(routes, /path: '\/vpn\/settings'/);
  assert.match(routes, /requires: \['VPN'\]/);
  assert.match(routes, /featureFlag: 'feat\.vpn\.enabled'/);
});

test('v2.1.2 vpn page binds quick connect, server connect, profile CRUD, and settings workflows', async () => {
  const page = await read('src/views/vpn/index.vue');

  assert.match(page, /readVpnSettings/);
  assert.match(page, /updateVpnSettings/);
  assert.match(page, /createVpnProfile/);
  assert.match(page, /updateVpnProfile/);
  assert.match(page, /deleteVpnProfile/);
  assert.match(page, /listVpnSessionHistory/);
  assert.match(page, /connectVpnSession/);
  assert.match(page, /NProgress/);
  assert.match(page, /profileModel/);
  assert.match(page, /settingsModel/);
});
