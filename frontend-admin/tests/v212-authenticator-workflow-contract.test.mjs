import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 authenticator service exposes CRUD, PIN, QR import, and backup APIs', async () => {
  const service = await read('src/service/api/authenticator.ts');

  assert.match(service, /readAuthenticatorEntry/);
  assert.match(service, /updateAuthenticatorEntry/);
  assert.match(service, /deleteAuthenticatorEntry/);
  assert.match(service, /importAuthenticatorEntries/);
  assert.match(service, /exportAuthenticatorEntries/);
  assert.match(service, /exportAuthenticatorBackup/);
  assert.match(service, /importAuthenticatorBackup/);
  assert.match(service, /updateAuthenticatorSecurity/);
  assert.match(service, /verifyAuthenticatorPin/);
  assert.match(service, /importAuthenticatorQrImage/);
  assert.match(service, /\/api\/v1\/authenticator\/import\/qr-image/);
});

test('v2.1.2 authenticator routes expose detail, import, backup, and settings entry points', async () => {
  const routes = await read('src/router/routes/custom-routes.ts');

  for (const routeName of [
    'security_authenticator_detail',
    'security_authenticator_import',
    'security_authenticator_backup',
    'security_authenticator_settings'
  ]) {
    assert.match(routes, new RegExp(`name: '${routeName}'`));
  }

  assert.match(routes, /path: '\/security\/authenticator\/:entryId'/);
  assert.match(routes, /path: '\/security\/authenticator\/import'/);
  assert.match(routes, /path: '\/security\/authenticator\/backup'/);
  assert.match(routes, /path: '\/security\/authenticator\/settings'/);
});

test('v2.1.2 authenticator page binds grid codes, PIN gate, QR import, and backup workflows', async () => {
  const page = await read('src/views/security/authenticator/index.vue');

  assert.match(page, /NProgress/);
  assert.match(page, /generatedCodes/);
  assert.match(page, /generateCodeForEntry/);
  assert.match(page, /verifyAuthenticatorPin/);
  assert.match(page, /updateAuthenticatorSecurity/);
  assert.match(page, /importAuthenticatorQrImage/);
  assert.match(page, /importAuthenticatorEntries/);
  assert.match(page, /exportAuthenticatorBackup/);
  assert.match(page, /importAuthenticatorBackup/);
  assert.match(page, /pinModel/);
  assert.match(page, /qrImportModel/);
});
