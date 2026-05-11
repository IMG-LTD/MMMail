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

export interface SheetsImportPayload {
  format: SheetsImportFormat
  content: string
}

export interface SheetsCleaningRulePayload {
  column: string
  operation: string
}

export interface SheetsInsight {
  id: string
  title: string
  summary: string
  severity: 'info' | 'warning' | 'critical'
}

export function listSheetsWorkbooks(token: string) {
  return httpClient.get<ApiResponse<SheetsWorkbookSummary[]>>('/api/v2/sheets', {
    token,
    query: { limit: 100 }
  })
}

export function readSheetsWorkbook(workbookId: string, token: string) {
  return httpClient.get<ApiResponse<SheetsWorkbookDetail>>(`/api/v2/sheets/${workbookId}`, { token })
}

export function updateSheetsWorkbookCells(workbookId: string, body: Record<string, unknown>, token: string) {
  return httpClient.patch<ApiResponse<SheetsWorkbookDetail>>(`/api/v2/sheets/${workbookId}`, { body, token })
}

export function createSheetsWorkbook(body: Record<string, unknown>, token: string) {
  return httpClient.post<ApiResponse<SheetsWorkbookDetail>>('/api/v2/sheets', { body, token })
}

export function importSheetsWorkbook(workbookId: string, body: SheetsImportPayload, token: string) {
  return httpClient.post<ApiResponse<SheetsWorkbookDetail>>(`/api/v2/sheets/${workbookId}/imports`, { body, token })
}

export function createSheetsCleaningRule(workbookId: string, body: SheetsCleaningRulePayload, token: string) {
  return httpClient.post<ApiResponse<SheetsWorkbookDetail>>(`/api/v2/sheets/${workbookId}/cleaning-rules`, { body, token })
}

export function listSheetsInsights(workbookId: string, token: string) {
  return httpClient.get<ApiResponse<SheetsInsight[]>>(`/api/v2/sheets/${workbookId}/insights`, { token })
}
