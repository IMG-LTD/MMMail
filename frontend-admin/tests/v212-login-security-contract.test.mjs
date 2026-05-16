import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 auth payload exposes login anomaly risk contract', async () => {
  const [authTypes, securityTypes] = await Promise.all([
    read('src/typings/api/auth.d.ts'),
    read('src/typings/api/security.d.ts')
  ]);

  for (const key of ['risk:', 'riskReasons:', 'secondFactorRequired:', 'securityEventId']) {
    assert.match(authTypes, new RegExp(key));
  }
  assert.match(securityTypes, /interface SecurityEvent/);
  assert.match(securityTypes, /type SecurityAction = 'block' \| 'force-logout' \| 'mark-safe'/);
});

test('v2.1.2 security event APIs expose user ack and admin action endpoints', async () => {
  const api = await read('src/service/api/security.ts');

  assert.match(api, /listSecurityEvents/);
  assert.match(api, /ackSecurityEvent/);
  assert.match(api, /listAdminSecurityAnomalies/);
  assert.match(api, /applyAdminSecurityAction/);
  assert.match(api, /\/api\/v1\/security\/events/);
  assert.match(api, /\/api\/v1\/admin\/security\/anomalies/);
});

test('v2.1.2 request layer matches MMMail backend response envelope', async () => {
  const [env, envProd, envTypes, request] = await Promise.all([
    read('.env'),
    read('.env.prod'),
    read('src/typings/vite-env.d.ts'),
    read('src/service/request/index.ts')
  ]);

  assert.match(env, /VITE_SERVICE_SUCCESS_CODE=0/);
  assert.match(request, /response\.data\.message \|\| response\.data\.msg/);
  assert.doesNotMatch(request, /apifoxToken:\s*'[^']+'/);
  assert.match(request, /VITE_APIFOX_TOKEN/);
  assert.match(env, /^VITE_APIFOX_TOKEN=$/m);
  assert.match(envProd, /^VITE_APIFOX_TOKEN=\$\{VITE_APIFOX_TOKEN:-\}$/m);
  assert.match(envTypes, /readonly VITE_APIFOX_TOKEN\?: string/);
});

test('v2.1.2 request failures expose trace details with copy action', async () => {
  const [appTypes, request, shared, zhCN, enUS] = await Promise.all([
    read('src/typings/app.d.ts'),
    read('src/service/request/index.ts'),
    read('src/service/request/shared.ts'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  assert.match(appTypes, /traceId\?: string/);
  assert.match(appTypes, /requestId\?: string/);
  assert.match(request, /x-request-id/);
  assert.match(request, /traceId/);
  assert.match(request, /requestId/);
  assert.match(request, /showErrorMsg\(request\.state,\s*\{/);
  assert.match(shared, /window\.\$notification/);
  assert.match(shared, /notification\.error/);
  assert.match(shared, /navigator\.clipboard\.writeText/);
  assert.match(shared, /NButton/);
  for (const locale of [zhCN, enUS]) {
    assert.match(locale, /traceId:/);
    assert.match(locale, /copyDetails:/);
    assert.match(locale, /copyDetailsSuccess:/);
    assert.match(locale, /copyDetailsFailed:/);
  }
});

test('v2.1.2 login flow surfaces risk challenge and lock countdown', async () => {
  const [store, pwdLogin, zhCN, enUS] = await Promise.all([
    read('src/store/modules/auth/index.ts'),
    read('src/views/_builtin/login/modules/pwd-login.vue'),
    read('src/locales/langs/zh-cn.ts'),
    read('src/locales/langs/en-us.ts')
  ]);

  assert.match(store, /notifyLoginSecurity/);
  assert.match(store, /handleLoginFailure/);
  assert.match(store, /loginLockMessage/);
  assert.match(store, /normalizeUserInfo/);
  assert.match(pwdLogin, /authStore\.handleLoginFailure\(error\)/);
  assert.match(pwdLogin, /page\.login\.security\.secondFactorRequired/);
  assert.match(pwdLogin, /page\.login\.security\.lockTitle/);
  assert.match(zhCN, /secondFactorRequired: '检测到新的登录位置/);
  assert.match(enUS, /secondFactorRequired: 'New sign-in location detected/);
});
