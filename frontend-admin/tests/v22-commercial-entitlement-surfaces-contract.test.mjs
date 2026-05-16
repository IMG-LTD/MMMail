import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const root = new URL('../', import.meta.url);

const readSource = path => readFile(new URL(path, root), 'utf8');

test('v2.2 commercial license API client uses real v2 endpoints', async () => {
  const [billingApi, billingTypes] = await Promise.all([
    readSource('src/service/api/billing.ts'),
    readSource('src/typings/api/billing.d.ts')
  ]);

  assert.match(billingApi, /readCommercialLicenseStatus/);
  assert.match(billingApi, /uploadCommercialLicense/);
  assert.match(billingApi, /\/api\/v2\/billing\/license\/status/);
  assert.match(billingApi, /\/api\/v2\/billing\/license/);
  assert.doesNotMatch(billingApi, /\(mock\)|mock.*success|fake.*success/i);

  assert.match(billingTypes, /interface CommercialLicenseStatus/);
  assert.match(billingTypes, /interface CommercialLicenseUploadPayload/);
  assert.match(billingTypes, /type CommercialLicenseState/);
});

test('settings page exposes license status and paste upload workflow', async () => {
  const [settingsPage, licensePanel] = await Promise.all([
    readSource('src/views/settings/index.vue'),
    readSource('src/views/settings/modules/LicensePanel.vue')
  ]);

  assert.match(settingsPage, /LicensePanel/);
  assert.match(licensePanel, /readCommercialLicenseStatus/);
  assert.match(licensePanel, /uploadCommercialLicense/);
  assert.match(licensePanel, /type="textarea"/);
  assert.match(licensePanel, /licenseKey/);
  assert.doesNotMatch(licensePanel, /\(mock\)|mock.*success|fake.*success/i);
});

test('admin billing shows external license billing status without payment success copy', async () => {
  const adminPage = await readSource('src/views/admin/index.vue');

  assert.match(adminPage, /readCommercialLicenseStatus/);
  assert.match(adminPage, /commercialLicenseStatus/);
  assert.match(adminPage, /page\.billing\.externalBillingStatus/);
  assert.doesNotMatch(adminPage, /payment.*success|paid.*success|\(mock\)/i);
});

test('admin exposes OIDC setup entry through the commercial entitlement gate', async () => {
  const [adminPage, oidcEntry, zhCN, enUS, zhTW, zhCommercial, enCommercial] = await Promise.all([
    readSource('src/views/admin/index.vue'),
    readSource('src/views/admin/modules/OidcSetupEntry.vue'),
    readSource('src/locales/langs/zh-cn.ts'),
    readSource('src/locales/langs/en-us.ts'),
    readSource('src/locales/langs/zh-tw.ts'),
    readSource('src/locales/langs/v22-commercial/zh-cn.ts'),
    readSource('src/locales/langs/v22-commercial/en-us.ts')
  ]);
  const localizedSources = [zhCN + zhCommercial, enUS + enCommercial, zhTW];

  assert.match(adminPage, /OidcSetupEntry/);
  assert.match(oidcEntry, /EntitlementGate/);
  assert.match(oidcEntry, /oidc\.sso/);
  assert.match(oidcEntry, /requiredEdition:\s*'BUSINESS'/);
  assert.match(oidcEntry, /page\.oidc\.configure/);
  assert.doesNotMatch(oidcEntry, /mock.*success|fake.*success|connected.*success/i);

  for (const locale of localizedSources) {
    assert.match(locale, /oidc:\s*{/);
    assert.match(locale, /configure/);
    assert.match(locale, /unavailableTitle/);
  }
});

test('blocked commercial state consumes backend entitlement details and is localized', async () => {
  const [gate, zhCN, enUS, zhTW, zhCommercial, enCommercial] = await Promise.all([
    readSource('src/components/access/EntitlementGate.vue'),
    readSource('src/locales/langs/zh-cn.ts'),
    readSource('src/locales/langs/en-us.ts'),
    readSource('src/locales/langs/zh-tw.ts'),
    readSource('src/locales/langs/v22-commercial/zh-cn.ts'),
    readSource('src/locales/langs/v22-commercial/en-us.ts')
  ]);
  const localizedSources = [zhCN + zhCommercial, enUS + enCommercial, zhTW];

  assert.match(gate, /requiredEdition/);
  assert.match(gate, /currentEdition/);
  assert.match(gate, /upgradeAction/);
  assert.match(gate, /backendEntitlement/);

  for (const locale of localizedSources) {
    assert.match(locale, /license:\s*{/);
    assert.match(locale, /requiredEdition/);
    assert.match(locale, /currentEdition/);
    assert.match(locale, /upgradeAction/);
    assert.match(locale, /externalBillingStatus/);
  }
});

test('commercial browser a11y gate covers auth and entitlement surfaces', async () => {
  const [packageJsonSource, e2eSpec, playwrightConfig] = await Promise.all([
    readSource('package.json'),
    readSource('e2e/v22-commercial-a11y.spec.ts'),
    readSource('playwright.config.ts')
  ]);
  const packageJson = JSON.parse(packageJsonSource);

  assert.match(packageJson.devDependencies['axe-core'], /\d/);
  assert.match(e2eSpec, /axe-core\/axe\.min\.js/);
  assert.match(e2eSpec, /assertNoSevereA11yViolations/);
  assert.match(e2eSpec, /\/login/);
  assert.match(e2eSpec, /\/login\/register/);
  assert.match(e2eSpec, /\/settings/);
  assert.match(e2eSpec, /\/admin\/billing/);
  assert.match(e2eSpec, /oidc\.sso/);
  assert.match(e2eSpec, /requiredEdition:\s*'BUSINESS'/);
  assert.match(e2eSpec, /critical|serious/);
  assert.match(e2eSpec, /api\/v2\/billing\/license\/status/);
  assert.match(playwrightConfig, /VITE_DEVTOOLS_ENABLED=N/);
});
