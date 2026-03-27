<script setup lang="ts">
import CalendarAvailabilityPanel from '~/components/calendar/CalendarAvailabilityPanel.vue'
import CalendarEventDetailPanel from '~/components/calendar/CalendarEventDetailPanel.vue'
import CalendarEventListPanel from '~/components/calendar/CalendarEventListPanel.vue'
import CalendarIcsTransferPanel from '~/components/calendar/CalendarIcsTransferPanel.vue'
import CalendarIncomingSharesPanel from '~/components/calendar/CalendarIncomingSharesPanel.vue'
import CalendarShareTable from '~/components/calendar/CalendarShareTable.vue'
import { useCalendarWorkspace } from '~/composables/useCalendarWorkspace'
import { useI18n } from '~/composables/useI18n'

definePageMeta({
  layout: 'default',
})

const { t } = useI18n()
const {
  agendaErrorMessage,
  agendaItems,
  agendaLoading,
  applySelectedContacts,
  availability,
  availabilityErrorMessage,
  availabilityLoading,
  availabilityQueryReady,
  contactOptions,
  currentRange,
  currentView,
  detailErrorMessage,
  detailLoading,
  editingCanDelete,
  eventShares,
  events,
  eventsErrorMessage,
  editingEventId,
  form,
  icsContent,
  importErrorMessage,
  importing,
  importResultSummary,
  incomingErrorMessage,
  incomingFilter,
  incomingShares,
  loading,
  moveRange,
  mutationShareId,
  onExportIcs,
  onImportIcs,
  onRemoveShare,
  onRespondShare,
  onShareEvent,
  onUpdateSharePermission,
  rangeOptions,
  rangeView,
  refreshWorkspace,
  removeEventById,
  resetForm,
  resetRangeToToday,
  respondingShareId,
  retryDetailLoad,
  retryEventsLoad,
  retryImport,
  saveErrorMessage,
  saving,
  selectedContactEmails,
  selectedEventDetail,
  selectedEventId,
  selectRangeView,
  shareForm,
  startEditEvent,
  submitEvent,
  updateImportContent,
  updateView,
  viewOptions,
  loadEventDetail,
} = useCalendarWorkspace()
</script>

