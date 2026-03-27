<script setup lang="ts">
import { computed } from 'vue'
import {
  buildNotificationChannelOptions,
  buildNotificationSeverityOptions,
  buildNotificationWorkflowOptions,
  type NotificationChannelFilter,
  type NotificationSeverityFilter,
  type NotificationWorkflowFilter
} from '~/utils/notification-center'
import { useI18n } from '~/composables/useI18n'

const severityFilter = defineModel<NotificationSeverityFilter>('severityFilter', { required: true })
const channelFilter = defineModel<NotificationChannelFilter>('channelFilter', { required: true })
const workflowFilter = defineModel<NotificationWorkflowFilter>('workflowFilter', { required: true })
const unreadOnly = defineModel<boolean>('unreadOnly', { required: true })
const includeSnoozed = defineModel<boolean>('includeSnoozed', { required: true })

const { t } = useI18n()
const severityOptions = computed(() => buildNotificationSeverityOptions(t))
const channelOptions = computed(() => buildNotificationChannelOptions(t))
const workflowOptions = computed(() => buildNotificationWorkflowOptions(t))
</script>

<template>
  <div class="filter-row mm-card">
    <el-select v-model="severityFilter" style="width: 170px">
      <el-option v-for="item in severityOptions" :key="item.value" :label="item.label" :value="item.value" />
    </el-select>
    <el-select v-model="channelFilter" style="width: 180px">
      <el-option v-for="item in channelOptions" :key="item.value" :label="item.label" :value="item.value" />
    </el-select>
    <el-select v-model="workflowFilter" style="width: 180px">
      <el-option v-for="item in workflowOptions" :key="item.value" :label="item.label" :value="item.value" />
    </el-select>
    <el-switch v-model="unreadOnly" :active-text="t('notifications.filters.unreadOnly')" />
    <el-switch v-model="includeSnoozed" :active-text="t('notifications.filters.includeSnoozed')" />
  </div>
</template>

<style scoped>
.filter-row {
  padding: 12px;
  display: flex;
  gap: 10px;
  align-items: center;
}

@media (max-width: 1024px) {
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
