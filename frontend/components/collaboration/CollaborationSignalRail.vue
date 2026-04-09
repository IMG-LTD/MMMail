<script setup lang="ts">
import type { PropType } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { CollaborationFilter, CollaborationFilterOption } from '~/utils/collaboration'

const props = defineProps({
  filterOptions: {
    type: Array as PropType<readonly CollaborationFilterOption[]>,
    required: true
  },
  productFilter: {
    type: String as PropType<CollaborationFilter>,
    required: true
  },
  contextDescription: {
    type: String,
    required: true
  },
  syncVersion: {
    type: String,
    required: true
  },
  syncUpdateCount: {
    type: Number,
    required: true
  },
  lastSyncedAt: {
    type: String,
    required: true
  }
})

const emit = defineEmits<{
  select: [value: CollaborationFilter]
}>()

const { t } = useI18n()

function formatDateTime(value: string): string {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

function onSelect(value: CollaborationFilter): void {
  emit('select', value)
}
</script>

<template>
  <aside class="signal-rail mm-card">
    <div class="rail-header">
      <h2>{{ t('collaboration.signals.title') }}</h2>
      <p>{{ contextDescription }}</p>
    </div>

    <div class="filter-list">
      <button
        v-for="option in props.filterOptions"
        :key="option.value"
        class="filter-chip"
        :class="{ active: props.productFilter === option.value }"
        @click="onSelect(option.value)"
      >
        <span>{{ option.label }}</span>
        <strong>{{ option.count }}</strong>
      </button>
    </div>

    <div class="rail-stats">
      <div class="stat-card">
        <span>{{ t('collaboration.signals.syncVersion') }}</span>
        <strong>{{ props.syncVersion }}</strong>
      </div>
      <div class="stat-card">
        <span>{{ t('collaboration.signals.updatedEvents') }}</span>
        <strong>{{ props.syncUpdateCount }}</strong>
      </div>
      <div class="stat-card">
        <span>{{ t('collaboration.signals.lastSynced') }}</span>
        <strong>{{ formatDateTime(props.lastSyncedAt) }}</strong>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.signal-rail {
  padding: 18px;
  background: rgba(6, 18, 31, 0.78);
  border: 1px solid rgba(120, 227, 214, 0.12);
  box-shadow: 0 16px 36px rgba(2, 8, 15, 0.35);
}

.rail-header h2 {
  margin: 0;
  font-size: 20px;
}

.rail-header p {
  margin: 8px 0 0;
  color: rgba(236, 246, 255, 0.7);
  line-height: 1.5;
}

.filter-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 18px;
}

.filter-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(120, 227, 214, 0.14);
  background: rgba(15, 37, 53, 0.82);
  color: #e8f7ff;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.filter-chip.active,
.filter-chip:hover {
  transform: translateY(-1px);
  border-color: rgba(120, 227, 214, 0.42);
  background: rgba(19, 57, 77, 0.96);
}

.rail-stats {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.stat-card {
  padding: 14px;
  border-radius: 16px;
  background: rgba(13, 33, 47, 0.82);
  border: 1px solid rgba(120, 227, 214, 0.1);
}

.stat-card span {
  display: block;
  color: rgba(236, 246, 255, 0.62);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  font-size: 18px;
}
</style>
