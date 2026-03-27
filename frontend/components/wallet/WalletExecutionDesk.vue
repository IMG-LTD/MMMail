<script setup lang="ts">
import { computed } from 'vue'
import type {
  WalletBatchActionResult,
  WalletExecutionOverview,
  WalletExecutionPlan,
  WalletPriorityTransaction,
  WalletReconciliationOverview,
  WalletTransaction
} from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import {
  walletOperationKey,
  walletRiskKey,
  walletRiskTagType,
  walletStatusKey,
  walletStatusTagType
} from '~/utils/wallet-workspace'

interface Props {
  executionOverview: WalletExecutionOverview | null
  executionPlan: WalletExecutionPlan | null
  reconciliationOverview: WalletReconciliationOverview | null
  priorityQueue: WalletPriorityTransaction[]
  lastBatchResult: WalletBatchActionResult | null
  batchMaxItems: number
  loadingOverview: boolean
  loadingPlan: boolean
  loadingReconciliation: boolean
  batchAdvancing: boolean
  batchRemediating: boolean
  batchReconciling: boolean
  advancingTxId: string
  remediatingTxId: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  refreshExecution: []
  refreshPlan: []
  refreshReconciliation: []
  updateBatchLimit: [value: number]
  batchAdvance: []
  batchRemediate: []
  batchReconcile: []
  advanceTx: [tx: WalletTransaction]
  remediateTx: [tx: WalletTransaction]
}>()

const { t } = useI18n()

const stageRows = computed(() => {
  if (!props.executionOverview) {
    return []
  }
  return [
    { key: 'PENDING', value: props.executionOverview.stageCounts.pendingCount },
    { key: 'SIGNED', value: props.executionOverview.stageCounts.signedCount },
    { key: 'BROADCASTED', value: props.executionOverview.stageCounts.broadcastedCount },
    { key: 'CONFIRMED', value: props.executionOverview.stageCounts.confirmedCount },
    { key: 'FAILED', value: props.executionOverview.stageCounts.failedCount }
  ]
})

function onBatchLimitChange(value: string | number | boolean): void {
  emit('updateBatchLimit', Number(value))
}
</script>

