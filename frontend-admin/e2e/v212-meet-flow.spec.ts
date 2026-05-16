import { expect, test } from '@playwright/test';
import { createE2EAccount, createMeetRoom, loginViaUi } from './real-backend';

test('v212 meet flow loads a backend room and joins with media controls', async ({ page, request }, testInfo) => {
  const account = await createE2EAccount(request, testInfo, 'meet');
  const topic = `E2E Meet ${Date.now()}`;
  await createMeetRoom(request, account, topic);

  await loginViaUi(page, account);
  await page.goto('/meet');

  await expect(page.getByText(topic).first()).toBeVisible();
  const lobby = page.locator('.n-card').filter({ hasText: /入会预览|大厅|Lobby/i });
  await lobby.getByRole('textbox').first().fill('E2E Host');
  await lobby.getByRole('switch').nth(0).click();
  await lobby.getByRole('button', { name: /加入会议|^加入$|^Join$/i }).click();
  await expect(page.getByText('E2E Host').first()).toBeVisible();
});
