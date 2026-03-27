<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type {
  SheetsExportFormat,
  SheetsImportSummary,
  SheetsWorkbookDetail,
  SheetsWorkbookExport
} from '~/types/sheets'
import { formatSheetsFormatLabel, formatSheetsTime, summarizeGridFootprint, summarizeSupportedFormats } from '~/utils/sheets'

const props = defineProps<{
  workbook: SheetsWorkbookDetail | null
  supportedImportFormats: string[]
  supportedExportFormats: string[]
  lastImported: SheetsImportSummary | null
  lastExport: SheetsWorkbookExport | null
  importing: boolean
  exporting: boolean
}>()

const emit = defineEmits<{
  import: [payload: { file: File; title: string }]
  export: [format: SheetsExportFormat]
}>()

const { t } = useI18n()
const fileInputRef = ref<HTMLInputElement | null>(null)
const importTitle = ref('')
const exportFormat = ref<SheetsExportFormat>('CSV')
const activeSheet = computed(() => {
  if (!props.workbook) {
    return null
  }
  return props.workbook.sheets.find((sheet) => sheet.id === props.workbook?.activeSheetId) || null
})
const exportScopeLabel = computed(() => {
  return exportFormat.value === 'JSON'
    ? t('sheets.trade.exportScopeJson')
    : t('sheets.trade.exportScopeActiveSheet')
})

watch(
  () => props.supportedExportFormats,
  (formats) => {
    const next = formats[0] as SheetsExportFormat | undefined
    if (next && !formats.includes(exportFormat.value)) {
      exportFormat.value = next
    }
  },
  { immediate: true }
)

function openFilePicker(): void {
  fileInputRef.value?.click()
}

function onFileSelected(event: Event): void {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }
  emit('import', { file, title: importTitle.value.trim() })
  importTitle.value = ''
  input.value = ''
}

function onExport(): void {
  emit('export', exportFormat.value)
}
</script>

<template>
  <section class="trade-panel mm-card">
    <header class="panel-head">
      <div>
        <p class="eyebrow">{{ t('sheets.trade.eyebrow') }}</p>
        <h3>{{ t('sheets.trade.title') }}</h3>
      </div>
      <div class="head-pills">
        <span class="pill">{{ t('sheets.trade.importFormats', { value: summarizeSupportedFormats(supportedImportFormats) }) }}</span>
        <span class="pill">{{ t('sheets.trade.exportFormats', { value: summarizeSupportedFormats(supportedExportFormats) }) }}</span>
      </div>
    </header>

    <article class="trade-card">
      <div class="card-copy">
        <strong>{{ t('sheets.trade.importTitle') }}</strong>
        <p>{{ t('sheets.trade.importDescription') }}</p>
      </div>
      <el-input v-model="importTitle" :placeholder="t('sheets.trade.importPlaceholder')" />
      <el-button type="primary" :loading="importing" @click="openFilePicker">{{ t('sheets.trade.chooseFile') }}</el-button>
      <input ref="fileInputRef" class="file-input" type="file" accept=".csv,.tsv,.xlsx" @change="onFileSelected" />
      <div v-if="lastImported" class="meta-grid">
        <span class="pill accent">{{ lastImported.title }}</span>
        <span class="pill accent">{{ formatSheetsFormatLabel(lastImported.sourceFormat) }}</span>
        <span class="pill accent">{{ summarizeGridFootprint(lastImported.rowCount, lastImported.colCount) }}</span>
        <span class="pill accent">{{ t('sheets.trade.formulas', { count: lastImported.formulaCellCount }) }}</span>
        <span class="pill accent">{{ formatSheetsTime(lastImported.importedAt) }}</span>
      </div>
    </article>

    <article class="trade-card">
      <div class="card-copy">
        <strong>{{ t('sheets.trade.exportTitle') }}</strong>
        <p>{{ t('sheets.trade.exportDescription') }}</p>
      </div>
      <div class="export-row">
        <el-select v-model="exportFormat" :disabled="!workbook || exporting">
          <el-option
            v-for="format in supportedExportFormats"
            :key="format"
            :label="formatSheetsFormatLabel(format)"
            :value="format"
          />
        </el-select>
        <el-button type="success" plain :disabled="!workbook" :loading="exporting" @click="onExport">{{ t('common.actions.export') }}</el-button>
      </div>
      <div v-if="workbook" class="meta-grid">
        <span class="pill">{{ workbook.title }}</span>
        <span class="pill">{{ t('sheets.trade.sheetCount', { count: workbook.sheetCount }) }}</span>
        <span class="pill">{{ t('sheets.trade.activeSheet', { value: activeSheet?.name || t('common.none') }) }}</span>
        <span class="pill">{{ exportScopeLabel }}</span>
        <span class="pill">{{ t('sheets.trade.formulas', { count: workbook.formulaCellCount }) }}</span>
        <span class="pill">{{ t('sheets.trade.errors', { count: workbook.computedErrorCount }) }}</span>
      </div>
      <div v-if="lastExport" class="meta-grid">
        <span class="pill accent">{{ lastExport.fileName }}</span>
        <span class="pill accent">{{ formatSheetsFormatLabel(lastExport.format) }}</span>
        <span class="pill accent">{{ formatSheetsTime(lastExport.exportedAt) }}</span>
      </div>
    </article>
  </section>
</template>

<style scoped>
.trade-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
}

.panel-head,
.head-pills,
.meta-grid,
.export-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.panel-head {
  justify-content: space-between;
  align-items: flex-start;
}

.panel-head h3,
.panel-head p,
.trade-card p,
.trade-card strong {
  margin: 0;
}

.eyebrow {
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 11px;
  color: #8d6b38;
}

.trade-card {
  padding: 16px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 251, 242, 0.94), rgba(255, 255, 255, 0.96));
  border: 1px solid rgba(141, 107, 56, 0.12);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.card-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.card-copy p {
  color: rgba(68, 56, 39, 0.72);
  line-height: 1.6;
}

.pill {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(246, 239, 226, 0.92);
  border: 1px solid rgba(141, 107, 56, 0.12);
  font-size: 12px;
}

.pill.accent {
  background: rgba(255, 249, 235, 0.98);
}

.file-input {
  display: none;
}
</style>