<template>
  <section class="wallet-execution-grid">
    <article class="mm-card wallet-panel" v-loading="loadingOverview">
      <div class="wallet-panel-head">
        <h2 class="mm-section-title">{{ t('wallet.workspace.execution.title') }}</h2>
        <div class="wallet-panel-actions">
          <el-button @click="emit('refreshExecution')">{{ t('wallet.workspace.execution.refreshExecution') }}</el-button>
          <el-button @click="emit('refreshPlan')">{{ t('wallet.workspace.execution.refreshPlan') }}</el-button>
        </div>
      </div>
      <div v-if="executionOverview" class="wallet-overview-grid">
        <article class="wallet-overview-card">
          <h3 class="mm-section-subtitle">{{ t('wallet.workspace.execution.health') }}</h3>
          <div class="wallet-overview-score">{{ executionOverview.executionHealthScore }}</div>
          <el-tag :type="walletRiskTagType(executionOverview.riskLevel)">
            {{ t(walletRiskKey(executionOverview.riskLevel)) }}
          </el-tag>
          <p class="mm-muted">{{ t('wallet.workspace.execution.blocked') }}: {{ executionOverview.blockedCount }}</p>
        </article>
        <article class="wallet-overview-card">
          <h3 class="mm-section-subtitle">{{ t('wallet.workspace.execution.stageDistribution') }}</h3>
          <div class="wallet-tag-row">
            <el-tag v-for="row in stageRows" :key="row.key" :type="walletStatusTagType(row.key as WalletTransaction['status'])">
              {{ t(walletStatusKey(row.key as WalletTransaction['status'])) }}: {{ row.value }}
            </el-tag>
          </div>
          <p class="mm-muted">{{ t('wallet.workspace.execution.generatedAt') }}: {{ executionOverview.generatedAt }}</p>
        </article>
      </div>
    </article>

    <article class="mm-card wallet-panel" v-loading="loadingPlan">
      <div class="wallet-panel-head">
        <h2 class="mm-section-title">{{ t('wallet.workspace.batch.title') }}</h2>
        <div class="wallet-panel-actions">
          <el-input-number :model-value="batchMaxItems" :min="1" :max="20" :step="1" @change="onBatchLimitChange" />
          <el-button type="primary" :loading="batchAdvancing" @click="emit('batchAdvance')">
            {{ t('wallet.workspace.batch.advance') }}
          </el-button>
          <el-button type="danger" plain :loading="batchRemediating" @click="emit('batchRemediate')">
            {{ t('wallet.workspace.batch.remediate') }}
          </el-button>
          <el-button type="warning" :loading="batchReconciling" @click="emit('batchReconcile')">
            {{ t('wallet.workspace.batch.reconcile') }}
          </el-button>
        </div>
      </div>
      <div v-if="executionPlan" class="wallet-tag-row">
        <el-tag type="info">{{ t('wallet.workspace.batch.recommendedAdvance') }}: {{ executionPlan.recommendedAdvanceCount }}</el-tag>
        <el-tag type="warning">{{ t('wallet.workspace.batch.recommendedRemediation') }}: {{ executionPlan.recommendedRemediationCount }}</el-tag>
        <el-tag type="success">{{ t('wallet.workspace.batch.riskDelta') }}: -{{ executionPlan.estimatedRiskDelta }}</el-tag>
      </div>
      <el-table v-if="executionPlan" :data="executionPlan.items" style="width: 100%">
        <el-table-column prop="transactionId" :label="t('wallet.workspace.transactions.columns.txId')" min-width="150" />
        <el-table-column :label="t('wallet.workspace.transactions.columns.status')" width="140">
          <template #default="scope">
            <el-tag :type="walletStatusTagType(scope.row.status)">{{ t(walletStatusKey(scope.row.status)) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('wallet.workspace.priority.recommended')" min-width="190">
          <template #default="scope">{{ t(walletOperationKey(scope.row.recommendedOperation)) }}</template>
        </el-table-column>
        <el-table-column prop="priority" :label="t('wallet.workspace.priority.priority')" width="100" />
        <el-table-column prop="reason" :label="t('wallet.workspace.priority.reason')" min-width="260" />
      </el-table>
      <el-alert
        v-if="lastBatchResult"
        class="wallet-batch-result"
        type="success"
        show-icon
        :closable="false"
        :title="t('wallet.workspace.batch.result', {
          operation: lastBatchResult.operation,
          success: lastBatchResult.successCount,
          failed: lastBatchResult.failedCount,
          skipped: lastBatchResult.skippedCount
        })"
      />
    </article>

    <article class="mm-card wallet-panel" v-loading="loadingReconciliation">
      <div class="wallet-panel-head">
        <h2 class="mm-section-title">{{ t('wallet.workspace.reconciliation.title') }}</h2>
        <el-button @click="emit('refreshReconciliation')">{{ t('wallet.workspace.reconciliation.refresh') }}</el-button>
      </div>
      <template v-if="reconciliationOverview">
        <div class="wallet-tag-row">
          <el-tag type="success">{{ t('wallet.workspace.reconciliation.integrity') }}: {{ reconciliationOverview.integrityScore }}</el-tag>
          <el-tag :type="walletRiskTagType(reconciliationOverview.riskLevel)">
            {{ t('wallet.workspace.reconciliation.risk') }}: {{ t(walletRiskKey(reconciliationOverview.riskLevel)) }}
          </el-tag>
          <el-tag type="danger">{{ t('wallet.workspace.reconciliation.mismatch') }}: {{ reconciliationOverview.mismatchCount }}</el-tag>
          <el-tag type="warning">{{ t('wallet.workspace.reconciliation.blocked') }}: {{ reconciliationOverview.blockedCount }}</el-tag>
          <el-tag type="danger">{{ t('wallet.workspace.reconciliation.failed') }}: {{ reconciliationOverview.failedCount }}</el-tag>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item :label="t('wallet.workspace.reconciliation.recommendedActions')">
            <div class="wallet-tag-row">
              <el-tag
                v-for="action in reconciliationOverview.recommendedActions"
                :key="`reconcile-action-${action}`"
                type="info"
              >
                {{ action }}
              </el-tag>
            </div>
          </el-descriptions-item>
          <el-descriptions-item :label="t('wallet.workspace.reconciliation.generatedAt')">
            {{ reconciliationOverview.generatedAt }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </article>

    <article class="mm-card wallet-panel">
      <h2 class="mm-section-title">{{ t('wallet.workspace.priority.title') }}</h2>
      <el-table :data="priorityQueue" style="width: 100%">
        <el-table-column prop="transactionId" :label="t('wallet.workspace.transactions.columns.txId')" min-width="150" />
        <el-table-column :label="t('wallet.workspace.transactions.columns.status')" width="140">
          <template #default="scope">
            <el-tag :type="walletStatusTagType(scope.row.status)">{{ t(walletStatusKey(scope.row.status)) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ageMinutes" :label="t('wallet.workspace.priority.ageMinutes')" width="120" />
        <el-table-column prop="reason" :label="t('wallet.workspace.priority.reason')" min-width="280" />
        <el-table-column :label="t('wallet.workspace.priority.recommended')" min-width="230">
          <template #default="scope">
            <div class="wallet-tag-row">
              <el-tag v-for="action in scope.row.recommendedActions" :key="`${scope.row.transactionId}-${action}`" size="small" type="warning">
                {{ t(walletOperationKey(action)) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('wallet.workspace.priority.actions')" width="220" fixed="right">
          <template #default="scope">
            <div class="wallet-tx-actions">
              <el-button
                size="small"
                type="primary"
                :loading="advancingTxId === scope.row.transactionId"
                @click="emit('advanceTx', scope.row)"
              >
                {{ t('wallet.workspace.actions.advance') }}
              </el-button>
              <el-button
                size="small"
                type="danger"
                plain
                :loading="remediatingTxId === scope.row.transactionId"
                @click="emit('remediateTx', scope.row)"
              >
                {{ t('wallet.workspace.actions.remediate') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </article>
  </section>
</template>

<style scoped>
.wallet-execution-grid {
  display: grid;
  gap: 16px;
}

.wallet-panel {
  padding: 14px;
}

.wallet-panel-head,
.wallet-panel-actions,
.wallet-tx-actions,
.wallet-tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.wallet-panel-head {
  justify-content: space-between;
  margin-bottom: 12px;
}

.wallet-overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.wallet-overview-card {
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.wallet-overview-score {
  font-size: 28px;
  font-weight: 700;
}

.wallet-batch-result {
  margin-top: 12px;
}

@media (max-width: 1100px) {
  .wallet-overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
