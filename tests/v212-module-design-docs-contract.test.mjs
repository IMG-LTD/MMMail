import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const root = new URL('../', import.meta.url);
const designDocPath = 'docs/superpowers/specs/2026-05-15-v212-module-design-coverage.md';
const collabPlaceholderPath = 'docs/superpowers/specs/2026-05-15-collab-sheets-board-design.md';

const requiredModules = [
  ['Wallet', ['entitlement: WALLET', 'featureFlag: feat.wallet.enabled', '/api/v1/wallet']],
  ['VPN', ['entitlement: VPN', 'featureFlag: feat.vpn.enabled', '/api/v1/vpn']],
  ['Meet', ['entitlement: MEET', '/api/v1/meet', '/api/v1/public/meet']],
  ['Contacts', ['/api/v1/contacts', '/api/v1/contact-groups', 'CSV import']],
  ['SimpleLogin', ['featureFlag: feat.simplelogin.enabled', '/api/v1/simplelogin']],
  ['Standard Notes', ['featureFlag: feat.notes.enabled', '/api/v1/standard-notes']],
  ['Authenticator', ['/api/v1/authenticator', 'PIN', 'TOTP']],
  ['Mail Rules', ['/api/v1/mail/rules', 'preview', 'enable toggle']],
  ['Mail Drag', ['drag', 'folder', 'label']],
  ['Drive Versions', ['version history', 'restore', 'compare']],
  ['Drive E2EE Share', ['fragment-only', 'readable-share', 'key material']],
  ['Domain', ['/api/v1/domains', 'dns-records', 'diagnostics']],
  ['Web Push', ['/api/v1/web-push', 'vapid-public-key', 'subscriptions']],
  ['Admin Billing', ['/api/v1/billing', 'quote', 'subscription']],
  ['Community', ['featureFlag: feat.community.enabled', 'posts', 'reports']],
  ['Search', ['/api/v1/search', 'reindex', 'permission filter']],
  ['Command Panel', ['/api/v2/command-center', 'catalog', 'recents']],
  ['Calendar Subscriptions', ['CalDAV', 'ICS', 'calendar subscriptions']],
  ['Calendar RRULE', ['RRULE', 'thisAndFollowing', 'recurrence']],
  ['Mail External Accounts', ['IMAP', 'SMTP', 'external accounts']],
  ['Collab CRDT', ['CRDT', 'snapshot', 'awareness']],
  ['Collab Sheets Board Placeholder', ['2026-05-15-collab-sheets-board-design.md', 'v2.1.3', 'placeholder']],
  ['Sheets Formula', ['formula', 'dependency graph', 'recalculate']],
  ['Collaboration Board', ['lexorank', 'task move', 'board']],
  ['Notification Realtime', ['WebSocket', 'since cursor', 'replay']],
  ['Login Security', ['login anomaly', 'risk', 'security events']]
];

async function readText(path) {
  return readFile(new URL(path, root), 'utf8');
}

test('v2.1.2 module design coverage document exists under superpowers specs', async () => {
  const designDoc = await readText(designDocPath);

  assert.match(designDoc, /^# v2\.1\.2 Module Design Coverage/m);
  assert.match(designDoc, /docs\/v212-migration-spec\.md §26\.6/);
  assert.match(designDoc, /No mock success paths/);
  assert.match(designDoc, /No silent fallback/);
});

test('v2.1.2 module design coverage lists every new module and placeholder item', async () => {
  const designDoc = await readText(designDocPath);

  for (const [moduleName, keywords] of requiredModules) {
    assert.match(designDoc, new RegExp(`^### ${moduleName}$`, 'm'), `${moduleName} section missing`);
    for (const keyword of keywords) {
      assert.ok(designDoc.includes(keyword), `${moduleName} missing ${keyword}`);
    }
  }
});

test('v2.1.2 module design coverage defines shared design review axes', async () => {
  const designDoc = await readText(designDocPath);

  for (const heading of ['Runtime contracts', 'State and error model', 'Access and audit', 'Verification']) {
    assert.match(designDoc, new RegExp(`^## ${heading}$`, 'm'));
  }
});

test('v2.1.2 collab sheets and board deferred work has an explicit sub-spec placeholder', async () => {
  const placeholder = await readText(collabPlaceholderPath);

  assert.match(placeholder, /^# Collab Sheets and Board v2\.1\.3 Placeholder Design/m);
  assert.match(placeholder, /docs\/v212-migration-spec\.md §18\.4\.3/);
  assert.match(placeholder, /Sheets/);
  assert.match(placeholder, /board/);
  assert.match(placeholder, /CRDT/);
  assert.match(placeholder, /No mock success paths/);
  assert.match(placeholder, /No silent fallback/);
});
