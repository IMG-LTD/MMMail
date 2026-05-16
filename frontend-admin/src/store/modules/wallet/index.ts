import { ref } from 'vue';
import { defineStore } from 'pinia';
import {
  advanceWalletTransaction,
  batchAdvanceWalletTransactions,
  batchReconcileWalletTransactions,
  batchRemediateWalletTransactions,
  broadcastWalletTransaction,
  confirmWalletTransaction,
  createWalletAccount,
  failWalletTransaction,
  listWalletAccounts,
  listWalletTransactions,
  readWalletExecutionOverview,
  readWalletExecutionPlan,
  readWalletExecutionTrace,
  readWalletReconciliationOverview,
  receiveWalletTransaction,
  remediateWalletTransaction,
  sendWalletTransaction,
  signWalletTransaction
} from '@/service/api';
import { SetupStoreId } from '@/enum';

const DEFAULT_TRANSACTION_LIMIT = 50;
const DEFAULT_PLAN_LIMIT = 20;

export const useWalletStore = defineStore(SetupStoreId.Wallet, () => {
  const accounts = ref<Api.Wallet.Account[]>([]);
  const transactions = ref<Api.Wallet.Transaction[]>([]);
  const pendingActions = ref<Api.Wallet.ExecutionPlanItem[]>([]);
  const overview = ref<Api.Wallet.ExecutionOverview | null>(null);
  const executionPlan = ref<Api.Wallet.ExecutionPlan | null>(null);
  const executionTrace = ref<Api.Wallet.ExecutionTrace | null>(null);
  const reconciliation = ref<Api.Wallet.ReconciliationOverview | null>(null);
  const selectedAccountId = ref('');
  const selectedTransactionId = ref('');

  function setSelectedAccountId(accountId: string) {
    selectedAccountId.value = accountId;
  }

  function setSelectedTransactionId(transactionId: string) {
    selectedTransactionId.value = transactionId;
  }

  async function loadAccounts() {
    const { data, error } = await listWalletAccounts();

    if (!error) {
      accounts.value = data;
      selectedAccountId.value ||= data[0]?.accountId || '';
    }
  }

  async function loadAccountRuntime(accountId = selectedAccountId.value) {
    if (!accountId) return;
    selectedAccountId.value = accountId;
    const [transactionResult, overviewResult, planResult, reconciliationResult] = await Promise.all([
      listWalletTransactions({ accountId, limit: DEFAULT_TRANSACTION_LIMIT }),
      readWalletExecutionOverview(accountId),
      readWalletExecutionPlan({ accountId, maxItems: DEFAULT_PLAN_LIMIT }),
      readWalletReconciliationOverview(accountId)
    ]);

    if (!transactionResult.error) transactions.value = transactionResult.data;
    if (!overviewResult.error) overview.value = overviewResult.data;
    if (!planResult.error) {
      executionPlan.value = planResult.data;
      pendingActions.value = planResult.data.items;
    }
    if (!reconciliationResult.error) reconciliation.value = reconciliationResult.data;
  }

  async function loadWallet() {
    await loadAccounts();
    await loadAccountRuntime();
  }

  async function loadTrace(transactionId = selectedTransactionId.value) {
    if (!transactionId) return;
    selectedTransactionId.value = transactionId;
    const { data, error } = await readWalletExecutionTrace(transactionId);

    if (!error) {
      executionTrace.value = data;
    }
  }

  async function refreshAfterTransaction(transaction?: Api.Wallet.Transaction) {
    if (transaction?.accountId) selectedAccountId.value = transaction.accountId;
    if (transaction?.transactionId) selectedTransactionId.value = transaction.transactionId;
    await loadAccountRuntime();
    await loadTrace();
  }

  async function createAccount(data: Api.Wallet.AccountPayload) {
    const result = await createWalletAccount(data);

    if (!result.error) {
      selectedAccountId.value = result.data.accountId;
      await loadWallet();
    }
    return result;
  }

  async function sendTransaction(data: Api.Wallet.SendPayload) {
    const result = await sendWalletTransaction(data);

    if (!result.error) {
      await refreshAfterTransaction(result.data);
    }
    return result;
  }

  async function receiveTransaction(data: Api.Wallet.ReceivePayload) {
    const result = await receiveWalletTransaction(data);

    if (!result.error) {
      await refreshAfterTransaction(result.data);
    }
    return result;
  }

  async function advanceTransaction(transactionId: string, data: Api.Wallet.AdvancePayload) {
    const result = await advanceWalletTransaction(transactionId, data);

    if (!result.error) {
      await refreshAfterTransaction(result.data.transaction);
    }
    return result;
  }

  async function remediateTransaction(transactionId: string, data: Api.Wallet.RemediatePayload) {
    const result = await remediateWalletTransaction(transactionId, data);

    if (!result.error) {
      await refreshAfterTransaction(result.data.transaction);
    }
    return result;
  }

  async function confirmTransaction(transactionId: string, data: Api.Wallet.ConfirmPayload) {
    const result = await confirmWalletTransaction(transactionId, data);

    if (!result.error) {
      await refreshAfterTransaction(result.data);
    }
    return result;
  }

  async function signTransaction(transactionId: string, data: Api.Wallet.SignPayload) {
    const result = await signWalletTransaction(transactionId, data);

    if (!result.error) {
      await refreshAfterTransaction(result.data);
    }
    return result;
  }

  async function broadcastTransaction(transactionId: string, data: Api.Wallet.BroadcastPayload) {
    const result = await broadcastWalletTransaction(transactionId, data);

    if (!result.error) {
      await refreshAfterTransaction(result.data);
    }
    return result;
  }

  async function failTransaction(transactionId: string, data: Api.Wallet.FailPayload) {
    const result = await failWalletTransaction(transactionId, data);

    if (!result.error) {
      await refreshAfterTransaction(result.data);
    }
    return result;
  }

  async function batchAdvance(data: Api.Wallet.BatchAdvancePayload) {
    const result = await batchAdvanceWalletTransactions(data);

    if (!result.error) {
      await loadAccountRuntime(data.accountId);
    }
    return result;
  }

  async function batchRemediate(data: Api.Wallet.BatchRemediatePayload) {
    const result = await batchRemediateWalletTransactions(data);

    if (!result.error) {
      await loadAccountRuntime(data.accountId);
    }
    return result;
  }

  async function batchReconcile(data: Api.Wallet.BatchReconcilePayload) {
    const result = await batchReconcileWalletTransactions(data);

    if (!result.error) {
      await loadAccountRuntime(data.accountId);
    }
    return result;
  }

  return {
    accounts,
    transactions,
    pendingActions,
    overview,
    executionPlan,
    executionTrace,
    reconciliation,
    selectedAccountId,
    selectedTransactionId,
    setSelectedAccountId,
    setSelectedTransactionId,
    loadAccounts,
    loadAccountRuntime,
    loadWallet,
    loadTrace,
    createAccount,
    sendTransaction,
    receiveTransaction,
    advanceTransaction,
    remediateTransaction,
    confirmTransaction,
    signTransaction,
    broadcastTransaction,
    failTransaction,
    batchAdvance,
    batchRemediate,
    batchReconcile
  };
});
