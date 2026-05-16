import { expect, test, type Page } from '@playwright/test';
import { createDocsNote, createE2EAccount, loginViaUi } from './real-backend';

test('v212 docs CRDT syncs edits between two browser contexts over the real websocket', async ({
  browser,
  request
}, testInfo) => {
  const account = await createE2EAccount(request, testInfo, 'docs');
  const noteTitle = `E2E Docs ${Date.now()}`;
  const mergedText = `CRDT merge ${Date.now()}`;
  await createDocsNote(request, account, noteTitle, '<p>Initial body</p>');

  const leftContext = await browser.newContext();
  const rightContext = await browser.newContext();
  const leftPage = await leftContext.newPage();
  const rightPage = await rightContext.newPage();

  try {
    await loginViaUi(leftPage, account);
    await loginViaUi(rightPage, account);
    await openNote(leftPage, noteTitle);
    await openNote(rightPage, noteTitle);

    await leftPage.locator('.ProseMirror').click();
    await leftPage.keyboard.type(` ${mergedText}`);

    await expect(rightPage.locator('.ProseMirror')).toContainText(mergedText);
  } finally {
    await leftContext.close();
    await rightContext.close();
  }
});

async function openNote(page: Page, noteTitle: string) {
  await page.goto('/docs');
  await page.getByText(noteTitle).first().click();
  await expect(page.locator('.ProseMirror')).toBeVisible();
}
