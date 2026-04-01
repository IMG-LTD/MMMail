<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { SheetsWorkbookDetail } from '~/types/sheets'
import { summarizeSupportedFormats } from '~/utils/sheets'

const props = defineProps<{
  workbookCount: number
  workbook: SheetsWorkbookDetail | null
  dirtyCount: number
  supportedImportFormats: string[]
  supportedExportFormats: string[]
}>()

const { t } = useI18n()

const readinessNotes = computed(() => {
  if (!props.workbook) {
    return [
      t('sheets.health.workbooks', { count: props.workbookCount }),
      t('sheets.health.multiSheetEnabled'),
      t('sheets.health.importReady'),
      t('sheets.health.exportReady')
    ]
  }

  const dirtyLabel = props.dirtyCount > 0
    ? t('sheets.health.dirty', { count: props.dirtyCount })
    : t('sheets.health.saved')
  const errorLabel = props.workbook.computedErrorCount > 0
    ? t('sheets.health.errors', { count: props.workbook.computedErrorCount })
    : t('sheets.health.noErrors')

  return [
    t('sheets.health.sheetCount', { count: props.workbook.sheetCount }),
    t('sheets.health.formulas', { count: props.workbook.formulaCellCount }),
    errorLabel,
    t('sheets.health.importFormats', { count: props.workbook.supportedImportFormats.length }),
    t('sheets.health.exportFormats', { count: props.workbook.supportedExportFormats.length }),
    dirtyLabel
  ]
})
</script>

<template>
  <aside
    class="knowledge-rail"
    data-testid="sheets-insight-rail"
    :data-state="workbook ? 'workbook' : 'empty'"
  >
    <article class="rail-card" data-testid="sheets-insight-scope">
      <p class="rail-eyebrow">{{ t('sheets.insight.scopeEyebrow') }}</p>
      <h3>{{ t('sheets.insight.scopeTitle') }}</h3>
      <ul>
        <li data-testid="sheets-insight-scope-tabs">{{ t('sheets.insight.scopeItems.tabs') }}</li>
        <li data-testid="sheets-insight-scope-formulas">{{ t('sheets.insight.scopeItems.formulas') }}</li>
        <li data-testid="sheets-insight-scope-import">{{ t('sheets.insight.scopeItems.import', { value: summarizeSupportedFormats(supportedImportFormats) }) }}</li>
        <li data-testid="sheets-insight-scope-export">{{ t('sheets.insight.scopeItems.export', { value: summarizeSupportedFormats(supportedExportFormats) }) }}</li>
        <li data-testid="sheets-insight-scope-readiness">{{ t('sheets.insight.scopeItems.readiness') }}</li>
      </ul>
    </article>

    <article class="rail-card accent" data-testid="sheets-insight-health">
      <p class="rail-eyebrow">{{ t('sheets.insight.healthEyebrow') }}</p>
      <div class="chips" data-testid="sheets-insight-health-chips">
        <span
          v-for="(note, index) in readinessNotes"
          :key="note"
          class="chip"
          :data-testid="`sheets-insight-health-chip-${index}`"
        >
          {{ note }}
        </span>
      </div>
    </article>

    <article class="rail-card" data-testid="sheets-insight-limits">
      <p class="rail-eyebrow">{{ t('sheets.insight.limitsEyebrow') }}</p>
      <ul>
        <li data-testid="sheets-insight-limit-tabs">{{ t('sheets.insight.limitItems.tabs') }}</li>
        <li data-testid="sheets-insight-limit-realtime">{{ t('sheets.insight.limitItems.realtime') }}</li>
        <li data-testid="sheets-insight-limit-charts">{{ t('sheets.insight.limitItems.charts') }}</li>
      </ul>
    </article>
  </aside>
</template>

<style scoped>
.knowledge-rail {
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

.rail-card h3,
.rail-card p,
.rail-card ul {
  margin: 0;
}

.rail-card ul {
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
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

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
  font-size: 12px;
}
</style>
