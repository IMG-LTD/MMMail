<script setup lang="ts">
import type { WalletExecutionTrace, WalletTransaction, WalletTransactionStatus } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import {
  canBroadcastWalletTransaction,
  canFinalizeWalletTransaction,
  canSignWalletTransaction,
  formatWalletWorkspaceAmount,
  walletStatusKey,
  walletStatusTagType,
  walletTransactionTypeKey
} from '~/utils/wallet-workspace'

interface Props {
  transactions: WalletTransaction[]
  txStatusFilter: 'ALL' | WalletTransactionStatus
  traceDialogVisible: boolean
  activeTrace: WalletExecutionTrace | null
  traceLoading: boolean
  signingTxId: string
  broadcastingTxId: string
  confirmingTxId: string
  failingTxId: string
}

defineProps<Props>()
const emit = defineEmits<{
  statusChange: [status: 'ALL' | WalletTransactionStatus]
  viewTrace: [tx: WalletTransaction]
  sign: [tx: WalletTransaction]
  broadcast: [tx: WalletTransaction]
  confirm: [tx: WalletTransaction]
  fail: [tx: WalletTransaction]
  closeTrace: []
}>()

const { t } = useI18n()
</script>

<template>
  <section class="mm-card wallet-transactions-card">
    <div class="wallet-transactions-head">
      <h2 class="mm-section-title">{{ t('wallet.workspace.transactions.title') }}</h2>
      <el-select
        :model-value="txStatusFilter"
        :placeholder="t('wallet.workspace.transactions.statusFilter')"
        style="width: 220px"
        @change="emit('statusChange', $event as 'ALL' | WalletTransactionStatus)"
      >
        <el-option :label="t('wallet.workspace.transactions.all')" value="ALL" />
        <el-option :label="t('wallet.workspace.status.PENDING')" value="PENDING" />
        <el-option :label="t('wallet.workspace.status.SIGNED')" value="SIGNED" />
        <el-option :label="t('wallet.workspace.status.BROADCASTED')" value="BROADCASTED" />
        <el-option :label="t('wallet.workspace.status.CONFIRMED')" value="CONFIRMED" />
        <el-option :label="t('wallet.workspace.status.FAILED')" value="FAILED" />
      </el-select>
    </div>

    <el-table :data="transactions" style="width: 100%">
      <el-table-column prop="transactionId" :label="t('wallet.workspace.transactions.columns.txId')" min-width="150" />
      <el-table-column :label="t('wallet.workspace.transactions.columns.type')" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.txType === 'RECEIVE' ? 'success' : 'warning'">
            {{ t(walletTransactionTypeKey(scope.row.txType)) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="counterpartyAddress" :label="t('wallet.workspace.transactions.columns.counterparty')" min-width="220" />
      <el-table-column :label="t('wallet.workspace.transactions.columns.amount')" min-width="150">
        <template #default="scope">{{ formatWalletWorkspaceAmount(scope.row.amountMinor, scope.row.assetSymbol) }}</template>
      </el-table-column>
      <el-table-column :label="t('wallet.workspace.transactions.columns.status')" width="130">
        <template #default="scope">
          <el-tag :type="walletStatusTagType(scope.row.status)">{{ t(walletStatusKey(scope.row.status)) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="confirmations" :label="t('wallet.workspace.transactions.columns.confirmations')" width="130" />
      <el-table-column prop="signatureHash" :label="t('wallet.workspace.transactions.columns.signature')" min-width="180" />
      <el-table-column prop="networkTxHash" :label="t('wallet.workspace.transactions.columns.txHash')" min-width="180" />
      <el-table-column prop="createdAt" :label="t('wallet.workspace.transactions.columns.createdAt')" min-width="180" />
      <el-table-column :label="t('wallet.workspace.transactions.columns.actions')" min-width="320" fixed="right">
        <template #default="scope">
          <div class="wallet-actions">
            <el-button size="small" type="info" plain :loading="traceLoading && activeTrace?.transactionId === scope.row.transactionId" @click="emit('viewTrace', scope.row)">
              {{ t('wallet.workspace.actions.trace') }}
            </el-button>
            <el-button size="small" type="primary" :disabled="!canSignWalletTransaction(scope.row)" :loading="signingTxId === scope.row.transactionId" @click="emit('sign', scope.row)">
              {{ t('wallet.workspace.actions.sign') }}
            </el-button>
            <el-button size="small" type="info" :disabled="!canBroadcastWalletTransaction(scope.row)" :loading="broadcastingTxId === scope.row.transactionId" @click="emit('broadcast', scope.row)">
              {{ t('wallet.workspace.actions.broadcast') }}
            </el-button>
            <el-button size="small" type="success" :disabled="!canFinalizeWalletTransaction(scope.row)" :loading="confirmingTxId === scope.row.transactionId" @click="emit('confirm', scope.row)">
              {{ t('wallet.workspace.actions.confirm') }}
            </el-button>
            <el-button size="small" type="danger" :disabled="!canFinalizeWalletTransaction(scope.row)" :loading="failingTxId === scope.row.transactionId" @click="emit('fail', scope.row)">
              {{ t('wallet.workspace.actions.fail') }}
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :model-value="traceDialogVisible" :title="t('wallet.workspace.trace.title')" width="720px" @close="emit('closeTrace')">
      <div v-loading="traceLoading">
        <template v-if="activeTrace">
          <div class="wallet-trace-tags">
            <el-tag type="primary">{{ t('wallet.workspace.trace.tx') }}: {{ activeTrace.transactionId }}</el-tag>
            <el-tag :type="walletStatusTagType(activeTrace.currentStatus)">
              {{ t('wallet.workspace.trace.status') }}: {{ t(walletStatusKey(activeTrace.currentStatus)) }}
            </el-tag>
            <el-tag type="success">{{ t('wallet.workspace.trace.integrity') }}: {{ activeTrace.integrityScore }}</el-tag>
          </div>
          <el-alert
            v-for="warning in activeTrace.warnings"
            :key="warning"
            class="wallet-trace-alert"
            type="warning"
            show-icon
            :closable="false"
            :title="warning"
          />
          <el-table :data="activeTrace.stageEvents" style="width: 100%; margin-top: 12px">
            <el-table-column prop="stage" :label="t('wallet.workspace.trace.columns.stage')" width="140" />
            <el-table-column prop="source" :label="t('wallet.workspace.trace.columns.source')" width="160" />
            <el-table-column prop="at" :label="t('wallet.workspace.trace.columns.at')" min-width="180" />
            <el-table-column prop="message" :label="t('wallet.workspace.trace.columns.message')" min-width="220" />
          </el-table>
        </template>
      </div>
    </el-dialog>
  </section>
</template>

<style scoped>
.wallet-transactions-card {
  padding: 14px;
}

.wallet-transactions-head,
.wallet-actions,
.wallet-trace-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.wallet-transactions-head {
  justify-content: space-between;
  margin-bottom: 12px;
}

.wallet-trace-alert {
  margin-top: 12px;
}
</style>
