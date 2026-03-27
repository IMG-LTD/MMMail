import { ElMessage } from 'element-plus'
import { ref, type Ref } from 'vue'
import type { WalletExecutionTrace, WalletTransaction } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { useWalletApi } from '~/composables/useWalletApi'
import {
  buildManualWalletTxHash,
  canAdvanceWalletTransaction,
  canBroadcastWalletTransaction,
  canFinalizeWalletTransaction,
  canRemediateWalletTransaction,
  canSignWalletTransaction,
  defaultWalletRemediationStrategy,
  walletStatusKey
} from '~/utils/wallet-workspace'

interface Options {
  refreshTransactions: () => Promise<void>
  refreshExecutionOverview: () => Promise<void>
  refreshExecutionPlan: () => Promise<void>
  refreshReconciliationOverview: () => Promise<void>
}

const DEFAULT_CONFIRMATIONS = 6
const DESK_ACTION_HINT = 'desk-action'
const DESK_FAIL_REASON = 'desk-fail'
const SIGNER_HINT_PREFIX = 'desk-sign-'

export function useWalletTransactionActions(options: Options) {
  const { t } = useI18n()
  const {
    advanceTransaction,
    broadcastTransaction,
    confirmTransaction,
    failTransaction,
    getExecutionTrace,
    remediateTransaction,
    signTransaction
  } = useWalletApi()

  const traceLoading = ref(false)
  const advancingTxId = ref('')
  const remediatingTxId = ref('')
  const signingTxId = ref('')
  const broadcastingTxId = ref('')
  const confirmingTxId = ref('')
  const failingTxId = ref('')
  const activeTrace = ref<WalletExecutionTrace | null>(null)
  const traceDialogVisible = ref(false)

  async function refreshRelatedData(): Promise<void> {
    await Promise.all([
      options.refreshTransactions(),
      options.refreshExecutionOverview(),
      options.refreshExecutionPlan(),
      options.refreshReconciliationOverview()
    ])
  }

  async function onAdvanceTx(tx: WalletTransaction): Promise<void> {
    if (!canAdvanceWalletTransaction(tx)) {
      ElMessage.warning(t('wallet.workspace.messages.advanceGuard'))
      return
    }
    advancingTxId.value = tx.transactionId
    try {
      const result = await advanceTransaction(tx.transactionId, {
        operatorHint: DESK_ACTION_HINT
      })
      await refreshRelatedData()
      ElMessage.success(t('wallet.workspace.messages.advanceDone', {
        from: t(walletStatusKey(result.fromStatus)),
        to: t(walletStatusKey(result.toStatus))
      }))
    } catch (error) {
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.advanceFailed'))
    } finally {
      advancingTxId.value = ''
    }
  }

  async function onRemediateTx(tx: WalletTransaction): Promise<void> {
    if (!canRemediateWalletTransaction(tx)) {
      ElMessage.warning(t('wallet.workspace.messages.remediateGuard'))
      return
    }
    remediatingTxId.value = tx.transactionId
    const strategy = defaultWalletRemediationStrategy(tx.status)
    try {
      const result = await remediateTransaction(tx.transactionId, {
        strategy,
        reason: DESK_ACTION_HINT
      })
      await refreshRelatedData()
      ElMessage.success(t('wallet.workspace.messages.remediateDone', { message: result.message || strategy }))
    } catch (error) {
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.remediateFailed'))
    } finally {
      remediatingTxId.value = ''
    }
  }

  async function onViewTrace(tx: WalletTransaction): Promise<void> {
    traceDialogVisible.value = true
    traceLoading.value = true
    activeTrace.value = null
    try {
      activeTrace.value = await getExecutionTrace(tx.transactionId)
    } catch (error) {
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.loadTraceFailed'))
    } finally {
      traceLoading.value = false
    }
  }

  async function onSignTx(tx: WalletTransaction): Promise<void> {
    if (!canSignWalletTransaction(tx)) {
      ElMessage.warning(t('wallet.workspace.messages.signGuard'))
      return
    }
    signingTxId.value = tx.transactionId
    try {
      await signTransaction(tx.transactionId, {
        signerHint: `${SIGNER_HINT_PREFIX}${Date.now()}`
      })
      await refreshRelatedData()
      ElMessage.success(t('wallet.workspace.messages.signed'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.signFailed'))
    } finally {
      signingTxId.value = ''
    }
  }

  async function onConfirmTx(tx: WalletTransaction): Promise<void> {
    if (!canFinalizeWalletTransaction(tx)) {
      ElMessage.warning(t('wallet.workspace.messages.confirmGuard'))
      return
    }
    confirmingTxId.value = tx.transactionId
    try {
      await confirmTransaction(tx.transactionId, {
        confirmations: DEFAULT_CONFIRMATIONS,
        networkTxHash: tx.networkTxHash || buildManualWalletTxHash(tx.transactionId)
      })
      await refreshRelatedData()
      ElMessage.success(t('wallet.workspace.messages.confirmed'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.confirmFailed'))
    } finally {
      confirmingTxId.value = ''
    }
  }

  async function onFailTx(tx: WalletTransaction): Promise<void> {
    if (!canFinalizeWalletTransaction(tx)) {
      ElMessage.warning(t('wallet.workspace.messages.failGuard'))
      return
    }
    failingTxId.value = tx.transactionId
    try {
      await failTransaction(tx.transactionId, {
        reason: DESK_FAIL_REASON
      })
      await refreshRelatedData()
      ElMessage.success(t('wallet.workspace.messages.failed'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.failFailed'))
    } finally {
      failingTxId.value = ''
    }
  }

  async function onBroadcastTx(tx: WalletTransaction): Promise<void> {
    if (!canBroadcastWalletTransaction(tx)) {
      ElMessage.warning(t('wallet.workspace.messages.broadcastGuard'))
      return
    }
    broadcastingTxId.value = tx.transactionId
    try {
      await broadcastTransaction(tx.transactionId, {
        networkTxHash: tx.networkTxHash || buildManualWalletTxHash(tx.transactionId)
      })
      await refreshRelatedData()
      ElMessage.success(t('wallet.workspace.messages.broadcasted'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('wallet.workspace.messages.broadcastFailed'))
    } finally {
      broadcastingTxId.value = ''
    }
  }

  function closeTraceDialog(): void {
    traceDialogVisible.value = false
  }

  return {
    traceLoading,
    advancingTxId,
    remediatingTxId,
    signingTxId,
    broadcastingTxId,
    confirmingTxId,
    failingTxId,
    activeTrace,
    traceDialogVisible,
    onAdvanceTx,
    onRemediateTx,
    onViewTrace,
    onSignTx,
    onConfirmTx,
    onFailTx,
    onBroadcastTx,
    closeTraceDialog
  }
}
