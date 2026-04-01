import type { ComputedRef, Ref } from 'vue'
import type { LocationQuery } from 'vue-router'
import type {
  ActiveSheetsCell,
  SheetCellEditInput,
  SheetsWorkbookDetail,
  SheetsWorkbookExport,
  SheetsWorkbookSummary
} from '~/types/sheets'
import { sortSheetsWorkbooks } from '~/utils/sheets'
import type { ApiClientError } from '~/utils/request'
import {
  buildSheetsWorkspaceRouteQuery,
  extractSheetsWorkspaceRouteState,
  hasSheetsWorkspaceRouteStateChanged
} from '~/utils/sheets-workspace-route'
import {
  buildSheetsWorkbookGrid,
  ensureSheetsActiveCell,
  toSheetsWorkbookSummary
} from '~/utils/sheets-workspace-state'

export interface SheetsWorkbookSelectionOptions {
  skipDiscardConfirm?: boolean
}

export type SelectSheetsWorkbook = (
  workbookId: string,
  syncRouteAfterLoad: boolean,
  options?: SheetsWorkbookSelectionOptions
) => Promise<boolean>

export function mergeSheetsWorkbookSummaryList(
  workbooks: SheetsWorkbookSummary[],
  summary: SheetsWorkbookSummary
): SheetsWorkbookSummary[] {
  const index = workbooks.findIndex((item) => item.id === summary.id)
  const next = [...workbooks]
  if (index >= 0) {
    next.splice(index, 1, summary)
  } else {
    next.unshift(summary)
  }
  return sortSheetsWorkbooks(next)
}

export function applySheetsWorkbookDetailState(options: {
  activeWorkbook: Ref<SheetsWorkbookDetail | null>
  activeCell: Ref<ActiveSheetsCell | null>
  localGrid: Ref<string[][]>
  dirtyEdits: Ref<Map<string, SheetCellEditInput>>
  lastExport: Ref<SheetsWorkbookExport | null>
  workbooks: Ref<SheetsWorkbookSummary[]>
}, detail: SheetsWorkbookDetail, preserveLocalState = false): void {
  const previousWorkbook = options.activeWorkbook.value
  const previousWorkbookId = previousWorkbook?.id ?? null
  const previousSheetId = previousWorkbook?.activeSheetId ?? null
  const shouldPreserveLocalState = Boolean(
    preserveLocalState
    && previousWorkbook
    && previousWorkbook.id === detail.id
    && previousWorkbook.activeSheetId === detail.activeSheetId
    && previousWorkbook.rowCount === detail.rowCount
    && previousWorkbook.colCount === detail.colCount
    && previousWorkbook.sheetCount === detail.sheetCount
    && previousWorkbook.sheets.map((sheet) => sheet.id).join(',') === detail.sheets.map((sheet) => sheet.id).join(',')
  )
  options.activeWorkbook.value = detail
  if (shouldPreserveLocalState) {
    options.localGrid.value = buildSheetsWorkbookGrid(detail, options.localGrid.value)
  } else {
    options.localGrid.value = buildSheetsWorkbookGrid(detail, detail.grid)
    options.dirtyEdits.value = new Map()
  }
  if (previousWorkbookId !== detail.id || previousSheetId !== detail.activeSheetId) {
    options.lastExport.value = null
  }
  options.activeCell.value = ensureSheetsActiveCell(options.activeCell.value, detail.rowCount, detail.colCount)
  options.workbooks.value = mergeSheetsWorkbookSummaryList(
    options.workbooks.value,
    toSheetsWorkbookSummary(detail, options.localGrid.value)
  )
}

