import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const files = {
  collaborationApi: new URL("../src/service/api/collaboration.ts", import.meta.url),
  collaborationView: new URL("../src/views/app/CollaborationView.vue", import.meta.url),
  docsApi: new URL("../src/service/api/docs.ts", import.meta.url),
  labsApi: new URL("../src/service/api/labs.ts", import.meta.url),
  labsModuleView: new URL("../src/views/app/LabsModuleView.vue", import.meta.url),
  labsOverviewView: new URL("../src/views/app/LabsOverviewView.vue", import.meta.url),
  sheetsApi: new URL("../src/service/api/sheets.ts", import.meta.url),
};

test("v2.1 docs and sheets API boundaries use section 14 endpoints", async () => {
  const [docsApi, sheetsApi] = await Promise.all([
    readFile(files.docsApi, "utf8"),
    readFile(files.sheetsApi, "utf8"),
  ]);

  for (const endpoint of ["/api/v2/docs", "/api/v2/sheets"]) {
    assert.match(`${docsApi}\n${sheetsApi}`, new RegExp(endpoint.replaceAll("/", "\\/")));
  }

  assert.match(docsApi, /`\/api\/v2\/docs\/\$\{noteId\}`/);
  assert.match(docsApi, /`\/api\/v2\/docs\/\$\{noteId\}\/comments`/);
  assert.match(docsApi, /`\/api\/v2\/docs\/\$\{noteId\}\/versions`/);
  assert.match(docsApi, /`\/api\/v2\/docs\/\$\{noteId\}\/share`/);
  assert.match(docsApi, /createDocsNote/);
  assert.match(docsApi, /listDocsComments/);
  assert.match(docsApi, /listDocsVersions/);
  assert.match(docsApi, /shareDocsNote/);
  assert.doesNotMatch(docsApi, /\/api\/v1\/docs/);

  assert.match(sheetsApi, /`\/api\/v2\/sheets\/\$\{workbookId\}`/);
  assert.match(sheetsApi, /`\/api\/v2\/sheets\/\$\{workbookId\}\/imports`/);
  assert.match(sheetsApi, /`\/api\/v2\/sheets\/\$\{workbookId\}\/cleaning-rules`/);
  assert.match(sheetsApi, /`\/api\/v2\/sheets\/\$\{workbookId\}\/insights`/);
  assert.match(sheetsApi, /createSheetsWorkbook/);
  assert.match(sheetsApi, /importSheetsWorkbook/);
  assert.match(sheetsApi, /createSheetsCleaningRule/);
  assert.match(sheetsApi, /listSheetsInsights/);
  assert.doesNotMatch(sheetsApi, /\/api\/v1\/sheets/);
});

test("v2.1 labs and collaboration runtime boundaries use shared clients", async () => {
  const [labsApi, collaborationApi, labsOverviewView, labsModuleView, collaborationView] =
    await Promise.all([
      readFile(files.labsApi, "utf8"),
      readFile(files.collaborationApi, "utf8"),
      readFile(files.labsOverviewView, "utf8"),
      readFile(files.labsModuleView, "utf8"),
      readFile(files.collaborationView, "utf8"),
    ]);

  for (const endpoint of [
    "/api/v2/labs/modules",
    "/api/v2/collaboration/projects",
    "/api/v2/collaboration/tasks",
    "/api/v2/collaboration/activity",
  ]) {
    assert.match(`${labsApi}\n${collaborationApi}`, new RegExp(endpoint.replaceAll("/", "\\/")));
  }

  assert.match(labsApi, /`\/api\/v2\/labs\/modules\/\$\{moduleKey\}`/);
  assert.match(labsApi, /`\/api\/v2\/labs\/modules\/\$\{moduleKey\}\/settings`/);
  assert.match(labsApi, /listLabsModules/);
  assert.match(labsApi, /readLabsModule/);
  assert.match(labsApi, /patchLabsModuleSettings/);
  assert.doesNotMatch(labsApi, /\/api\/v1\/labs/);

  assert.match(collaborationApi, /`\/api\/v2\/collaboration\/projects\/\$\{projectId\}`/);
  assert.match(collaborationApi, /`\/api\/v2\/collaboration\/tasks\/\$\{taskId\}`/);
  assert.match(collaborationApi, /`\/api\/v2\/collaboration\/tasks\/\$\{taskId\}\/comments`/);
  assert.match(collaborationApi, /createCollaborationProject/);
  assert.match(collaborationApi, /patchCollaborationTask/);
  assert.match(collaborationApi, /commentCollaborationTask/);

  assert.match(labsOverviewView, /useAuthStore/);
  assert.match(labsOverviewView, /listLabsModules/);
  assert.match(labsOverviewView, /latestLabsRequest/);
  assert.doesNotMatch(labsOverviewView, /const curated = \[/);
  assert.doesNotMatch(labsOverviewView, /const hidden = \[/);

  assert.match(labsModuleView, /useAuthStore/);
  assert.match(labsModuleView, /readLabsModule/);
  assert.match(labsModuleView, /patchLabsModuleSettings/);
  assert.match(labsModuleView, /latestLabsModuleRequest/);

  assert.match(collaborationView, /useAuthStore/);
  assert.match(collaborationView, /useScopeGuard/);
  assert.match(collaborationView, /listCollaborationProjects/);
  assert.match(collaborationView, /listCollaborationTasks/);
  assert.match(collaborationView, /listCollaborationActivity/);
  assert.match(collaborationView, /latestCollaborationRequest/);
  assert.doesNotMatch(collaborationView, /\/api\/v2\/workspace\/aggregation/);
  assert.doesNotMatch(collaborationView, /const cards = \[/);
});
