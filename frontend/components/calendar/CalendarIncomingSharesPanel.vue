<script setup lang="ts">
import { computed } from 'vue'
import type { CalendarIncomingShare } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import {
  buildCalendarIncomingShareCounts,
  filterCalendarIncomingShares,
  formatCalendarDateTime,
  type CalendarIncomingShareFilter
} from '~/utils/calendar-availability'

const props = defineProps<{
  shares: CalendarIncomingShare[]
  filter: CalendarIncomingShareFilter
  busyShareId: string
}>()

const emit = defineEmits<{
  (event: 'update:filter', value: CalendarIncomingShareFilter): void
  (event: 'respond', payload: { shareId: string; response: 'ACCEPT' | 'DECLINE' }): void
}>()

const { t } = useI18n()

const counts = computed(() => buildCalendarIncomingShareCounts(props.shares))
const filteredShares = computed(() => filterCalendarIncomingShares(props.shares, props.filter))
const filterOptions: CalendarIncomingShareFilter[] = ['ALL', 'NEEDS_ACTION', 'ACCEPTED', 'DECLINED']

function filterLabel(value: CalendarIncomingShareFilter): string {
  if (value === 'ALL') return t('calendar.incoming.filters.all')
  if (value === 'NEEDS_ACTION') return t('calendar.incoming.filters.needsAction')
  if (value === 'ACCEPTED') return t('calendar.incoming.filters.accepted')
  return t('calendar.incoming.filters.declined')
}

function statusType(status: CalendarIncomingShare['responseStatus']): 'warning' | 'success' | 'info' {
  if (status === 'NEEDS_ACTION') return 'warning'
  if (status === 'ACCEPTED') return 'success'
  return 'info'
}

function onFilterChange(value: string | number | boolean): void {
  if (value === 'ALL' || value === 'NEEDS_ACTION' || value === 'ACCEPTED' || value === 'DECLINED') {
    emit('update:filter', value)
  }
}
</script>

<template>
  <section class="mm-card incoming-panel">
    <div class="incoming-panel__head">
      <div>
        <p class="incoming-panel__eyebrow">{{ t('calendar.incoming.eyebrow') }}</p>
        <h2 class="mm-section-title">{{ t('calendar.incoming.title') }}</h2>
        <p class="incoming-panel__copy">{{ t('calendar.incoming.description') }}</p>
      </div>
      <div class="incoming-panel__summary">
        <article v-for="key in filterOptions" :key="key">
          <span>{{ filterLabel(key) }}</span>
          <strong>{{ counts[key] }}</strong>
        </article>
      </div>
    </div>

    <el-radio-group :model-value="filter" size="small" @update:model-value="onFilterChange">
      <el-radio-button v-for="key in filterOptions" :key="key" :label="key">{{ filterLabel(key) }}</el-radio-button>
    </el-radio-group>

    <el-empty v-if="filteredShares.length === 0" :description="t('calendar.incoming.empty')" :image-size="72" />
    <div v-else class="incoming-list">
      <article v-for="item in filteredShares" :key="item.shareId" class="incoming-card">
        <div>
          <p class="incoming-card__eyebrow">{{ t('calendar.incoming.cardEyebrow') }}</p>
          <h3>{{ item.eventTitle }}</h3>
          <p>{{ t('calendar.incoming.owner', { email: item.ownerEmail || t('calendar.labels.unknownOwner') }) }}</p>
          <p>{{ t('calendar.incoming.updatedAt', { value: formatCalendarDateTime(item.updatedAt) }) }}</p>
        </div>
        <div class="incoming-card__meta">
          <el-tag type="primary" effect="plain">{{ t(`calendar.permissions.${item.permission.toLowerCase()}`) }}</el-tag>
          <el-tag :type="statusType(item.responseStatus)" effect="light">{{ t(`calendar.status.${item.responseStatus.toLowerCase()}`) }}</el-tag>
        </div>
        <div v-if="item.responseStatus === 'NEEDS_ACTION'" class="incoming-card__actions">
          <el-button
            type="success"
            :loading="busyShareId === item.shareId"
            @click="emit('respond', { shareId: item.shareId, response: 'ACCEPT' })"
          >
            {{ t('calendar.actions.accept') }}
          </el-button>
          <el-button
            type="danger"
            plain
            :loading="busyShareId === item.shareId"
            @click="emit('respond', { shareId: item.shareId, response: 'DECLINE' })"
          >
            {{ t('calendar.actions.decline') }}
          </el-button>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.incoming-panel {
  padding: 18px;
}

.incoming-panel__head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  margin-bottom: 16px;
}

.incoming-panel__eyebrow,
.incoming-card__eyebrow {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #53718c;
}

.incoming-panel__copy,
.incoming-card p {
  color: #64748b;
}

.incoming-panel__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(72px, 88px));
  gap: 10px;
}

.incoming-panel__summary article {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(241, 245, 249, 0.95);
}

.incoming-panel__summary span {
  display: block;
  font-size: 11px;
  color: #64748b;
}

.incoming-panel__summary strong {
  display: block;
  margin-top: 6px;
  font-size: 22px;
  color: #0f172a;
}

.incoming-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.incoming-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  padding: 16px;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.94), rgba(255, 255, 255, 0.98));
}

.incoming-card h3 {
  margin: 0;
}

.incoming-card__meta,
.incoming-card__actions {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

@media (max-width: 1024px) {
  .incoming-panel__head,
  .incoming-card {
    grid-template-columns: 1fr;
  }

  .incoming-panel__summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
