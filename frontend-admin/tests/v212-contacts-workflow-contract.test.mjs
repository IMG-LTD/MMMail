import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 contacts service exposes favorite, csv import/export, and duplicate merge APIs', async () => {
  const [contactsApi, contactsTypes] = await Promise.all([
    read('src/service/api/contacts.ts'),
    read('src/typings/api/expanded.d.ts')
  ]);

  for (const marker of [
    'updateContact',
    'deleteContact',
    'favoriteContact',
    'unfavoriteContact',
    'importContactsCsv',
    'exportContacts',
    'listContactDuplicates',
    'mergeDuplicateContacts',
    'listContactSuggestions'
  ]) {
    assert.match(contactsApi, new RegExp(marker));
  }

  for (const endpoint of [
    '/api/v1/contacts/import/csv',
    '/api/v1/contacts/export',
    '/api/v1/contacts/duplicates',
    '/api/v1/contacts/duplicates/merge',
    '/api/v1/contacts/suggestions'
  ]) {
    assert.match(contactsApi, new RegExp(endpoint.replaceAll('/', '\\/')));
  }

  for (const typeName of [
    'ImportCsvPayload',
    'ImportResult',
    'DuplicateGroup',
    'MergeDuplicatesPayload',
    'Suggestion'
  ]) {
    assert.match(contactsTypes, new RegExp(`interface ${typeName}`));
  }
});

test('v2.1.2 contacts page binds import, favorite, duplicate merge, and export flows', async () => {
  const contactsPage = await read('src/views/contacts/index.vue');

  for (const marker of [
    'favoriteContact',
    'unfavoriteContact',
    'importContactsCsv',
    'exportContacts',
    'listContactDuplicates',
    'mergeDuplicateContacts',
    'submitImportCsv',
    'mergeDuplicateGroup',
    'toggleFavorite',
    'runExportContacts'
  ]) {
    assert.match(contactsPage, new RegExp(marker));
  }

  assert.match(contactsPage, /NDataTable/);
  assert.match(contactsPage, /NSwitch/);
  assert.match(contactsPage, /csvModel/);
  assert.match(contactsPage, /duplicateGroups/);
  assert.match(contactsPage, /exportPayload/);
});
