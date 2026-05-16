import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 global search uses the global search backend contract', async () => {
  const [apiIndex, searchApi, searchTypes, searchModal] = await Promise.all([
    read('src/service/api/index.ts'),
    read('src/service/api/search.ts'),
    read('src/typings/api/search.d.ts'),
    read('src/layouts/modules/global-search/components/search-modal.vue')
  ]);

  assert.match(apiIndex, /export \* from '\.\/search'/);
  assert.match(searchApi, /readSearchSuggestions/);
  assert.match(searchApi, /readSearchResults/);
  assert.match(searchApi, /\/api\/v1\/search\/suggestions/);
  assert.match(searchApi, /\/api\/v1\/search/);
  assert.match(searchTypes, /namespace Search/);
  assert.match(searchTypes, /Suggestion/);
  assert.match(searchTypes, /SearchResult/);
  assert.match(searchModal, /readSearchSuggestions/);
  assert.match(searchModal, /DEFAULT_SEARCH_LIMIT/);
});

test('v2.1.2 global search renders backend result metadata instead of local menus', async () => {
  const searchResult = await read('src/layouts/modules/global-search/components/search-result.vue');

  assert.match(searchResult, /Api\.Search\.Suggestion\[\]/);
  assert.match(searchResult, /item\.title/);
  assert.match(searchResult, /item\.moduleType/);
  assert.match(searchResult, /item\.path/);
  assert.doesNotMatch(searchResult, /App\.Global\.Menu\[\]/);
});

test('v2.1.2 global search has a dedicated search result page', async () => {
  const [routes, imports, searchPage] = await Promise.all([
    read('src/router/routes/index.ts'),
    read('src/router/elegant/imports.ts'),
    read('src/views/search/index.vue')
  ]);

  assert.match(routes, /name:\s*'search'/);
  assert.match(routes, /path:\s*'\/search'/);
  assert.match(imports, /search:\s*\(\) => import\("@\/views\/search\/index\.vue"\)/);
  assert.match(searchPage, /readSearchResults/);
  assert.match(searchPage, /readSearchFacets/);
  assert.match(searchPage, /route\.query\.q/);
});

test('v2.1.2 global search opens from Ctrl or Cmd K', async () => {
  const globalSearch = await read('src/layouts/modules/global-search/index.vue');

  assert.match(globalSearch, /onKeyStroke/);
  assert.match(globalSearch, /ctrlKey/);
  assert.match(globalSearch, /metaKey/);
  assert.match(globalSearch, /SEARCH_SHORTCUT_KEY/);
});
