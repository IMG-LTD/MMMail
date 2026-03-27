<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { SheetsWorkbookDetail, SheetsWorkbookSheet } from '~/types/sheets'

const props = defineProps<{
  workbook: SheetsWorkbookDetail | null
  busy: boolean
  canManageSheets: boolean
}>()

const emit = defineEmits<{
  selectSheet: [sheetId: string]
  createSheet: []
  renameSheet: [sheet: SheetsWorkbookSheet]
  deleteSheet: [sheet: SheetsWorkbookSheet]
}>()

const { t } = useI18n()

function onCommand(sheet: SheetsWorkbookSheet, command: string | number | object): void {
  if (command === 'rename') {
    emit('renameSheet', sheet)
    return
  }
  if (command === 'delete') {
    emit('deleteSheet', sheet)
  }
}
</script>

<template>
  <section v-if="workbook" class="tabs-shell mm-card">
    <header class="tabs-header">
      <div>
        <p>{{ t('sheets.tabs.eyebrow') }}</p>
        <h3>{{ t('sheets.tabs.title') }}</h3>
      </div>
      <span class="tabs-count">{{ t('sheets.tabs.count', { count: workbook.sheetCount }) }}</span>
    </header>

    <div class="tabs-strip">
      <article
        v-for="sheet in workbook.sheets"
        :key="sheet.id"
        class="sheet-tab"
        :class="{ 'sheet-tab--active': sheet.id === workbook.activeSheetId }"
      >
        <button
          class="sheet-tab__main"
          type="button"
          :disabled="busy"
          @click="emit('selectSheet', sheet.id)"
        >
          <span class="sheet-tab__title">{{ sheet.name }}</span>
          <span class="sheet-tab__meta">{{ t('sheets.tabs.dimensions', { rows: sheet.rowCount, cols: sheet.colCount }) }}</span>
        </button>

        <span v-if="sheet.id === workbook.activeSheetId" class="sheet-tab__badge">{{ t('sheets.tabs.active') }}</span>

        <el-dropdown v-if="canManageSheets" trigger="click" :disabled="busy" @command="onCommand(sheet, $event)">
          <button class="sheet-tab__menu" type="button" :disabled="busy" @click.stop>
            <span>⋯</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="rename">{{ t('sheets.tabs.rename') }}</el-dropdown-item>
              <el-dropdown-item command="delete" :disabled="workbook.sheets.length === 1">{{ t('sheets.tabs.delete') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </article>

      <button v-if="canManageSheets" class="sheet-create" type="button" :disabled="busy" @click="emit('createSheet')">
        <span>＋</span>
        <span>{{ t('sheets.tabs.new') }}</span>
      </button>
    </div>
  </section>
</template>

<style scoped>
.tabs-shell {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 252, 0.96));
}

.tabs-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.tabs-header p,
.tabs-header h3 {
  margin: 0;
}

.tabs-header p {
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--mm-muted);
}

.tabs-header h3 {
  margin-top: 6px;
  font-size: 18px;
}

.tabs-count {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(15, 110, 110, 0.08);
  color: var(--mm-primary-dark);
  font-size: 12px;
}

.tabs-strip {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.sheet-tab,
.sheet-create {
  flex: 0 0 auto;
}

.sheet-tab {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-width: 220px;
  padding: 6px;
  border-radius: 18px;
  border: 1px solid rgba(15, 110, 110, 0.12);
  background: rgba(255, 255, 255, 0.94);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.sheet-tab--active {
  border-color: rgba(15, 110, 110, 0.32);
  box-shadow: 0 14px 28px rgba(9, 71, 71, 0.08);
  background: linear-gradient(135deg, rgba(232, 247, 247, 0.96), rgba(247, 252, 252, 0.98));
}

.sheet-tab__main {
  border: none;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 8px 10px;
  text-align: left;
  cursor: pointer;
}

.sheet-tab__title {
  font-weight: 700;
  color: var(--mm-text);
}

.sheet-tab__meta {
  font-size: 12px;
  color: var(--mm-muted);
}

.sheet-tab__badge {
  justify-self: start;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 110, 110, 0.1);
  color: var(--mm-primary-dark);
  font-size: 11px;
}

.sheet-tab__menu {
  align-self: stretch;
  width: 36px;
  border: none;
  border-radius: 12px;
  background: rgba(15, 110, 110, 0.06);
  color: var(--mm-primary-dark);
  cursor: pointer;
}

.sheet-create {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  min-height: 64px;
  border-radius: 18px;
  border: 1px dashed rgba(15, 110, 110, 0.24);
  background: rgba(255, 255, 255, 0.88);
  color: var(--mm-primary-dark);
  cursor: pointer;
  font-weight: 600;
}

@media (max-width: 768px) {
  .sheet-tab {
    min-width: 200px;
  }
}
</style>
