import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile, readdir, stat } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

const phaseOneRoutes = [
  '/home',
  '/mail',
  '/calendar',
  '/drive',
  '/docs',
  '/sheets',
  '/pass',
  '/collaboration',
  '/command-center',
  '/notifications',
  '/admin',
  '/settings',
  '/community',
  '/contacts',
  '/wallet',
  '/vpn',
  '/meet',
  '/security/authenticator'
];

const phaseOneViewFiles = [
  'src/views/home/index.vue',
  'src/views/mail/index.vue',
  'src/views/calendar/index.vue',
  'src/views/drive/index.vue',
  'src/views/docs/index.vue',
  'src/views/sheets/index.vue',
  'src/views/pass/index.vue',
  'src/views/collaboration/index.vue',
  'src/views/command-center/index.vue',
  'src/views/notifications/index.vue',
  'src/views/admin/index.vue',
  'src/views/settings/index.vue',
  'src/views/community/index.vue',
  'src/views/contacts/index.vue',
  'src/views/wallet/index.vue',
  'src/views/vpn/index.vue',
  'src/views/meet/index.vue',
  'src/views/security/authenticator/index.vue'
];

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

async function exists(relativePath) {
  try {
    await stat(new URL(relativePath, root));
    return true;
  } catch {
    return false;
  }
}

async function walk(relativeDir) {
  const dirUrl = new URL(relativeDir, root);
  const entries = await readdir(dirUrl, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const child = `${relativeDir.replace(/\/$/, '')}/${entry.name}`;
    if (entry.isDirectory()) files.push(...(await walk(child)));
    if (entry.isFile()) files.push(child);
  }
  return files;
}

test('v2.1.2 phase 1 exposes project scripts and style discipline gate', async () => {
  const pkg = JSON.parse(await read('package.json'));

  assert.equal(pkg.scripts['test:v212'], 'node --test tests/*.test.mjs');
  assert.equal(pkg.scripts['check:style-discipline'], 'node scripts/check-style-discipline.mjs');
  assert.equal(await exists('scripts/check-style-discipline.mjs'), true);
});

test('v2.1.2 phase 1 rewires MMMail env, theme, and module colors', async () => {
  const [env, envTest, envProd, theme, uno, defaultPreset, darkPreset, watermarkSettings, zhCN] = await Promise.all([
    read('.env'),
    read('.env.test'),
    read('.env.prod'),
    read('src/theme/settings.ts'),
    read('uno.config.ts'),
    read('src/theme/preset/default.json'),
    read('src/theme/preset/dark.json'),
    read('src/layouts/modules/theme-drawer/modules/general/modules/watermark-settings.vue'),
    read('src/locales/langs/zh-cn.ts')
  ]);

  assert.match(env, /VITE_APP_TITLE=MMMail/);
  assert.match(env, /VITE_STORAGE_PREFIX=MMMAIL_/);
  assert.match(envTest, /VITE_SERVICE_BASE_URL=http:\/\/127\.0\.0\.1:8080/);
  assert.match(envProd, /VITE_SERVICE_BASE_URL=\$\{VITE_SERVICE_BASE_URL\}/);
  assert.match(theme, /themeColor:\s*'#2D9D8F'/);
  assert.match(theme, /mode:\s*'vertical-mix'/);
  assert.match(theme, /mode:\s*'chrome'/);
  assert.match(theme, /visible:\s*false/);
  assert.doesNotMatch(defaultPreset + darkPreset + watermarkSettings, /SoybeanAdmin/);
  assert.doesNotMatch(zhCN, /Soybean 默认主题预设/);
  for (const colorName of [
    'module-mail',
    'module-calendar',
    'module-drive',
    'module-docs',
    'module-sheets',
    'module-pass',
    'module-admin',
    'module-settings'
  ]) {
    assert.match(uno, new RegExp(`['"]${colorName}['"]`));
  }
});

