import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('round7 auth forms submit through real login and register APIs', async () => {
  const [pwdLogin, register, authApi] = await Promise.all([
    read('src/views/_builtin/login/modules/pwd-login.vue'),
    read('src/views/_builtin/login/modules/register.vue'),
    read('src/service/api/auth.ts')
  ]);

  assert.match(authApi, /\/api\/v2\/auth\/login/);
  assert.match(authApi, /\/api\/v2\/auth\/register/);
  assert.match(pwdLogin, /fetchLogin\(model\.email,\s*model\.password\)/);
  assert.match(register, /fetchRegister\(model\.displayName,\s*model\.email,\s*model\.password\)/);
  assert.match(pwdLogin, /@submit\.prevent="handleSubmit"/);
  assert.match(register, /@click="handleSubmit"/);
  assert.match(register, /page\.login\.register\.submit/);
});

test('round7 auth inputs expose accessible names', async () => {
  const [pwdLogin, register] = await Promise.all([
    read('src/views/_builtin/login/modules/pwd-login.vue'),
    read('src/views/_builtin/login/modules/register.vue')
  ]);

  for (const key of ['emailPlaceholder', 'passwordPlaceholder']) {
    assert.match(pwdLogin, new RegExp(`:?aria-label="\\$t\\('page\\.login\\.common\\.${key}'\\)"`));
  }

  for (const key of ['userNamePlaceholder', 'emailPlaceholder', 'passwordPlaceholder', 'confirmPasswordPlaceholder']) {
    assert.match(register, new RegExp(`:?aria-label="\\$t\\('page\\.login\\.common\\.${key}'\\)"`));
  }
});

test('round7 code login does not simulate captcha or login success', async () => {
  const [codeLogin, captcha, register, resetPwd, appTypes, zhCN, zhTW, enUS] = await Promise.all([
    read('src/views/_builtin/login/modules/code-login.vue'),
    read('src/hooks/business/captcha.ts'),
    read('src/views/_builtin/login/modules/register.vue'),
    read('src/views/_builtin/login/modules/reset-pwd.vue'),
    read('src/typings/app.d.ts'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/zh-tw.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  assert.doesNotMatch(codeLogin + captcha, /validateSuccess|sendCodeSuccess|setTimeout|window\.\$message\?\.success/);
  assert.doesNotMatch(register, /validateSuccess/);
  assert.doesNotMatch(resetPwd, /validateSuccess|window\.\$message\?\.success/);
  assert.match(codeLogin, /page\.login\.codeLogin\.unavailable/);
  assert.match(captcha, /page\.login\.codeLogin\.unavailable/);
  assert.match(resetPwd, /page\.login\.resetPwd\.unavailable/);

  for (const source of [appTypes, zhCN, zhTW, enUS]) {
    assert.match(source, /unavailable:/);
  }
});

test('round7 auth shell follows the current admin visual system', async () => {
  const [loginShell, appTypes, zhCN, enUS] = await Promise.all([
    read('src/views/_builtin/login/index.vue'),
    read('src/typings/app.d.ts'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  assert.doesNotMatch(loginShell, /WaveBg/);
  assert.match(loginShell, /auth-shell/);
  assert.match(loginShell, /auth-brand-panel/);
  assert.match(loginShell, /auth-form-panel/);
  assert.match(loginShell, /auth-logo-mark/);
  assert.match(loginShell, /auth-tool-button/);
  assert.match(loginShell, /auth-lang-select/);
  assert.doesNotMatch(loginShell, /<Transition[^>]*appear/);
  assert.doesNotMatch(loginShell, /<SystemLogo/);
  assert.doesNotMatch(loginShell, /<NTag/);
  assert.doesNotMatch(loginShell, /ThemeSchemaSwitch/);
  assert.doesNotMatch(loginShell, /LangSwitch/);
  assert.match(loginShell, /import \{ computed, defineAsyncComponent \} from 'vue';/);
  assert.match(loginShell, /import PwdLogin from '\.\/modules\/pwd-login\.vue';/);

  for (const moduleName of ['code-login', 'register', 'reset-pwd', 'bind-wechat']) {
    assert.match(
      loginShell,
      new RegExp(`defineAsyncComponent\\(\\(\\) => import\\('\\./modules/${moduleName}\\.vue'\\)\\)`)
    );
  }

  for (const componentName of ['CodeLogin', 'Register', 'ResetPwd', 'BindWechat']) {
    assert.doesNotMatch(loginShell, new RegExp(`import ${componentName} from`));
  }

  for (const source of [appTypes, zhCN, enUS]) {
    assert.match(source, /headline:/);
    assert.match(source, /subtitle:/);
  }
});
