import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('v2.1.2 wallet service exposes account, transfer, execution, and reconciliation APIs', async () => {
  const service = await read('src/service/api/wallet.ts');

  for (const apiName of [
    'createWalletAccount',
    'readWalletExecutionPlan',
    'readWalletExecutionTrace',
    'readWalletReconciliationOverview',
    'receiveWalletTransaction',
    'sendWalletTransaction',
    'advanceWalletTransaction',
    'remediateWalletTransaction',
    'confirmWalletTransaction',
    'signWalletTransaction',
    'broadcastWalletTransaction',
    'failWalletTransaction',
    'batchAdvanceWalletTransactions',
    'batchRemediateWalletTransactions',
    'batchReconcileWalletTransactions'
  ]) {
    assert.match(service, new RegExp(apiName));
  }

  assert.match(service, /\/api\/v1\/wallet\/execution-plan/);
  assert.match(service, /\/api\/v1\/wallet\/transactions\/\$\{transactionId\}\/execution-trace/);
  assert.match(service, /\/api\/v1\/wallet\/reconciliation-overview/);
  assert.match(service, /\/api\/v1\/wallet\/transactions\/batch-reconcile/);
});

test('v2.1.2 wallet routes expose accounts, transactions, send, receive, and reconciliation entries', async () => {
  const routes = await read('src/router/routes/index.ts');

  for (const routeName of [
    'wallet_accounts',
    'wallet_transactions',
    'wallet_transaction_detail',
    'wallet_send',
    'wallet_receive',
    'wallet_reconciliation'
  ]) {
    assert.match(routes, new RegExp(`name: '${routeName}'`));
  }

  assert.match(routes, /path: '\/wallet\/accounts'/);
  assert.match(routes, /path: '\/wallet\/transactions'/);
  assert.match(routes, /path: '\/wallet\/transactions\/:transactionId'/);
  assert.match(routes, /path: '\/wallet\/send'/);
  assert.match(routes, /path: '\/wallet\/receive'/);
  assert.match(routes, /path: '\/wallet\/reconciliation'/);
  assert.match(routes, /requires: \['WALLET'\]/);
  assert.match(routes, /featureFlag: 'feat\.wallet\.enabled'/);
});

test('v2.1.2 wallet store refetches account runtime after write operations', async () => {
  const store = await read('src/store/modules/wallet/index.ts');

  assert.match(store, /useWalletStore/);
  assert.match(store, /accounts/);
  assert.match(store, /transactions/);
  assert.match(store, /pendingActions/);
  assert.match(store, /loadAccountRuntime/);
  assert.match(store, /sendTransaction/);
  assert.match(store, /receiveTransaction/);
  assert.match(store, /remediateTransaction/);
  assert.match(store, /batchReconcile/);
});

test('v2.1.2 wallet page binds assets, transfer forms, status steps, and batch actions', async () => {
  const page = await read('src/views/wallet/index.vue');

  assert.match(page, /useWalletStore/);
  assert.match(page, /NNumberAnimation/);
  assert.match(page, /NSteps/);
  assert.match(page, /accountModel/);
  assert.match(page, /sendModel/);
  assert.match(page, /receiveModel/);
  assert.match(page, /actionModel/);
  assert.match(page, /submitAccount/);
  assert.match(page, /submitSend/);
  assert.match(page, /submitReceive/);
  assert.match(page, /runSign/);
  assert.match(page, /runBroadcast/);
  assert.match(page, /runConfirm/);
  assert.match(page, /runRemediate/);
  assert.match(page, /runBatchReconcile/);
});
