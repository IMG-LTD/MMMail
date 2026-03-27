<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { StandardNotesOverview } from '~/types/standard-notes'
import { buildStandardNotesHealthChips } from '~/utils/standard-notes'

const props = defineProps<{
  overview: StandardNotesOverview | null
  loading?: boolean
}>()

const emit = defineEmits<{
  refresh: []
}>()
const { t } = useI18n()

const metricCards = computed(() => {
  if (!props.overview) {
    return [
      { label: t('standardNotes.metric.notes'), value: '--', hint: t('standardNotes.metric.hint.privateKnowledge') },
      { label: t('standardNotes.metric.folders'), value: '--', hint: t('standardNotes.metric.hint.collections') },
      { label: t('standardNotes.metric.tasks'), value: '--', hint: t('standardNotes.metric.hint.checklistExecution') },
      { label: t('standardNotes.metric.export'), value: '--', hint: t('standardNotes.metric.hint.backupReadiness') }
    ]
  }
  return [
    {
      label: t('standardNotes.metric.notes'),
      value: String(props.overview.totalNoteCount),
      hint: t('standardNotes.metric.hint.archived', { count: props.overview.archivedNoteCount })
    },
    { label: t('standardNotes.metric.folders'), value: String(props.overview.folderCount), hint: t('standardNotes.metric.hint.collections') },
    {
      label: t('standardNotes.metric.tasks'),
      value: String(props.overview.checklistTaskCount),
      hint: t('standardNotes.metric.hint.completed', { count: props.overview.completedChecklistTaskCount })
    },
    {
      label: t('standardNotes.metric.export'),
      value: props.overview.exportReady ? t('standardNotes.metric.exportReady') : t('standardNotes.metric.exportStandby'),
      hint: props.overview.generatedAt || '--'
    }
  ]
})

const healthChips = computed(() => buildStandardNotesHealthChips(props.overview, t))
</script>

<template>
  <section class="standard-notes-hero">
    <div class="hero-copy">
      <p class="hero-eyebrow">{{ t('standardNotes.hero.eyebrow') }}</p>
      <h1>{{ t('standardNotes.hero.title') }}</h1>
      <p class="hero-subtitle">{{ t('standardNotes.hero.subtitle') }}</p>
      <div class="hero-actions">
        <el-button :loading="loading" @click="emit('refresh')">{{ t('standardNotes.hero.refreshWorkspace') }}</el-button>
      </div>
      <div class="hero-meta">
        <span class="hero-pill">{{ t('standardNotes.hero.meta.collections') }}</span>
        <span class="hero-pill">{{ t('standardNotes.hero.meta.isolation') }}</span>
        <span class="hero-pill">{{ t('standardNotes.hero.meta.generated', { time: overview?.generatedAt || '--' }) }}</span>
      </div>
      <div class="hero-strip">
        <span v-for="chip in healthChips" :key="chip" class="chip">{{ chip }}</span>
      </div>
    </div>

    <div class="hero-metrics">
      <article v-for="metric in metricCards" :key="metric.label" class="metric-card">
        <span class="metric-label">{{ metric.label }}</span>
        <strong class="metric-value">{{ metric.value }}</strong>
        <span class="metric-hint">{{ metric.hint }}</span>
      </article>
    </div>
  </section>
</template>

<style scoped>
.standard-notes-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 1fr);
  gap: 18px;
  padding: 28px;
  border-radius: 30px;
  color: #111827;
  background:
    radial-gradient(circle at top left, rgba(212, 196, 168, 0.35), transparent 30%),
    linear-gradient(135deg, #f8f2e7 0%, #eee3d2 48%, #e1d4c0 100%);
  border: 1px solid rgba(99, 72, 50, 0.12);
  box-shadow: 0 28px 80px rgba(78, 60, 38, 0.14);
}
.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.hero-eyebrow {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 12px;
  color: #7c5d3d;
}
.hero-copy h1 {
  margin: 0;
  font-size: clamp(34px, 4vw, 54px);
  line-height: 0.96;
}
.hero-subtitle,
.metric-hint {
  color: rgba(40, 33, 25, 0.74);
}
.hero-subtitle {
  margin: 0;
  max-width: 62ch;
}
.hero-actions,
.hero-meta,
.hero-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.hero-pill,
.chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.58);
  border: 1px solid rgba(99, 72, 50, 0.12);
  font-size: 12px;
}
.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.metric-card {
  min-height: 130px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(99, 72, 50, 0.12);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.metric-label {
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 11px;
  color: rgba(103, 76, 48, 0.7);
}
.metric-value {
  font-size: 34px;
}
@media (max-width: 1100px) {
  .standard-notes-hero {
    grid-template-columns: 1fr;
  }
}
</style>
