<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { SheetsWorkbookSummary } from '~/types/sheets'
import { formatSheetsTime, summarizeGridFootprint } from '~/utils/sheets'

const props = defineProps<{
  workbooks: SheetsWorkbookSummary[]
  activeWorkbookId: string | null
  loading: boolean
  busyWorkbookId: string | null
}>()

const emit = defineEmits<{
  select: [workbookId: string]
  rename: [workbook: SheetsWorkbookSummary]
  delete: [workbook: SheetsWorkbookSummary]
  create: []
}>()

const { t } = useI18n()
</script>

<template>
  <section class="sidebar-shell mm-card">
    <header class="sidebar-header">
      <div>
        <p>{{ t('sheets.sidebar.eyebrow') }}</p>
        <h2>{{ t('sheets.sidebar.title') }}</h2>
      </div>
      <el-button type="primary" plain size="small" @click="emit('create')">{{ t('common.actions.create') }}</el-button>
    </header>

    <el-skeleton v-if="loading" :rows="5" animated />

    <div v-else-if="!workbooks.length" class="sidebar-empty">
      <strong>{{ t('sheets.sidebar.emptyTitle') }}</strong>
      <p>{{ t('sheets.sidebar.emptyDescription') }}</p>
    </div>

    <div v-else class="sidebar-list">
      <div
        v-for="workbook in workbooks"
        :key="workbook.id"
        class="sidebar-item"
        :class="{ 'sidebar-item--active': workbook.id === activeWorkbookId }"
        role="button"
        tabindex="0"
        @click="emit('select', workbook.id)"
        @keydown.enter.prevent="emit('select', workbook.id)"
        @keydown.space.prevent="emit('select', workbook.id)"
      >
        <div class="sidebar-item__copy">
          <strong>{{ workbook.title }}</strong>
          <div class="sidebar-item__badges">
            <span class="sidebar-badge">{{ workbook.scope === 'OWNED' ? t('sheets.filters.owned') : t('sheets.filters.shared') }}</span>
            <span class="sidebar-badge sidebar-badge--soft">
              {{ workbook.permission === 'OWNER'
                ? t('sheets.share.permission.owner')
                : workbook.permission === 'EDIT'
                  ? t('sheets.share.permission.edit')
                  : t('sheets.share.permission.view') }}
            </span>
          </div>
          <span>{{ summarizeGridFootprint(workbook.rowCount, workbook.colCount) }}</span>
          <small>{{ t('sheets.sidebar.sheetCount', { count: workbook.sheetCount }) }}</small>
          <small v-if="workbook.scope === 'SHARED'">{{ t('sheets.meta.owner', { value: workbook.ownerDisplayName || workbook.ownerEmail }) }}</small>
          <small v-else>{{ t('sheets.meta.collaborators', { count: workbook.collaboratorCount }) }}</small>
          <small>
            {{ t('sheets.sidebar.stats', {
              filled: workbook.filledCellCount,
              formulas: workbook.formulaCellCount,
              errors: workbook.computedErrorCount
            }) }}
          </small>
          <small>{{ t('sheets.sidebar.openedAt', { value: formatSheetsTime(workbook.lastOpenedAt || workbook.updatedAt) }) }}</small>
        </div>
        <div class="sidebar-item__meta">
          <span>v{{ workbook.currentVersion }}</span>
          <div v-if="workbook.permission === 'OWNER'" class="sidebar-item__actions">
            <el-button link size="small" :disabled="busyWorkbookId === workbook.id" @click.stop="emit('rename', workbook)">{{ t('common.actions.rename') }}</el-button>
            <el-button link type="danger" size="small" :disabled="busyWorkbookId === workbook.id" @click.stop="emit('delete', workbook)">{{ t('common.actions.delete') }}</el-button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.sidebar-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
  min-height: 420px;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.sidebar-header p {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--mm-muted);
}

.sidebar-header h2 {
  margin: 6px 0 0;
  font-size: 24px;
}

.sidebar-empty {
  padding: 22px 18px;
  border-radius: 18px;
  border: 1px dashed rgba(15, 110, 110, 0.14);
  background: rgba(15, 110, 110, 0.04);
}

.sidebar-empty strong {
  display: block;
  margin-bottom: 8px;
}

.sidebar-empty p {
  margin: 0;
  color: var(--mm-muted);
  line-height: 1.7;
}

.sidebar-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar-item {
  width: 100%;
  padding: 14px;
  border: 1px solid rgba(15, 110, 110, 0.08);
  border-radius: 18px;
  background: #fff;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.sidebar-item:hover,
.sidebar-item--active {
  transform: translateY(-1px);
  border-color: rgba(15, 110, 110, 0.28);
  box-shadow: 0 14px 28px rgba(9, 71, 71, 0.08);
}

.sidebar-item--active {
  background: linear-gradient(135deg, rgba(232, 247, 247, 0.94), rgba(247, 252, 252, 0.96));
}

.sidebar-item__copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sidebar-item__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.sidebar-badge {
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(15, 110, 110, 0.1);
  color: var(--mm-primary-dark);
  font-size: 11px;
}

.sidebar-badge--soft {
  background: rgba(15, 110, 110, 0.05);
}

.sidebar-item__copy strong {
  font-size: 15px;
}

.sidebar-item__copy span,
.sidebar-item__copy small {
  color: var(--mm-muted);
}

.sidebar-item__meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  color: var(--mm-primary-dark);
  font-size: 12px;
}

.sidebar-item__actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}
</style>
