import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const files = {
  baseLayout: new URL("../src/layouts/base-layout/BaseLayout.vue", import.meta.url),
  globalCss: new URL("../src/styles/global.css", import.meta.url),
  loginForm: new URL("../src/views/public/auth/LoginFormPanel.vue", import.meta.url),
  mailCss: new URL("../src/views/app/mail-surface-view.css", import.meta.url),
  registerView: new URL("../src/views/public/RegisterView.vue", import.meta.url),
  routes: new URL("../src/app/router/routes.ts", import.meta.url),
  settingsView: new URL("../src/views/app/SettingsWorkspaceView.vue", import.meta.url),
  shellSideNav: new URL("../src/layouts/modules/ShellSideNav.vue", import.meta.url),
  shellStore: new URL("../src/store/modules/shell.ts", import.meta.url),
};

test("round 7 protected routing contract is present on disk", async () => {
  const routes = await readFile(files.routes, "utf8");

  assert.match(routes, /redirect:\s*'\/workspace'/);
  assert.match(routes, /path:\s*'\/workspace'/);
  assert.match(routes, /path:\s*'\/admin'/);
  assert.match(routes, /path:\s*'\/mail\/inbox'/);
  assert.ok(routes.indexOf("path: '/mail/inbox'") < routes.indexOf("path: '/mail/:id'"));
  assert.match(routes, /path:\s*'\/mail\/:id'[\s\S]*?redirect:\s*'\/404'/);
  assert.doesNotMatch(routes, /redirect:\s*to\s*=>\s*`\/conversations\/\$\{String\(to\.params\.id/);
});

test("round 7 command palette shortcut opens from Ctrl+K and Command+K", async () => {
  const baseLayout = await readFile(files.baseLayout, "utf8");

  assert.match(baseLayout, /onMounted\(/);
  assert.match(baseLayout, /onBeforeUnmount\(/);
  assert.match(baseLayout, /window\.addEventListener\('keydown',\s*handleCommandPaletteShortcut\)/);
  assert.match(
    baseLayout,
    /window\.removeEventListener\('keydown',\s*handleCommandPaletteShortcut\)/,
  );
  assert.match(baseLayout, /\(event\.ctrlKey\s*\|\|\s*event\.metaKey\)/);
  assert.match(baseLayout, /event\.key\.toLowerCase\(\)\s*===\s*'k'/);
  assert.match(baseLayout, /event\.preventDefault\(\)/);
  assert.match(baseLayout, /shellStore\.openCommandPalette\(\)/);
});

test("round 7 public auth forms expose accessible labels and register submit action", async () => {
  const [loginForm, registerView] = await Promise.all([
    readFile(files.loginForm, "utf8"),
    readFile(files.registerView, "utf8"),
  ]);

  assert.match(loginForm, /input-props/);
  assert.match(loginForm, /Work email/);
  assert.match(loginForm, /Password/);

  assert.match(registerView, /@submit\.prevent/);
  assert.match(registerView, /attr-type="submit"/);
  assert.match(registerView, /Create Account/);
  assert.equal((registerView.match(/input-props/g) || []).length, 4);
  for (const label of ["Workspace name", "Primary admin email", "Password", "Confirm password"]) {
    assert.match(registerView, new RegExp(label));
  }
});

test("round 7 visual deltas are encoded as explicit v2.1.1 tokens", async () => {
  const [baseLayout, globalCss, mailCss, settingsView, shellSideNav, shellStore] =
    await Promise.all([
      readFile(files.baseLayout, "utf8"),
      readFile(files.globalCss, "utf8"),
      readFile(files.mailCss, "utf8"),
      readFile(files.settingsView, "utf8"),
      readFile(files.shellSideNav, "utf8"),
      readFile(files.shellStore, "utf8"),
    ]);

  assert.match(shellStore, /sideNavCollapsed\s*=\s*ref\(false\)/);
  assert.match(baseLayout, /applyInitialSlimNav/);
  assert.match(baseLayout, /shellStore\.setSideNavCollapsed\(true\)/);
  assert.match(baseLayout, /const SIDE_NAV_COLLAPSED_WIDTH = 64/);
  assert.match(
    baseLayout,
    /\.base-layout--nav-collapsed\s*\{\s*--base-layout-side-nav-width:\s*64px/,
  );
  assert.match(shellSideNav, /width:\s*64px/);
  assert.match(shellSideNav, /min-width:\s*20px/);

  assert.match(globalCss, /--mm-app-bg:\s*#f5f6fa/);
  assert.match(
    globalCss,
    /background:\s*color-mix\(in srgb, var\(--mm-surface\) 68%, transparent\)/,
  );
  assert.match(globalCss, /backdrop-filter:\s*blur\(14px\)/);

  assert.match(mailCss, /grid-template-columns:\s*210px minmax\(300px, 380px\) minmax\(0, 1fr\)/);
  assert.match(mailCss, /min-height:\s*68px/);
  assert.match(mailCss, /border-bottom:\s*1px solid var\(--mm-border\)/);

  assert.match(settingsView, /\.settings-shell__nav-active::before/);
  assert.match(
    settingsView,
    /background:\s*color-mix\(in srgb, var\(--mm-brand-primary\) 10%, var\(--mm-surface\)\)/,
  );
});
