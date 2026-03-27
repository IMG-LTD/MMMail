<script setup lang="ts">
import { ref } from 'vue'
import type { SuiteNotificationItem } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import {
  formatNotificationDateTime,
  severityTagType,
  translateNotificationChannel,
  translateNotificationSeverity,
  translateNotificationState,
  translateNotificationWorkflow,
  workflowTagType
} from '~/utils/notification-center'

const props = defineProps<{
  items: SuiteNotificationItem[]
  loading?: boolean
  executingActionId: string
}>()

const emit = defineEmits<{
  selectionChange: [items: SuiteNotificationItem[]]
  open: [item: SuiteNotificationItem]
  runAction: [item: SuiteNotificationItem]
}>()

const { locale, t } = useI18n()
const tableRef = ref<{
  clearSelection: () => void
  toggleRowSelection: (row: SuiteNotificationItem, selected?: boolean) => void
} | null>(null)

function clearSelection(): void {
  tableRef.value?.clearSelection()
}

function toggleRowSelection(row: SuiteNotificationItem, selected?: boolean): void {
  tableRef.value?.toggleRowSelection(row, selected)
}

defineExpose({
  clearSelection,
  toggleRowSelection
})
</script>

<template>
  <el-table
    ref="tableRef"
    :data="items"
    v-loading="loading"
    row-key="notificationId"
    size="small"
    @selection-change="emit('selectionChange', $event)"
  >
    <el-table-column type="selection" width="48" />
    <el-table-column :label="t('notifications.table.columns.state')" width="110">
      <template #default="{ row }">
        <el-tag :type="row.read ? 'info' : 'success'" effect="dark">
          {{ translateNotificationState(row.read, t) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column :label="t('notifications.table.columns.workflow')" width="130">
      <template #default="{ row }">
        <el-tag :type="workflowTagType(row.workflowStatus)" effect="dark">
          {{ translateNotificationWorkflow(row.workflowStatus, t) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column :label="t('notifications.table.columns.channel')" width="130">
      <template #default="{ row }">
        <el-tag effect="plain">{{ translateNotificationChannel(row.channel, t) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column :label="t('notifications.table.columns.severity')" width="120">
      <template #default="{ row }">
        <el-tag :type="severityTagType(row.severity)" effect="dark">
          {{ translateNotificationSeverity(row.severity, t) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="title" :label="t('notifications.table.columns.title')" width="260" />
    <el-table-column prop="message" :label="t('notifications.table.columns.message')" />
    <el-table-column :label="t('notifications.table.columns.assignee')" width="180">
      <template #default="{ row }">
        <span>{{ row.assignedToDisplayName || t('common.none') }}</span>
      </template>
    </el-table-column>
    <el-table-column :label="t('notifications.table.columns.snoozedUntil')" width="180">
      <template #default="{ row }">
        <span>{{ formatNotificationDateTime(row.snoozedUntil, locale) }}</span>
      </template>
    </el-table-column>
    <el-table-column :label="t('notifications.table.columns.createdAt')" width="200">
      <template #default="{ row }">
        <span>{{ formatNotificationDateTime(row.createdAt, locale) }}</span>
      </template>
    </el-table-column>
    <el-table-column :label="t('notifications.table.columns.actions')" width="220">
      <template #default="{ row }">
        <div class="actions">
          <el-button link type="primary" @click="emit('open', row)">{{ t('notifications.table.actions.open') }}</el-button>
          <el-button
            v-if="row.actionCode"
            link
            type="warning"
            :loading="executingActionId === row.notificationId"
            @click="emit('runAction', row)"
          >
            {{ t('notifications.table.actions.runAction') }}
          </el-button>
        </div>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
