<script setup lang="ts">
import { computed, type CSSProperties } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { ActiveSheetsCell, SheetsGrid } from '~/types/sheets'
import { buildCellEditKey, columnLabel, resolveCellPresentation } from '~/utils/sheets'

const CELL_WIDTH = 132
const HEADER_WIDTH = 52
const HEADER_HEIGHT = 43
const ROW_HEIGHT = 42

const props = defineProps<{
  grid: SheetsGrid
  computedGrid: SheetsGrid
  savedGrid: SheetsGrid
  activeCell: ActiveSheetsCell | null
  frozenRowCount: number
  frozenColCount: number
  searchMatchKeys: string[]
  loading: boolean
  saving: boolean
  readonly: boolean
}>()

const emit = defineEmits<{
  selectCell: [payload: ActiveSheetsCell]
  cellChange: [payload: { rowIndex: number; colIndex: number; value: string }]
}>()

const { t } = useI18n()
const columnCount = computed(() => props.grid[0]?.length ?? 0)
const rowNumbers = computed(() => props.grid.map((_, index) => index + 1))
const searchKeySet = computed(() => new Set(props.searchMatchKeys))
const presentationGrid = computed(() => {
  return props.grid.map((row, rowIndex) => {
    return row.map((_, colIndex) => resolveCellPresentation({
      rawGrid: props.grid,
      savedGrid: props.savedGrid,
      computedGrid: props.computedGrid,
      pendingLabel: t('sheets.formula.pending'),
      rowIndex,
      colIndex
    }))
  })
})

function onCellClick(rowIndex: number, colIndex: number): void {
  emit('selectCell', { rowIndex, colIndex })
}

function onCellInput(rowIndex: number, colIndex: number, value: string): void {
  emit('cellChange', { rowIndex, colIndex, value })
}

function isActiveCell(rowIndex: number, colIndex: number): boolean {
  return props.activeCell?.rowIndex === rowIndex && props.activeCell?.colIndex === colIndex
}

function isSearchMatch(rowIndex: number, colIndex: number): boolean {
  return searchKeySet.value.has(buildCellEditKey(rowIndex, colIndex))
}

function buildColumnHeaderStyle(colIndex: number): CSSProperties {
  if (colIndex >= props.frozenColCount) {
    return {}
  }
  return {
    left: `${HEADER_WIDTH + CELL_WIDTH * colIndex}px`,
    zIndex: 5
  }
}

function buildRowHeaderStyle(rowIndex: number): CSSProperties {
  if (rowIndex >= props.frozenRowCount) {
    return {}
  }
  return {
    top: `${HEADER_HEIGHT + ROW_HEIGHT * rowIndex}px`,
    zIndex: 4
  }
}

function buildCellStyle(rowIndex: number, colIndex: number): CSSProperties {
  const style: CSSProperties = {}
  if (rowIndex < props.frozenRowCount) {
    style.top = `${HEADER_HEIGHT + ROW_HEIGHT * rowIndex}px`
  }
  if (colIndex < props.frozenColCount) {
    style.left = `${HEADER_WIDTH + CELL_WIDTH * colIndex}px`
  }
  if (style.top || style.left) {
    style.position = 'sticky'
    style.zIndex = rowIndex < props.frozenRowCount && colIndex < props.frozenColCount ? 4 : 3
  }
  return style
}
</script>

