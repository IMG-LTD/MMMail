<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { SheetsWorkbookDetail } from '~/types/sheets'
import { formatSheetsTime, summarizeGridFootprint } from '~/utils/sheets'

const props = defineProps<{
  workbook: SheetsWorkbookDetail | null
  workbookCount: number
  dirtyCount: number
  saving: boolean
  refreshing: boolean
  creating: boolean
  lastExportedAt: string | null
}>()

const emit = defineEmits<{
  create: []
  refresh: []
  save: []
}>()

const { t } = useI18n()

const metricCards = computed(() => {
  if (!props.workbook) {
    return [
      {
        label: t('sheets.hero.metrics.workbooks.label'),
        value: String(props.workbookCount),
        hint: t('sheets.hero.metrics.workbooks.hint')
      },
      {
        label: t('sheets.hero.metrics.formulas.label'),
        value: t('sheets.hero.metrics.formulas.value'),
        hint: t('sheets.hero.metrics.formulas.hint')
      },
      {
        label: t('sheets.hero.metrics.import.label'),
        value: t('sheets.hero.metrics.import.value'),
        hint: t('sheets.hero.metrics.import.hint')
      },
      {
        label: t('sheets.hero.metrics.export.label'),
        value: t('sheets.hero.metrics.export.value'),
        hint: t('sheets.hero.metrics.export.hint')
      }
    ]
  }

  return [
    {
      label: t('sheets.hero.metrics.grid.label'),
      value: summarizeGridFootprint(props.workbook.rowCount, props.workbook.colCount),
      hint: t('sheets.hero.metrics.grid.hint', { count: props.workbook.filledCellCount })
    },
    {
      label: t('sheets.hero.metrics.formulaCells.label'),
      value: String(props.workbook.formulaCellCount),
      hint: t('sheets.hero.metrics.formulaCells.hint', { count: props.workbook.computedErrorCount })
    },
    {
      label: t('sheets.hero.metrics.version.label'),
      value: `v${props.workbook.currentVersion}`,
      hint: t('sheets.hero.metrics.version.hint', { count: props.dirtyCount })
    },
    {
      label: t('sheets.hero.metrics.exportStatus.label'),
      value: props.lastExportedAt ? t('sheets.hero.metrics.exportStatus.ready') : t('sheets.hero.metrics.exportStatus.standby'),
      hint: formatSheetsTime(props.lastExportedAt)
    }
  ]
})
</script>

<template>
  <section class="hero-shell mm-card">
    <div class="hero-copy">
      <span class="hero-badge">{{ t('sheets.hero.badge') }}</span>
      <h1 data-testid="sheets-hero-title">{{ workbook ? workbook.title : t('sheets.hero.emptyTitle') }}</h1>
      <p data-testid="sheets-hero-description">
        {{ workbook
          ? t('sheets.hero.activeDescription')
          : t('sheets.hero.emptyDescription') }}
      </p>
      <div class="hero-meta">
        <span class="hero-pill">{{ t('sheets.hero.pills.foundation') }}</span>
        <span class="hero-pill">{{ t('sheets.hero.pills.collaboration') }}</span>
        <span v-if="workbook" class="hero-pill">{{ workbook.canEdit ? t('sheets.meta.editAccess') : t('sheets.meta.readOnly') }}</span>
        <span class="hero-pill">{{ t('sheets.hero.pills.lastExport', { value: formatSheetsTime(lastExportedAt) }) }}</span>
      </div>
    </div>

    <div class="hero-side">
      <div class="hero-metrics">
        <article v-for="metric in metricCards" :key="metric.label" class="metric-card">
          <span class="metric-label">{{ metric.label }}</span>
          <strong class="metric-value">{{ metric.value }}</strong>
          <span class="metric-hint">{{ metric.hint }}</span>
        </article>
      </div>
      <div class="hero-actions">
        <el-button
          data-testid="sheets-hero-create"
          type="primary"
          :loading="creating"
          @click="emit('create')"
        >
          {{ t('sheets.actions.newWorkbook') }}
        </el-button>
        <el-button
          data-testid="sheets-hero-refresh"
          :loading="refreshing"
          @click="emit('refresh')"
        >
          {{ t('common.actions.refresh') }}
        </el-button>
        <el-button
          data-testid="sheets-hero-save"
          type="success"
          plain
          :disabled="!workbook || dirtyCount === 0 || !workbook.canEdit"
          :loading="saving"
          @click="emit('save')"
        >
          {{ t('common.actions.saveChanges') }}
        </el-button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero-shell {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(360px, 1fr);
  gap: 20px;
  padding: 28px;
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(214, 174, 84, 0.18), transparent 24%),
    radial-gradient(circle at top left, rgba(92, 201, 178, 0.22), transparent 30%),
    linear-gradient(135deg, rgba(15, 110, 110, 0.96), rgba(8, 52, 52, 0.96));
  color: #f4fcfb;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero-badge {
  display: inline-flex;
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(30px, 3vw, 42px);
  line-height: 1.04;
}

.hero-copy p {
  max-width: 64ch;
  margin: 0;
  color: rgba(244, 252, 251, 0.82);
  line-height: 1.7;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-pill {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.14);
  font-size: 12px;
}

.hero-side {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-card,
.hero-actions {
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
}

.metric-card {
  min-height: 126px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.metric-label {
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(244, 252, 251, 0.76);
}

.metric-value {
  font-size: 24px;
}

.metric-hint {
  color: rgba(244, 252, 251, 0.72);
}

.hero-actions {
  display: flex;
  gap: 12px;
  padding: 16px;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: wrap;
}

@media (max-width: 1180px) {
  .hero-shell {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
