<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

const SHORT_SNOOZE_HOURS = 2
const LONG_SNOOZE_HOURS = 24

defineProps<{
  selectedCount: number
  selectedActionCount: number
  lastWorkflowOperationId: string
  workflowOperating: boolean
}>()

const assigneeUserId = defineModel<number | null>('assigneeUserId', { required: true })
const assigneeDisplayName = defineModel<string>('assigneeDisplayName', { required: true })

const emit = defineEmits<{
  markSelectedRead: []
  runSelectedActions: []
  markAllRead: []
  undoLastWorkflowOperation: []
  archiveSelected: []
  ignoreSelected: []
  snoozeSelected: [hours: number]
  restoreSelected: []
  assignSelected: []
}>()

const { t } = useI18n()
</script>

<template>
  <div class="batch-bar mm-card">
    <div class="batch-summary">{{ t('notifications.batch.selected', { count: selectedCount }) }}</div>
    <div class="batch-actions">
      <el-button :disabled="selectedCount === 0" @click="emit('markSelectedRead')">
        {{ t('notifications.batch.markSelectedRead') }}
      </el-button>
      <el-button :disabled="selectedActionCount === 0" type="warning" @click="emit('runSelectedActions')">
        {{ t('notifications.batch.runSelectedActions') }}
      </el-button>
      <el-button type="success" @click="emit('markAllRead')">{{ t('notifications.batch.markAllRead') }}</el-button>
      <el-button
        type="warning"
        plain
        :disabled="!lastWorkflowOperationId"
        :loading="workflowOperating"
        @click="emit('undoLastWorkflowOperation')"
      >
        {{ t('notifications.batch.undoLastOperation') }}
      </el-button>
      <el-button type="primary" :disabled="selectedCount === 0" :loading="workflowOperating" @click="emit('archiveSelected')">
        {{ t('notifications.batch.archiveSelected') }}
      </el-button>
      <el-button type="danger" :disabled="selectedCount === 0" :loading="workflowOperating" @click="emit('ignoreSelected')">
        {{ t('notifications.batch.ignoreSelected') }}
      </el-button>
      <el-button type="info" :disabled="selectedCount === 0" :loading="workflowOperating" @click="emit('snoozeSelected', SHORT_SNOOZE_HOURS)">
        {{ t('notifications.batch.snooze2h') }}
      </el-button>
      <el-button type="info" :disabled="selectedCount === 0" :loading="workflowOperating" @click="emit('snoozeSelected', LONG_SNOOZE_HOURS)">
        {{ t('notifications.batch.snooze24h') }}
      </el-button>
      <el-button type="success" :disabled="selectedCount === 0" :loading="workflowOperating" @click="emit('restoreSelected')">
        {{ t('notifications.batch.restoreSelected') }}
      </el-button>
    </div>
    <div class="undo-state">
      <span>{{ t('notifications.batch.lastOperation', { value: lastWorkflowOperationId || t('common.none') }) }}</span>
    </div>
    <div class="assign-panel">
      <el-input-number v-model="assigneeUserId" :min="1" :placeholder="t('notifications.batch.assigneeId')" />
      <el-input v-model="assigneeDisplayName" :placeholder="t('notifications.batch.assigneeName')" />
      <el-button type="warning" :disabled="selectedCount === 0" :loading="workflowOperating" @click="emit('assignSelected')">
        {{ t('notifications.batch.assignSelected') }}
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.batch-bar {
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.batch-summary {
  color: #355161;
  font-size: 13px;
  font-weight: 600;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.assign-panel {
  display: grid;
  grid-template-columns: 140px 180px auto;
  gap: 8px;
  align-items: center;
}

.undo-state {
  color: #607481;
  font-size: 12px;
}

@media (max-width: 1024px) {
  .batch-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .assign-panel {
    grid-template-columns: 1fr;
  }
}
</style>
