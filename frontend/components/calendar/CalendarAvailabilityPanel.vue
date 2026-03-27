<script setup lang="ts">
import { computed } from 'vue'
import type { CalendarAvailability, CalendarParticipantAvailability } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { buildCalendarAvailabilitySummary, formatCalendarDateTime } from '~/utils/calendar-availability'

const props = defineProps<{
  availability: CalendarAvailability | null
  loading: boolean
  queryReady: boolean
  errorMessage: string
}>()

const { t } = useI18n()

const summary = computed(() => buildCalendarAvailabilitySummary(props.availability))

function statusType(item: CalendarParticipantAvailability): 'success' | 'warning' | 'info' {
  if (item.availability === 'FREE') return 'success'
  if (item.availability === 'BUSY') return 'warning'
  return 'info'
}

function statusLabel(item: CalendarParticipantAvailability): string {
  if (item.availability === 'FREE') return t('calendar.availability.status.free')
  if (item.availability === 'BUSY') return t('calendar.availability.status.busy')
  return t('calendar.availability.status.unknown')
}
</script>

<template>
  <aside class="availability-panel mm-card">
    <div class="availability-panel__head">
      <div>
        <p class="availability-panel__eyebrow">{{ t('calendar.availability.eyebrow') }}</p>
        <h3>{{ t('calendar.availability.title') }}</h3>
        <p class="availability-panel__copy">{{ t('calendar.availability.description') }}</p>
      </div>
      <div class="availability-panel__summary">
        <article>
          <span>{{ t('calendar.availability.metrics.attendees') }}</span>
          <strong>{{ summary.attendeeCount }}</strong>
        </article>
        <article>
          <span>{{ t('calendar.availability.metrics.busy') }}</span>
          <strong>{{ summary.busyCount }}</strong>
        </article>
        <article>
          <span>{{ t('calendar.availability.metrics.free') }}</span>
          <strong>{{ summary.freeCount }}</strong>
        </article>
      </div>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />
    <el-alert
      v-else-if="errorMessage"
      type="error"
      :title="errorMessage"
      :closable="false"
      show-icon
    />
    <el-empty v-else-if="!queryReady" :description="t('calendar.availability.emptyQuery')" :image-size="68" />
    <el-empty v-else-if="!availability || availability.attendees.length === 0" :description="t('calendar.availability.emptyResults')" :image-size="68" />
    <div v-else class="availability-list">
      <article v-for="item in availability.attendees" :key="item.email" class="availability-item">
        <div class="availability-item__top">
          <div>
            <strong>{{ item.email }}</strong>
            <p>{{ t('calendar.availability.conflicts', { count: item.overlapCount }) }}</p>
          </div>
          <el-tag :type="statusType(item)" effect="light">{{ statusLabel(item) }}</el-tag>
        </div>
        <div v-if="item.busySlots.length" class="availability-item__slots">
          <span v-for="(slot, index) in item.busySlots" :key="`${item.email}-${index}`" class="slot-pill">
            {{ formatCalendarDateTime(slot.startAt) }} → {{ formatCalendarDateTime(slot.endAt) }}
          </span>
        </div>
        <p v-else class="availability-item__free">{{ t('calendar.availability.noConflicts') }}</p>
      </article>
    </div>
  </aside>
</template>

<style scoped>
.availability-panel {
  padding: 18px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: linear-gradient(180deg, rgba(244, 248, 255, 0.96), rgba(255, 255, 255, 0.98));
}

.availability-panel__head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  margin-bottom: 16px;
}

.availability-panel__eyebrow {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #53718c;
}

.availability-panel__copy {
  margin: 6px 0 0;
  color: #526173;
}

.availability-panel__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(72px, 96px));
  gap: 10px;
}

.availability-panel__summary article {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(11, 36, 58, 0.9);
  color: #eff6ff;
}

.availability-panel__summary span {
  display: block;
  font-size: 11px;
  color: rgba(239, 246, 255, 0.72);
}

.availability-panel__summary strong {
  display: block;
  margin-top: 6px;
  font-size: 24px;
}

.availability-list {
  display: grid;
  gap: 12px;
}

.availability-item {
  padding: 14px;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.92);
}

.availability-item__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.availability-item__top p,
.availability-item__free {
  margin: 6px 0 0;
  color: #64748b;
}

.availability-item__slots {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.slot-pill {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(245, 158, 11, 0.12);
  color: #9a3412;
  font-size: 12px;
}

@media (max-width: 1200px) {
  .availability-panel__head {
    grid-template-columns: 1fr;
  }

  .availability-panel__summary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
