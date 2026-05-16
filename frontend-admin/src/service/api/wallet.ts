import { request } from '../request';

export function listWalletAccounts(params: Api.Wallet.AccountParams = {}) {
  return request<Api.Wallet.Account[]>({ url: '/api/v1/wallet/accounts', params });
}

export function createWalletAccount(data: Api.Wallet.AccountPayload) {
  return request<Api.Wallet.Account>({
    url: '/api/v1/wallet/accounts',
    method: 'post',
    data
  });
}

export function listWalletTransactions(params: Api.Wallet.TransactionParams) {
  return request<Api.Wallet.Transaction[]>({ url: '/api/v1/wallet/transactions', params });
}

export function readWalletExecutionOverview(accountId: string) {
  return request<Api.Wallet.ExecutionOverview>({ url: '/api/v1/wallet/execution-overview', params: { accountId } });
}

export function readWalletExecutionPlan(params: Api.Wallet.ExecutionPlanParams) {
  return request<Api.Wallet.ExecutionPlan>({ url: '/api/v1/wallet/execution-plan', params });
}

export function readWalletExecutionTrace(transactionId: string) {
  return request<Api.Wallet.ExecutionTrace>({ url: `/api/v1/wallet/transactions/${transactionId}/execution-trace` });
}

export function readWalletReconciliationOverview(accountId: string) {
  return request<Api.Wallet.ReconciliationOverview>({
    url: '/api/v1/wallet/reconciliation-overview',
    params: { accountId }
  });
}

export function receiveWalletTransaction(data: Api.Wallet.ReceivePayload) {
  return request<Api.Wallet.Transaction>({
    url: '/api/v1/wallet/transactions/receive',
    method: 'post',
    data
  });
}

export function sendWalletTransaction(data: Api.Wallet.SendPayload) {
  return request<Api.Wallet.Transaction>({
    url: '/api/v1/wallet/transactions/send',
    method: 'post',
    data
  });
}

export function advanceWalletTransaction(transactionId: string, data: Api.Wallet.AdvancePayload = {}) {
  return request<Api.Wallet.ActionResult>({
    url: `/api/v1/wallet/transactions/${transactionId}/advance`,
    method: 'post',
    data
  });
}

export function remediateWalletTransaction(transactionId: string, data: Api.Wallet.RemediatePayload) {
  return request<Api.Wallet.ActionResult>({
    url: `/api/v1/wallet/transactions/${transactionId}/remediate`,
    method: 'post',
    data
  });
}

export function confirmWalletTransaction(transactionId: string, data: Api.Wallet.ConfirmPayload) {
  return request<Api.Wallet.Transaction>({
    url: `/api/v1/wallet/transactions/${transactionId}/confirm`,
    method: 'post',
    data
  });
}

export function signWalletTransaction(transactionId: string, data: Api.Wallet.SignPayload) {
  return request<Api.Wallet.Transaction>({
    url: `/api/v1/wallet/transactions/${transactionId}/sign`,
    method: 'post',
    data
  });
}

export function broadcastWalletTransaction(transactionId: string, data: Api.Wallet.BroadcastPayload) {
  return request<Api.Wallet.Transaction>({
    url: `/api/v1/wallet/transactions/${transactionId}/broadcast`,
    method: 'post',
    data
  });
}

export function failWalletTransaction(transactionId: string, data: Api.Wallet.FailPayload) {
  return request<Api.Wallet.Transaction>({
    url: `/api/v1/wallet/transactions/${transactionId}/fail`,
    method: 'post',
    data
  });
}

export function batchAdvanceWalletTransactions(data: Api.Wallet.BatchAdvancePayload) {
  return request<Api.Wallet.BatchActionResult>({
    url: '/api/v1/wallet/transactions/batch-advance',
    method: 'post',
    data
  });
}

export function batchRemediateWalletTransactions(data: Api.Wallet.BatchRemediatePayload) {
  return request<Api.Wallet.BatchActionResult>({
    url: '/api/v1/wallet/transactions/batch-remediate',
    method: 'post',
    data
  });
}

export function batchReconcileWalletTransactions(data: Api.Wallet.BatchReconcilePayload) {
  return request<Api.Wallet.BatchActionResult>({
    url: '/api/v1/wallet/transactions/batch-reconcile',
    method: 'post',
    data
  });
}
