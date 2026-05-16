import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const files = {
  loginView: new URL("../src/views/public/LoginView.vue", import.meta.url),
  authShell: new URL("../src/views/public/auth/PublicAuthShell.vue", import.meta.url),
  formPanel: new URL("../src/views/public/auth/LoginFormPanel.vue", import.meta.url),
  legalBar: new URL("../src/views/public/auth/LoginLegalBar.vue", import.meta.url),
  helpers: new URL("../src/views/public/auth/login-view-helpers.ts", import.meta.url),
  css: new URL("../src/views/public/auth/login-view.css", import.meta.url),
  report: new URL("../scripts/v21-visual-qa/report.mjs", import.meta.url),
  riskRegister: new URL(
    "../../docs/superpowers/progress/v21-visual-parity-risk-register.md",
    import.meta.url,
  ),
  progress: new URL(
    "../../docs/superpowers/progress/v21-implementation-progress.md",
    import.meta.url,
  ),
};

test("public auth login view is split into focused files under the size limit", async () => {
  const [view, authShell, formPanel, legalBar, helpers, css] = await Promise.all([
    readFile(files.loginView, "utf8"),
    readFile(files.authShell, "utf8"),
    readFile(files.formPanel, "utf8"),
    readFile(files.legalBar, "utf8"),
    readFile(files.helpers, "utf8"),
    readFile(files.css, "utf8"),
  ]);

  for (const [name, source] of Object.entries({
    LoginView: view,
    authShell,
    formPanel,
    legalBar,
    helpers,
    css,
  })) {
    assert.ok(source.split("\n").length <= 500, `${name} must stay at or below 500 lines`);
  }

  assert.match(view, /PublicAuthShell/);
  assert.match(view, /LoginFormPanel/);
  assert.match(authShell, /public-auth-shell/);
  assert.match(authShell, /public-auth-rail/);
  assert.match(authShell, /public-auth-context/);
  assert.match(authShell, /LoginLegalBar/);
  assert.match(authShell, /MMMail/);
  assert.match(formPanel, /public-auth-form/);
  assert.match(formPanel, /Enterprise SSO|企业单点登录|企業單一登入/);
  assert.doesNotMatch(formPanel, /Two-factor authentication|双重验证|雙重驗證/);
  assert.match(legalBar, /\/boundary/);
  assert.match(helpers, /publicAuthSecurityItems/);
  assert.match(css, /\.public-auth-shell/);
});

test("visual qa report links to a committed design parity risk register", async () => {
  const [report, riskRegister, progress] = await Promise.all([
    readFile(files.report, "utf8"),
    readFile(files.riskRegister, "utf8"),
    readFile(files.progress, "utf8"),
  ]);

  assert.match(report, /Visual parity risk register/);
  assert.match(report, /v21-visual-parity-risk-register\.md/);
  assert.match(
    riskRegister,
    /\| UI group \| Source design \| QA evidence \| Status \| Notes \| Owner slice \|/,
  );
  for (const group of ["PublicAuthShareSystem", "Login", "Register", "Boundary", "System"]) {
    assert.match(riskRegister, new RegExp(group));
  }
  assert.match(progress, /frontend-v21-final-visual-parity-public-auth-closure/);
});
