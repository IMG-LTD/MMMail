import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 phase 3 exposes expanded module service APIs', async () => {
  const [docs, sheets, pass, collaboration, commandCenter, admin, apiIndex] = await Promise.all([
    read('src/service/api/docs.ts'),
    read('src/service/api/sheets.ts'),
    read('src/service/api/pass.ts'),
    read('src/service/api/collaboration.ts'),
    read('src/service/api/command-center.ts'),
    read('src/service/api/admin.ts'),
    read('src/service/api/index.ts')
  ]);

  assert.match(docs, /\/api\/v2\/docs/);
  assert.match(sheets, /\/api\/v2\/sheets/);
  assert.match(pass, /\/api\/v2\/pass\/items/);
  assert.match(pass, /\/api\/v2\/pass\/monitor/);
  assert.match(collaboration, /\/api\/v2\/collaboration\/projects/);
  assert.match(commandCenter, /\/api\/v2\/command-center\/commands/);
  assert.match(admin, /\/api\/v1\/orgs\/\$\{orgId\}\/admin-console\/summary/);

  for (const moduleName of ['docs', 'sheets', 'pass', 'collaboration', 'command-center', 'admin']) {
    assert.match(apiIndex, new RegExp(`export \\* from '\\./${moduleName}'`));
  }
});

test('v2.1.2 phase 3 docs and sheets pages support list, create, detail, and update flows', async () => {
  const [docsPage, sheetsPage] = await Promise.all([
    read('src/views/docs/index.vue'),
    read('src/views/sheets/index.vue')
  ]);

  for (const marker of ['listDocsNotes', 'createDocsNote', 'readDocsNote', 'updateDocsNote']) {
    assert.match(docsPage, new RegExp(marker));
  }
  assert.match(docsPage, /NDataTable/);
  assert.match(docsPage, /NInput/);
  assert.doesNotMatch(docsPage, /NEmpty/);

  for (const marker of [
    'listSheetsWorkbooks',
    'createSheetsWorkbook',
    'readSheetsWorkbook',
    'updateSheetsWorkbookCells'
  ]) {
    assert.match(sheetsPage, new RegExp(marker));
  }
  assert.match(sheetsPage, /NDataTable/);
  assert.match(sheetsPage, /NInputNumber/);
  assert.doesNotMatch(sheetsPage, /NEmpty/);
});

test('v2.1.2 phase 3 pass, collaboration, command center, and admin pages bind real services', async () => {
  const [passPage, collaborationPage, commandPage, adminPage] = await Promise.all([
    read('src/views/pass/index.vue'),
    read('src/views/collaboration/index.vue'),
    read('src/views/command-center/index.vue'),
    read('src/views/admin/index.vue')
  ]);

  for (const marker of ['listPassItems', 'createPassItem', 'listPassVaults', 'readPassMonitor']) {
    assert.match(passPage, new RegExp(marker));
  }
  assert.match(passPage, /NDataTable/);
  assert.match(passPage, /NForm/);

  for (const marker of ['listCollaborationProjects', 'createCollaborationProject', 'listCollaborationTasks']) {
    assert.match(collaborationPage, new RegExp(marker));
  }
  assert.match(collaborationPage, /NDataTable/);
  assert.match(collaborationPage, /NForm/);

  for (const marker of ['listCommandCenterCommands', 'readCommandCenterCommand']) {
    assert.match(commandPage, new RegExp(marker));
  }
  assert.match(commandPage, /NDataTable/);
  assert.match(commandPage, /NDescriptions/);

  for (const marker of ['readAdminSummary', 'listAdminDomains', 'listAdminProductAccess', 'listAdminMemberSessions']) {
    assert.match(adminPage, new RegExp(marker));
  }
  assert.match(adminPage, /useOrgStore/);
  assert.match(adminPage, /NStatistic/);
  assert.match(adminPage, /NDataTable/);
});

test('v2.1.2 phase 3 adds expanded module i18n namespaces', async () => {
  const [appTypes, zhCN, zhTW, enUS] = await Promise.all([
    read('src/typings/app.d.ts'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/zh-tw.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  for (const key of ['docs', 'sheets', 'pass', 'collaboration', 'commandCenter', 'admin']) {
    assert.match(appTypes, new RegExp(`${key}: \\{[\\s\\S]*title`));
  }

  for (const source of [zhCN, zhTW, enUS]) {
    for (const key of ['docs', 'sheets', 'pass', 'collaboration', 'commandCenter', 'admin']) {
      assert.match(source, new RegExp(`${key}:`));
    }
  }
});
