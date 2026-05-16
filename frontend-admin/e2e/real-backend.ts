import { expect, type APIRequestContext, type Page, type TestInfo } from '@playwright/test';

const API_SUCCESS_CODE = 0;
const DEFAULT_BACKEND_PORT = process.env.MMMAIL_E2E_BACKEND_PORT || 18080;
const DEFAULT_API_BASE_URL = `http://127.0.0.1:${DEFAULT_BACKEND_PORT}`;
const E2E_PASSWORD = 'Password_123';

export interface E2EAccount {
  displayName: string;
  email: string;
  orgId: string;
  password: string;
  token: string;
}

interface ApiEnvelope<T> {
  code: number;
  data: T;
  message?: string;
}

interface AuthPayload {
  accessToken: string;
}

interface OrgPayload {
  id: string;
}

interface TeamSpacePayload {
  id: string;
  name: string;
}

interface WalletAccountPayload {
  accountId: string;
}

interface WalletTransactionPayload {
  transactionId: string;
}

interface MeetRoomPayload {
  id: string;
  topic: string;
}

interface DocsNotePayload {
  id: string;
  title: string;
}

export function apiBaseUrl() {
  return process.env.MMMAIL_E2E_API_BASE_URL || DEFAULT_API_BASE_URL;
}

export async function createE2EAccount(
  request: APIRequestContext,
  testInfo: TestInfo,
  label: string,
  options: { createOrg?: boolean } = {}
): Promise<E2EAccount> {
  const email = uniqueEmail(testInfo, label);
  const displayName = `E2E ${label}`;
  const register = await postJson<AuthPayload>(request, '/api/v2/auth/register', {
    displayName,
    email,
    password: E2E_PASSWORD
  });
  const orgId = options.createOrg === false ? '' : await createOrg(request, register.accessToken, label);
  if (orgId) {
    await enableSuitePreviewAccess(request, register.accessToken);
  }
  return { displayName, email, orgId, password: E2E_PASSWORD, token: register.accessToken };
}

export async function loginViaUi(page: Page, account: E2EAccount) {
  await page.goto('/login');
  await page.getByPlaceholder(/邮箱|email/i).fill(account.email);
  await page.getByPlaceholder(/密码|password/i).fill(account.password);
  await page.getByRole('button', { name: /确认|confirm/i }).click();
  await page.waitForFunction(hasStoredToken, undefined, { timeout: 20_000 });
  if (account.orgId) {
    await page.waitForURL(/\/home$/);
  }
}

export async function createTeamSpace(request: APIRequestContext, account: E2EAccount, name: string) {
  return postJson<TeamSpacePayload>(
    request,
    `/api/v1/orgs/${account.orgId}/team-spaces`,
    { description: 'Docker e2e team space', name, storageLimitMb: 64 },
    authHeaders(account)
  );
}

export async function createWalletAccount(request: APIRequestContext, account: E2EAccount, walletName: string) {
  return postJson<WalletAccountPayload>(
    request,
    '/api/v1/wallet/accounts',
    { address: `bc1q${sanitize(walletName)}000000000000000000000000`, assetSymbol: 'BTC', walletName },
    authHeaders(account)
  );
}

export async function receiveWalletTransaction(
  request: APIRequestContext,
  account: E2EAccount,
  accountId: string,
  amountMinor: number
) {
  return postJson<WalletTransactionPayload>(
    request,
    '/api/v1/wallet/transactions/receive',
    {
      accountId,
      amountMinor,
      assetSymbol: 'BTC',
      memo: 'Docker e2e deposit',
      sourceAddress: `bc1qsource${Date.now()}000000000000000000000`
    },
    authHeaders(account)
  );
}

export async function createMeetRoom(request: APIRequestContext, account: E2EAccount, topic: string) {
  return postJson<MeetRoomPayload>(
    request,
    '/api/v1/meet/rooms',
    { accessLevel: 'PUBLIC', maxParticipants: 12, topic },
    authHeaders(account)
  );
}

export async function createDocsNote(request: APIRequestContext, account: E2EAccount, title: string, content: string) {
  return postJson<DocsNotePayload>(request, '/api/v2/docs', { content, title }, authHeaders(account));
}

export async function postJson<T>(
  request: APIRequestContext,
  path: string,
  data: unknown,
  headers: Record<string, string> = {}
) {
  const response = await request.post(`${apiBaseUrl()}${path}`, { data, headers });
  expect(response.ok(), `${path} returned ${response.status()}`).toBe(true);
  const body = (await response.json()) as ApiEnvelope<T>;
  expect(body.code, body.message || path).toBe(API_SUCCESS_CODE);
  return body.data;
}

async function createOrg(request: APIRequestContext, token: string, label: string) {
  const org = await postJson<OrgPayload>(
    request,
    '/api/v1/orgs',
    { name: `E2E ${label} Org ${Date.now()}` },
    { Authorization: `Bearer ${token}` }
  );
  return org.id;
}

async function enableSuitePreviewAccess(request: APIRequestContext, token: string) {
  const headers = { Authorization: `Bearer ${token}` };
  await postJson(request, '/api/v1/suite/subscription/change', { planCode: 'UNLIMITED' }, headers);
  await postJson(request, '/api/v1/meet/access/activate', {}, headers);
}

function authHeaders(account: E2EAccount) {
  return {
    Authorization: `Bearer ${account.token}`,
    'X-Org-Id': account.orgId
  };
}

function uniqueEmail(testInfo: TestInfo, label: string) {
  const safeTitle = sanitize(testInfo.title).slice(0, 24);
  return `e2e-${label}-${safeTitle}-${Date.now()}@mmmail.local`;
}

function sanitize(value: string) {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, '');
}

function hasStoredToken() {
  return Object.keys(localStorage).some(key => {
    if (!key.toLowerCase().endsWith('token')) {
      return false;
    }
    return Boolean(JSON.parse(localStorage.getItem(key) || 'null'));
  });
}
