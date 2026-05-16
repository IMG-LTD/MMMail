import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const root = new URL('../', import.meta.url);

const controllerDocs = [
  ['CommunityController', ['/api/v1/community/posts', '/api/v1/community/reports', 'moderation']],
  ['SearchController', ['/api/v1/search', '/suggestions', '/reindex/{moduleType}']],
  ['DomainController', ['/api/v1/domains', '/dns-records', '/diagnostics']],
  ['WebPushController', ['/api/v1/web-push/vapid-public-key', '/subscriptions', '/test']],
  ['SecurityEventController', ['/api/v1/security/events', '/ack', '/api/v1/admin/security/anomalies']],
  ['MailExternalAccountController', ['/api/v1/mail/external-accounts', '/test', '/sync']],
  ['SheetsFormulaController', ['/api/v1/sheets/{workbookId}/cells/evaluate', '/dependency-graph', '/recalculate']],
  ['CollabController', ['/api/v1/collab/{resourceType}/{resourceId}/snapshot', 'WebSocket updates', '/awareness']],
  ['NotificationRealtimeController', ['/api/v2/notifications/since', 'cursor', 'WebSocket']],
  ['V21OpsController command panel', ['/api/v2/command-center/catalog', '/recents', '/quick-search']]
];

async function readText(path) {
  return readFile(new URL(path, root), 'utf8');
}

test('v2.1.2 API spec documents every new backend controller surface', async () => {
  const apiSpec = await readText('docs/api-spec.md');

  assert.match(apiSpec, /^## 5\. v2\.1\.2 新增控制器与端点/m);
  assert.match(apiSpec, /docs\/v212-migration-spec\.md §26\.6/);

  for (const [controllerName, snippets] of controllerDocs) {
    assert.match(apiSpec, new RegExp(`^### ${controllerName}$`, 'm'), `${controllerName} section missing`);
    for (const snippet of snippets) {
      assert.ok(apiSpec.includes(snippet), `${controllerName} missing ${snippet}`);
    }
  }
});
