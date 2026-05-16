import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 phase 4 exposes ready F-B service APIs', async () => {
  const [filters, drive, billing, apiIndex] = await Promise.all([
    read('src/service/api/mail-filters.ts'),
    read('src/service/api/drive.ts'),
    read('src/service/api/billing.ts'),
    read('src/service/api/index.ts')
  ]);

  assert.match(filters, /\/api\/v1\/mail-filters/);
  assert.match(filters, /\/api\/v1\/mail-filters\/preview/);
  assert.match(drive, /\/api\/v1\/drive\/files\/\$\{fileId\}\/versions/);
  assert.match(drive, /\/api\/v1\/drive\/files\/\$\{fileId\}\/versions\/\$\{versionId\}\/restore/);
  assert.match(drive, /\/api\/v2\/public-share\/capabilities/);
  assert.match(billing, /\/api\/v1\/suite\/pricing\/offers/);
  assert.match(billing, /\/api\/v1\/suite\/billing\/overview/);
  assert.match(billing, /\/api\/v1\/suite\/billing\/center/);
  assert.match(billing, /\/api\/v1\/suite\/billing\/subscription-actions/);
  assert.match(apiIndex, /export \* from '\.\/mail-filters'/);
  assert.match(apiIndex, /export \* from '\.\/billing'/);
});

test('v2.1.2 phase 4 binds ready F-B pages to real services', async () => {
  const [mailPage, mailRulesPanel, drivePage, adminPage] = await Promise.all([
    read('src/views/mail/index.vue'),
    read('src/views/mail/rules/MailRulesPanel.vue'),
    read('src/views/drive/index.vue'),
    read('src/views/admin/index.vue')
  ]);
  const mailRuleSource = `${mailPage}\n${mailRulesPanel}`;

  assert.match(mailRuleSource, /listMailFilters/);
  assert.match(mailRuleSource, /createMailFilter/);
  assert.match(mailRuleSource, /previewMailFilter/);
  assert.match(drivePage, /listDriveFileVersions/);
  assert.match(drivePage, /restoreDriveFileVersion/);
  assert.match(drivePage, /readPublicShareCapabilities/);
  assert.match(adminPage, /readSuiteBillingOverview/);
  assert.match(adminPage, /readSuiteBillingCenter/);
  assert.match(adminPage, /executeSuiteBillingSubscriptionAction/);
});

test('v2.1.2 phase 4 adds ready F-B i18n namespaces', async () => {
  const [appTypes, zhCN, zhTW, enUS] = await Promise.all([
    read('src/typings/app.d.ts'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/zh-tw.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  for (const key of ['mailFilters', 'driveVersions', 'billing']) {
    assert.match(appTypes, new RegExp(`${key}: \\{[\\s\\S]*title`));
  }

  for (const source of [zhCN, zhTW, enUS]) {
    assert.match(source, /mailFilters:/);
    assert.match(source, /driveVersions:/);
    assert.match(source, /billing:/);
  }
});
