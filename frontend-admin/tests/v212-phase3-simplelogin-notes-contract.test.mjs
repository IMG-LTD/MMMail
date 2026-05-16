import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 phase 3 exposes SimpleLogin and Standard Notes service APIs', async () => {
  const [simpleLogin, notes, apiIndex] = await Promise.all([
    read('src/service/api/simplelogin.ts'),
    read('src/service/api/standard-notes.ts'),
    read('src/service/api/index.ts')
  ]);

  assert.match(simpleLogin, /\/api\/v1\/simplelogin\/overview/);
  assert.match(simpleLogin, /\/api\/v1\/simplelogin\/orgs\/\$\{orgId\}\/relay-policies/);
  assert.match(notes, /\/api\/v1\/standard-notes\/overview/);
  assert.match(notes, /\/api\/v1\/standard-notes\/folders/);
  assert.match(notes, /\/api\/v1\/standard-notes\/notes/);
  assert.match(notes, /\/api\/v1\/standard-notes\/export/);
  assert.match(notes, /checklist-items/);
  assert.match(apiIndex, /export \* from '\.\/simplelogin'/);
  assert.match(apiIndex, /export \* from '\.\/standard-notes'/);
});

test('v2.1.2 phase 3 exposes SimpleLogin and Standard Notes routes and pages', async () => {
  const [routes, accessMeta, imports, simpleLoginPage, notesPage] = await Promise.all([
    read('src/router/elegant/routes.ts'),
    read('src/router/routes/access-meta.ts'),
    read('src/router/elegant/imports.ts'),
    read('src/views/integrations/simplelogin/index.vue'),
    read('src/views/notes/index.vue')
  ]);

  assert.match(routes, /\/integrations\/simplelogin/);
  assert.match(accessMeta, /feat\.simplelogin\.enabled/);
  assert.match(accessMeta, /requires: \['SIMPLE_LOGIN'\]/);
  assert.match(routes, /\/notes/);
  assert.match(routes, /feat\.notes\.enabled/);
  assert.match(imports, /integrations_simplelogin/);
  assert.match(imports, /notes:/);
  assert.match(simpleLoginPage, /readSimpleLoginOverview/);
  assert.match(simpleLoginPage, /listSimpleLoginRelayPolicies/);
  assert.match(simpleLoginPage, /createSimpleLoginRelayPolicy/);
  assert.match(notesPage, /readStandardNotesOverview/);
  assert.match(notesPage, /listStandardNoteFolders/);
  assert.match(notesPage, /listStandardNotes/);
  assert.match(notesPage, /createStandardNote/);
  assert.match(notesPage, /toggleStandardNoteChecklistItem/);
  assert.match(notesPage, /exportStandardNotes/);
});

test('v2.1.2 phase 3 adds SimpleLogin and Standard Notes i18n namespaces', async () => {
  const [appTypes, zhCN, zhTW, enUS] = await Promise.all([
    read('src/typings/app.d.ts'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/zh-tw.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  for (const key of ['simpleLogin', 'standardNotes']) {
    assert.match(appTypes, new RegExp(`${key}: \\{[\\s\\S]*title`));
  }

  for (const source of [zhCN, zhTW, enUS]) {
    assert.match(source, /simpleLogin:/);
    assert.match(source, /standardNotes:/);
  }
});
