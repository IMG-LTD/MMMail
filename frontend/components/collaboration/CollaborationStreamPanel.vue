<script setup lang="ts">
import type { PropType } from 'vue'
import { useI18n } from '~/composables/useI18n'
import {
  isExternalCollaborationEvent,
  type MainlineCollaborationEvent
} from '~/utils/collaboration'

const props = defineProps({
  loading: {
    type: Boolean,
    required: true
  },
  items: {
    type: Array as PropType<readonly MainlineCollaborationEvent[]>,
    required: true
  },
  syncCursor: {
    type: Number,
    required: true
  },
  emptyStateDescription: {
    type: String,
    required: true
  },
  currentSessionId: {
    type: String,
    required: true
  }
})

const emit = defineEmits<{
  open: [item: MainlineCollaborationEvent]
}>()

const { t } = useI18n()

function formatDateTime(value: string): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

function productTagType(productCode: string): 'success' | 'warning' | 'primary' {
  if (productCode === 'MAIL') {
    return 'success'
  }
  if (productCode === 'CALENDAR') {
    return 'warning'
  }
  return 'primary'
}

function isExternalSession(item: MainlineCollaborationEvent): boolean {
  return isExternalCollaborationEvent(item, props.currentSessionId)
}

function openItem(item: MainlineCollaborationEvent): void {
  emit('open', item)
}
</script>

<template>
  <main class="stream-panel mm-card">
    <div class="panel-header">
      <div>
        <h2>{{ t('collaboration.stream.title') }}</h2>
        <p>{{ t('collaboration.stream.visibleCount', { count: props.items.length }) }}</p>
      </div>
      <el-tag type="info" effect="plain">{{ t('collaboration.stream.cursor', { cursor: props.syncCursor }) }}</el-tag>
    </div>

    <div v-if="props.loading" class="stream-loading">
      <el-skeleton :rows="6" animated />
    </div>

    <div v-else-if="props.items.length === 0" class="empty-state">
      <h3>{{ t('collaboration.stream.emptyTitle') }}</h3>
      <p>{{ props.emptyStateDescription }}</p>
    </div>

    <div v-else class="stream-list">
      <button
        v-for="item in props.items"
        :key="`${item.eventId}-${item.productCode}`"
        class="stream-card"
        @click="openItem(item)"
      >
        <div class="card-topline">
          <el-tag :type="productTagType(item.productCode)" effect="dark">{{ item.productCode }}</el-tag>
          <span class="event-type">{{ item.eventType }}</span>
          <span class="event-time">{{ formatDateTime(item.createdAt) }}</span>
        </div>
        <div class="card-content">
          <h3>{{ item.title }}</h3>
          <p>{{ item.summary }}</p>
        </div>
        <div class="card-footer">
          <span>{{ item.actorEmail || t('collaboration.stream.unknownActor') }}</span>
          <span v-if="isExternalSession(item)" class="external-pill">{{ t('collaboration.stream.otherSession') }}</span>
          <span v-else class="route-pill">{{ t('collaboration.stream.openProduct') }}</span>
        </div>
      </button>
    </div>
  </main>
</template>

<style scoped>
.stream-panel {
  padding: 18px;
  background: rgba(6, 18, 31, 0.78);
  border: 1px solid rgba(120, 227, 214, 0.12);
  box-shadow: 0 16px 36px rgba(2, 8, 15, 0.35);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.panel-header h2 {
  margin: 0;
  font-size: 20px;
}

.panel-header p {
  margin: 8px 0 0;
  color: rgba(236, 246, 255, 0.7);
  line-height: 1.5;
}

.stream-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.stream-card {
  width: 100%;
  text-align: left;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(120, 227, 214, 0.12);
  background: linear-gradient(180deg, rgba(17, 41, 58, 0.96), rgba(11, 28, 42, 0.96));
  color: inherit;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.stream-card:hover {
  transform: translateY(-2px);
  border-color: rgba(120, 227, 214, 0.34);
  box-shadow: 0 18px 32px rgba(2, 8, 15, 0.28);
}

.card-topline,
.card-footer {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.event-type,
.event-time,
.route-pill,
.external-pill {
  color: rgba(236, 246, 255, 0.66);
  font-size: 12px;
}

.card-content h3 {
  margin: 14px 0 8px;
  font-size: 18px;
}

.card-content p {
  margin: 0 0 14px;
  color: rgba(236, 246, 255, 0.76);
  line-height: 1.6;
}

.route-pill,
.external-pill {
  margin-left: auto;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(120, 227, 214, 0.12);
}

.external-pill {
  background: rgba(250, 204, 21, 0.15);
  color: #fde68a;
}

.empty-state {
  display: grid;
  place-items: center;
  min-height: 320px;
  text-align: center;
  border-radius: 18px;
  border: 1px dashed rgba(120, 227, 214, 0.2);
  background: rgba(12, 28, 42, 0.72);
}

.empty-state h3 {
  margin: 0 0 10px;
}

.empty-state p {
  max-width: 420px;
  margin: 0;
  color: rgba(236, 246, 255, 0.72);
}
</style>
