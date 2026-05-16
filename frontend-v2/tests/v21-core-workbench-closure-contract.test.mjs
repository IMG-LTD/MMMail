import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const viewFiles = ["MailSurfaceView.vue", "CalendarView.vue", "PassSectionView.vue"];
const sourceFiles = {
  routeSurfaces: new URL("../src/shared/content/route-surfaces.ts", import.meta.url),
  routes: new URL("../src/app/router/routes.ts", import.meta.url),
};
const componentFiles = [
  "mail/MailFolderRail.vue",
  "mail/MailMessageList.vue",
  "mail/MailThreadReader.vue",
  "mail/MailTrustPanel.vue",
  "mail/MailComposePanel.vue",
  "calendar/CalendarFilterSidebar.vue",
  "calendar/CalendarBoard.vue",
  "calendar/CalendarEventDrawer.vue",
  "calendar/CalendarConflictPanel.vue",
  "pass/PassVaultRail.vue",
  "pass/PassItemList.vue",
  "pass/PassItemDetail.vue",
  "pass/PassShareSettingsModal.vue",
  "pass/PassRiskMonitorPanel.vue",
  "pass/PassConfirmDialog.vue",
];
const selectors = [
  "mail-folder-rail",
  "mail-message-list",
  "mail-thread-reader",
  "mail-compose-trigger",
  "mail-compose-panel",
  "mail-trust-panel",
  "mail-attachment-strip",
  "mail-send-error",
  "mail-send-retry",
  "mail-discard-confirmation",
  "calendar-filter-sidebar",
  "calendar-board",
  "calendar-event-trigger",
  "calendar-event-drawer",
  "calendar-conflict-panel",
  "calendar-resource-state",
  "calendar-save-error",
  "calendar-save-retry",
  "pass-vault-rail",
  "pass-item-list",
  "pass-item-detail",
  "pass-secret-reveal",
  "pass-secure-link-trigger",
  "pass-share-settings-modal",
  "pass-rotate-confirmation",
  "pass-revoke-confirmation",
  "pass-risk-monitor-panel",
  "pass-risk-detail",
  "pass-action-error",
  "pass-action-retry",
];

test("core workbench closure splits large views and exposes stable selectors", async () => {
  const views = await Promise.all(viewFiles.map((file) => readAppFile(file)));
  const components = await Promise.all(componentFiles.map((file) => readAppFile(file)));

  for (const [index, source] of views.entries()) {
    assert.ok(
      source.split("\n").length <= 500,
      `${viewFiles[index]} must stay at or below 500 lines`,
    );
  }

  assert.match(views[0], /MailComposePanel/);
  assert.match(views[0], /mail-surface-view\.css/);
  assert.match(views[1], /CalendarEventDrawer/);
  assert.match(views[1], /calendar-view\.css/);
  assert.match(views[2], /PassShareSettingsModal/);
  assert.match(views[2], /pass-section-view\.css/);

  const source = components.join("\n");
  for (const selector of selectors) {
    assert.match(source, new RegExp(selector));
  }
});

function readAppFile(file) {
  return readFile(new URL(`../src/views/app/${file}`, import.meta.url), "utf8");
}

test("pass monitor route stays inside the unified pass workbench", async () => {
  const [routes, routeSurfaces] = await Promise.all([
    readFile(sourceFiles.routes, "utf8"),
    readFile(sourceFiles.routeSurfaces, "utf8"),
  ]);
  const passMonitorStart = routes.indexOf("path: '/pass/monitor'");
  assert.notEqual(passMonitorStart, -1, "missing /pass/monitor route");
  const passMonitorRecord = routes.slice(
    passMonitorStart,
    routes.indexOf("\n  {", passMonitorStart + 1),
  );

  assert.match(passMonitorRecord, /component:\s*PassSectionView/);
  assert.match(passMonitorRecord, /surfaceKey:\s*'pass-monitor'/);
  assert.match(routeSurfaces, /key:\s*'pass-monitor'/);
});
