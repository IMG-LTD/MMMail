import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 command panel service binds catalog, pin, recents, and quick search contracts', async () => {
  const [apiIndex, commandApi, commandTypes] = await Promise.all([
    read('src/service/api/index.ts'),
    read('src/service/api/command-center.ts'),
    read('src/typings/api/expanded.d.ts')
  ]);

  assert.match(apiIndex, /export \* from '\.\/command-center'/);

  for (const marker of [
    'listCommandPanelCatalog',
    'listCommandPanelRecents',
    'pinCommandPanelCommand',
    'quickSearchCommandPanel'
  ]) {
    assert.match(commandApi, new RegExp(marker));
  }

  for (const endpoint of [
    '/api/v2/command-center/catalog',
    '/api/v2/command-center/recents',
    '/api/v2/command-center/pin',
    '/api/v2/command-center/quick-search'
  ]) {
    assert.match(commandApi, new RegExp(endpoint.replaceAll('/', '\\/')));
  }

  assert.match(commandApi, /method: 'post'/);

  for (const typeName of ['CatalogItem', 'CommandAction', 'Preference', 'Recent', 'QuickSearchItem']) {
    assert.match(commandTypes, new RegExp(`interface ${typeName}`));
  }
});

test('v2.1.2 command center page provides actionable command panel interactions', async () => {
  const commandPage = await read('src/views/command-center/index.vue');

  for (const marker of [
    'listCommandPanelCatalog',
    'listCommandPanelRecents',
    'pinCommandPanelCommand',
    'quickSearchCommandPanel'
  ]) {
    assert.match(commandPage, new RegExp(marker));
  }

  assert.match(commandPage, /useRouter/);
  assert.match(commandPage, /router\.push/);
  assert.match(commandPage, /action\.payload\.routePath/);
  assert.match(commandPage, /NInput/);
  assert.match(commandPage, /NButton/);
  assert.match(commandPage, /NTabs/);
  assert.match(commandPage, /NDataTable/);
});