export function clearSheetsWorkspaceSelection(options: {
  activeWorkbook: Ref<SheetsWorkbookDetail | null>
  activeCell: Ref<ActiveSheetsCell | null>
  localGrid: Ref<string[][]>
  dirtyEdits: Ref<Map<string, SheetCellEditInput>>
  conflictMessage: Ref<string>
  lastExport: Ref<SheetsWorkbookExport | null>
}): void {
  options.activeWorkbook.value = null
  options.activeCell.value = null
  options.localGrid.value = []
  options.dirtyEdits.value = new Map()
  options.conflictMessage.value = ''
  options.lastExport.value = null
}

export async function confirmSheetsWorkspaceDiscard(options: {
  dirtyEdits: Ref<Map<string, SheetCellEditInput>>
  activeSheetName: ComputedRef<string>
  fallbackTitle: string
  confirmDiscardChanges: (name: string) => Promise<boolean>
}): Promise<boolean> {
  if (options.dirtyEdits.value.size === 0) {
    return true
  }
  return options.confirmDiscardChanges(options.activeSheetName.value || options.fallbackTitle)
}

export async function syncSheetsWorkspaceRoute(options: {
  query: LocationQuery
  workbookId: string | null
  replace: (location: { path: string; query: ReturnType<typeof buildSheetsWorkspaceRouteQuery> }) => Promise<unknown>
}): Promise<void> {
  const nextState = {
    ...extractSheetsWorkspaceRouteState(options.query),
    workbookId: options.workbookId
  }
  if (!hasSheetsWorkspaceRouteStateChanged(options.query, nextState)) {
    return
  }
  await options.replace({
    path: '/sheets',
    query: buildSheetsWorkspaceRouteQuery(options.query, nextState)
  })
}

export async function handleDeletedSheetsWorkbook(options: {
  activeWorkbookId: string | null
  currentWorkbookId: string
  nextWorkbookId: string | null
  selectWorkbook: SelectSheetsWorkbook
  clearSelection: () => void
  syncRoute: (workbookId: string | null) => Promise<void>
  skipDiscardConfirm?: boolean
}): Promise<void> {
  if (options.activeWorkbookId !== options.currentWorkbookId) {
    return
  }
  if (options.nextWorkbookId) {
    await options.selectWorkbook(options.nextWorkbookId, true, {
      skipDiscardConfirm: options.skipDiscardConfirm
    })
    return
  }
  options.clearSelection()
  await options.syncRoute(null)
}

export async function refreshSheetsWorkbookList(options: {
  preferredWorkbookId: string | null
  listWorkbooks: (limit?: number) => Promise<SheetsWorkbookSummary[]>
  workbooks: Ref<SheetsWorkbookSummary[]>
  activeWorkbook: Ref<SheetsWorkbookDetail | null>
  localGrid: Ref<string[][]>
  loadingList: Ref<boolean>
  clearSelection: () => void
  syncRoute: (workbookId: string | null) => Promise<void>
  selectWorkbook: SelectSheetsWorkbook
  selectionOptions?: SheetsWorkbookSelectionOptions
  onLoadError: (error: unknown) => void
}): Promise<boolean> {
  options.loadingList.value = true
  try {
    const next = sortSheetsWorkbooks(await options.listWorkbooks(100))
    options.workbooks.value = next
    if (!next.length) {
      options.clearSelection()
      await options.syncRoute(null)
      return true
    }
    const targetId = options.preferredWorkbookId && next.some((item) => item.id === options.preferredWorkbookId)
      ? options.preferredWorkbookId
      : next[0].id
    if (!options.activeWorkbook.value || options.activeWorkbook.value.id !== targetId) {
      return await options.selectWorkbook(targetId, true, options.selectionOptions)
    }
    options.workbooks.value = mergeSheetsWorkbookSummaryList(
      options.workbooks.value,
      toSheetsWorkbookSummary(options.activeWorkbook.value, options.localGrid.value)
    )
    return true
  } catch (error) {
    options.onLoadError(error)
    return false
  } finally {
    options.loadingList.value = false
  }
}

export function isSheetsConflictError(error: unknown): boolean {
  const normalized = error as ApiClientError
  return normalized.code === 30032 || normalized.status === 409
}
