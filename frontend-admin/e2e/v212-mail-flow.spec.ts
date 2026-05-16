import { expect, test } from '@playwright/test';
import { createE2EAccount, loginViaUi } from './real-backend';

test('v212 mail flow saves a draft through the real backend and reads it from drafts', async ({
  page,
  request
}, testInfo) => {
  const account = await createE2EAccount(request, testInfo, 'mail');
  const subject = `E2E Draft ${Date.now()}`;

  await loginViaUi(page, account);
  await page.goto('/mail/drafts');
  await page.getByRole('button', { name: /写邮件|写信|Compose/i }).click();

  const drawer = page.getByRole('dialog');
  await drawer.getByRole('textbox').nth(0).fill('recipient@mmmail.local');
  await drawer.getByRole('textbox').nth(1).fill(subject);
  await drawer.getByRole('textbox').nth(2).fill('Draft body persisted by docker e2e.');
  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/api/v2/mail/drafts') &&
        response.request().method() === 'POST' &&
        response.status() === 200
    ),
    drawer.getByRole('button', { name: /保存草稿|Save draft/i }).click()
  ]);

  await page.goto('/mail/drafts');
  await expect(page.getByText(subject).first()).toBeVisible();
});
