import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import type {
  WalletAccount,
  WalletBatchActionResult,
  WalletExecutionOverview,
  WalletExecutionPlan,
  WalletReconciliationOverview,
  WalletRemediationStrategy,
  WalletTransaction,
  WalletTransactionStatus
} from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useWalletApi } from '~/composables/useWalletApi'
import { defaultWalletRemediationStrategy } from '~/utils/wallet-workspace'

interface WalletAccountPayload {
  walletName: string
  assetSymbol: string
  address: string
}

interface WalletTransferPayload {
  amountMinor: number
  address: string
  memo: string
}

const DEFAULT_BATCH_MAX_ITEMS = 5
const BATCH_OPERATOR_HINT = 'desk-batch'
const BATCH_RECONCILE_STRATEGY = 'AUTO'

export function useWalletWorkspace() {
  const { t } = useI18n()
  const {
    batchAdvanceTransactions,
    batchReconcileTransactions,
    batchRemediateTransactions,
    createAccount,
    getExecutionOverview,
    getExecutionPlan,
    getReconciliationOverview,
    listAccounts,
    listTransactions,
    receive,
    send
  } = useWalletApi()

  const loading = ref(false)
  const loadingOverview = ref(false)
  const loadingPlan = ref(false)
  const loadingReconciliation = ref(false)
  const creating = ref(false)
  const receiving = ref(false)
  const sending = ref(false)
  const batchAdvancing = ref(false)
  const batchRemediating = ref(false)
  const batchReconciling = ref(false)
  const accounts = ref<WalletAccount[]>([])
  const transactions = ref<WalletTransaction[]>([])
  const executionOverview = ref<WalletExecutionOverview | null>(null)
  const executionPlan = ref<WalletExecutionPlan | null>(null)
  const reconciliationOverview = ref<WalletReconciliationOverview | null>(null)
  const lastBatchResult = ref<WalletBatchActionResult | null>(null)
  const selectedAccountId = ref('')
  const txStatusFilter = ref<'ALL' | WalletTransactionStatus>('ALL')
  const batchMaxItems = ref(DEFAULT_BATCH_MAX_ITEMS)
  const walletBalanceMasked = ref(false)

  const selectedAccount = computed(() => accounts.value.find((item) => item.accountId === selectedAccountId.value) ?? null)
  const priorityQueue = computed(() => executionOverview.value?.priorityTransactions || [])

  async function refreshTransactions(): Promise<void> {
    if (!selectedAccountId.value) {
      transactions.value = []
      return
    }
    const status = txStatusFilter.value === 'ALL' ? undefined : txStatusFilter.value
    transactions.value = await listTransactions(selectedAccountId.value, status, 100)
  }

  async function refreshExecutionOverview(): Promise<void> {
    if (!selectedAccountId.value) {
      executionOverview.value = null
      return
    }
    loadingOverview.value = true
    try {
      executionOverview.value = await getExecutionOverview(selectedAccountId.value)
    } catch (error) {
      executionOverview.value = null
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.loadOverviewFailed'))
    } finally {
      loadingOverview.value = false
    }
  }

  async function refreshExecutionPlan(): Promise<void> {
    if (!selectedAccountId.value) {
      executionPlan.value = null
      return
    }
    loadingPlan.value = true
    try {
      executionPlan.value = await getExecutionPlan(selectedAccountId.value, batchMaxItems.value)
    } catch (error) {
      executionPlan.value = null
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.loadPlanFailed'))
    } finally {
      loadingPlan.value = false
    }
  }

  async function refreshReconciliationOverview(): Promise<void> {
    if (!selectedAccountId.value) {
      reconciliationOverview.value = null
      return
    }
    loadingReconciliation.value = true
    try {
      reconciliationOverview.value = await getReconciliationOverview(selectedAccountId.value)
    } catch (error) {
      reconciliationOverview.value = null
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.loadReconciliationFailed'))
    } finally {
      loadingReconciliation.value = false
    }
  }

  async function refreshAccounts(withTransactions = true): Promise<void> {
    accounts.value = await listAccounts(100)
    if (!selectedAccountId.value && accounts.value.length > 0) {
      selectedAccountId.value = accounts.value[0].accountId
    }
    if (!accounts.value.some((item) => item.accountId === selectedAccountId.value)) {
      selectedAccountId.value = accounts.value[0]?.accountId || ''
    }
    if (!withTransactions) {
      return
    }
    await Promise.all([
      refreshTransactions(),
      refreshExecutionOverview(),
      refreshExecutionPlan(),
      refreshReconciliationOverview()
    ])
  }

  async function loadData(): Promise<void> {
    loading.value = true
    try {
      await refreshAccounts(true)
    } catch (error) {
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.loadWalletFailed'))
    } finally {
      loading.value = false
    }
  }

  async function onAccountChange(accountId: string): Promise<void> {
    selectedAccountId.value = accountId
    await Promise.all([
      refreshTransactions(),
      refreshExecutionOverview(),
      refreshExecutionPlan(),
      refreshReconciliationOverview()
    ])
  }

  function onWalletMaskingChange(masked: boolean): void {
    walletBalanceMasked.value = masked
  }

  async function onWalletParityAccountImported(accountId: string): Promise<void> {
    selectedAccountId.value = accountId
    await refreshAccounts(true)
  }

  async function onCreateAccount(payload: WalletAccountPayload): Promise<void> {
    if (payload.walletName.trim().length < 2) {
      ElMessage.warning(t('wallet.workspace.messages.walletNameInvalid'))
      return
    }
    if (payload.address.trim().length < 16) {
      ElMessage.warning(t('wallet.workspace.messages.addressInvalid'))
      return
    }
    creating.value = true
    try {
      const account = await createAccount({
        walletName: payload.walletName.trim(),
        assetSymbol: payload.assetSymbol.trim() || 'BTC',
        address: payload.address.trim()
      })
      selectedAccountId.value = account.accountId
      await refreshAccounts(true)
      ElMessage.success(t('wallet.workspace.messages.accountCreated'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.createAccountFailed'))
    } finally {
      creating.value = false
    }
  }

  async function onReceive(payload: WalletTransferPayload): Promise<void> {
    if (!selectedAccount.value) {
      ElMessage.warning(t('wallet.workspace.messages.selectAccount'))
      return
    }
    if (payload.amountMinor <= 0) {
      ElMessage.warning(t('wallet.workspace.messages.receiveAmountInvalid'))
      return
    }
    if (payload.address.trim().length < 16) {
      ElMessage.warning(t('wallet.workspace.messages.sourceAddressInvalid'))
      return
    }
    receiving.value = true
    try {
      await receive({
        accountId: selectedAccount.value.accountId,
        amountMinor: payload.amountMinor,
        assetSymbol: selectedAccount.value.assetSymbol,
        sourceAddress: payload.address.trim(),
        memo: payload.memo.trim() || undefined
      })
      await refreshAccounts(true)
      ElMessage.success(t('wallet.workspace.messages.receiveRecorded'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.receiveFailed'))
    } finally {
      receiving.value = false
    }
  }

  async function onSend(payload: WalletTransferPayload): Promise<void> {
    if (!selectedAccount.value) {
      ElMessage.warning(t('wallet.workspace.messages.selectAccount'))
      return
    }
    if (payload.amountMinor <= 0) {
      ElMessage.warning(t('wallet.workspace.messages.sendAmountInvalid'))
      return
    }
    if (payload.address.trim().length < 16) {
      ElMessage.warning(t('wallet.workspace.messages.targetAddressInvalid'))
      return
    }
    sending.value = true
    try {
      await send({
        accountId: selectedAccount.value.accountId,
        amountMinor: payload.amountMinor,
        assetSymbol: selectedAccount.value.assetSymbol,
        targetAddress: payload.address.trim(),
        memo: payload.memo.trim() || undefined
      })
      await refreshAccounts(true)
      ElMessage.success(t('wallet.workspace.messages.sendRecorded'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.sendFailed'))
    } finally {
      sending.value = false
    }
  }

  async function onBatchAdvance(): Promise<void> {
    if (!selectedAccountId.value) {
      ElMessage.warning(t('wallet.workspace.messages.selectAccount'))
      return
    }
    batchAdvancing.value = true
    try {
      lastBatchResult.value = await batchAdvanceTransactions({
        accountId: selectedAccountId.value,
        maxItems: batchMaxItems.value,
        operatorHint: BATCH_OPERATOR_HINT
      })
      await refreshAccounts(true)
      ElMessage.success(t('wallet.workspace.messages.batchAdvanceDone', {
        success: lastBatchResult.value.successCount,
        processed: lastBatchResult.value.processedCount
      }))
    } catch (error) {
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.batchAdvanceFailed'))
    } finally {
      batchAdvancing.value = false
    }
  }

  async function onBatchRemediate(): Promise<void> {
    if (!selectedAccountId.value) {
      ElMessage.warning(t('wallet.workspace.messages.selectAccount'))
      return
    }
    const strategy: WalletRemediationStrategy = defaultWalletRemediationStrategy('FAILED')
    batchRemediating.value = true
    try {
      lastBatchResult.value = await batchRemediateTransactions({
        accountId: selectedAccountId.value,
        maxItems: batchMaxItems.value,
        strategy,
        reason: BATCH_OPERATOR_HINT
      })
      await refreshAccounts(true)
      ElMessage.success(t('wallet.workspace.messages.batchRemediateDone', {
        success: lastBatchResult.value.successCount,
        processed: lastBatchResult.value.processedCount
      }))
    } catch (error) {
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.batchRemediateFailed'))
    } finally {
      batchRemediating.value = false
    }
  }

  async function onBatchReconcile(): Promise<void> {
    if (!selectedAccountId.value) {
      ElMessage.warning(t('wallet.workspace.messages.selectAccount'))
      return
    }
    batchReconciling.value = true
    try {
      lastBatchResult.value = await batchReconcileTransactions({
        accountId: selectedAccountId.value,
        maxItems: batchMaxItems.value,
        strategy: BATCH_RECONCILE_STRATEGY
      })
      await refreshAccounts(true)
      ElMessage.success(t('wallet.workspace.messages.batchReconcileDone', {
        success: lastBatchResult.value.successCount,
        processed: lastBatchResult.value.processedCount
      }))
    } catch (error) {
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.batchReconcileFailed'))
    } finally {
      batchReconciling.value = false
    }
  }

  onMounted(() => {
    void loadData()
  })

  return {
    loading,
    loadingOverview,
    loadingPlan,
    loadingReconciliation,
    creating,
    receiving,
    sending,
    batchAdvancing,
    batchRemediating,
    batchReconciling,
    accounts,
    transactions,
    executionOverview,
    executionPlan,
    reconciliationOverview,
    lastBatchResult,
    selectedAccountId,
    txStatusFilter,
    batchMaxItems,
    walletBalanceMasked,
    selectedAccount,
    priorityQueue,
    refreshTransactions,
    refreshExecutionOverview,
    refreshExecutionPlan,
    refreshReconciliationOverview,
    loadData,
    onAccountChange,
    onWalletMaskingChange,
    onWalletParityAccountImported,
    onCreateAccount,
    onReceive,
    onSend,
    onBatchAdvance,
    onBatchRemediate,
    onBatchReconcile
  }
}
