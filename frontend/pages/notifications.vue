<script setup lang="ts">
import NotificationsBatchBar from '~/components/notifications/NotificationsBatchBar.vue'
import NotificationsFilterBar from '~/components/notifications/NotificationsFilterBar.vue'
import NotificationsOperationHistoryPanel from '~/components/notifications/NotificationsOperationHistoryPanel.vue'
import NotificationsSyncPanel from '~/components/notifications/NotificationsSyncPanel.vue'
import NotificationsTable from '~/components/notifications/NotificationsTable.vue'
import { useI18n } from '~/composables/useI18n'
import { useNotificationsWorkspace } from '~/composables/useNotificationsWorkspace'

definePageMeta({
  layout: 'default'
})

const { t } = useI18n()
const {
  assigneeDisplayNameInput,
  assigneeUserIdInput,
  archiveSelected,
  assignSelected,
  channelFilter,
  currentSessionId,
  executingActionId,
  filteredItems,
  historyUndoOperatingId,
  ignoreSelected,
  includeSnoozed,
  lastSyncedAt,
  lastWorkflowOperationId,
  latestSyncEvent,
  loading,
  markEverythingRead,
  markSelectedRead,
  onSelectionChange,
  openRoute,
  operationHistory,
  refreshNotifications,
  reconnectSync,
  restoreSelected,
  runAction,
  runSelectedActions,
  selectedActionCodes,
  selectedItems,
  severityFilter,
  snoozeSelected,
  summaryText,
  syncConflictMessage,
  syncCursor,
  syncErrorMessage,
  syncStatus,
  syncStatusTagType,
  syncUpdateCount,
  syncVersion,
  tableRef,
  unreadOnly,
  undoHistoryOperation,
  undoLastWorkflowOperation,
  workflowFilter,
  workflowOperating
} = useNotificationsWorkspace()

useHead(() => ({
  title: t('page.notifications.title')
}))
</script>

<template>
  <section class="notifications-page mm-card">
    <header class="hero">
      <div>
        <h1>{{ t('notifications.hero.title') }}</h1>
        <p>{{ summaryText }}</p>
      </div>
      <el-button :loading="loading" @click="refreshNotifications">
        {{ t('common.actions.refresh') }}
      </el-button>
    </header>

    <NotificationsSyncPanel
      :sync-status="syncStatus"
      :sync-error-message="syncErrorMessage"
      :sync-cursor="syncCursor"
      :sync-version="syncVersion"
      :last-synced-at="lastSyncedAt"
      :sync-update-count="syncUpdateCount"
      :sync-conflict-message="syncConflictMessage"
      :latest-sync-event="latestSyncEvent"
      :current-session-id="currentSessionId"
      :status-tag-type="syncStatusTagType"
      @refresh="refreshNotifications"
      @reconnect="reconnectSync"
    />

    <NotificationsFilterBar
      v-model:severity-filter="severityFilter"
      v-model:channel-filter="channelFilter"
      v-model:workflow-filter="workflowFilter"
      v-model:unread-only="unreadOnly"
      v-model:include-snoozed="includeSnoozed"
    />

    <NotificationsBatchBar
      v-model:assignee-user-id="assigneeUserIdInput"
      v-model:assignee-display-name="assigneeDisplayNameInput"
      :selected-count="selectedItems.length"
      :selected-action-count="selectedActionCodes.length"
      :last-workflow-operation-id="lastWorkflowOperationId"
      :workflow-operating="workflowOperating"
      @mark-selected-read="markSelectedRead"
      @run-selected-actions="runSelectedActions"
      @mark-all-read="markEverythingRead"
      @undo-last-workflow-operation="undoLastWorkflowOperation"
      @archive-selected="archiveSelected"
      @ignore-selected="ignoreSelected"
      @snooze-selected="snoozeSelected"
      @restore-selected="restoreSelected"
      @assign-selected="assignSelected"
    />

    <NotificationsOperationHistoryPanel
      :history="operationHistory"
      :workflow-operating="workflowOperating"
      :history-undo-operating-id="historyUndoOperatingId"
      @undo="undoHistoryOperation"
    />

    <NotificationsTable
      ref="tableRef"
      :items="filteredItems"
      :loading="loading"
      :executing-action-id="executingActionId"
      @selection-change="onSelectionChange"
      @open="openRoute"
      @run-action="runAction"
    />
  </section>
</template>

<style scoped>
.notifications-page {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero h1 {
  margin: 0;
  font-size: 26px;
  color: #143749;
}

.hero p {
  margin: 4px 0 0;
  color: #607481;
}

@media (max-width: 1024px) {
  .hero {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
}
</style>
