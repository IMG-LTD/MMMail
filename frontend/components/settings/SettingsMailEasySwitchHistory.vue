<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { MailEasySwitchSession } from '~/types/mail-easy-switch'
import {
  resolveMailEasySwitchMetricTotal,
  resolveMailEasySwitchStatusType
} from '~/utils/mail-easy-switch'

const props = defineProps<{
  loading: boolean
  sessions: MailEasySwitchSession[]
}>()

const emit = defineEmits<{
  delete: [sessionId: string]
}>()

const { t } = useI18n()
const currentSessions = computed(() => props.sessions.filter((item) => item.status === 'RUNNING'))
const historySessions = computed(() => props.sessions.filter((item) => item.status !== 'RUNNING'))

function formatDate(value: string | null): string {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}
</script>

<template>
  <section class="history" v-loading="props.loading">
    <div class="history-section">
      <div class="history-head">
        <h3>{{ t('settings.easySwitch.sections.current') }}</h3>
        <span class="history-copy">{{ t('settings.easySwitch.sections.currentDescription') }}</span>
      </div>
      <el-empty v-if="currentSessions.length === 0" :description="t('settings.easySwitch.empty.current')" />
      <div v-else class="session-list">
        <article v-for="session in currentSessions" :key="session.id" class="session-card">
          <div class="session-topline">
            <div>
              <div class="session-title">{{ session.sourceEmail }}</div>
              <div class="session-meta">{{ t(`settings.easySwitch.providers.${session.provider.toLowerCase()}`) }}</div>
            </div>
            <el-tag :type="resolveMailEasySwitchStatusType(session.status)">
              {{ t(`settings.easySwitch.status.${session.status.toLowerCase()}`) }}
            </el-tag>
          </div>
        </article>
      </div>
    </div>

    <div class="history-section">
      <div class="history-head">
        <h3>{{ t('settings.easySwitch.sections.history') }}</h3>
        <span class="history-copy">{{ t('settings.easySwitch.sections.historyDescription') }}</span>
      </div>
      <el-empty v-if="historySessions.length === 0" :description="t('settings.easySwitch.empty.history')" />
      <div v-else class="session-list">
        <article v-for="session in historySessions" :key="session.id" class="session-card">
          <div class="session-topline">
            <div>
              <div class="session-title">{{ session.sourceEmail }}</div>
              <div class="session-meta">
                {{ t(`settings.easySwitch.providers.${session.provider.toLowerCase()}`) }}
                · {{ formatDate(session.completedAt || session.createdAt) }}
              </div>
            </div>
            <el-tag :type="resolveMailEasySwitchStatusType(session.status)">
              {{ t(`settings.easySwitch.status.${session.status.toLowerCase()}`) }}
            </el-tag>
          </div>
          <div class="metric-grid">
            <div class="metric-item">
              <span>{{ t('settings.easySwitch.metrics.totalImported') }}</span>
              <strong>{{ resolveMailEasySwitchMetricTotal(session) }}</strong>
            </div>
            <div class="metric-item">
              <span>{{ t('settings.easySwitch.metrics.contacts') }}</span>
              <strong>{{ session.contactsCreated + session.contactsUpdated }}</strong>
            </div>
            <div class="metric-item">
              <span>{{ t('settings.easySwitch.metrics.calendar') }}</span>
              <strong>{{ session.calendarImported }}</strong>
            </div>
            <div class="metric-item">
              <span>{{ t('settings.easySwitch.metrics.mail') }}</span>
              <strong>{{ session.mailImported }}</strong>
            </div>
          </div>
          <div class="breakdown">
            <span>{{ t('settings.easySwitch.metrics.contactsBreakdown', { created: session.contactsCreated, updated: session.contactsUpdated, skipped: session.contactsSkipped, invalid: session.contactsInvalid }) }}</span>
            <span>{{ t('settings.easySwitch.metrics.calendarBreakdown', { imported: session.calendarImported, invalid: session.calendarInvalid }) }}</span>
            <span>{{ t('settings.easySwitch.metrics.mailBreakdown', { imported: session.mailImported, skipped: session.mailSkipped, invalid: session.mailInvalid, folder: session.importedMailFolder }) }}</span>
          </div>
          <el-alert
            v-if="session.errorMessage"
            :title="session.errorMessage"
            type="error"
            :closable="false"
            class="session-error"
          />
          <div class="session-actions">
            <el-button text type="danger" @click="emit('delete', session.id)">
              {{ t('settings.easySwitch.actions.deleteHistory') }}
            </el-button>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.history,
.history-section,
.session-list {
  display: grid;
  gap: 12px;
}

.history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.history-head h3,
.session-title {
  margin: 0;
  font-weight: 600;
}

.history-copy,
.breakdown,
.session-meta {
  color: var(--mm-muted);
  font-size: 13px;
  line-height: 1.5;
}

.session-card {
  border: 1px solid rgba(107, 83, 209, 0.14);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 16px 40px rgba(91, 79, 165, 0.08);
  padding: 16px;
}

.session-topline {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.metric-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.metric-item {
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(122, 98, 235, 0.07);
  display: grid;
  gap: 4px;
}

.metric-item strong {
  font-size: 18px;
}

.breakdown {
  margin-top: 12px;
  display: grid;
  gap: 4px;
}

.session-error {
  margin-top: 12px;
}

.session-actions {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .history-head,
  .session-topline {
    flex-direction: column;
  }
}
</style>