<template>
  <div class="mm-page calendar-page">
    <section class="mm-card panel panel--hero">
      <div>
        <p class="panel__eyebrow">{{ t('calendar.hero.eyebrow') }}</p>
        <h1 class="mm-section-title">{{ t('calendar.hero.title') }}</h1>
        <p class="panel__copy">{{ t('calendar.hero.description') }}</p>
      </div>

      <div class="hero-row">
        <div class="toggle-group">
          <el-button
            v-for="option in viewOptions"
            :key="option.value"
            :data-testid="`calendar-surface-${option.value}`"
            :type="currentView === option.value ? 'primary' : 'default'"
            size="small"
            @click="updateView(option.value)"
          >
            {{ option.label }}
          </el-button>
        </div>

        <div v-if="currentView === 'events'" class="toggle-group">
          <el-button
            v-for="option in rangeOptions"
            :key="option.value"
            :data-testid="`calendar-range-${option.value}`"
            :type="rangeView === option.value ? 'primary' : 'default'"
            size="small"
            @click="selectRangeView(option.value)"
          >
            {{ option.label }}
          </el-button>
        </div>
      </div>

      <div v-if="currentView === 'events'" class="hero-row hero-row--range">
        <div class="toggle-group">
          <el-button data-testid="calendar-range-previous" size="small" @click="moveRange(-1)">
            {{ t('calendar.actions.previous') }}
          </el-button>
          <el-button data-testid="calendar-range-today" size="small" @click="resetRangeToToday()">
            {{ t('calendar.actions.today') }}
          </el-button>
          <el-button data-testid="calendar-range-next" size="small" @click="moveRange(1)">
            {{ t('calendar.actions.next') }}
          </el-button>
        </div>
        <el-tag type="info" effect="light" data-testid="calendar-range-label">{{ currentRange.label }}</el-tag>
      </div>

      <div class="hero-row">
        <el-button type="primary" @click="refreshWorkspace">{{ t('calendar.actions.refresh') }}</el-button>
        <div class="range-copy">
          <span>{{ currentRange.from }}</span>
          <span>→</span>
          <span>{{ currentRange.to }}</span>
        </div>
      </div>
    </section>

    <section class="workspace-grid">
      <div class="workspace-main">
        <CalendarEventDetailPanel
          :detail="selectedEventDetail"
          :loading="detailLoading"
          :error-message="detailErrorMessage"
          @edit="startEditEvent"
          @remove="removeEventById($event.eventId, $event.canDelete)"
          @retry="retryDetailLoad"
        />

        <section class="mm-card panel">
          <div class="section-header">
            <div>
              <h2 class="mm-section-title">{{ t('calendar.editor.title') }}</h2>
              <p class="panel__copy">{{ t('calendar.editor.description') }}</p>
            </div>
            <el-tag :type="editingCanDelete ? 'success' : 'info'" effect="light">
              {{ editingEventId ? t('calendar.labels.editingExisting') : t('calendar.labels.creatingNew') }}
            </el-tag>
          </div>

          <el-alert
            v-if="saveErrorMessage"
            :title="saveErrorMessage"
            type="error"
            :closable="false"
            show-icon
            data-testid="calendar-save-error"
          />

          <div class="two-col">
            <el-input v-model="form.title" data-testid="calendar-form-title" :placeholder="t('calendar.form.title')" />
            <el-input v-model="form.location" :placeholder="t('calendar.form.location')" />
          </div>

          <div class="form-grid">
            <el-input v-model="form.startAt" data-testid="calendar-form-start" :placeholder="t('calendar.form.startAt')" />
            <el-input v-model="form.endAt" data-testid="calendar-form-end" :placeholder="t('calendar.form.endAt')" />
            <el-input v-model="form.timezone" data-testid="calendar-form-timezone" :placeholder="t('calendar.form.timezone')" />
            <el-input-number v-model="form.reminderMinutes" :min="0" :max="10080" :step="5" />
          </div>

          <div class="switch-row">
            <el-switch v-model="form.allDay" :active-text="t('calendar.form.allDay')" />
          </div>

          <el-input v-model="form.description" type="textarea" :rows="3" :placeholder="t('calendar.form.description')" />

          <h3 class="mm-section-subtitle">{{ t('calendar.attendees.title') }}</h3>
          <div class="attendee-tools">
            <el-select
              v-model="selectedContactEmails"
              filterable
              multiple
              collapse-tags
              :placeholder="t('calendar.attendees.selectContacts')"
            >
              <el-option
                v-for="option in contactOptions"
                :key="option.email"
                :label="option.label"
                :value="option.email"
              />
            </el-select>
            <el-button @click="applySelectedContacts(selectedContactEmails)">
              {{ t('calendar.actions.addSelected') }}
            </el-button>
          </div>

          <el-input
            v-model="form.attendeesText"
            data-testid="calendar-attendees-textarea"
            type="textarea"
            :rows="4"
            :placeholder="t('calendar.attendees.placeholder')"
          />

          <div class="action-row">
            <el-button data-testid="calendar-submit" type="success" :loading="saving" @click="submitEvent">
              {{ editingEventId ? t('calendar.actions.updateEvent') : t('calendar.actions.createEvent') }}
            </el-button>
            <el-button @click="resetForm">{{ t('calendar.actions.reset') }}</el-button>
          </div>

          <div class="share-panel">
            <h3 class="mm-section-subtitle">{{ t('calendar.share.title') }}</h3>
            <p class="panel__copy">{{ t('calendar.share.description') }}</p>
            <div class="share-form-row">
              <el-input v-model="shareForm.targetEmail" :placeholder="t('calendar.share.targetEmail')" />
              <el-select v-model="shareForm.permission" :placeholder="t('calendar.share.permission')">
                <el-option :label="t('calendar.permissions.view')" value="VIEW" />
                <el-option :label="t('calendar.permissions.edit')" value="EDIT" />
              </el-select>
              <el-button type="primary" @click="onShareEvent">{{ t('calendar.actions.share') }}</el-button>
            </div>
            <CalendarShareTable
              :shares="eventShares"
              :mutation-share-id="mutationShareId"
              @update-permission="onUpdateSharePermission"
              @remove="onRemoveShare"
            />
          </div>
        </section>

        <CalendarIcsTransferPanel
          :content="icsContent"
          :error-message="importErrorMessage"
          :importing="importing"
          :result-summary="importResultSummary"
          @export="onExportIcs"
          @import="onImportIcs"
          @retry="retryImport"
          @update:content="updateImportContent"
        />

        <section class="mm-card panel">
          <h2 class="mm-section-title">{{ t('calendar.agenda.title') }}</h2>
          <el-alert
            v-if="agendaErrorMessage"
            :title="agendaErrorMessage"
            type="error"
            :closable="false"
            show-icon
          />
          <el-skeleton v-else-if="agendaLoading" :rows="3" animated />
          <el-empty v-else-if="agendaItems.length === 0" :description="t('calendar.events.empty')" :image-size="56" />
          <ul v-else class="agenda-list">
            <li v-for="item in agendaItems" :key="item.id">
              <strong>{{ item.title }}</strong>
              <span>{{ item.startAt }}</span>
              <span v-if="item.location">{{ item.location }}</span>
            </li>
          </ul>
        </section>

        <el-alert
          v-if="incomingErrorMessage"
          :title="incomingErrorMessage"
          type="error"
          :closable="false"
          show-icon
        />

        <CalendarIncomingSharesPanel
          v-if="currentView === 'incoming-shares'"
          :shares="incomingShares"
          :filter="incomingFilter"
          :busy-share-id="respondingShareId"
          @update:filter="incomingFilter = $event"
          @respond="onRespondShare"
        />

        <CalendarEventListPanel
          v-else
          :events="events"
          :error-message="eventsErrorMessage"
          :loading="loading"
          :range-view="rangeView"
          :selected-event-id="selectedEventId"
          @edit="startEditEvent"
          @remove="removeEventById($event.eventId, $event.canDelete)"
          @retry="retryEventsLoad"
          @select="loadEventDetail"
        />
      </div>

      <CalendarAvailabilityPanel
        :availability="availability"
        :loading="availabilityLoading"
        :query-ready="availabilityQueryReady"
        :error-message="availabilityErrorMessage"
      />
    </section>
  </div>
</template>

<style scoped>
.calendar-page,
.workspace-main,
.agenda-list {
  display: grid;
  gap: 18px;
}

.panel {
  padding: 18px;
}

.panel--hero {
  display: grid;
  gap: 16px;
}

.panel__eyebrow {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #53718c;
}

.panel__copy,
.range-copy,
.agenda-list span {
  color: #64748b;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(320px, 0.9fr);
  gap: 18px;
  align-items: start;
}

.hero-row,
.toggle-group,
.two-col,
.form-grid,
.switch-row,
.action-row,
.attendee-tools,
.share-form-row,
.section-header {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

.hero-row--range {
  justify-content: space-between;
}

.range-copy {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.two-col,
.form-grid {
  display: grid;
}

.two-col {
  grid-template-columns: 1fr 1fr;
}

.form-grid {
  grid-template-columns: 1fr 1fr 1fr 180px;
  gap: 10px;
}

.share-panel {
  display: grid;
  gap: 12px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--mm-line);
}

.agenda-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.agenda-list li {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.94);
}

@media (max-width: 1280px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1024px) {
  .two-col,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
