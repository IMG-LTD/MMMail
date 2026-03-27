<script setup lang="ts">
import { computed } from 'vue'
import type { SuiteNotificationSyncEvent } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import type { NotificationSyncStatus } from '~/composables/useNotificationSyncStream'
import {
  buildLatestSyncSummary,
  buildSyncStatusLabel,
  buildSyncStatusText,
  formatNotificationDateTime
} from '~/utils/notification-center'

const props = defineProps<{
  syncStatus: NotificationSyncStatus
  syncErrorMessage: string
  syncCursor: number
  syncVersion: string
  lastSyncedAt: string
  syncUpdateCount: number
  syncConflictMessage: string
  latestSyncEvent: SuiteNotificationSyncEvent | null
  currentSessionId: string
  statusTagType: 'success' | 'warning' | 'info'
}>()

const emit = defineEmits<{
  refresh: []
  reconnect: []
}>()

const { locale, t } = useI18n()
const syncStatusLabel = computed(() => buildSyncStatusLabel(props.syncStatus, t))
const syncStatusText = computed(() => buildSyncStatusText(props.syncStatus, props.syncErrorMessage, t))
const latestSyncSummary = computed(() => buildLatestSyncSummary(props.latestSyncEvent, props.currentSessionId, t))
const latestSyncEventAt = computed(() => formatNotificationDateTime(props.latestSyncEvent?.createdAt, locale.value))
const lastSyncedText = computed(() => formatNotificationDateTime(props.lastSyncedAt, locale.value))
const showReconnect = computed(() => props.syncStatus !== 'CONNECTED')
</script>

<template>
  <section class="sync-panel mm-card">
    <div class="sync-panel__top">
      <div class="sync-panel__status">
        <el-tag :type="statusTagType" effect="dark">{{ syncStatusLabel }}</el-tag>
        <span>{{ syncStatusText }}</span>
      </div>
      <div class="sync-panel__actions">
        <el-button text @click="emit('refresh')">{{ t('notifications.sync.actions.refreshNow') }}</el-button>
        <el-button v-if="showReconnect" text type="warning" @click="emit('reconnect')">
          {{ t('notifications.sync.actions.reconnect') }}
        </el-button>
      </div>
    </div>
    <div class="sync-panel__meta">
      <span>{{ t('notifications.sync.meta.cursor', { value: syncCursor }) }}</span>
      <span>{{ t('notifications.sync.meta.version', { value: syncVersion }) }}</span>
      <span>{{ t('notifications.sync.meta.lastSync', { value: lastSyncedText }) }}</span>
      <span>{{ t('notifications.sync.meta.updates', { value: syncUpdateCount }) }}</span>
    </div>
    <el-alert
      v-if="syncConflictMessage"
      type="warning"
      :closable="false"
      show-icon
      :title="syncConflictMessage"
    />
    <div class="sync-panel__event">
      <strong>{{ t('notifications.sync.latestEvent') }}</strong>
      <span>{{ latestSyncSummary }}</span>
      <span v-if="latestSyncEvent">{{ latestSyncEventAt }}</span>
    </div>
  </section>
</template>

<style scoped>
.sync-panel {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: linear-gradient(135deg, #f6fbff, #eef7ff 52%, #f8fbff);
  border: 1px solid #dbe7f2;
}

.sync-panel__top,
.sync-panel__meta,
.sync-panel__event,
.sync-panel__status {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.sync-panel__top {
  justify-content: space-between;
}

.sync-panel__meta,
.sync-panel__event {
  color: #4c6473;
  font-size: 13px;
}

@media (max-width: 1024px) {
  .sync-panel__top {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
