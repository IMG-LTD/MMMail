import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const files = {
  loginForm: new URL("../src/views/public/auth/LoginFormPanel.vue", import.meta.url),
  registerView: new URL("../src/views/public/RegisterView.vue", import.meta.url),
};

test("login form submits real credentials through auth API and stores the session", async () => {
  const content = await readFile(files.loginForm, "utf8");

  assert.match(content, /import\s+\{\s*login\s+as\s+loginApi\s*\}/);
  assert.match(content, /useAuthStore/);
  assert.match(content, /useRouter/);
  assert.match(content, /email\s*=\s*ref/);
  assert.match(content, /password\s*=\s*ref/);
  assert.match(content, /v-model:value="email"/);
  assert.match(content, /v-model:value="password"/);
  assert.match(content, /function\s+submitLogin/);
  assert.match(
    content,
    /await\s+loginApi\(\{\s*email:\s*email\.value\.trim\(\),\s*password:\s*password\.value/,
  );
  assert.match(content, /authStore\.applySession\(session\)/);
  assert.match(content, /router\.push\(resolvePostAuthRedirect\(\)\)/);
  assert.match(content, /@submit\.prevent="submitLogin\(\)"/);
  assert.match(content, /attr-type="submit"/);
  assert.match(content, /:loading="submitting"/);
  assert.match(content, /v-if="errorMessage"/);
});

test("register form submits account data through auth API and stores the session", async () => {
  const content = await readFile(files.registerView, "utf8");

  assert.match(content, /import\s+\{\s*register\s+as\s+registerApi\s*\}/);
  assert.match(content, /useAuthStore/);
  assert.match(content, /useRouter/);
  for (const stateName of ["workspaceName", "email", "password", "confirmPassword"]) {
    assert.match(content, new RegExp(`${stateName}\\s*=\\s*ref`));
    assert.match(content, new RegExp(`v-model:value="${stateName}"`));
  }
  assert.match(content, /function\s+submitRegistration/);
  assert.match(content, /if\s*\(password\.value\s*!==\s*confirmPassword\.value\)/);
  assert.match(
    content,
    /await\s+registerApi\(\{\s*displayName:\s*workspaceName\.value\.trim\(\),\s*email:\s*email\.value\.trim\(\),\s*password:\s*password\.value/,
  );
  assert.match(content, /authStore\.applySession\(session\)/);
  assert.match(content, /router\.push\('\/workspace'\)/);
  assert.match(content, /@submit\.prevent="submitRegistration\(\)"/);
  assert.match(content, /attr-type="submit"/);
  assert.match(content, /:loading="submitting"/);
  assert.match(content, /v-if="errorMessage"/);
});
