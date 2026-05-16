import { expect, test } from '@playwright/test';
import { createE2EAccount, loginViaUi } from './real-backend';

const GATED_ROUTES = ['/business-overview', '/wallet', '/meet', '/admin'];

test('v212 entitlement gates reject protected routes for an account without org scope', async ({
  page,
  request
}, testInfo) => {
  const account = await createE2EAccount(request, testInfo, 'gates', { createOrg: false });

  await loginViaUi(page, account);

  for (const route of GATED_ROUTES) {
    await page.goto(route);
    await expect(page).toHaveURL(/\/403$/);
    await expect(page.getByText(/403|无权限|Forbidden|No permission/i).first()).toBeVisible();
  }
});
