<script setup lang="ts">
import { computed } from 'vue'
import type { CalendarEvent } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import {
  buildCalendarEventGroups,
  formatStoredDateTime,
  type CalendarRangeView,
} from '~/utils/calendar-workspace'

const props = defineProps<{
  events: CalendarEvent[]
  errorMessage: string
  loading: boolean
  rangeView: CalendarRangeView
  selectedEventId: string
}>()

const emit = defineEmits<{
  (event: 'edit', eventId: string): void
  (event: 'remove', payload: { eventId: string; canDelete: boolean }): void
  (event: 'retry'): void
  (event: 'select', eventId: string): void
}>()

const { t } = useI18n()
const groups = computed(() => buildCalendarEventGroups(props.events, props.rangeView))

function accessLabel(event: CalendarEvent): string {
  if (event.sharePermission === 'OWNER') {
    return t('calendar.permissions.owner')
  }
  return t(`calendar.permissions.${event.sharePermission.toLowerCase()}`)
}

function eventRangeLabel(event: CalendarEvent): string {
  if (event.allDay) {
    return `${event.startAt.slice(0, 10)} · ${event.timezone} · ${t('calendar.detail.allDay')}`
  }
  return `${formatStoredDateTime(event.startAt)} → ${formatStoredDateTime(event.endAt)} · ${event.timezone}`
}
</script>

<template>
  <section class="mm-card event-panel">
    <div class="event-panel__head">
      <div>
        <h2 class="mm-section-title">{{ t('calendar.events.title') }}</h2>
        <p class="event-panel__copy">{{ t('calendar.events.description') }}</p>
      </div>
      <el-tag type="info" effect="light">{{ events.length }}</el-tag>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />
    <div v-else-if="errorMessage" class="event-panel__error" data-testid="calendar-events-error">
      <el-alert :title="errorMessage" type="error" :closable="false" show-icon />
      <el-button data-testid="calendar-events-retry" type="primary" text @click="emit('retry')">
        {{ t('calendar.actions.retry') }}
      </el-button>
    </div>
    <el-empty
      v-else-if="groups.length === 0"
      data-testid="calendar-events-empty"
      :description="t('calendar.events.empty')"
      :image-size="72"
    />
    <div v-else class="event-group-list">
      <section v-for="group in groups" :key="group.key" class="event-group">
        <header class="event-group__head">
          <h3>{{ group.label }}</h3>
          <span>{{ group.items.length }}</span>
        </header>

        <article
          v-for="item in group.items"
          :key="item.id"
          class="event-card"
          :class="{ 'event-card--selected': selectedEventId === item.id }"
          data-testid="calendar-event-card"
        >
          <div class="event-card__body">
            <div>
              <h4>{{ item.title }}</h4>
              <p>{{ eventRangeLabel(item) }}</p>
              <p v-if="item.location">{{ item.location }}</p>
            </div>
            <div class="event-card__meta">
              <el-tag size="small" type="primary" effect="plain">{{ accessLabel(item) }}</el-tag>
              <el-tag v-if="item.shared" size="small" effect="light">{{ item.ownerEmail || t('calendar.labels.unknownOwner') }}</el-tag>
            </div>
          </div>

          <div class="event-card__footer">
            <span>{{ t('calendar.events.attendees', { count: item.attendeeCount }) }}</span>
            <div class="event-card__actions">
              <el-button
                data-testid="calendar-view-details"
                type="primary"
                text
                @click="emit('select', item.id)"
              >
                {{ t('calendar.actions.view') }}
              </el-button>
              <el-button
                data-testid="calendar-edit-event"
                type="primary"
                text
                :disabled="!item.canEdit"
                @click="emit('edit', item.id)"
              >
                {{ t('calendar.actions.edit') }}
              </el-button>
              <el-button
                data-testid="calendar-delete-event"
                type="danger"
                text
                :disabled="!item.canDelete"
                @click="emit('remove', { eventId: item.id, canDelete: item.canDelete })"
              >
                {{ t('calendar.actions.delete') }}
              </el-button>
            </div>
          </div>
        </article>
      </section>
    </div>
  </section>
</template>

<style scoped>
.event-panel {
  padding: 18px;
}

.event-panel__head,
.event-panel__error,
.event-card__body,
.event-card__footer,
.event-group__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.event-panel__copy {
  margin: 6px 0 0;
  color: #64748b;
}

.event-group-list {
  display: grid;
  gap: 14px;
}

.event-group {
  display: grid;
  gap: 10px;
}

.event-group__head h3,
.event-card h4 {
  margin: 0;
}

.event-group__head span,
.event-card p,
.event-card__footer span {
  color: #64748b;
}

.event-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.94);
}

.event-card--selected {
  border-color: rgba(37, 99, 235, 0.32);
  box-shadow: 0 14px 28px rgba(37, 99, 235, 0.08);
}

.event-card__meta,
.event-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

@media (max-width: 1024px) {
  .event-panel__head,
  .event-panel__error,
  .event-card__body,
  .event-card__footer,
  .event-group__head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