test('v2.1.2 phase 1 adapts MMMail auth, org scope, and access checks', async () => {
  const [authApi, request, authStore, orgStore, authTypes] = await Promise.all([
    read('src/service/api/auth.ts'),
    read('src/service/request/index.ts'),
    read('src/store/modules/auth/index.ts'),
    read('src/store/modules/org/index.ts'),
    read('src/typings/api/auth.d.ts')
  ]);

  assert.match(authApi, /\/api\/v2\/auth\/login/);
  assert.match(authApi, /\/api\/v2\/auth\/register/);
  assert.match(authApi, /\/api\/v2\/auth\/refresh/);
  assert.match(request, /@sa\/axios/);
  assert.match(request, /X-Org-Id/);
  assert.match(orgStore, /currentOrgId/);
  assert.match(authStore, /entitlements/);
  assert.match(authStore, /featureFlags/);
  assert.match(authStore, /function canAccess/);
  assert.match(authTypes, /interface AuthPayload/);
  assert.match(authTypes, /interface AccessDecisionMeta/);
});

test('v2.1.2 phase 1 provides i18n language entries and builtin auth copy', async () => {
  const [locale, naive, dayjs, appTypes, login, register] = await Promise.all([
    read('src/locales/locale.ts'),
    read('src/locales/naive.ts'),
    read('src/locales/dayjs.ts'),
    read('src/typings/app.d.ts'),
    read('src/views/_builtin/login/modules/pwd-login.vue'),
    read('src/views/_builtin/login/modules/register.vue')
  ]);

  assert.match(locale, /'zh-TW':/);
  assert.match(naive, /zhTW/);
  assert.match(dayjs, /zh-tw/);
  assert.match(appTypes, /'zh-CN' \| 'zh-TW' \| 'en-US'/);
  assert.match(login, /fetchLogin/);
  assert.match(register, /fetchRegister/);
  assert.doesNotMatch(login + register, /SoybeanAdmin/);
});

test('v2.1.2 phase 1 exposes route skeletons and shared entitlement gate', async () => {
  const [routes, gate] = await Promise.all([
    read('src/router/elegant/routes.ts'),
    read('src/components/access/EntitlementGate.vue')
  ]);

  for (const routePath of phaseOneRoutes) {
    assert.match(routes, new RegExp(`path:\\s*'${routePath.replace(/\//g, '\\/')}'`));
  }
  for (const viewFile of phaseOneViewFiles) {
    assert.equal(await exists(viewFile), true, `${viewFile} should exist`);
  }
  for (const fallback of ['upgrade', 'contact-sales', 'trial', 'forbidden']) {
    assert.match(gate, new RegExp(fallback));
  }
  assert.match(gate, /featureFlag/);
  assert.match(gate, /orgRequired/);
  assert.match(gate, /requires/);
});

test('v2.1.2 phase 1 keeps soybean style discipline enforceable', async () => {
  const files = await walk('src');
  const sources = await Promise.all(files.filter(file => /\.(ts|vue|css|scss)$/.test(file)).map(file => read(file)));
  const allSource = sources.join('\n');

  assert.doesNotMatch(allSource, /--mm-|--v211-/);
  assert.doesNotMatch(allSource, /import\s+(?:['"]axios['"]|[^'"]+\s+from\s+['"]axios['"])/);
  assert.doesNotMatch(
    allSource,
    /element-plus|ant-design-vue|tailwindcss|@tailwindcss|from\s+['"]moment['"]|sortablejs|chart\.js|from\s+['"]d3(?:-[^'"]+)?['"]|mapbox/i
  );
});

test('v2.1.2 style discipline rejects every default-denied dependency family', async () => {
  const script = await read('scripts/check-style-discipline.mjs');

  for (const family of ['tailwindcss', '@tailwindcss', 'moment', 'd3-', 'mapbox-gl']) {
    assert.match(script, new RegExp(family.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
  }
  assert.match(script, /from\\s\+\['"\]axios/);
});
