import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 mail services expose external account contracts', async () => {
  const [service, types] = await Promise.all([read('src/service/api/mail.ts'), read('src/typings/api/mail.d.ts')]);

  for (const marker of [
    'listMailExternalAccounts',
    'createMailExternalAccount',
    'readMailExternalAccount',
    'updateMailExternalAccount',
    'deleteMailExternalAccount',
    'testMailExternalAccount',
    'syncMailExternalAccount'
  ]) {
    assert.match(service, new RegExp(marker));
  }

  for (const endpoint of [
    '/api/v1/mail/external-accounts',
    '/api/v1/mail/external-accounts/${accountId}',
    '/api/v1/mail/external-accounts/${accountId}/test',
    '/api/v1/mail/external-accounts/${accountId}/sync'
  ]) {
    assert.match(service, new RegExp(endpoint.replaceAll('/', '\\/').replaceAll('$', '\\$')));
  }

  for (const typeName of [
    'ExternalServer',
    'ExternalAccount',
    'ExternalAccountPayload',
    'ExternalAccountTest',
    'ExternalAccountSync'
  ]) {
    assert.match(types, new RegExp(`interface ${typeName}`));
  }
});

test('v2.1.2 mail page binds external account setup, test, and sync flows', async () => {
  const [page, panel] = await Promise.all([
    read('src/views/mail/index.vue'),
    read('src/views/mail/external/MailExternalAccountsPanel.vue')
  ]);
  const source = `${page}\n${panel}`;

  for (const marker of [
    'MailExternalAccountsPanel',
    'listMailExternalAccounts',
    'createMailExternalAccount',
    'updateMailExternalAccount',
    'deleteMailExternalAccount',
    'testMailExternalAccount',
    'syncMailExternalAccount',
    'externalAccounts',
    'externalAccountModel',
    'loadExternalAccounts',
    'submitExternalAccount'
  ]) {
    assert.match(source, new RegExp(marker));
  }
});

test('v2.1.2 mail external account labels are translated', async () => {
  const [appTypes, zhCN, enUS] = await Promise.all([
    read('src/typings/app.d.ts'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  for (const key of [
    'externalAccounts',
    'provider',
    'authMode',
    'imapHost',
    'smtpHost',
    'testConnection',
    'sync',
    'deleteAccount'
  ]) {
    assert.match(appTypes, new RegExp(`${key}: string`));
    assert.match(zhCN, new RegExp(`${key}: '`));
    assert.match(enUS, new RegExp(`${key}: '`));
  }
});
