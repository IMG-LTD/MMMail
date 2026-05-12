<script setup lang="ts">
export interface DataGridColumn {
  key: string
  label: string
  width?: string
}

export interface DataGridCell {
  columnKey: string
  rowKey: string
  value: string
}

interface DataGridCoordinate {
  columnKey: string
  rowKey: string
}

interface DataGridRange {
  end: DataGridCoordinate
  start: DataGridCoordinate
}

interface ProtectedRange {
  label: string
  range: DataGridRange
}

const props = withDefaults(
  defineProps<{
    activeCell?: DataGridCoordinate
    cells: readonly DataGridCell[]
    columns: readonly DataGridColumn[]
    protectedRanges?: readonly ProtectedRange[]
    readonly?: boolean
    rows: readonly { key: string; label: string }[]
    selectedRange?: DataGridRange
    stickyFirstColumn?: boolean
    stickyHeader?: boolean
  }>(),
  {
    activeCell: undefined,
    protectedRanges: () => [],
    readonly: false,
    selectedRange: undefined,
    stickyFirstColumn: false,
    stickyHeader: false
  }
)

const emit = defineEmits<{
  activeCell: [cell: DataGridCoordinate]
  editRequest: [cell: DataGridCoordinate]
  protectRange: [range: DataGridRange]
  rangeChange: [range: DataGridRange]
}>()

function getCell(rowKey: string, columnKey: string) {
  return props.cells.find(cell => cell.rowKey === rowKey && cell.columnKey === columnKey)
}

function isActive(rowKey: string, columnKey: string) {
  return props.activeCell?.rowKey === rowKey && props.activeCell.columnKey === columnKey
}

function isLocked(rowKey: string, columnKey: string) {
  return props.protectedRanges.some(item => item.range.start.rowKey === rowKey && item.range.start.columnKey === columnKey)
}

function moveActive(rowIndex: number, columnIndex: number, event: KeyboardEvent) {
  const next = resolveNextCell({ rowIndex, columnIndex, key: event.key })
  if (!next) {
    return
  }
  event.preventDefault()
  emit('activeCell', next)
}

function resolveNextCell(input: { columnIndex: number; key: string; rowIndex: number }) {
  const rowDelta = input.key === 'ArrowDown' ? 1 : input.key === 'ArrowUp' ? -1 : 0
  const columnDelta = input.key === 'ArrowRight' ? 1 : input.key === 'ArrowLeft' ? -1 : 0
  const row = props.rows[input.rowIndex + rowDelta]
  const column = props.columns[input.columnIndex + columnDelta]
  return row && column ? { rowKey: row.key, columnKey: column.key } : null
}
</script>

<template>
  <section
    class="data-grid"
    :class="{
      'data-grid--mobile': true,
      'data-grid--readonly': readonly,
      'data-grid--sticky-header': stickyHeader,
      'data-grid--sticky-first-column': stickyFirstColumn
    }"
  >
    <div class="data-grid__scroll" role="grid" aria-label="Spreadsheet data grid">
      <div class="data-grid__row data-grid__row--header" role="row">
        <div class="data-grid__corner" role="columnheader">#</div>
        <div v-for="column in columns" :key="column.key" class="data-grid__header" :style="{ width: column.width }" role="columnheader">
          {{ column.label }}
        </div>
      </div>
      <div v-for="(row, rowIndex) in rows" :key="row.key" class="data-grid__row" role="row">
        <div class="data-grid__row-header" role="rowheader">{{ row.label }}</div>
        <button
          v-for="(column, columnIndex) in columns"
          :key="column.key"
          class="data-grid__cell"
          :class="{ 'data-grid__cell--active': isActive(row.key, column.key), 'data-grid__cell--locked': isLocked(row.key, column.key) }"
          role="gridcell"
          type="button"
          @click="emit('activeCell', { rowKey: row.key, columnKey: column.key })"
          @dblclick="emit('editRequest', { rowKey: row.key, columnKey: column.key })"
          @keydown="moveActive(rowIndex, columnIndex, $event)"
        >
          <span>{{ getCell(row.key, column.key)?.value || '' }}</span>
          <small v-if="isLocked(row.key, column.key)">Locked</small>
        </button>
      </div>
    </div>
    <p v-if="selectedRange" class="data-grid__range">
      Selected {{ selectedRange.start.rowKey }}:{{ selectedRange.start.columnKey }} to {{ selectedRange.end.rowKey }}:{{
        selectedRange.end.columnKey
      }}
    </p>
  </section>
</template>

<style scoped>
.data-grid {
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-md);
  background: var(--mm-surface);
  overflow: hidden;
}

.data-grid__scroll {
  overflow: auto;
}

.data-grid__row {
  display: grid;
  grid-template-columns: 64px repeat(var(--data-grid-columns, 8), minmax(120px, 1fr));
  min-width: 780px;
}

.data-grid__corner,
.data-grid__header,
.data-grid__row-header,
.data-grid__cell {
  min-height: 40px;
  padding: 9px 10px;
  border: 0;
  border-right: 1px solid var(--mm-border);
  border-bottom: 1px solid var(--mm-border);
  background: var(--mm-surface);
  color: var(--mm-text-primary);
  font-size: 12px;
  text-align: left;
}

.data-grid__corner,
.data-grid__header,
.data-grid__row-header {
  background: var(--mm-surface-soft);
  color: var(--mm-text-secondary);
  font-weight: 800;
}

.data-grid__cell {
  display: grid;
  gap: 2px;
  cursor: cell;
}

.data-grid__cell small {
  color: var(--mm-warning);
  font-size: 10px;
  font-weight: 800;
}

.data-grid__cell--active {
  box-shadow: inset 0 0 0 2px var(--mm-brand-primary);
}

.data-grid__cell--locked {
  background: color-mix(in srgb, var(--mm-warning) 8%, var(--mm-surface));
}

.data-grid__range {
  margin: 0;
  padding: 10px 12px;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.data-grid--sticky-header .data-grid__row--header {
  position: sticky;
  top: 0;
  z-index: 2;
}

.data-grid--sticky-first-column .data-grid__row-header,
.data-grid--sticky-first-column .data-grid__corner {
  position: sticky;
  left: 0;
  z-index: 3;
}

@media (max-width: 520px) {
  .data-grid--mobile .data-grid__row {
    min-width: 620px;
  }
}
</style>
