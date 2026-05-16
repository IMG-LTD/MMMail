import { expect, test } from '@playwright/test';
import { createE2EAccount, createTeamSpace, loginViaUi } from './real-backend';

test('v212 business overview renders backend team-space data and opens the drawer', async ({
  page,
  request
}, testInfo) => {
  const account = await createE2EAccount(request, testInfo, 'business');
  const teamName = `E2E Team ${Date.now()}`;
  await createTeamSpace(request, account, teamName);

  await loginViaUi(page, account);
  await page.goto('/business-overview');

  await expect(page.getByText(teamName).first()).toBeVisible();
  await page
    .getByRole('button', { name: /查看|Open/i })
    .first()
    .click();
  await expect(page.getByRole('dialog').getByText(teamName)).toBeVisible();
});
