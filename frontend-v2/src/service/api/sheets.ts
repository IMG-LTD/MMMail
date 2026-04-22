import { httpClient } from '@/service/request/http'
import type { ApiResponse } from '@/shared/types/api'

export type SheetsGrid = string[][]
export type SheetsImportFormat = 'CSV' | 'TSV' | 'XLSX'
export type SheetsExportFormat = 'CSV' | 'TSV' | 'JSON'
export type SheetsPermission = 'OWNER' | 'VIEW' | 'EDIT'
export type SheetsScope = 'OWNED' | 'SHARED'

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

export function listSheetsWorkbooks(token: string) {
  return httpClient.get<ApiResponse<SheetsWorkbookSummary[]>>('/api/v1/sheets/workbooks', {
    token,
    query: { limit: 100 }
  })
}

export function readSheetsWorkbook(workbookId: string, token: string) {
  return httpClient.get<ApiResponse<SheetsWorkbookDetail>>(`/api/v1/sheets/workbooks/${workbookId}`, { token })
}

export function updateSheetsWorkbookCells(workbookId: string, body: Record<string, unknown>, token: string) {
  return httpClient.put<ApiResponse<SheetsWorkbookDetail>>(`/api/v1/sheets/workbooks/${workbookId}/cells`, { body, token })
}
