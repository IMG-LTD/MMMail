import { computed, ref, watch, type ComputedRef, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { useSheetsApi } from '~/composables/useSheetsApi'
import type { ApiClientError } from '~/utils/request'
import type {
  ActiveSheetsCell,
  SheetsGrid,
  SheetsSortDirection,
  SheetsWorkbookDetail,
  SheetsWorkbookSheet,
  SheetsWorkbookSummary
} from '~/types/sheets'
import { countFilledCells, countFormulaCells, normalizeSheetsGrid, sortSheetsWorkbooks } from '~/utils/sheets'
import { findSheetsSearchMatches } from '~/utils/sheets-search'

interface UseSheetsDataToolsOptions {
  workbooks: Ref<SheetsWorkbookSummary[]>
  activeWorkbook: Ref<SheetsWorkbookDetail | null>
  activeSheet: ComputedRef<SheetsWorkbookSheet | null>
  activeCell: Ref<ActiveSheetsCell | null>
  localGrid: Ref<SheetsGrid>
  computedGrid: ComputedRef<SheetsGrid>
  conflictMessage: Ref<string>
  dirtyCount: ComputedRef<number>
}

interface SortSheetPayload {
  direction: SheetsSortDirection
  includeHeader: boolean
}

export function useSheetsDataTools(options: UseSheetsDataToolsOptions) {
  const api = useSheetsApi()
  const { t } = useI18n()

  const toolsBusy = ref(false)
  const searchQuery = ref('')

  const searchMatches = computed(() => findSheetsSearchMatches({
    rawGrid: options.localGrid.value,
    computedGrid: options.computedGrid.value,
    query: searchQuery.value
  }))
  const searchMatchCount = computed(() => searchMatches.value.length)
  const searchMatchKeys = computed(() => searchMatches.value.map((item) => item.key))
  const frozenRowCount = computed(() => options.activeSheet.value?.frozenRowCount ?? 0)
  const frozenColCount = computed(() => options.activeSheet.value?.frozenColCount ?? 0)

  watch(() => options.activeWorkbook.value?.id ?? null, () => {
    searchQuery.value = ''
  })

  async function onSortSheet(payload: SortSheetPayload): Promise<boolean> {
    if (!canRunMutation(true)) {
      return false
    }
    const workbook = options.activeWorkbook.value
    const sheet = options.activeSheet.value
    const activeCell = options.activeCell.value
    if (!workbook || !sheet || !activeCell) {
      return false
    }
    toolsBusy.value = true
    try {
      const detail = await api.sortSheet(workbook.id, sheet.id, {
        currentVersion: workbook.currentVersion,
        columnIndex: activeCell.colIndex,
        direction: payload.direction,
        includeHeader: payload.includeHeader
      })
      applyWorkbookDetail(detail)
      ElMessage.success(t('sheets.messages.sortSheetSuccess'))
      return true
    } catch (error) {
      handleMutationError(error, 'sheets.messages.sortSheetFailed')
      return false
    } finally {
      toolsBusy.value = false
    }
  }

  async function onFreezeRowsToActiveCell(): Promise<boolean> {
    const activeCell = options.activeCell.value
    return updateFreezeState(activeCell ? activeCell.rowIndex + 1 : null, frozenColCount.value)
  }

  async function onFreezeColsToActiveCell(): Promise<boolean> {
    const activeCell = options.activeCell.value
    return updateFreezeState(frozenRowCount.value, activeCell ? activeCell.colIndex + 1 : null)
  }

  async function onClearFreeze(): Promise<boolean> {
    return updateFreezeState(0, 0)
  }

  function updateSearchQuery(value: string): void {
    searchQuery.value = value
  }

  async function updateFreezeState(nextFrozenRowCount: number | null, nextFrozenColCount: number | null): Promise<boolean> {
    if (!canRunMutation(nextFrozenRowCount !== 0 || nextFrozenColCount != 0)) {
      return false
    }
    const workbook = options.activeWorkbook.value
    const sheet = options.activeSheet.value
    if (!workbook || !sheet) {
      return false
    }
    toolsBusy.value = true
    try {
      const detail = await api.freezeSheet(workbook.id, sheet.id, {
        currentVersion: workbook.currentVersion,
        frozenRowCount: nextFrozenRowCount ?? 0,
        frozenColCount: nextFrozenColCount ?? 0
      })
      applyWorkbookDetail(detail)
      ElMessage.success(t('sheets.messages.freezeSheetSuccess'))
      return true
    } catch (error) {
      handleMutationError(error, 'sheets.messages.freezeSheetFailed')
      return false
    } finally {
      toolsBusy.value = false
    }
  }

  function canRunMutation(requireSelection: boolean): boolean {
    if (!options.activeWorkbook.value || !options.activeSheet.value) {
      return false
    }
    if (options.activeWorkbook.value.permission !== 'OWNER') {
      ElMessage.warning(t('sheets.messages.ownerOnlyStructureAction'))
      return false
    }
    if (requireSelection && !options.activeCell.value) {
      return false
    }
    if (options.dirtyCount.value > 0) {
      ElMessage.warning(t('sheets.messages.saveBeforeDataTools'))
      return false
    }
    return true
  }

  function applyWorkbookDetail(detail: SheetsWorkbookDetail): void {
    options.activeWorkbook.value = detail
    options.localGrid.value = normalizeSheetsGrid(detail.grid, detail.rowCount, detail.colCount)
    options.conflictMessage.value = ''
    syncActiveCell(detail.rowCount, detail.colCount)
    mergeWorkbookSummary(buildWorkbookSummary(detail))
  }

  function syncActiveCell(rowCount: number, colCount: number): void {
    const activeCell = options.activeCell.value
    if (rowCount <= 0 || colCount <= 0) {
      options.activeCell.value = null
      return
    }
    if (!activeCell) {
      options.activeCell.value = { rowIndex: 0, colIndex: 0 }
      return
    }
    options.activeCell.value = {
      rowIndex: Math.min(activeCell.rowIndex, rowCount - 1),
      colIndex: Math.min(activeCell.colIndex, colCount - 1)
    }
  }

  function mergeWorkbookSummary(summary: SheetsWorkbookSummary): void {
    const index = options.workbooks.value.findIndex((item) => item.id === summary.id)
    const next = [...options.workbooks.value]
    if (index >= 0) {
      next.splice(index, 1, summary)
    } else {
      next.unshift(summary)
    }
    options.workbooks.value = sortSheetsWorkbooks(next)
  }

  function buildWorkbookSummary(detail: SheetsWorkbookDetail): SheetsWorkbookSummary {
    const normalizedGrid = normalizeSheetsGrid(detail.grid, detail.rowCount, detail.colCount)
    return {
      id: detail.id,
      title: detail.title,
      rowCount: detail.rowCount,
      colCount: detail.colCount,
      filledCellCount: countFilledCells(normalizedGrid),
      formulaCellCount: countFormulaCells(normalizedGrid),
      computedErrorCount: detail.computedErrorCount,
      currentVersion: detail.currentVersion,
      sheetCount: detail.sheetCount,
      activeSheetId: detail.activeSheetId,
      updatedAt: detail.updatedAt,
      lastOpenedAt: detail.lastOpenedAt,
      permission: detail.permission,
      scope: detail.scope,
      ownerEmail: detail.ownerEmail,
      ownerDisplayName: detail.ownerDisplayName,
      collaboratorCount: detail.collaboratorCount,
      canEdit: detail.canEdit
    }
  }

  function handleMutationError(error: unknown, fallbackMessageKey: string): void {
    if (isConflict(error)) {
      options.conflictMessage.value = t('sheets.messages.versionConflict')
      ElMessage.warning(options.conflictMessage.value)
      return
    }
    ElMessage.error((error as Error).message || t(fallbackMessageKey))
  }

  function isConflict(error: unknown): boolean {
    const normalized = error as ApiClientError
    return normalized.code === 30032 || normalized.status === 409
  }

  return {
    toolsBusy,
    searchQuery,
    searchMatches,
    searchMatchCount,
    searchMatchKeys,
    frozenRowCount,
    frozenColCount,
    updateSearchQuery,
    onSortSheet,
    onFreezeRowsToActiveCell,
    onFreezeColsToActiveCell,
    onClearFreeze
  }
}
