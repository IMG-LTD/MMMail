import { expect, test } from '@playwright/test';
import type { Page, Route } from '@playwright/test';

const ACCESS_TOKEN = 'e2e-access-token';
const REFRESH_TOKEN = 'e2e-refresh-token';
const API_SUCCESS_CODE = 0;

test.beforeEach(async ({ page }) => {
  await registerAuthRoutes(page);
  await registerWorkspaceRoutes(page);
});

test('fetchLogin authenticates from the redesigned login shell', async ({ page }) => {
  await page.goto('/login');

  await expect(page.getByText('MMMail').first()).toBeVisible();
  await page.getByPlaceholder(/邮箱|email/i).fill('user@mmmail.local');
  await page.getByPlaceholder(/密码|password/i).fill('123456');
  await page.getByRole('button', { name: /确认|confirm/i }).click();

  await page.waitForURL(/\/home$/);
  await expect(page.getByText(/Workspace|工作台|System Status|系统状态/i).first()).toBeVisible();
});

test('fetchRegister creates an account from the redesigned register flow', async ({ page }) => {
  await page.goto('/login/register');

  await page.getByPlaceholder(/用户名|user name/i).fill('E2EUser');
  await page.getByPlaceholder(/邮箱|email/i).fill('new-user@mmmail.local');
  await page.getByPlaceholder(/^请输入密码$|enter password$/i).fill('123456');
  await page.getByPlaceholder(/再次输入密码|password again/i).fill('123456');
  await page.getByRole('button', { name: /创建账户|create account/i }).click();

  await page.waitForURL(/\/home$/);
  await expect(page.getByText(/Workspace|工作台|System Status|系统状态/i).first()).toBeVisible();
});

async function registerAuthRoutes(page: Page) {
  await page.route('**/api/v2/auth/login', async route => {
    expect(route.request().method()).toBe('POST');
    expect(await route.request().postDataJSON()).toMatchObject({
      email: 'user@mmmail.local',
      password: '123456'
    });
    await fulfillJson(route, authResponse('MMMail User'));
  });

  await page.route('**/api/v2/auth/register', async route => {
    expect(route.request().method()).toBe('POST');
    expect(await route.request().postDataJSON()).toMatchObject({
      displayName: 'E2EUser',
      email: 'new-user@mmmail.local',
      password: '123456'
    });
    await fulfillJson(route, authResponse('E2EUser'));
  });

  await page.route('**/api/v2/auth/me', async route => {
    await fulfillJson(route, okResponse(userInfo('MMMail User')));
  });
}

async function registerWorkspaceRoutes(page: Page) {
  await page.route('**/api/v2/workspace/summary', async route => {
    await fulfillJson(
      route,
      okResponse({
        productCards: [{ key: 'mail', label: 'Mail', value: '12', state: 'ready', updatedAt: '2026-05-16T00:00:00Z' }],
        recommendationCount: 2,
        systemStatus: 'ready'
      })
    );
  });

  await page.route('**/api/v2/workspace/activity', async route => {
    await fulfillJson(route, okResponse([]));
  });

  await page.route('**/api/v2/workspace/tasks', async route => {
    await fulfillJson(route, okResponse([]));
  });
}

async function fulfillJson(route: Route, body: unknown) {
  await route.fulfill({
    status: 200,
    headers: {
      'access-control-allow-origin': '*',
      'content-type': 'application/json'
    },
    body: JSON.stringify(body)
  });
}

function okResponse(data: unknown) {
  return { code: API_SUCCESS_CODE, message: 'ok', data };
}

function authResponse(displayName: string) {
  return okResponse({
    accessToken: ACCESS_TOKEN,
    refreshToken: REFRESH_TOKEN,
    user: userInfo(displayName),
    currentOrgId: 'org_e2e',
    entitlements: ['WALLET', 'VPN', 'MEET', 'SIMPLE_LOGIN'],
    featureFlags: ['feat.wallet.enabled', 'feat.vpn.enabled', 'feat.meet.enabled', 'feat.simplelogin.enabled'],
    risk: 'low',
    riskReasons: [],
    secondFactorRequired: false
  });
}

function userInfo(displayName: string) {
  return {
    userId: 'user_e2e',
    userName: displayName,
    displayName,
    email: 'user@mmmail.local',
    roles: ['R_SUPER', 'ORG_ADMIN', 'BILLING_ADMIN'],
    buttons: []
  };
}
