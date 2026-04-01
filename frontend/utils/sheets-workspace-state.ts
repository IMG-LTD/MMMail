import type {
  ActiveSheetsCell,
  SheetCellEditInput,
  SheetsWorkbookDetail,
  SheetsWorkbookSheet,
  SheetsWorkbookSummary
} from '~/types/sheets'
import {
  buildCellEditKey,
  countFilledCells,
  countFormulaCells,
  normalizeSheetsGrid
} from '~/utils/sheets'

export function findSheetsWorkbookSheetById(
  workbook: SheetsWorkbookDetail | null,
  sheetId?: string | null
): SheetsWorkbookSheet | null {
  if (!workbook || !sheetId) {
    return null
  }
  return workbook.sheets.find((sheet) => sheet.id === sheetId) || null
}

export function buildSheetsWorkbookGrid(
  workbook: SheetsWorkbookDetail | null,
  grid: string[][] | undefined
): string[][] {
  if (!workbook || !grid) {
    return []
  }
  return normalizeSheetsGrid(grid, workbook.rowCount, workbook.colCount)
}

export function toSheetsWorkbookSummary(
  detail: SheetsWorkbookDetail,
  localGrid: string[][]
): SheetsWorkbookSummary {
  const normalizedGrid = normalizeSheetsGrid(localGrid, detail.rowCount, detail.colCount)
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

export function ensureSheetsActiveCell(
  activeCell: ActiveSheetsCell | null,
  rowCount: number,
  colCount: number
): ActiveSheetsCell | null {
  if (rowCount <= 0 || colCount <= 0) {
    return null
  }
  if (!activeCell) {
    return { rowIndex: 0, colIndex: 0 }
  }
  return {
    rowIndex: Math.min(activeCell.rowIndex, rowCount - 1),
    colIndex: Math.min(activeCell.colIndex, colCount - 1)
  }
}

export function mergeSheetsDirtyEdit(
  dirtyEdits: ReadonlyMap<string, SheetCellEditInput>,
  workbook: SheetsWorkbookDetail | null,
  payload: SheetCellEditInput
): Map<string, SheetCellEditInput> {
  const next = new Map(dirtyEdits)
  const key = buildCellEditKey(payload.rowIndex, payload.colIndex)
  const originalValue = workbook?.grid[payload.rowIndex]?.[payload.colIndex] ?? ''
  if (payload.value === originalValue) {
    next.delete(key)
    return next
  }
  next.set(key, payload)
  return next
}
