import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 phase 3 exposes new module service APIs for existing backend controllers', async () => {
  const [contacts, wallet, vpn, meet, authenticator, apiIndex] = await Promise.all([
    read('src/service/api/contacts.ts'),
    read('src/service/api/wallet.ts'),
    read('src/service/api/vpn.ts'),
    read('src/service/api/meet.ts'),
    read('src/service/api/authenticator.ts'),
    read('src/service/api/index.ts')
  ]);

  assert.match(contacts, /\/api\/v1\/contacts/);
  assert.match(contacts, /\/api\/v1\/contact-groups/);
  assert.match(wallet, /\/api\/v1\/wallet\/accounts/);
  assert.match(wallet, /\/api\/v1\/wallet\/transactions/);
  assert.match(vpn, /\/api\/v1\/vpn\/servers/);
  assert.match(vpn, /\/api\/v1\/vpn\/sessions\/quick-connect/);
  assert.match(meet, /\/api\/v1\/meet\/rooms/);
  assert.match(authenticator, /\/api\/v1\/authenticator\/entries/);

  for (const moduleName of ['contacts', 'wallet', 'vpn', 'meet', 'authenticator']) {
    assert.match(apiIndex, new RegExp(`export \\* from '\\./${moduleName}'`));
  }
});

test('v2.1.2 phase 3 new module pages bind services or explicit unavailable state', async () => {
  const [contactsPage, walletPage, vpnPage, meetPage, authenticatorPage, communityPage] = await Promise.all([
    read('src/views/contacts/index.vue'),
    read('src/views/wallet/index.vue'),
    read('src/views/vpn/index.vue'),
    read('src/views/meet/index.vue'),
    read('src/views/security/authenticator/index.vue'),
    read('src/views/community/index.vue')
  ]);

  assert.match(contactsPage, /listContacts/);
  assert.match(contactsPage, /createContact/);
  assert.match(contactsPage, /listContactGroups/);
  assert.match(walletPage, /useWalletStore/);
  assert.match(walletPage, /sendModel/);
  assert.match(walletPage, /receiveModel/);
  assert.match(vpnPage, /listVpnServers/);
  assert.match(vpnPage, /quickConnectVpn/);
  assert.match(vpnPage, /disconnectVpn/);
  assert.match(meetPage, /listMeetRooms/);
  assert.match(meetPage, /createMeetRoom/);
  assert.match(meetPage, /readMeetAccessOverview/);
  assert.match(authenticatorPage, /listAuthenticatorEntries/);
  assert.match(authenticatorPage, /createAuthenticatorEntry/);
  assert.match(authenticatorPage, /generateAuthenticatorCode/);
  assert.match(communityPage, /listCommunityPosts/);
  assert.match(communityPage, /createCommunityPost/);
  assert.match(communityPage, /createCommunityReport/);
});

test('v2.1.2 phase 3 adds new module i18n namespaces', async () => {
  const [appTypes, zhCN, zhTW, enUS] = await Promise.all([
    read('src/typings/app.d.ts'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/zh-tw.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  for (const key of ['contacts', 'wallet', 'vpn', 'meet', 'authenticator', 'community']) {
    assert.match(appTypes, new RegExp(`${key}: \\{[\\s\\S]*title`));
  }

  for (const source of [zhCN, zhTW, enUS]) {
    for (const key of ['contacts', 'wallet', 'vpn', 'meet', 'authenticator', 'community']) {
      assert.match(source, new RegExp(`${key}:`));
    }
  }
});
