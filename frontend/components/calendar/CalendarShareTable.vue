<script setup lang="ts">
import type { CalendarEventShare } from '~/types/api'
import { useI18n } from '~/composables/useI18n'

const props = defineProps<{
  shares: CalendarEventShare[]
  mutationShareId: string
}>()

const emit = defineEmits<{
  (event: 'update-permission', payload: { shareId: string; permission: 'VIEW' | 'EDIT' }): void
  (event: 'remove', shareId: string): void
}>()

const { t } = useI18n()

function statusType(status: CalendarEventShare['responseStatus']): 'warning' | 'success' | 'info' {
  if (status === 'NEEDS_ACTION') return 'warning'
  if (status === 'ACCEPTED') return 'success'
  return 'info'
}

function onPermissionChange(shareId: string, value: string | number | boolean): void {
  if (value === 'VIEW' || value === 'EDIT') {
    emit('update-permission', { shareId, permission: value })
  }
}
</script>

<template>
  <el-table :data="shares" style="width: 100%">
    <el-table-column prop="targetEmail" :label="t('calendar.share.columns.target')" min-width="180" />
    <el-table-column :label="t('calendar.share.columns.permission')" width="160">
      <template #default="scope">
        <el-select
          :model-value="scope.row.permission"
          size="small"
          :disabled="mutationShareId === scope.row.id"
          @update:model-value="onPermissionChange(scope.row.id, $event)"
        >
          <el-option :label="t('calendar.permissions.view')" value="VIEW" />
          <el-option :label="t('calendar.permissions.edit')" value="EDIT" />
        </el-select>
      </template>
    </el-table-column>
    <el-table-column :label="t('calendar.share.columns.status')" width="130">
      <template #default="scope">
        <el-tag :type="statusType(scope.row.responseStatus)" effect="light">{{ t(`calendar.status.${scope.row.responseStatus.toLowerCase()}`) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column :label="t('calendar.share.columns.updatedAt')" min-width="170">
      <template #default="scope">
        {{ scope.row.updatedAt.replace('T', ' ').slice(0, 19) }}
      </template>
    </el-table-column>
    <el-table-column :label="t('calendar.share.columns.actions')" width="100">
      <template #default="scope">
        <el-button
          type="danger"
          text
          :loading="mutationShareId === scope.row.id"
          @click="emit('remove', scope.row.id)"
        >
          {{ t('calendar.actions.remove') }}
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
