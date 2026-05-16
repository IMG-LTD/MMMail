import { createRequire } from 'node:module';
import { expect, test, type Page, type Route } from '@playwright/test';

const require = createRequire(import.meta.url);
const AXE_CORE_PATH = require.resolve('axe-core/axe.min.js');
const API_SUCCESS_CODE = 0;
const ACCESS_TOKEN = 'v22-a11y-access-token';
const REFRESH_TOKEN = 'v22-a11y-refresh-token';
const ORG_ID = 'org_v22_a11y';
const E2E_USER = 'a11y@mmmail.local';
const E2E_PASSWORD = 'Password_123';
const OIDC_REQUIRED_FEATURE = 'oidc.sso';
const SEVERE_IMPACTS = new Set(['critical', 'serious']);
const DOCUMENT_A11Y_SCOPE = 'document';
const LICENSE_A11Y_SCOPE = '[data-testid="license-a11y-scope"]';
const BILLING_A11Y_SCOPE = '[data-testid="billing-status-a11y-scope"]';
const OIDC_A11Y_SCOPE = '[data-testid="oidc-a11y-scope"]';
const OIDC_BACKEND_ENTITLEMENT = {
  requiredEdition: 'BUSINESS',
  currentEdition: 'UNKNOWN',
  upgradeAction: 'contact-sales'
} as const;

interface AxeViolation {
  id: string;
  impact?: string;
  help: string;
  nodes: Array<{ target: string[]; failureSummary?: string }>;
}

interface AxeResult {
  violations: AxeViolation[];
}

declare global {
  interface Window {
    axe: {
      run: (context: Document | Element, options: unknown) => Promise<AxeResult>;
    };
  }
}

test.beforeEach(async ({ page }) => {
  await registerUnexpectedApiGuard(page);
  await registerAuthRoutes(page);
  await registerWorkspaceRoutes(page);
  await registerSettingsRoutes(page);
  await registerAdminRoutes(page);
  await registerBillingRoutes(page);
});

test('v22 login and register shells have no severe a11y violations', async ({ page }) => {
  await page.goto('/login');
  await expect(page.getByText('MMMail').first()).toBeVisible();
  await assertNoSevereA11yViolations(page);

  await page.goto('/login/register');
  await expect(page.getByRole('button', { name: /创建账户|create account/i })).toBeVisible();
  await assertNoSevereA11yViolations(page);
});

test('v22 license settings panel has no severe a11y violations', async ({ page }) => {
  await authenticate(page);
  await page.goto('/settings');

  await expect(page.getByText(/许可证|License/i).first()).toBeVisible();
  await expect(page.getByText(/外部计费状态|External billing status/i).first()).toBeVisible();
  await assertNoSevereA11yViolations(page, LICENSE_A11Y_SCOPE);
});

test('v22 billing and OIDC blocked state have no severe a11y violations', async ({ page }) => {
  await authenticate(page);
  await page.goto('/admin/billing');

  await expect(page.getByText(/外部计费状态|External billing status/i).first()).toBeVisible();
  await expect(page.getByText(/OIDC|单点登录/i).first()).toBeVisible();
  await expect(page.getByText(OIDC_BACKEND_ENTITLEMENT.requiredEdition).first()).toBeVisible();
  expect(authEntitlements()).not.toContain(OIDC_REQUIRED_FEATURE);
  await assertNoSevereA11yViolations(page, BILLING_A11Y_SCOPE);
  await assertNoSevereA11yViolations(page, OIDC_A11Y_SCOPE);
});

async function authenticate(page: Page) {
  await page.goto('/login');
  await page.getByPlaceholder(/邮箱|email/i).fill(E2E_USER);
  await page.getByPlaceholder(/密码|password/i).fill(E2E_PASSWORD);
  await page.getByRole('button', { name: /确认|confirm/i }).click();
  await page.waitForURL(/\/home$/);
}

