export type SheetsGrid = string[][]
export type SheetsImportFormat = 'CSV' | 'TSV' | 'XLSX'
export type SheetsExportFormat = 'CSV' | 'TSV' | 'JSON'
export type SheetsSortDirection = 'ASC' | 'DESC'
export type SheetsPermission = 'OWNER' | 'VIEW' | 'EDIT'
export type SheetsScope = 'OWNED' | 'SHARED'
export type SheetsShareResponseStatus = 'NEEDS_ACTION' | 'ACCEPTED' | 'DECLINED'
export type SheetsScopeFilter = 'ALL' | 'OWNED' | 'SHARED'

export interface SheetsWorkbookSheet {
  id: string
  name: string
  rowCount: number
  colCount: number
  frozenRowCount: number
  frozenColCount: number
  filledCellCount: number
  formulaCellCount: number
  computedErrorCount: number
  grid: SheetsGrid
  computedGrid: SheetsGrid
}

export interface SheetsWorkbookSummary {
  id: string
  title: string
  rowCount: number
  colCount: number
  filledCellCount: number
  formulaCellCount: number
  computedErrorCount: number
  currentVersion: number
  sheetCount: number
  activeSheetId: string
  updatedAt: string
  lastOpenedAt: string | null
  permission: SheetsPermission
  scope: SheetsScope
  ownerEmail: string
  ownerDisplayName: string
  collaboratorCount: number
  canEdit: boolean
}

export interface SheetsWorkbookDetail extends SheetsWorkbookSummary {
  sheets: SheetsWorkbookSheet[]
  grid: SheetsGrid
  computedGrid: SheetsGrid
  supportedImportFormats: SheetsImportFormat[]
  supportedExportFormats: SheetsExportFormat[]
  createdAt: string
  canManageShares: boolean
  canRestoreVersions: boolean
}

export interface SheetsWorkbookShare {
  shareId: string
  collaboratorUserId: string
  collaboratorEmail: string
  collaboratorDisplayName: string
  permission: Extract<SheetsPermission, 'VIEW' | 'EDIT'>
  responseStatus: SheetsShareResponseStatus
  createdAt: string
  updatedAt: string
}

export interface SheetsIncomingShare {
  shareId: string
  workbookId: string
  workbookTitle: string
  ownerEmail: string
  ownerDisplayName: string
  permission: Extract<SheetsPermission, 'VIEW' | 'EDIT'>
  responseStatus: SheetsShareResponseStatus
  updatedAt: string
}

export interface SheetsWorkbookVersion {
  versionId: string
  versionNo: number
  title: string
  rowCount: number
  colCount: number
  createdByUserId: string
  createdByEmail: string
  createdByDisplayName: string
  sourceEvent: string
  createdAt: string
}

export interface CreateSheetsWorkbookRequest {
  title: string
  rowCount?: number
  colCount?: number
}

export interface RenameSheetsWorkbookRequest {
  title: string
}

export interface CreateSheetsWorkbookShareRequest {
  targetEmail: string
  permission: Extract<SheetsPermission, 'VIEW' | 'EDIT'>
}

export interface UpdateSheetsWorkbookShareRequest {
  permission: Extract<SheetsPermission, 'VIEW' | 'EDIT'>
}

export interface RespondSheetsWorkbookShareRequest {
  response: 'ACCEPT' | 'DECLINE'
}

export interface CreateSheetsWorkbookSheetRequest {
  name?: string
  rowCount?: number
  colCount?: number
}

export interface RenameSheetsWorkbookSheetRequest {
  name: string
}

export interface SetActiveSheetsWorkbookSheetRequest {
  sheetId: string
}

export interface SheetCellEditInput {
  rowIndex: number
  colIndex: number
  value: string
}

export interface UpdateSheetsWorkbookCellsRequest {
  currentVersion: number
  sheetId?: string
  edits: SheetCellEditInput[]
}

export interface SortSheetsWorkbookSheetRequest {
  currentVersion: number
  columnIndex: number
  direction: SheetsSortDirection
  includeHeader: boolean
}

export interface FreezeSheetsWorkbookSheetRequest {
  currentVersion: number
  frozenRowCount: number
  frozenColCount: number
}

export interface ImportSheetsWorkbookRequest {
  file: File
  title?: string
}

export interface SheetsWorkbookExport {
  fileName: string
  format: SheetsExportFormat
  content: string
  formulaCellCount: number
  computedErrorCount: number
  exportedAt: string
}

export interface ActiveSheetsCell {
  rowIndex: number
  colIndex: number
}

export interface SheetsImportSummary {
  title: string
  rowCount: number
  colCount: number
  formulaCellCount: number
  sourceFormat: SheetsImportFormat | 'UNKNOWN'
  importedAt: string
}
