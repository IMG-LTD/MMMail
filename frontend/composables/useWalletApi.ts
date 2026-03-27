import type {
  AdvanceWalletTransactionRequest,
  ApiResponse,
  BatchAdvanceWalletTransactionsRequest,
  BatchReconcileWalletTransactionsRequest,
  BatchRemediateWalletTransactionsRequest,
  BroadcastWalletTransactionRequest,
  ConfirmWalletTransactionRequest,
  CreateWalletAccountRequest,
  FailWalletTransactionRequest,
  RemediateWalletTransactionRequest,
  ReceiveWalletTransactionRequest,
  SendWalletTransactionRequest,
  SignWalletTransactionRequest,
  WalletAccount,
  WalletActionResult,
  WalletBatchActionResult,
  WalletExecutionOverview,
  WalletExecutionPlan,
  WalletExecutionTrace,
  WalletReconciliationOverview,
  WalletTransaction,
  WalletTransactionStatus
} from '~/types/api'

export function useWalletApi() {
  const { $apiClient } = useNuxtApp()

  async function listAccounts(limit = 20): Promise<WalletAccount[]> {
    const response = await $apiClient.get<ApiResponse<WalletAccount[]>>('/api/v1/wallet/accounts', {
      params: { limit }
    })
    return response.data.data
  }

  async function createAccount(payload: CreateWalletAccountRequest): Promise<WalletAccount> {
    const response = await $apiClient.post<ApiResponse<WalletAccount>>('/api/v1/wallet/accounts', payload)
    return response.data.data
  }

  async function listTransactions(accountId: string, status?: WalletTransactionStatus, limit = 50): Promise<WalletTransaction[]> {
    const response = await $apiClient.get<ApiResponse<WalletTransaction[]>>('/api/v1/wallet/transactions', {
      params: {
        accountId,
        status: status || undefined,
        limit
      }
    })
    return response.data.data
  }

  async function receive(payload: ReceiveWalletTransactionRequest): Promise<WalletTransaction> {
    const response = await $apiClient.post<ApiResponse<WalletTransaction>>('/api/v1/wallet/transactions/receive', payload)
    return response.data.data
  }

  async function send(payload: SendWalletTransactionRequest): Promise<WalletTransaction> {
    const response = await $apiClient.post<ApiResponse<WalletTransaction>>('/api/v1/wallet/transactions/send', payload)
    return response.data.data
  }

  async function confirmTransaction(transactionId: string, payload: ConfirmWalletTransactionRequest): Promise<WalletTransaction> {
    const response = await $apiClient.post<ApiResponse<WalletTransaction>>(
      `/api/v1/wallet/transactions/${transactionId}/confirm`,
      payload
    )
    return response.data.data
  }

  async function signTransaction(transactionId: string, payload: SignWalletTransactionRequest): Promise<WalletTransaction> {
    const response = await $apiClient.post<ApiResponse<WalletTransaction>>(
      `/api/v1/wallet/transactions/${transactionId}/sign`,
      payload
    )
    return response.data.data
  }

  async function broadcastTransaction(transactionId: string, payload: BroadcastWalletTransactionRequest): Promise<WalletTransaction> {
    const response = await $apiClient.post<ApiResponse<WalletTransaction>>(
      `/api/v1/wallet/transactions/${transactionId}/broadcast`,
      payload
    )
    return response.data.data
  }

  async function failTransaction(transactionId: string, payload: FailWalletTransactionRequest): Promise<WalletTransaction> {
    const response = await $apiClient.post<ApiResponse<WalletTransaction>>(
      `/api/v1/wallet/transactions/${transactionId}/fail`,
      payload
    )
    return response.data.data
  }

  async function getExecutionOverview(accountId: string): Promise<WalletExecutionOverview> {
    const response = await $apiClient.get<ApiResponse<WalletExecutionOverview>>('/api/v1/wallet/execution-overview', {
      params: { accountId }
    })
    return response.data.data
  }

  async function getExecutionPlan(accountId: string, maxItems = 10): Promise<WalletExecutionPlan> {
    const response = await $apiClient.get<ApiResponse<WalletExecutionPlan>>('/api/v1/wallet/execution-plan', {
      params: {
        accountId,
        maxItems
      }
    })
    return response.data.data
  }

  async function getExecutionTrace(transactionId: string): Promise<WalletExecutionTrace> {
    const response = await $apiClient.get<ApiResponse<WalletExecutionTrace>>(
      `/api/v1/wallet/transactions/${transactionId}/execution-trace`
    )
    return response.data.data
  }

  async function getReconciliationOverview(accountId: string): Promise<WalletReconciliationOverview> {
    const response = await $apiClient.get<ApiResponse<WalletReconciliationOverview>>('/api/v1/wallet/reconciliation-overview', {
      params: { accountId }
    })
    return response.data.data
  }

  async function advanceTransaction(
    transactionId: string,
    payload: AdvanceWalletTransactionRequest = {}
  ): Promise<WalletActionResult> {
    const response = await $apiClient.post<ApiResponse<WalletActionResult>>(
      `/api/v1/wallet/transactions/${transactionId}/advance`,
      payload
    )
    return response.data.data
  }

  async function remediateTransaction(
    transactionId: string,
    payload: RemediateWalletTransactionRequest
  ): Promise<WalletActionResult> {
    const response = await $apiClient.post<ApiResponse<WalletActionResult>>(
      `/api/v1/wallet/transactions/${transactionId}/remediate`,
      payload
    )
    return response.data.data
  }

  async function batchAdvanceTransactions(
    payload: BatchAdvanceWalletTransactionsRequest
  ): Promise<WalletBatchActionResult> {
    const response = await $apiClient.post<ApiResponse<WalletBatchActionResult>>(
      '/api/v1/wallet/transactions/batch-advance',
      payload
    )
    return response.data.data
  }

  async function batchRemediateTransactions(
    payload: BatchRemediateWalletTransactionsRequest
  ): Promise<WalletBatchActionResult> {
    const response = await $apiClient.post<ApiResponse<WalletBatchActionResult>>(
      '/api/v1/wallet/transactions/batch-remediate',
      payload
    )
    return response.data.data
  }

  async function batchReconcileTransactions(
    payload: BatchReconcileWalletTransactionsRequest
  ): Promise<WalletBatchActionResult> {
    const response = await $apiClient.post<ApiResponse<WalletBatchActionResult>>(
      '/api/v1/wallet/transactions/batch-reconcile',
      payload
    )
    return response.data.data
  }

  return {
    listAccounts,
    createAccount,
    listTransactions,
    receive,
    send,
    signTransaction,
    broadcastTransaction,
    confirmTransaction,
    failTransaction,
    getExecutionOverview,
    getExecutionPlan,
    getExecutionTrace,
    getReconciliationOverview,
    advanceTransaction,
    remediateTransaction,
    batchAdvanceTransactions,
    batchRemediateTransactions,
    batchReconcileTransactions
  }
}
