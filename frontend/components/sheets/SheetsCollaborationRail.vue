<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { SuiteCollaborationEvent } from '~/types/api'
import { type SheetsTemplatePreset } from '~/utils/sheets-collaboration'

const props = defineProps<{
  events: SuiteCollaborationEvent[]
  loading: boolean
  errorMessage: string
  creatingTemplateCode: string | null
  templates: SheetsTemplatePreset[]
}>()

const emit = defineEmits<{
  createTemplate: [template: SheetsTemplatePreset]
  openEvent: [item: SuiteCollaborationEvent]
}>()

const { t } = useI18n()

function formatTime(value: string): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '—'
}
</script>

<template>
  <aside class="collaboration-rail">
    <article class="rail-card accent">
      <p class="rail-eyebrow">{{ t('sheets.collaboration.eyebrow') }}</p>
      <h3>{{ t('sheets.collaboration.title') }}</h3>
      <p class="rail-copy">{{ t('sheets.collaboration.description') }}</p>
    </article>

    <article class="rail-card">
      <div class="rail-head">
        <div>
          <p class="rail-eyebrow">{{ t('sheets.collaboration.templatesEyebrow') }}</p>
          <h3>{{ t('sheets.collaboration.templatesTitle') }}</h3>
        </div>
      </div>
      <div class="template-list">
        <button
          v-for="template in templates"
          :key="template.code"
          class="template-card"
          type="button"
          :disabled="creatingTemplateCode === template.code"
          @click="emit('createTemplate', template)"
        >
          <strong>{{ t(template.titleKey) }}</strong>
          <span>{{ t(template.descriptionKey) }}</span>
          <small>{{ t('sheets.collaboration.useTemplate') }}</small>
        </button>
      </div>
    </article>

    <article class="rail-card">
      <div class="rail-head">
        <div>
          <p class="rail-eyebrow">{{ t('sheets.collaboration.eventsEyebrow') }}</p>
          <h3>{{ t('sheets.collaboration.eventsTitle') }}</h3>
        </div>
        <span class="event-count">{{ events.length }}</span>
      </div>

      <el-skeleton v-if="loading" :rows="4" animated />
      <el-alert v-else-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" />
      <el-empty v-else-if="events.length === 0" :description="t('sheets.collaboration.empty')" :image-size="72" />
      <div v-else class="event-list">
        <button
          v-for="item in events.slice(0, 5)"
          :key="item.eventId"
          class="event-item"
          type="button"
          @click="emit('openEvent', item)"
        >
          <strong>{{ item.title }}</strong>
          <span>{{ item.summary }}</span>
          <small>{{ formatTime(item.createdAt) }}</small>
        </button>
      </div>
    </article>
  </aside>
</template>

<style scoped>
.collaboration-rail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rail-card {
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(15, 110, 110, 0.1);
  box-shadow: 0 18px 44px rgba(9, 71, 71, 0.08);
}

.rail-card.accent {
  background: linear-gradient(180deg, rgba(8, 40, 40, 0.96), rgba(13, 73, 73, 0.96));
  color: #f4fcfb;
}

.rail-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.rail-card h3,
.rail-card p {
  margin: 0;
}

.rail-copy {
  margin-top: 8px;
  line-height: 1.7;
  color: rgba(244, 252, 251, 0.82);
}

.rail-eyebrow {
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 11px;
  color: #0f766e;
}

.accent .rail-eyebrow {
  color: rgba(199, 245, 240, 0.82);
}

.template-list,
.event-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.template-card,
.event-item {
  width: 100%;
  text-align: left;
  border: 1px solid rgba(15, 110, 110, 0.1);
  border-radius: 18px;
  background: #fff;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  cursor: pointer;
}

.template-card:disabled {
  cursor: progress;
  opacity: 0.72;
}

.template-card strong,
.event-item strong {
  font-size: 15px;
}

.template-card span,
.template-card small,
.event-item span,
.event-item small,
.event-count {
  color: var(--mm-muted);
}

.event-count {
  min-width: 28px;
  height: 28px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 110, 110, 0.08);
  color: var(--mm-primary-dark);
  font-size: 12px;
}
</style>
