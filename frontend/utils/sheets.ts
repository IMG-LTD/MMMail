import type {
  SheetCellEditInput,
  SheetsExportFormat,
  SheetsGrid,
  SheetsImportFormat,
  SheetsWorkbookDetail
} from '~/types/sheets'

export const DEFAULT_SHEETS_ROW_COUNT = 20
export const DEFAULT_SHEETS_COL_COUNT = 12
export const PENDING_FORMULA_LABEL = 'Pending…'
export const SUPPORTED_SHEETS_IMPORT_FORMATS: SheetsImportFormat[] = ['CSV', 'TSV', 'XLSX']
export const SUPPORTED_SHEETS_EXPORT_FORMATS: SheetsExportFormat[] = ['CSV', 'TSV', 'JSON']

export interface SheetsCellPresentation {
  rawValue: string
  savedRawValue: string
  computedValue: string
  displayValue: string
  isFormula: boolean
  isDirty: boolean
  isDirtyFormula: boolean
  hasError: boolean
}

interface ResolveCellPresentationOptions {
  rawGrid: SheetsGrid
  savedGrid?: SheetsGrid | null
  computedGrid?: SheetsGrid | null
  pendingLabel?: string
  rowIndex: number
  colIndex: number
}

interface FormulaHintLabels {
  emptySelection: string
  emptyCell: string
  dirtyFormula: string
  error: string
  formula: string
  literal: string
}

interface SheetsHealthOptions {
  workbookCount: number
  workbook: SheetsWorkbookDetail | null
  dirtyCount: number
}

export function createEmptyGrid(
  rowCount = DEFAULT_SHEETS_ROW_COUNT,
  colCount = DEFAULT_SHEETS_COL_COUNT
): SheetsGrid {
  return Array.from({ length: rowCount }, () => Array.from({ length: colCount }, () => ''))
}

export function normalizeSheetsGrid(
  grid: SheetsGrid | null | undefined,
  rowCount = DEFAULT_SHEETS_ROW_COUNT,
  colCount = DEFAULT_SHEETS_COL_COUNT
): SheetsGrid {
  return Array.from({ length: rowCount }, (_, rowIndex) => {
    const sourceRow = Array.isArray(grid?.[rowIndex]) ? grid?.[rowIndex] : []
    return Array.from({ length: colCount }, (_, colIndex) => String(sourceRow?.[colIndex] ?? ''))
  })
}

export function columnLabel(index: number): string {
  let value = index + 1
  let label = ''
  while (value > 0) {
    const remainder = (value - 1) % 26
    label = String.fromCharCode(65 + remainder) + label
    value = Math.floor((value - 1) / 26)
  }
  return label
}

export function cellLabel(rowIndex: number, colIndex: number): string {
  return `${columnLabel(colIndex)}${rowIndex + 1}`
}

export function countFilledCells(grid: SheetsGrid): number {
  return grid.reduce((total, row) => total + row.filter((cell) => cell.trim().length > 0).length, 0)
}

export function countFormulaCells(grid: SheetsGrid): number {
  return grid.reduce((total, row) => {
    return total + row.filter((value) => isFormulaValue(value)).length
  }, 0)
}

export function applyCellValue(
  grid: SheetsGrid,
  rowIndex: number,
  colIndex: number,
  value: string
): SheetsGrid {
  return grid.map((row, currentRowIndex) => {
    if (currentRowIndex !== rowIndex) {
      return [...row]
    }
    return row.map((cell, currentColIndex) => currentColIndex === colIndex ? value : cell)
  })
}

export function summarizeGridFootprint(rowCount: number, colCount: number): string {
  return `${rowCount} rows × ${colCount} columns`
}

export function sortSheetsWorkbooks<T extends { lastOpenedAt: string | null; updatedAt: string }>(items: T[]): T[] {
  return [...items].sort((left, right) => {
    const leftValue = Date.parse(left.lastOpenedAt || left.updatedAt)
    const rightValue = Date.parse(right.lastOpenedAt || right.updatedAt)
    return rightValue - leftValue
  })
}

export function buildCellEditKey(rowIndex: number, colIndex: number): string {
  return `${rowIndex}:${colIndex}`
}

export function collectCellEdits(editMap: Map<string, SheetCellEditInput>): SheetCellEditInput[] {
  return Array.from(editMap.values()).sort((left, right) => {
    if (left.rowIndex === right.rowIndex) {
      return left.colIndex - right.colIndex
    }
    return left.rowIndex - right.rowIndex
  })
}

