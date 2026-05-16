import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 phase 4 exposes custom domain and web push service APIs', async () => {
  const [admin, settings] = await Promise.all([read('src/service/api/admin.ts'), read('src/service/api/settings.ts')]);

  assert.match(admin, /\/api\/v1\/domains/);
  assert.match(admin, /\/api\/v1\/domains\/\$\{domainId\}\/dns-records/);
  assert.match(admin, /\/api\/v1\/domains\/\$\{domainId\}\/diagnostics/);
  assert.match(admin, /\/api\/v1\/domains\/\$\{domainId\}\/verify/);
  assert.match(settings, /\/api\/v1\/web-push\/vapid-public-key/);
  assert.match(settings, /\/api\/v1\/web-push\/subscriptions/);
  assert.match(settings, /\/api\/v1\/web-push\/test/);
});

test('v2.1.2 phase 4 settings page binds custom domain and web push workflows', async () => {
  const settingsPage = await read('src/views/settings/index.vue');

  assert.match(settingsPage, /listAdminDomains/);
  assert.match(settingsPage, /createAdminDomain/);
  assert.match(settingsPage, /listAdminDomainDnsRecords/);
  assert.match(settingsPage, /readAdminDomainDiagnostics/);
  assert.match(settingsPage, /verifyAdminDomain/);
  assert.match(settingsPage, /readWebPushPublicKey/);
  assert.match(settingsPage, /listWebPushSubscriptions/);
  assert.match(settingsPage, /registerWebPushSubscription/);
  assert.match(settingsPage, /deleteWebPushSubscription/);
  assert.match(settingsPage, /testWebPushSubscription/);
});

test('v2.1.2 phase 4 adds settings domain and web push i18n namespaces', async () => {
  const [appTypes, zhCN, zhTW, enUS] = await Promise.all([
    read('src/typings/app.d.ts'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/zh-tw.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  for (const key of ['domains', 'webPush']) {
    assert.match(appTypes, new RegExp(`${key}: \\{[\\s\\S]*title`));
  }

  for (const source of [zhCN, zhTW, enUS]) {
    assert.match(source, /domains:/);
    assert.match(source, /webPush:/);
  }
});