async function assertNoSevereA11yViolations(page: Page, selector = DOCUMENT_A11Y_SCOPE) {
  await waitForAppIdle(page);
  await page.addScriptTag({ path: AXE_CORE_PATH });
  const result = await page.evaluate(scopeSelector => {
    const context = scopeSelector === 'document' ? document : document.querySelector(scopeSelector);
    if (!context) {
      throw new Error(`A11y scope not found: ${scopeSelector}`);
    }
    return window.axe.run(context, {
      resultTypes: ['violations'],
      runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] }
    });
  }, selector);
  const severeViolations = result.violations.filter(violation => SEVERE_IMPACTS.has(violation.impact || ''));

  expect(formatViolations(severeViolations)).toEqual([]);
}

async function waitForAppIdle(page: Page) {
  await page.locator('#nprogress').waitFor({ state: 'detached' });
  await expect(page.locator('vite-error-overlay')).toHaveCount(0);
}

function formatViolations(violations: AxeViolation[]) {
  return violations.map(violation => {
    const targets = violation.nodes.map(node => node.target.join(' ')).join(', ');
    return `${violation.impact}:${violation.id}:${violation.help}:${targets}`;
  });
}

async function registerUnexpectedApiGuard(page: Page) {
  await page.route(
    url => url.pathname.startsWith('/api/'),
    route => {
      throw new Error(`Unexpected API request in v22 a11y gate: ${route.request().method()} ${route.request().url()}`);
    }
  );
}

async function registerAuthRoutes(page: Page) {
  await page.route('**/api/v2/auth/login', async route => {
    expect(route.request().method()).toBe('POST');
    await fulfillJson(route, okResponse(authPayload()));
  });

  await page.route('**/api/v2/auth/me', async route => {
    expect(route.request().method()).toBe('GET');
    await fulfillJson(route, okResponse(userInfo()));
  });
}

async function registerWorkspaceRoutes(page: Page) {
  await page.route('**/api/v2/workspace/summary', async route => {
    await fulfillJson(route, okResponse(workspaceSummary()));
  });
  await page.route('**/api/v2/workspace/activity', async route => {
    await fulfillJson(route, okResponse([]));
  });
  await page.route('**/api/v2/workspace/tasks', async route => {
    await fulfillJson(route, okResponse([]));
  });
}

async function registerSettingsRoutes(page: Page) {
  await page.route('**/api/v2/settings/profile', async route => {
    await fulfillJson(route, okResponse(settingsProfile()));
  });
  await page.route('**/api/v2/settings/security', async route => {
    await fulfillJson(route, okResponse({ mfaEnabled: true, recoveryEmail: 'recovery@mmmail.local' }));
  });
  await page.route('**/api/v2/settings/devices', async route => {
    await fulfillJson(route, okResponse([]));
  });
  await page.route('**/api/v2/settings/notifications', async route => {
    await fulfillJson(route, okResponse({ emailDigest: true, productUpdates: false }));
  });
  await page.route('**/api/v1/web-push/vapid-public-key', async route => {
    await fulfillJson(route, okResponse({ publicKey: 'v22-a11y-vapid-public-key' }));
  });
  await page.route('**/api/v1/web-push/subscriptions', async route => {
    await fulfillJson(route, okResponse([]));
  });
}

async function registerAdminRoutes(page: Page) {
  await page.route(`**/api/v1/orgs/${ORG_ID}/admin-console/summary`, async route => {
    await fulfillJson(route, okResponse(adminSummary()));
  });
  await page.route('**/api/v1/domains', async route => {
    await fulfillJson(route, okResponse([]));
  });
  await page.route(`**/api/v1/orgs/${ORG_ID}/admin-console/product-access`, async route => {
    await fulfillJson(route, okResponse([]));
  });
  await page.route(`**/api/v1/orgs/${ORG_ID}/admin-console/member-sessions`, async route => {
    await fulfillJson(route, okResponse([]));
  });
}

