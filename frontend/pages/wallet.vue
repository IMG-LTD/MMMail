<script setup lang="ts">
import type { WalletTransactionStatus } from '~/types/api'
import { useWalletTransactionActions } from '~/composables/useWalletTransactionActions'
import { useI18n } from '~/composables/useI18n'
import { useWalletWorkspace } from '~/composables/useWalletWorkspace'

const { t } = useI18n()
const workspace = useWalletWorkspace()
const transactionActions = useWalletTransactionActions({
  refreshTransactions: workspace.refreshTransactions,
  refreshExecutionOverview: workspace.refreshExecutionOverview,
  refreshExecutionPlan: workspace.refreshExecutionPlan,
  refreshReconciliationOverview: workspace.refreshReconciliationOverview
})

useHead(() => ({
  title: t('page.wallet.title')
}))

async function onBatchLimitChange(value: number): Promise<void> {
  workspace.batchMaxItems.value = value
  await workspace.refreshExecutionPlan()
}

async function onStatusChange(status: 'ALL' | WalletTransactionStatus): Promise<void> {
  workspace.txStatusFilter.value = status
  await workspace.refreshTransactions()
}
</script>

<template>
  <div class="mm-page" v-loading="workspace.loading.value">
    <section class="wallet-shell">
      <WalletWorkspaceHeader
        :accounts="workspace.accounts.value"
        :selected-account-id="workspace.selectedAccountId.value"
        :selected-account="workspace.selectedAccount.value"
        :wallet-balance-masked="workspace.walletBalanceMasked.value"
        @refresh="workspace.loadData"
        @change-account="workspace.onAccountChange"
      />

      <WalletParityPanel
        :account="workspace.selectedAccount.value"
        @account-imported="workspace.onWalletParityAccountImported"
        @masking-change="workspace.onWalletMaskingChange"
      />

      <WalletOperationsPanel
        :creating="workspace.creating.value"
        :receiving="workspace.receiving.value"
        :sending="workspace.sending.value"
        @create-account="workspace.onCreateAccount"
        @receive="workspace.onReceive"
        @send="workspace.onSend"
      />

      <WalletExecutionDesk
        :execution-overview="workspace.executionOverview.value"
        :execution-plan="workspace.executionPlan.value"
        :reconciliation-overview="workspace.reconciliationOverview.value"
        :priority-queue="workspace.priorityQueue.value"
        :last-batch-result="workspace.lastBatchResult.value"
        :batch-max-items="workspace.batchMaxItems.value"
        :loading-overview="workspace.loadingOverview.value"
        :loading-plan="workspace.loadingPlan.value"
        :loading-reconciliation="workspace.loadingReconciliation.value"
        :batch-advancing="workspace.batchAdvancing.value"
        :batch-remediating="workspace.batchRemediating.value"
        :batch-reconciling="workspace.batchReconciling.value"
        :advancing-tx-id="transactionActions.advancingTxId.value"
        :remediating-tx-id="transactionActions.remediatingTxId.value"
        @refresh-execution="workspace.refreshExecutionOverview"
        @refresh-plan="workspace.refreshExecutionPlan"
        @refresh-reconciliation="workspace.refreshReconciliationOverview"
        @update-batch-limit="onBatchLimitChange"
        @batch-advance="workspace.onBatchAdvance"
        @batch-remediate="workspace.onBatchRemediate"
        @batch-reconcile="workspace.onBatchReconcile"
        @advance-tx="transactionActions.onAdvanceTx"
        @remediate-tx="transactionActions.onRemediateTx"
      />

      <WalletTransactionsPanel
        :transactions="workspace.transactions.value"
        :tx-status-filter="workspace.txStatusFilter.value"
        :trace-dialog-visible="transactionActions.traceDialogVisible.value"
        :active-trace="transactionActions.activeTrace.value"
        :trace-loading="transactionActions.traceLoading.value"
        :signing-tx-id="transactionActions.signingTxId.value"
        :broadcasting-tx-id="transactionActions.broadcastingTxId.value"
        :confirming-tx-id="transactionActions.confirmingTxId.value"
        :failing-tx-id="transactionActions.failingTxId.value"
        @status-change="onStatusChange"
        @view-trace="transactionActions.onViewTrace"
        @sign="transactionActions.onSignTx"
        @broadcast="transactionActions.onBroadcastTx"
        @confirm="transactionActions.onConfirmTx"
        @fail="transactionActions.onFailTx"
        @close-trace="transactionActions.closeTraceDialog"
      />
    </section>
  </div>
</template>

<style scoped>
.wallet-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
