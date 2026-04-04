<script setup lang="ts">
import type { CalendarEventDetail } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import { formatStoredDateTime } from '~/utils/calendar-workspace'

const props = defineProps<{
  detail: CalendarEventDetail | null
  errorMessage: string
  loading: boolean
}>()

const emit = defineEmits<{
  (event: 'edit', eventId: string): void
  (event: 'remove', payload: { eventId: string; canDelete: boolean }): void
  (event: 'retry'): void
}>()

const { t } = useI18n()

function reminderLabel(value: number | null): string {
  if (value == null) {
    return t('calendar.detail.noReminder')
  }
  return t('calendar.detail.reminderMinutes', { minutes: value })
}
</script>

<template>
  <section class="mm-card detail-panel" data-testid="calendar-detail-panel">
    <div class="detail-panel__head">
      <div>
        <p class="detail-panel__eyebrow">{{ t('calendar.detail.eyebrow') }}</p>
        <h2 class="mm-section-title">{{ t('calendar.detail.title') }}</h2>
      </div>
      <div v-if="detail" class="detail-panel__actions">
        <el-button
          data-testid="calendar-detail-edit"
          type="primary"
          text
          :disabled="!detail.canEdit"
          @click="emit('edit', detail.id)"
        >
          {{ t('calendar.actions.edit') }}
        </el-button>
        <el-button
          data-testid="calendar-detail-delete"
          type="danger"
          text
          :disabled="!detail.canDelete"
          @click="emit('remove', { eventId: detail.id, canDelete: detail.canDelete })"
        >
          {{ t('calendar.actions.delete') }}
        </el-button>
      </div>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />
    <div v-else-if="errorMessage" class="detail-panel__error" data-testid="calendar-detail-error">
      <el-alert :title="errorMessage" type="error" :closable="false" show-icon />
      <el-button type="primary" text @click="emit('retry')">{{ t('calendar.actions.retry') }}</el-button>
    </div>
    <el-empty
      v-else-if="!detail"
      :description="t('calendar.detail.empty')"
      :image-size="72"
    />
    <div v-else class="detail-panel__content" data-testid="calendar-detail-content">
      <div class="detail-panel__hero">
        <div>
          <h3 data-testid="calendar-detail-title">{{ detail.title }}</h3>
          <p>{{ detail.ownerEmail || t('calendar.labels.unknownOwner') }}</p>
        </div>
        <el-tag size="small" effect="light">{{ t(`calendar.permissions.${detail.sharePermission.toLowerCase()}`) }}</el-tag>
      </div>

      <dl class="detail-grid">
        <div>
          <dt>{{ t('calendar.detail.start') }}</dt>
          <dd>{{ formatStoredDateTime(detail.startAt) }}</dd>
        </div>
        <div>
          <dt>{{ t('calendar.detail.end') }}</dt>
          <dd>{{ formatStoredDateTime(detail.endAt) }}</dd>
        </div>
        <div>
          <dt>{{ t('calendar.detail.timezone') }}</dt>
          <dd>{{ detail.timezone }}</dd>
        </div>
        <div>
          <dt>{{ t('calendar.detail.reminder') }}</dt>
          <dd>{{ reminderLabel(detail.reminderMinutes) }}</dd>
        </div>
      </dl>

      <p v-if="detail.allDay" class="detail-panel__flag">{{ t('calendar.detail.allDay') }}</p>
      <p v-if="detail.location" class="detail-panel__location">{{ detail.location }}</p>
      <p v-if="detail.description" class="detail-panel__description">{{ detail.description }}</p>

      <section class="detail-attendees">
        <h4>{{ t('calendar.attendees.title') }}</h4>
        <el-empty
          v-if="detail.attendees.length === 0"
          :description="t('calendar.detail.attendeesEmpty')"
          :image-size="56"
        />
        <ul v-else>
          <li v-for="attendee in detail.attendees" :key="attendee.id">
            <strong>{{ attendee.displayName || attendee.email }}</strong>
            <span>{{ attendee.email }}</span>
            <el-tag size="small" effect="plain">{{ attendee.responseStatus }}</el-tag>
          </li>
        </ul>
        <p
          v-if="detail.attendees.length > 0"
          class="detail-attendees__hint"
          data-testid="calendar-detail-attendees-hint"
        >
          {{ t('calendar.detail.attendeesHint') }}
        </p>
      </section>
    </div>
  </section>
</template>

<style scoped>
.detail-panel {
  padding: 18px;
  display: grid;
  gap: 14px;
}

.detail-panel__head,
.detail-panel__hero,
.detail-panel__actions,
.detail-panel__error {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.detail-panel__eyebrow {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #53718c;
}

.detail-panel__hero h3 {
  margin: 0;
}

.detail-panel__hero p,
.detail-panel__description,
.detail-panel__location,
.detail-panel__flag {
  margin: 6px 0 0;
  color: #64748b;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-grid dt {
  font-size: 12px;
  color: #64748b;
}

.detail-grid dd {
  margin: 4px 0 0;
  font-weight: 600;
}

.detail-attendees ul {
  display: grid;
  gap: 10px;
  padding: 0;
  margin: 12px 0 0;
  list-style: none;
}

.detail-attendees li {
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.94);
}

.detail-attendees span {
  color: #64748b;
}

.detail-attendees__hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #64748b;
}

@media (max-width: 1024px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