async function registerBillingRoutes(page: Page) {
  await page.route('**/api/v1/suite/pricing/offers', async route => {
    await fulfillJson(route, okResponse([pricingOffer()]));
  });
  await page.route('**/api/v1/suite/billing/overview', async route => {
    await fulfillJson(route, okResponse(billingOverview()));
  });
  await page.route('**/api/v1/suite/billing/center', async route => {
    await fulfillJson(route, okResponse(billingCenter()));
  });
  await page.route('**/api/v2/billing/license/status', async route => {
    await fulfillJson(route, okResponse(licenseStatus()));
  });
}

async function fulfillJson(route: Route, body: unknown) {
  await route.fulfill({
    status: 200,
    headers: { 'access-control-allow-origin': '*', 'content-type': 'application/json' },
    body: JSON.stringify(body)
  });
}

function okResponse(data: unknown) {
  return { code: API_SUCCESS_CODE, message: 'ok', data };
}

function authPayload() {
  return {
    accessToken: ACCESS_TOKEN,
    refreshToken: REFRESH_TOKEN,
    user: userInfo(),
    currentOrgId: ORG_ID,
    entitlements: authEntitlements(),
    featureFlags: [],
    risk: 'low',
    riskReasons: [],
    secondFactorRequired: false
  };
}

function userInfo() {
  return {
    userId: 'user_v22_a11y',
    userName: 'V22 A11y',
    displayName: 'V22 A11y',
    email: E2E_USER,
    roles: ['R_SUPER', 'admin', 'BILLING_ADMIN'],
    buttons: [],
    currentOrgId: ORG_ID,
    entitlements: authEntitlements(),
    featureFlags: []
  };
}

function authEntitlements() {
  return ['admin.read', 'settings.read'];
}

function workspaceSummary() {
  return {
    productCards: [{ key: 'mail', label: 'Mail', value: '12', state: 'ready', updatedAt: '2026-05-16T00:00:00Z' }],
    recommendationCount: 0,
    systemStatus: 'ready'
  };
}

function settingsProfile() {
  return {
    autoSaveSeconds: 30,
    displayName: 'V22 A11y',
    driveVersionRetentionCount: 20,
    driveVersionRetentionDays: 180,
    mailAddressMode: 'PROTON_ADDRESS',
    preferredLocale: 'zh-CN',
    signature: '',
    timezone: 'Asia/Shanghai',
    undoSendSeconds: 10
  };
}

function adminSummary() {
  return {
    orgId: ORG_ID,
    orgName: 'V22 A11y Org',
    memberCount: 1,
    adminCount: 1,
    domainCount: 0,
    enabledProductCount: 1,
    generatedAt: '2026-05-16T00:00:00Z'
  };
}

function pricingOffer() {
  return {
    code: 'BUSINESS_ANNUAL',
    name: 'Business Annual',
    defaultBillingCycle: 'ANNUAL',
    defaultSeatCount: 1,
    priceValue: '1200'
  };
}

function billingOverview() {
  return {
    activePlanCode: 'FREE',
    activePlanName: 'Free',
    latestDraft: null,
    selfServeOfferCodes: ['BUSINESS_ANNUAL'],
    contactSalesOfferCodes: []
  };
}

function billingCenter() {
  return {
    subscriptionSummary: {
      activePlanCode: 'FREE',
      activePlanName: 'Free',
      billingCycle: 'MONTHLY',
      seatCount: 1,
      autoRenew: false,
      currentPeriodEndsAt: '2026-06-16T00:00:00Z',
      pendingActionCode: '',
      pendingOfferCode: '',
      pendingOfferName: '',
      pendingEffectiveAt: ''
    },
    paymentMethods: [],
    invoices: [],
    availableActions: []
  };
}

function licenseStatus() {
  return {
    orgId: ORG_ID,
    state: 'ACTIVE',
    edition: 'BUSINESS',
    features: ['admin.read', 'settings.read'],
    externalBillingStatus: 'EXTERNAL',
    expiresAt: '2027-05-16T00:00:00Z',
    syncedAt: '2026-05-16T00:00:00Z'
  };
}
