import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const files = {
  authShell: new URL("../src/views/public/auth/PublicAuthShell.vue", import.meta.url),
  helpers: new URL("../src/views/public/auth/login-view-helpers.ts", import.meta.url),
  loginForm: new URL("../src/views/public/auth/LoginFormPanel.vue", import.meta.url),
  loginView: new URL("../src/views/public/LoginView.vue", import.meta.url),
  registerView: new URL("../src/views/public/RegisterView.vue", import.meta.url),
  css: new URL("../src/views/public/auth/login-view.css", import.meta.url),
};

test("public auth pages share the v2.1.1 shell bridge layout", async () => {
  const [authShell, helpers, loginView, registerView, css] = await Promise.all([
    readFile(files.authShell, "utf8"),
    readFile(files.helpers, "utf8"),
    readFile(files.loginView, "utf8"),
    readFile(files.registerView, "utf8"),
    readFile(files.css, "utf8"),
  ]);

  assert.match(loginView, /PublicAuthShell/);
  assert.match(registerView, /PublicAuthShell/);
  assert.match(authShell, /public-auth-shell/);
  assert.match(authShell, /public-auth-rail/);
  assert.match(authShell, /public-auth-context/);
  assert.match(authShell, /auth-mode-switch/);
  assert.match(authShell, /LoginLegalBar/);
  assert.match(helpers, /publicAuthSecurityItems/);
  assert.match(css, /grid-template-columns:\s*220px minmax\(0,\s*1fr\) 300px/);
  assert.doesNotMatch(css, /hero-surface|scene-desk|scene-monitor|scene-chair/);
});

test("login flow uses a real form and removes the fake second-factor panel", async () => {
  const content = await readFile(files.loginForm, "utf8");

  assert.match(content, /<form[^>]+class="public-auth-form"/);
  assert.match(content, /@submit\.prevent="submitLogin\(\)"/);
  assert.match(content, /attr-type="submit"/);
  assert.match(content, /resolvePostAuthRedirect/);
  assert.doesNotMatch(content, /otp-grid|mfa-strip|Two-factor authentication|双重验证|雙重驗證/);
});

test("register flow exposes live setup progress and password strength state", async () => {
  const content = await readFile(files.registerView, "utf8");

  assert.match(content, /registrationProgressItems/);
  assert.match(content, /passwordStrengthScore/);
  assert.match(content, /public-auth-form/);
  assert.match(content, /register-strength__segment--active/);
  assert.match(content, /@submit\.prevent="submitRegistration\(\)"/);
  assert.doesNotMatch(content, /register-grid__story|第 2 步 \/ 共 3 步|Step 2 \/ 3/);
});