export function getCellValue(grid: SheetsGrid | null | undefined, rowIndex: number, colIndex: number): string {
  return String(grid?.[rowIndex]?.[colIndex] ?? '')
}

export function isFormulaValue(value: string | null | undefined): boolean {
  return String(value || '').trim().startsWith('=') && String(value || '').trim().length > 1
}

export function isFormulaError(value: string | null | undefined): boolean {
  return String(value || '').trim().startsWith('#')
}

export function resolveCellPresentation(options: ResolveCellPresentationOptions): SheetsCellPresentation {
  const rawValue = getCellValue(options.rawGrid, options.rowIndex, options.colIndex)
  const savedRawValue = getCellValue(options.savedGrid || options.rawGrid, options.rowIndex, options.colIndex)
  const computedValue = getCellValue(options.computedGrid || options.rawGrid, options.rowIndex, options.colIndex)
  const isFormula = isFormulaValue(rawValue)
  const isDirty = rawValue !== savedRawValue
  const isDirtyFormula = isFormula && isDirty
  const pendingLabel = options.pendingLabel || PENDING_FORMULA_LABEL
  const displayValue = isDirtyFormula ? pendingLabel : isFormula ? computedValue : rawValue

  return {
    rawValue,
    savedRawValue,
    computedValue,
    displayValue,
    isFormula,
    isDirty,
    isDirtyFormula,
    hasError: isFormulaError(computedValue)
  }
}

const DEFAULT_FORMULA_HINT_LABELS: FormulaHintLabels = {
  emptySelection: 'Select a cell to inspect or edit.',
  emptyCell: 'Empty cell. Type text, numbers, or formulas that start with =.',
  dirtyFormula: 'Unsaved formula — save changes to recompute.',
  error: 'Formula returned an explicit evaluator error.',
  formula: 'Computed preview from the saved workbook state.',
  literal: 'Literal cell value.'
}

export function buildFormulaHint(
  presentation: SheetsCellPresentation | null,
  labels: FormulaHintLabels = DEFAULT_FORMULA_HINT_LABELS
): string {
  if (!presentation) {
    return labels.emptySelection
  }
  if (!presentation.rawValue) {
    return labels.emptyCell
  }
  if (presentation.isDirtyFormula) {
    return labels.dirtyFormula
  }
  if (presentation.hasError) {
    return labels.error
  }
  if (presentation.isFormula) {
    return labels.formula
  }
  return labels.literal
}

export function buildSheetsHealthChips(options: SheetsHealthOptions): string[] {
  if (!options.workbook) {
    return [
      `${options.workbookCount} workbooks`,
      'Multi-sheet tabs enabled',
      'CSV / TSV / XLSX import ready',
      'CSV / TSV / JSON export ready'
    ]
  }

  const dirtyLabel = options.dirtyCount > 0 ? `${options.dirtyCount} unsaved edits` : 'All changes saved'
  const errorLabel = options.workbook.computedErrorCount > 0
    ? `${options.workbook.computedErrorCount} formula errors`
    : 'No formula errors'

  return [
    `${options.workbook.sheetCount} sheets`,
    `${options.workbook.formulaCellCount} formulas`,
    errorLabel,
    `${options.workbook.supportedImportFormats.length} import formats`,
    `${options.workbook.supportedExportFormats.length} export formats`,
    dirtyLabel
  ]
}

export function summarizeSupportedFormats(formats: string[]): string {
  return formats.join(' / ')
}

export function formatSheetsTime(value: string | null | undefined): string {
  if (!value) {
    return '—'
  }
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleString()
}

export function formatSheetsFormatLabel(format: string): string {
  return format.toUpperCase()
}

export function resolveImportFormat(fileName: string): SheetsImportFormat | 'UNKNOWN' {
  const extension = fileName.split('.').pop()?.trim().toUpperCase()
  if (extension === 'CSV' || extension === 'TSV' || extension === 'XLSX') {
    return extension
  }
  return 'UNKNOWN'
}

export function buildExportMimeType(format: string): string {
  return format.toUpperCase() === 'JSON' ? 'application/json;charset=utf-8' : 'text/plain;charset=utf-8'
}

export function downloadTextFile(content: string, fileName: string, mimeType: string): void {
  const blob = new Blob([content], { type: mimeType })
  const blobUrl = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = blobUrl
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  URL.revokeObjectURL(blobUrl)
}
