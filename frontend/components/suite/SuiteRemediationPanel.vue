<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { SuiteRemediationAction, SuiteRemediationExecutionResult } from '~/types/api'
import { canExecuteAction, remediationExecutionStatusLabel } from '~/utils/suite-operations'

interface Props {
  priorityActions: SuiteRemediationAction[]
  lastExecutionResult: SuiteRemediationExecutionResult | null
  runningActionCode: string
  executeAction: (action: SuiteRemediationAction) => Promise<void>
}

defineProps<Props>()
const { t } = useI18n()
</script>

<template>
  <section class="mm-card suite-panel">
    <h2 class="mm-section-title">{{ t('suite.operations.remediation.title') }}</h2>
    <el-alert
      v-if="lastExecutionResult"
      :type="lastExecutionResult.status === 'SUCCESS' ? 'success' : lastExecutionResult.status === 'NO_OP' ? 'info' : 'warning'"
      :closable="false"
      class="posture-alert"
      show-icon
      :title="t('suite.operations.remediation.lastResult.title', {
        product: lastExecutionResult.productCode,
        action: lastExecutionResult.actionCode,
        status: remediationExecutionStatusLabel(lastExecutionResult.status, t)
      })"
      :description="lastExecutionResult.message"
    />
    <el-table :data="priorityActions" style="width: 100%">
      <el-table-column :label="t('suite.operations.remediation.columns.priority')" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.priority === 'P0' ? 'danger' : scope.row.priority === 'P1' ? 'warning' : 'info'">
            {{ scope.row.priority }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="productCode" :label="t('suite.operations.remediation.columns.product')" width="140" />
      <el-table-column :label="t('suite.operations.remediation.columns.action')" min-width="420">
        <template #default="scope">
          <div class="stack-cell">
            <span>{{ scope.row.action }}</span>
            <el-tag v-if="scope.row.actionCode" size="small" type="info">{{ scope.row.actionCode }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="t('suite.operations.remediation.columns.execute')" width="150">
        <template #default="scope">
          <el-button
            size="small"
            type="primary"
            plain
            :disabled="!canExecuteAction(scope.row)"
            :loading="scope.row.actionCode ? runningActionCode === scope.row.actionCode : false"
            @click="void executeAction(scope.row)"
          >
            {{ canExecuteAction(scope.row) ? t('suite.operations.remediation.actions.execute') : t('suite.operations.remediation.actions.manual') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
.suite-panel {
  padding: 20px;
}

.posture-alert {
  margin-top: 10px;
}

.stack-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
