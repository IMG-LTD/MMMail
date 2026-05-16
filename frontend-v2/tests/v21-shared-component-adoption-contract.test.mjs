import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const files = {
  baseLayout: new URL("../src/layouts/base-layout/BaseLayout.vue", import.meta.url),
  themeDrawer: new URL("../src/layouts/modules/ThemeDrawer.vue", import.meta.url),
  shellCommandPalette: new URL("../src/layouts/modules/ShellCommandPalette.vue", import.meta.url),
  shellQuickCreateModal: new URL(
    "../src/layouts/modules/ShellQuickCreateModal.vue",
    import.meta.url,
  ),
  commandCenter: new URL("../src/views/app/CommandCenterView.vue", import.meta.url),
  notifications: new URL("../src/views/app/NotificationsView.vue", import.meta.url),
  sheetsWorkspace: new URL("../src/views/app/SheetsWorkspaceView.vue", import.meta.url),
};

test("v2.1 shell mounts shared command palette and quick-create modal", async () => {
  const [baseLayout, commandPalette, quickCreate, themeDrawer] = await Promise.all([
    readFile(files.baseLayout, "utf8"),
    readFile(files.shellCommandPalette, "utf8"),
    readFile(files.shellQuickCreateModal, "utf8"),
    readFile(files.themeDrawer, "utf8"),
  ]);

  assert.match(baseLayout, /ShellCommandPalette/);
  assert.match(baseLayout, /ShellQuickCreateModal/);
  assert.match(commandPalette, /CommandPalette/);
  assert.match(commandPalette, /shellNavGroups/);
  assert.match(commandPalette, /useShellStore/);
  assert.match(commandPalette, /useRouter/);
  assert.match(quickCreate, /Modal/);
  assert.match(quickCreate, /quickCreateOpen/);
  assert.match(quickCreate, /closeQuickCreate/);
  assert.match(themeDrawer, /Drawer/);
  assert.doesNotMatch(themeDrawer, /NDrawerContent/);
});

test("v2.1 operational modules consume shared QA components", async () => {
  const [commandCenter, notifications, sheetsWorkspace] = await Promise.all([
    readFile(files.commandCenter, "utf8"),
    readFile(files.notifications, "utf8"),
    readFile(files.sheetsWorkspace, "utf8"),
  ]);

  assert.match(commandCenter, /ChartCard/);
  assert.match(commandCenter, /DataTable/);
  assert.match(commandCenter, /TerminalLog/);
  assert.match(commandCenter, /ErrorState/);
  assert.match(notifications, /ChartCard/);
  assert.match(notifications, /DataTable/);
  assert.match(notifications, /ErrorState/);
  assert.match(sheetsWorkspace, /DataTable/);
  assert.match(sheetsWorkspace, /DataGrid/);
  assert.match(sheetsWorkspace, /ErrorState/);
});
