<script setup lang="ts">
import EmptyState from './EmptyState.vue'
import ErrorState from './ErrorState.vue'

export interface DataTableColumn {
  align?: 'left' | 'center' | 'right'
  cellSlot?: string
  key: string
  label: string
  sortable?: boolean
  width?: string
}

type DataTableRow = Readonly<Record<string, unknown>>
type DataTableDensity = 'comfortable' | 'compact'
type SortDirection = 'ascending' | 'descending' | 'none'

const props = withDefaults(
  defineProps<{
    columns: readonly DataTableColumn[]
    density?: DataTableDensity
    empty?: boolean
    error?: string
    loading?: boolean
    permissionDenied?: boolean
    premiumLocked?: boolean
    rowKey?: string
    rows: readonly DataTableRow[]
    selectedKeys?: readonly string[]
    sortBy?: string
    sortDirection?: SortDirection
    stacked?: boolean
  }>(),
  {
    density: 'comfortable',
    empty: false,
    error: undefined,
    loading: false,
    permissionDenied: false,
    premiumLocked: false,
    rowKey: 'id',
    selectedKeys: () => [],
    sortBy: undefined,
    sortDirection: 'none',
    stacked: false
  }
)

const emit = defineEmits<{
  retry: []
  rowAction: [row: DataTableRow]
  select: [keys: readonly string[]]
  sort: [column: DataTableColumn]
}>()

function getRowKey(row: DataTableRow, index: number) {
  const value = row[props.rowKey]
  return typeof value === 'string' || typeof value === 'number' ? String(value) : `row-${index}`
}

function getCellValue(row: DataTableRow, column: DataTableColumn) {
  const value = row[column.key]
  return value === undefined || value === null || value === '' ? '-' : String(value)
}

function getAriaSort(column: DataTableColumn) {
  return props.sortBy === column.key ? props.sortDirection : 'none'
}

function toggleSelection(row: DataTableRow, index: number) {
  const key = getRowKey(row, index)
  const keys = props.selectedKeys.includes(key)
    ? props.selectedKeys.filter(selectedKey => selectedKey !== key)
    : [...props.selectedKeys, key]
  emit('select', keys)
}
</script>

<template>
  <section class="data-table" :class="[`data-table--${density}`, { 'data-table--stacked': stacked }]">
    <div v-if="loading" class="data-table__state" aria-busy="true">Loading table data</div>
    <ErrorState
      v-else-if="error"
      :description="error"
      retry-label="Retry"
      title="Table failed to load"
      variant="inline"
      @retry="emit('retry')"
    />
    <EmptyState
      v-else-if="permissionDenied"
      description="You do not have permission to view these records."
      title="Permission required"
      variant="permission"
    />
    <EmptyState
      v-else-if="premiumLocked"
      description="This table is available with a Premium workspace entitlement."
      title="Premium feature"
      variant="premium"
    />
    <EmptyState v-else-if="empty || rows.length === 0" description="No records match the current view." title="No records" />
    <div v-else class="data-table__scroll">
      <table>
        <thead>
          <tr>
            <th scope="col" class="data-table__selection">Select</th>
            <th
              v-for="column in columns"
              :key="column.key"
              :aria-sort="getAriaSort(column)"
              :style="{ width: column.width, textAlign: column.align || 'left' }"
              scope="col"
            >
              <button v-if="column.sortable" type="button" @click="emit('sort', column)">
                {{ column.label }}
                <span class="data-table__sort-label">{{ getAriaSort(column) }}</span>
              </button>
              <span v-else>{{ column.label }}</span>
            </th>
            <th scope="col">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in rows" :key="getRowKey(row, index)">
            <td class="data-table__selection">
              <input
                :aria-label="`Select row ${index + 1}`"
                :checked="selectedKeys.includes(getRowKey(row, index))"
                type="checkbox"
                @change="toggleSelection(row, index)"
              />
            </td>
            <td v-for="column in columns" :key="column.key" :data-label="column.label" :style="{ textAlign: column.align || 'left' }">
              <slot :name="column.cellSlot || `cell-${column.key}`" :column="column" :row="row" :value="row[column.key]">
                {{ getCellValue(row, column) }}
              </slot>
            </td>
            <td>
              <button type="button" @click="emit('rowAction', row)">Open</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.data-table {
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius-md);
  background: var(--mm-surface);
  overflow: hidden;
}

.data-table__scroll {
  overflow: auto;
}

.data-table table {
  width: 100%;
  min-width: 680px;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  min-height: 48px;
  padding: 12px 14px;
  border-bottom: 1px solid var(--mm-border);
  color: var(--mm-text-primary);
  font-size: 13px;
  line-height: 1.45;
  vertical-align: middle;
}

.data-table th {
  background: var(--mm-surface-soft);
  color: var(--mm-text-secondary);
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
}

.data-table th button,
.data-table td button {
  min-width: 40px;
  min-height: 34px;
  padding: 0 10px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--mm-brand-primary);
  font-weight: 800;
}

.data-table__selection {
  width: 72px;
}

.data-table__sort-label {
  margin-left: 6px;
  color: var(--mm-text-muted);
  font-size: 10px;
}

.data-table__state {
  padding: 24px;
  color: var(--mm-text-secondary);
}

.data-table--compact th,
.data-table--compact td {
  min-height: 38px;
  padding: 8px 10px;
  font-size: 12px;
}

.data-table--stacked table {
  min-width: 0;
}

@media (max-width: 900px) {
  .data-table td button {
    min-width: 44px;
    min-height: 40px;
  }
}

@media (max-width: 520px) {
  .data-table--stacked thead {
    display: none;
  }

  .data-table--stacked tr,
  .data-table--stacked td {
    display: block;
  }

  .data-table--stacked td {
    min-height: 0;
    padding: 10px 14px;
  }

  .data-table--stacked td::before {
    content: attr(data-label);
    display: block;
    color: var(--mm-text-muted);
    font-size: 11px;
    font-weight: 800;
    text-transform: uppercase;
  }
}
</style>
