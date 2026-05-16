import { expect, test } from '@playwright/test';
import { createE2EAccount, createWalletAccount, loginViaUi, receiveWalletTransaction } from './real-backend';

test('v212 wallet flow shows a backend deposit and signs the pending transaction', async ({
  page,
  request
}, testInfo) => {
  const account = await createE2EAccount(request, testInfo, 'wallet');
  const walletName = `E2E Wallet ${Date.now()}`;
  const wallet = await createWalletAccount(request, account, walletName);
  const transaction = await receiveWalletTransaction(request, account, wallet.accountId, 25_000);

  await loginViaUi(page, account);
  await page.goto(`/wallet/transactions/${transaction.transactionId}`);

  await expect(page.getByText(walletName).first()).toBeVisible();
  await expect(page.getByText(transaction.transactionId).first()).toBeVisible();
  await page.getByRole('button', { name: /签名|Sign/i }).click();
  await expect(page.getByText(/SIGNED|已签名/i).first()).toBeVisible();
});
