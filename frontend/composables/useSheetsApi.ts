import type { ApiResponse } from '~/types/api'
import type {
  CreateSheetsWorkbookRequest,
  CreateSheetsWorkbookShareRequest,
  CreateSheetsWorkbookSheetRequest,
  FreezeSheetsWorkbookSheetRequest,
  ImportSheetsWorkbookRequest,
  RenameSheetsWorkbookRequest,
  RenameSheetsWorkbookSheetRequest,
  RespondSheetsWorkbookShareRequest,
  SetActiveSheetsWorkbookSheetRequest,
  SortSheetsWorkbookSheetRequest,
  SheetsIncomingShare,
  SheetsWorkbookDetail,
  SheetsWorkbookExport,
  SheetsWorkbookShare,
  SheetsWorkbookSummary,
  SheetsWorkbookVersion,
  UpdateSheetsWorkbookShareRequest,
  UpdateSheetsWorkbookCellsRequest
} from '~/types/sheets'

export function useSheetsApi() {
  const { $apiClient } = useNuxtApp()

  async function listWorkbooks(limit = 100): Promise<SheetsWorkbookSummary[]> {
    const response = await $apiClient.get<ApiResponse<SheetsWorkbookSummary[]>>('/api/v1/sheets/workbooks', {
      params: { limit }
    })
    return response.data.data
  }

  async function createWorkbook(payload: CreateSheetsWorkbookRequest): Promise<SheetsWorkbookDetail> {
    const response = await $apiClient.post<ApiResponse<SheetsWorkbookDetail>>('/api/v1/sheets/workbooks', payload)
    return response.data.data
  }

  async function importWorkbook(payload: ImportSheetsWorkbookRequest): Promise<SheetsWorkbookDetail> {
    const formData = new FormData()
    formData.append('file', payload.file)
    if (payload.title?.trim()) {
      formData.append('title', payload.title.trim())
    }
    const response = await $apiClient.post<ApiResponse<SheetsWorkbookDetail>>('/api/v1/sheets/workbooks/import', formData)
    return response.data.data
  }

  async function getWorkbook(workbookId: string): Promise<SheetsWorkbookDetail> {
    const response = await $apiClient.get<ApiResponse<SheetsWorkbookDetail>>(`/api/v1/sheets/workbooks/${workbookId}`)
    return response.data.data
  }

  async function exportWorkbook(workbookId: string, format: string): Promise<SheetsWorkbookExport> {
    const response = await $apiClient.get<ApiResponse<SheetsWorkbookExport>>(`/api/v1/sheets/workbooks/${workbookId}/export`, {
      params: { format }
    })
    return response.data.data
  }

  async function renameWorkbook(workbookId: string, payload: RenameSheetsWorkbookRequest): Promise<SheetsWorkbookDetail> {
    const response = await $apiClient.put<ApiResponse<SheetsWorkbookDetail>>(
      `/api/v1/sheets/workbooks/${workbookId}/rename`,
      payload
    )
    return response.data.data
  }

  async function createSheet(
    workbookId: string,
    payload: CreateSheetsWorkbookSheetRequest = {}
  ): Promise<SheetsWorkbookDetail> {
    const response = await $apiClient.post<ApiResponse<SheetsWorkbookDetail>>(
      `/api/v1/sheets/workbooks/${workbookId}/sheets`,
      payload
    )
    return response.data.data
  }

  async function renameSheet(
    workbookId: string,
    sheetId: string,
    payload: RenameSheetsWorkbookSheetRequest
  ): Promise<SheetsWorkbookDetail> {
    const response = await $apiClient.put<ApiResponse<SheetsWorkbookDetail>>(
      `/api/v1/sheets/workbooks/${workbookId}/sheets/${sheetId}/rename`,
      payload
    )
    return response.data.data
  }

  async function deleteSheet(workbookId: string, sheetId: string): Promise<SheetsWorkbookDetail> {
    const response = await $apiClient.delete<ApiResponse<SheetsWorkbookDetail>>(
      `/api/v1/sheets/workbooks/${workbookId}/sheets/${sheetId}`
    )
    return response.data.data
  }

  async function setActiveSheet(
    workbookId: string,
    payload: SetActiveSheetsWorkbookSheetRequest
  ): Promise<SheetsWorkbookDetail> {
    const response = await $apiClient.put<ApiResponse<SheetsWorkbookDetail>>(
      `/api/v1/sheets/workbooks/${workbookId}/active-sheet`,
      payload
    )
    return response.data.data
  }

  async function updateWorkbookCells(
    workbookId: string,
    payload: UpdateSheetsWorkbookCellsRequest
  ): Promise<SheetsWorkbookDetail> {
    const response = await $apiClient.put<ApiResponse<SheetsWorkbookDetail>>(
      `/api/v1/sheets/workbooks/${workbookId}/cells`,
      payload
    )
    return response.data.data
  }

  async function sortSheet(
    workbookId: string,
    sheetId: string,
    payload: SortSheetsWorkbookSheetRequest
  ): Promise<SheetsWorkbookDetail> {
    const response = await $apiClient.put<ApiResponse<SheetsWorkbookDetail>>(
      `/api/v1/sheets/workbooks/${workbookId}/sheets/${sheetId}/sort`,
      payload
    )
    return response.data.data
  }

  async function freezeSheet(
    workbookId: string,
    sheetId: string,
    payload: FreezeSheetsWorkbookSheetRequest
  ): Promise<SheetsWorkbookDetail> {
    const response = await $apiClient.put<ApiResponse<SheetsWorkbookDetail>>(
      `/api/v1/sheets/workbooks/${workbookId}/sheets/${sheetId}/freeze`,
      payload
    )
    return response.data.data
  }

  async function deleteWorkbook(workbookId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/sheets/workbooks/${workbookId}`)
  }

  async function listShares(workbookId: string): Promise<SheetsWorkbookShare[]> {
    const response = await $apiClient.get<ApiResponse<SheetsWorkbookShare[]>>(`/api/v1/sheets/workbooks/${workbookId}/shares`)
    return response.data.data
  }

  async function createShare(workbookId: string, payload: CreateSheetsWorkbookShareRequest): Promise<SheetsWorkbookShare> {
    const response = await $apiClient.post<ApiResponse<SheetsWorkbookShare>>(
      `/api/v1/sheets/workbooks/${workbookId}/shares`,
      payload
    )
    return response.data.data
  }

  async function updateShare(
    workbookId: string,
    shareId: string,
    payload: UpdateSheetsWorkbookShareRequest
  ): Promise<SheetsWorkbookShare> {
    const response = await $apiClient.put<ApiResponse<SheetsWorkbookShare>>(
      `/api/v1/sheets/workbooks/${workbookId}/shares/${shareId}`,
      payload
    )
    return response.data.data
  }

  async function removeShare(workbookId: string, shareId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/sheets/workbooks/${workbookId}/shares/${shareId}`)
  }

  async function listIncomingShares(): Promise<SheetsIncomingShare[]> {
    const response = await $apiClient.get<ApiResponse<SheetsIncomingShare[]>>('/api/v1/sheets/workbooks/incoming-shares')
    return response.data.data
  }

  async function respondIncomingShare(
    shareId: string,
    payload: RespondSheetsWorkbookShareRequest
  ): Promise<SheetsIncomingShare> {
    const response = await $apiClient.post<ApiResponse<SheetsIncomingShare>>(
      `/api/v1/sheets/workbooks/incoming-shares/${shareId}/respond`,
      payload
    )
    return response.data.data
  }

  async function listVersions(workbookId: string): Promise<SheetsWorkbookVersion[]> {
    const response = await $apiClient.get<ApiResponse<SheetsWorkbookVersion[]>>(`/api/v1/sheets/workbooks/${workbookId}/versions`)
    return response.data.data
  }

  async function restoreVersion(workbookId: string, versionId: string): Promise<SheetsWorkbookDetail> {
    const response = await $apiClient.post<ApiResponse<SheetsWorkbookDetail>>(
      `/api/v1/sheets/workbooks/${workbookId}/versions/${versionId}/restore`
    )
    return response.data.data
  }

  return {
    listWorkbooks,
    createWorkbook,
    importWorkbook,
    getWorkbook,
    exportWorkbook,
    renameWorkbook,
    createSheet,
    renameSheet,
    deleteSheet,
    setActiveSheet,
    updateWorkbookCells,
    sortSheet,
    freezeSheet,
    deleteWorkbook,
    listShares,
    createShare,
    updateShare,
    removeShare,
    listIncomingShares,
    respondIncomingShare,
    listVersions,
    restoreVersion
  }
}
