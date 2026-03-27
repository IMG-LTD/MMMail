<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { ActiveSheetsCell, SheetsSortDirection } from '~/types/sheets'
import { columnLabel } from '~/utils/sheets'

const props = defineProps<{
  activeCell: ActiveSheetsCell | null
  searchQuery: string
  matchCount: number
  frozenRowCount: number
  frozenColCount: number
  dirtyCount: number
  busy: boolean
  loading: boolean
  canManage: boolean
}>()

const emit = defineEmits<{
  'update:search-query': [value: string]
  sort: [payload: { direction: SheetsSortDirection; includeHeader: boolean }]
  freezeRows: []
  freezeCols: []
  clearFreeze: []
}>()

const { t } = useI18n()
const includeHeader = ref(true)

const selectedRowLabel = computed(() => props.activeCell ? String(props.activeCell.rowIndex + 1) : '—')
const selectedColLabel = computed(() => props.activeCell ? columnLabel(props.activeCell.colIndex) : '—')
const selectedCellLabel = computed(() => {
  if (!props.activeCell) {
    return t('sheets.dataTools.selectionEmpty')
  }
  return `${selectedColLabel.value}${selectedRowLabel.value}`
})
const actionsDisabled = computed(() => props.busy || props.loading || !props.activeCell || !props.canManage)

function onSearchChange(value: string | number): void {
  emit('update:search-query', String(value ?? ''))
}

function onSort(direction: SheetsSortDirection): void {
  emit('sort', { direction, includeHeader: includeHeader.value })
}
</script>

<template>
  <section class="data-tools mm-card">
    <header class="data-tools__header">
      <div>
        <p>{{ t('sheets.dataTools.eyebrow') }}</p>
        <h2>{{ t('sheets.dataTools.title') }}</h2>
      </div>
      <span>{{ t('sheets.dataTools.description') }}</span>
    </header>

    <div class="data-tools__search">
      <label class="data-tools__label">{{ t('sheets.dataTools.searchLabel') }}</label>
      <div class="data-tools__search-row">
        <el-input
          :model-value="searchQuery"
          clearable
          :placeholder="t('sheets.dataTools.searchPlaceholder')"
          :disabled="loading"
          @update:model-value="onSearchChange"
        />
        <span class="data-tools__badge">{{ t('sheets.dataTools.matches', { count: matchCount }) }}</span>
      </div>
    </div>

    <div class="data-tools__grid">
      <article class="data-tools__block">
        <div class="data-tools__block-head">
          <strong>{{ t('sheets.dataTools.sortLabel') }}</strong>
          <el-switch v-model="includeHeader" :disabled="busy || loading" />
        </div>
        <p class="data-tools__helper">{{ t('sheets.dataTools.includeHeader') }}</p>
        <div class="data-tools__actions">
          <el-button :disabled="actionsDisabled" @click="onSort('ASC')">
            {{ t('sheets.dataTools.sortAsc', { value: selectedColLabel }) }}
          </el-button>
          <el-button :disabled="actionsDisabled" @click="onSort('DESC')">
            {{ t('sheets.dataTools.sortDesc', { value: selectedColLabel }) }}
          </el-button>
        </div>
      </article>

      <article class="data-tools__block">
        <div class="data-tools__block-head">
          <strong>{{ t('sheets.dataTools.freezeLabel') }}</strong>
          <span class="data-tools__subtle">{{ t('sheets.dataTools.selectionLabel', { value: selectedCellLabel }) }}</span>
        </div>
        <div class="data-tools__actions data-tools__actions--stacked">
          <el-button :disabled="actionsDisabled" @click="emit('freezeRows')">
            {{ t('sheets.dataTools.freezeRowsTo', { value: selectedRowLabel }) }}
          </el-button>
          <el-button :disabled="actionsDisabled" @click="emit('freezeCols')">
            {{ t('sheets.dataTools.freezeColsTo', { value: selectedColLabel }) }}
          </el-button>
          <el-button text :disabled="busy || loading" @click="emit('clearFreeze')">
            {{ t('sheets.dataTools.clearFreeze') }}
          </el-button>
        </div>
      </article>
    </div>

    <footer class="data-tools__footer">
      <span class="data-tools__chip">{{ t('sheets.dataTools.freezeStatus', { rows: frozenRowCount, cols: frozenColCount }) }}</span>
      <span v-if="dirtyCount > 0" class="data-tools__hint">{{ t('sheets.dataTools.dirtyHint', { count: dirtyCount }) }}</span>
      <span v-else-if="!canManage" class="data-tools__hint">{{ t('sheets.messages.ownerOnlyStructureAction') }}</span>
    </footer>
  </section>
</template>

<style scoped>
.data-tools {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
  background:
    radial-gradient(circle at top right, rgba(15, 110, 110, 0.1), transparent 36%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 249, 249, 0.96));
}

.data-tools__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
}

.data-tools__header p,
.data-tools__label {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--mm-muted);
}

.data-tools__header h2 {
  margin: 6px 0 0;
  font-size: 22px;
}

.data-tools__header span,
.data-tools__helper,
.data-tools__subtle,
.data-tools__hint {
  color: var(--mm-muted);
  font-size: 13px;
  line-height: 1.6;
}

.data-tools__search,
.data-tools__block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.data-tools__search-row,
.data-tools__block-head,
.data-tools__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.data-tools__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.data-tools__block {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(15, 110, 110, 0.08);
  background: rgba(255, 255, 255, 0.72);
}

.data-tools__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.data-tools__actions--stacked {
  align-items: flex-start;
}

.data-tools__badge,
.data-tools__chip {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(15, 110, 110, 0.08);
  color: var(--mm-primary-dark);
  font-size: 12px;
  white-space: nowrap;
}

@media (max-width: 960px) {
  .data-tools__header,
  .data-tools__search-row,
  .data-tools__footer,
  .data-tools__block-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .data-tools__grid {
    grid-template-columns: 1fr;
  }
}
</style>
