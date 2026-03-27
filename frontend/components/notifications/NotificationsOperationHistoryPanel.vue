<script setup lang="ts">
import { computed } from 'vue'
import type {
  SuiteNotificationOperationHistory,
  SuiteNotificationOperationHistoryItem
} from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import {
  buildOperationHistoryLine,
  buildOperationHistorySummary,
  formatNotificationDateTime,
  operationTagType,
  translateNotificationOperation
} from '~/utils/notification-center'

const props = defineProps<{
  history: SuiteNotificationOperationHistory | null
  workflowOperating: boolean
  historyUndoOperatingId: string
}>()

const emit = defineEmits<{
  undo: [item: SuiteNotificationOperationHistoryItem]
}>()

const { locale, t } = useI18n()
const historySummary = computed(() => buildOperationHistorySummary(props.history, t))

function canUndo(item: SuiteNotificationOperationHistoryItem): boolean {
  return item.undoAvailable && !props.workflowOperating
}
</script>

<template>
  <section class="operation-history mm-card">
    <header class="operation-history__header">
      <h2>{{ t('notifications.history.title') }}</h2>
      <span>{{ historySummary }}</span>
    </header>
    <el-empty
      v-if="!history || history.items.length === 0"
      :description="t('notifications.history.empty')"
    />
    <div v-else class="operation-history__list">
      <article
        v-for="item in history.items"
        :key="item.operationId"
        class="operation-history__item"
      >
        <div class="operation-history__meta">
          <div class="operation-history__title">
            <el-tag :type="operationTagType(item.operation)" effect="dark">
              {{ translateNotificationOperation(item.operation, t) }}
            </el-tag>
            <span class="operation-history__id">{{ item.operationId }}</span>
          </div>
          <p>{{ buildOperationHistoryLine(item, t) }}</p>
          <p>{{ formatNotificationDateTime(item.executedAt, locale) }}</p>
        </div>
        <el-button
          type="warning"
          plain
          :disabled="!canUndo(item)"
          :loading="historyUndoOperatingId === item.operationId"
          @click="emit('undo', item)"
        >
          {{ item.undoAvailable ? t('notifications.history.undo') : t('notifications.history.undone') }}
        </el-button>
      </article>
    </div>
  </section>
</template>

<style scoped>
.operation-history {
  padding: 12px;
}

.operation-history__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.operation-history__header h2 {
  margin: 0;
  font-size: 18px;
  color: #1f3f54;
}

.operation-history__header span {
  color: #607481;
  font-size: 13px;
}

.operation-history__list {
  display: grid;
  gap: 10px;
}

.operation-history__item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid #d9e7ec;
  background: linear-gradient(135deg, #f8fbfc, #eef5f8);
}

.operation-history__meta p {
  margin: 4px 0 0;
  color: #355161;
  font-size: 12px;
}

.operation-history__title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.operation-history__id {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  color: #607481;
}
</style>