<template>
  <section class="editor-shell mm-card">
    <header class="editor-header">
      <div>
        <p>{{ t('sheets.grid.eyebrow') }}</p>
        <h2>{{ t('sheets.grid.title') }}</h2>
      </div>
      <span>{{ t('sheets.grid.description') }}</span>
    </header>

    <el-skeleton v-if="loading" :rows="8" animated />

    <div v-else-if="!grid.length" class="editor-empty">
      <strong>{{ t('sheets.grid.emptyTitle') }}</strong>
      <p>{{ t('sheets.grid.emptyDescription') }}</p>
    </div>

    <div v-else class="editor-scroll">
      <table class="editor-table">
        <thead>
          <tr>
            <th class="corner-cell">#</th>
            <th
              v-for="colIndex in columnCount"
              :key="colIndex"
              class="col-header"
              :style="buildColumnHeaderStyle(colIndex - 1)"
            >
              {{ columnLabel(colIndex - 1) }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, rowIndex) in grid" :key="rowNumbers[rowIndex]">
            <th class="row-header" :style="buildRowHeaderStyle(rowIndex)">{{ rowNumbers[rowIndex] }}</th>
            <td
              v-for="(_, colIndex) in row"
              :key="`${rowIndex}-${colIndex}`"
              :style="buildCellStyle(rowIndex, colIndex)"
              :class="{
                'cell--active': isActiveCell(rowIndex, colIndex),
                'cell--formula': presentationGrid[rowIndex][colIndex].isFormula,
                'cell--pending': presentationGrid[rowIndex][colIndex].isDirtyFormula,
                'cell--error': presentationGrid[rowIndex][colIndex].hasError,
                'cell--match': isSearchMatch(rowIndex, colIndex),
                'cell--frozen': rowIndex < frozenRowCount || colIndex < frozenColCount
              }"
            >
              <el-input
                v-if="isActiveCell(rowIndex, colIndex)"
                :model-value="presentationGrid[rowIndex][colIndex].rawValue"
                size="small"
                :disabled="saving || readonly"
                @update:model-value="onCellInput(rowIndex, colIndex, String($event ?? ''))"
              />
              <button
                v-else
                class="cell-button"
                type="button"
                :disabled="saving"
                @click="onCellClick(rowIndex, colIndex)"
              >
                <span>{{ presentationGrid[rowIndex][colIndex].displayValue || ' ' }}</span>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.editor-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
  min-height: 620px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 252, 0.96));
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
}

.editor-header p {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--mm-muted);
}

.editor-header h2 {
  margin: 6px 0 0;
  font-size: 24px;
}

.editor-header span {
  color: var(--mm-muted);
  font-size: 13px;
}

.editor-empty {
  padding: 28px 18px;
  border-radius: 18px;
  border: 1px dashed rgba(15, 110, 110, 0.2);
  background: rgba(15, 110, 110, 0.03);
}

.editor-empty strong {
  display: block;
  margin-bottom: 8px;
}

.editor-empty p {
  margin: 0;
  color: var(--mm-muted);
  line-height: 1.7;
}

.editor-scroll {
  overflow: auto;
  padding-bottom: 4px;
}

.editor-table {
  width: 100%;
  min-width: 980px;
  border-collapse: separate;
  border-spacing: 0;
}

.editor-table th,
.editor-table td {
  height: 42px;
  border-right: 1px solid rgba(15, 110, 110, 0.08);
  border-bottom: 1px solid rgba(15, 110, 110, 0.08);
  background: #fff;
}

.editor-table thead th {
  position: sticky;
  top: 0;
  z-index: 3;
}

.corner-cell,
.col-header,
.row-header {
  background: linear-gradient(180deg, #f1f8f8, #e8f2f2);
  color: var(--mm-primary-dark);
  font-weight: 600;
}

.corner-cell,
.row-header {
  position: sticky;
  left: 0;
  z-index: 4;
}

.corner-cell {
  top: 0;
  z-index: 6;
}

.corner-cell,
.row-header {
  width: 52px;
  min-width: 52px;
}

.col-header,
.row-header {
  text-align: center;
  font-size: 12px;
}

.editor-table td {
  min-width: 132px;
  width: 132px;
  padding: 0;
  background: rgba(255, 255, 255, 0.96);
}

.cell-button {
  width: 100%;
  min-height: 42px;
  border: none;
  padding: 0 10px;
  background: transparent;
  text-align: left;
  color: var(--mm-text);
  cursor: pointer;
}

.cell-button:hover {
  background: rgba(15, 110, 110, 0.04);
}

.cell--formula .cell-button {
  color: var(--mm-primary-dark);
  font-weight: 600;
}

.cell--pending .cell-button {
  background: rgba(255, 244, 214, 0.92);
}

.cell--error .cell-button {
  background: rgba(254, 242, 242, 0.96);
  color: #b42318;
}

.cell--match {
  box-shadow: inset 0 0 0 2px rgba(252, 191, 73, 0.55);
  background: linear-gradient(180deg, rgba(255, 249, 235, 0.98), rgba(255, 255, 255, 0.96));
}

.cell--frozen {
  background: linear-gradient(180deg, rgba(244, 251, 251, 0.98), rgba(255, 255, 255, 0.98));
}

.cell--active {
  outline: 2px solid rgba(15, 110, 110, 0.34);
  outline-offset: -2px;
}
</style>
