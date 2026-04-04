import type {
  ApiResponse,
  BatchCreateDriveShareRequest,
  CreateEncryptedPublicShareRequest,
  CreateDriveCollaboratorShareRequest,
  CreateDriveFolderRequest,
  DriveCollaboratorShare,
  DriveCollaboratorSharedItem,
  CreateDriveShareRequest,
  DriveBatchActionResult,
  DriveBatchShareResult,
  DriveFileVersion,
  DriveUploadE2eePayload,
  DriveIncomingCollaboratorShare,
  DriveItem,
  DriveSavedShare,
  DriveShareAccessLog,
  DriveShareLink,
  DriveTrashItem,
  DriveUsage,
  DriveVersionCleanupResult,
  ListDriveItemsParams,
  MoveDriveItemRequest,
  PublicDriveShareMetadata,
  RespondDriveCollaboratorShareRequest,
  UpdateDriveCollaboratorShareRequest,
  UpdateDriveShareRequest
} from '~/types/api'

interface DownloadedFile {
  blob: Blob
  fileName: string
}

interface PreviewedFile extends DownloadedFile {
  mimeType: string
  truncated: boolean
}

const PUBLIC_SHARE_PASSWORD_HEADER = 'X-Drive-Share-Password'

export function useDriveApi() {
  const { $apiClient } = useNuxtApp()

  async function listItems(params: ListDriveItemsParams): Promise<DriveItem[]> {
    const response = await $apiClient.get<ApiResponse<DriveItem[]>>('/api/v1/drive/items', {
      params: {
        parentId: params.parentId ?? undefined,
        keyword: params.keyword || undefined,
        itemType: params.itemType || undefined,
        limit: params.limit || undefined
      }
    })
    return response.data.data
  }

  async function createFolder(payload: CreateDriveFolderRequest): Promise<DriveItem> {
    const response = await $apiClient.post<ApiResponse<DriveItem>>('/api/v1/drive/folders', payload)
    return response.data.data
  }

  async function uploadFile(
    file: File,
    parentId?: string | null,
    e2ee?: DriveUploadE2eePayload,
  ): Promise<DriveItem> {
    const response = await $apiClient.post<ApiResponse<DriveItem>>(
      '/api/v1/drive/files/upload',
      buildFileFormData(file, parentId, e2ee),
    )
    return response.data.data
  }

  async function renameItem(itemId: string, name: string): Promise<DriveItem> {
    const response = await $apiClient.put<ApiResponse<DriveItem>>(`/api/v1/drive/items/${itemId}/rename`, { name })
    return response.data.data
  }

  async function moveItem(itemId: string, payload: MoveDriveItemRequest): Promise<DriveItem> {
    const response = await $apiClient.put<ApiResponse<DriveItem>>(`/api/v1/drive/items/${itemId}/move`, payload)
    return response.data.data
  }

  async function deleteItem(itemId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/drive/items/${itemId}`)
  }

  async function batchDeleteItems(itemIds: string[]): Promise<DriveBatchActionResult> {
    const response = await $apiClient.post<ApiResponse<DriveBatchActionResult>>('/api/v1/drive/items/batch/delete', {
      itemIds
    })
    return response.data.data
  }

  async function batchCreateShares(payload: BatchCreateDriveShareRequest): Promise<DriveBatchShareResult> {
    const response = await $apiClient.post<ApiResponse<DriveBatchShareResult>>('/api/v1/drive/items/batch/shares', payload)
    return response.data.data
  }

  async function listTrashItems(limit = 100): Promise<DriveTrashItem[]> {
    const response = await $apiClient.get<ApiResponse<DriveTrashItem[]>>('/api/v1/drive/trash/items', {
      params: { limit }
    })
    return response.data.data
  }

  async function batchRestoreTrashItems(itemIds: string[]): Promise<DriveBatchActionResult> {
    const response = await $apiClient.post<ApiResponse<DriveBatchActionResult>>('/api/v1/drive/trash/items/batch/restore', {
      itemIds
    })
    return response.data.data
  }

  async function restoreTrashItem(itemId: string): Promise<DriveTrashItem> {
    const response = await $apiClient.post<ApiResponse<DriveTrashItem>>(`/api/v1/drive/trash/items/${itemId}/restore`)
    return response.data.data
  }

  async function batchPurgeTrashItems(itemIds: string[]): Promise<DriveBatchActionResult> {
    const response = await $apiClient.post<ApiResponse<DriveBatchActionResult>>('/api/v1/drive/trash/items/batch/purge', {
      itemIds
    })
    return response.data.data
  }

  async function purgeTrashItem(itemId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/drive/trash/items/${itemId}`)
  }

  async function listShares(itemId: string): Promise<DriveShareLink[]> {
    const response = await $apiClient.get<ApiResponse<DriveShareLink[]>>(`/api/v1/drive/items/${itemId}/shares`)
    return response.data.data
  }

  async function createShare(itemId: string, payload: CreateDriveShareRequest): Promise<DriveShareLink> {
    const response = await $apiClient.post<ApiResponse<DriveShareLink>>(`/api/v1/drive/items/${itemId}/shares`, payload)
    return response.data.data
  }

  async function createEncryptedPublicShare(
    itemId: string,
    payload: CreateEncryptedPublicShareRequest,
  ): Promise<DriveShareLink> {
    const response = await $apiClient.post<ApiResponse<DriveShareLink>>(
      `/api/v1/drive/items/${itemId}/shares/e2ee`,
      buildEncryptedPublicShareFormData(payload),
    )
    return response.data.data
  }

  async function listCollaboratorShares(itemId: string): Promise<DriveCollaboratorShare[]> {
    const response = await $apiClient.get<ApiResponse<DriveCollaboratorShare[]>>(`/api/v1/drive/items/${itemId}/collaborator-shares`)
    return response.data.data
  }

  async function createCollaboratorShare(
    itemId: string,
    payload: CreateDriveCollaboratorShareRequest
  ): Promise<DriveCollaboratorShare> {
    const response = await $apiClient.post<ApiResponse<DriveCollaboratorShare>>(
      `/api/v1/drive/items/${itemId}/collaborator-shares`,
      payload
    )
    return response.data.data
  }

  async function updateCollaboratorShare(
    itemId: string,
    shareId: string,
    payload: UpdateDriveCollaboratorShareRequest
  ): Promise<DriveCollaboratorShare> {
    const response = await $apiClient.put<ApiResponse<DriveCollaboratorShare>>(
      `/api/v1/drive/items/${itemId}/collaborator-shares/${shareId}`,
      payload
    )
    return response.data.data
  }

  async function removeCollaboratorShare(itemId: string, shareId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/drive/items/${itemId}/collaborator-shares/${shareId}`)
  }

  async function updateShare(shareId: string, payload: UpdateDriveShareRequest): Promise<DriveShareLink> {
    const response = await $apiClient.put<ApiResponse<DriveShareLink>>(`/api/v1/drive/shares/${shareId}`, payload)
    return response.data.data
  }

  async function revokeShare(shareId: string): Promise<DriveShareLink> {
    const response = await $apiClient.post<ApiResponse<DriveShareLink>>(`/api/v1/drive/shares/${shareId}/revoke`)
    return response.data.data
  }

  async function getUsage(): Promise<DriveUsage> {
    const response = await $apiClient.get<ApiResponse<DriveUsage>>('/api/v1/drive/usage')
    return response.data.data
  }

  async function downloadFile(itemId: string): Promise<DownloadedFile> {
    return downloadBlob($apiClient, `/api/v1/drive/files/${itemId}/download`, `drive-file-${itemId}`)
  }

  async function previewFile(itemId: string): Promise<PreviewedFile> {
    return previewBlob($apiClient, `/api/v1/drive/files/${itemId}/preview`, `drive-preview-${itemId}`)
  }

  async function listFileVersions(itemId: string, limit = 50): Promise<DriveFileVersion[]> {
    const response = await $apiClient.get<ApiResponse<DriveFileVersion[]>>(`/api/v1/drive/files/${itemId}/versions`, {
      params: { limit }
    })
    return response.data.data
  }

  async function uploadFileVersion(
    itemId: string,
    file: File,
    e2ee?: DriveUploadE2eePayload,
  ): Promise<DriveItem> {
    const formData = buildFileFormData(file, undefined, e2ee)
    const response = await $apiClient.post<ApiResponse<DriveItem>>(`/api/v1/drive/files/${itemId}/versions`, formData)
    return response.data.data
  }

  async function restoreFileVersion(itemId: string, versionId: string): Promise<DriveItem> {
    const response = await $apiClient.post<ApiResponse<DriveItem>>(`/api/v1/drive/files/${itemId}/versions/${versionId}/restore`)
    return response.data.data
  }

  async function cleanupFileVersions(itemId: string): Promise<DriveVersionCleanupResult> {
    const response = await $apiClient.post<ApiResponse<DriveVersionCleanupResult>>(`/api/v1/drive/files/${itemId}/versions/cleanup`)
    return response.data.data
  }

  async function getPublicShareMetadata(token: string): Promise<PublicDriveShareMetadata> {
    const response = await $apiClient.get<ApiResponse<PublicDriveShareMetadata>>(`/api/v1/public/drive/shares/${token}/metadata`)
    return response.data.data
  }

  async function listPublicShareItems(
    token: string,
    parentId?: string | null,
    password?: string
  ): Promise<DriveItem[]> {
    const response = await $apiClient.get<ApiResponse<DriveItem[]>>(`/api/v1/public/drive/shares/${token}/items`, {
      params: {
        parentId: parentId ?? undefined
      },
      headers: buildPublicShareHeaders(password)
    })
    return response.data.data
  }

  async function downloadPublicShareFile(token: string, password?: string): Promise<DownloadedFile> {
    return downloadBlob(
      $apiClient,
      `/api/v1/public/drive/shares/${token}/download`,
      `shared-file-${token.slice(0, 8)}`,
      buildPublicShareHeaders(password)
    )
  }

  async function previewPublicShareFile(token: string, password?: string): Promise<PreviewedFile> {
    return previewBlob(
      $apiClient,
      `/api/v1/public/drive/shares/${token}/preview`,
      `shared-preview-${token.slice(0, 8)}`,
      buildPublicShareHeaders(password)
    )
  }

  async function downloadPublicShareItem(
    token: string,
    itemId: string,
    password?: string
  ): Promise<DownloadedFile> {
    return downloadBlob(
      $apiClient,
      `/api/v1/public/drive/shares/${token}/items/${itemId}/download`,
      `shared-item-${itemId}`,
      buildPublicShareHeaders(password)
    )
  }

  async function previewPublicShareItem(
    token: string,
    itemId: string,
    password?: string
  ): Promise<PreviewedFile> {
    return previewBlob(
      $apiClient,
      `/api/v1/public/drive/shares/${token}/items/${itemId}/preview`,
      `shared-item-preview-${itemId}`,
      buildPublicShareHeaders(password)
    )
  }

  async function uploadPublicShareFile(
    token: string,
    file: File,
    parentId?: string | null,
    password?: string
  ): Promise<DriveItem> {
    const response = await $apiClient.post<ApiResponse<DriveItem>>(
      `/api/v1/public/drive/shares/${token}/files/upload`,
      buildFileFormData(file, parentId),
      {
        headers: buildPublicShareHeaders(password)
      }
    )
    return response.data.data
  }

  async function listShareAccessLogs(params?: {
    action?: string
    accessStatus?: string
    limit?: number
  }): Promise<DriveShareAccessLog[]> {
    const response = await $apiClient.get<ApiResponse<DriveShareAccessLog[]>>('/api/v1/drive/shares/access-logs', {
      params: {
        action: params?.action || undefined,
        accessStatus: params?.accessStatus || undefined,
        limit: params?.limit || undefined
      }
    })
    return response.data.data
  }

  async function listSharedWithMe(): Promise<DriveSavedShare[]> {
    const response = await $apiClient.get<ApiResponse<DriveSavedShare[]>>('/api/v1/drive/shared-with-me')
    return response.data.data
  }

  async function listIncomingCollaboratorShares(): Promise<DriveIncomingCollaboratorShare[]> {
    const response = await $apiClient.get<ApiResponse<DriveIncomingCollaboratorShare[]>>('/api/v1/drive/collaborator-shares/incoming')
    return response.data.data
  }

  async function respondCollaboratorShare(
    shareId: string,
    payload: RespondDriveCollaboratorShareRequest
  ): Promise<DriveIncomingCollaboratorShare> {
    const response = await $apiClient.post<ApiResponse<DriveIncomingCollaboratorShare>>(
      `/api/v1/drive/collaborator-shares/${shareId}/respond`,
      payload
    )
    return response.data.data
  }

  async function listCollaboratorSharedWithMe(): Promise<DriveCollaboratorSharedItem[]> {
    const response = await $apiClient.get<ApiResponse<DriveCollaboratorSharedItem[]>>('/api/v1/drive/collaborator-shares/shared-with-me')
    return response.data.data
  }

  async function listCollaboratorSharedItems(params: {
    shareId: string
    parentId?: string | null
    keyword?: string
    itemType?: string
    limit?: number
  }): Promise<DriveItem[]> {
    const response = await $apiClient.get<ApiResponse<DriveItem[]>>(
      `/api/v1/drive/collaborator-shares/${params.shareId}/items`,
      {
        params: {
          parentId: params.parentId ?? undefined,
          keyword: params.keyword || undefined,
          itemType: params.itemType || undefined,
          limit: params.limit || undefined
        }
      }
    )
    return response.data.data
  }

  async function createCollaboratorFolder(
    shareId: string,
    payload: CreateDriveFolderRequest
  ): Promise<DriveItem> {
    const response = await $apiClient.post<ApiResponse<DriveItem>>(
      `/api/v1/drive/collaborator-shares/${shareId}/folders`,
      payload
    )
    return response.data.data
  }

  async function uploadCollaboratorFile(
    shareId: string,
    file: File,
    parentId?: string | null
  ): Promise<DriveItem> {
    const response = await $apiClient.post<ApiResponse<DriveItem>>(
      `/api/v1/drive/collaborator-shares/${shareId}/files/upload`,
      buildFileFormData(file, parentId)
    )
    return response.data.data
  }

  async function downloadCollaboratorFile(shareId: string, itemId: string): Promise<DownloadedFile> {
    return downloadBlob(
      $apiClient,
      `/api/v1/drive/collaborator-shares/${shareId}/files/${itemId}/download`,
      `collaborator-file-${itemId}`
    )
  }

  async function previewCollaboratorFile(shareId: string, itemId: string): Promise<PreviewedFile> {
    return previewBlob(
      $apiClient,
      `/api/v1/drive/collaborator-shares/${shareId}/files/${itemId}/preview`,
      `collaborator-preview-${itemId}`
    )
  }

  async function saveSharedWithMe(token: string, password?: string): Promise<DriveSavedShare> {
    const response = await $apiClient.post<ApiResponse<DriveSavedShare>>('/api/v1/drive/shared-with-me', {
      token,
      password: password?.trim() || undefined
    })
    return response.data.data
  }

  async function removeSharedWithMe(savedShareId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/drive/shared-with-me/${savedShareId}`)
  }

  return {
    listItems,
    createFolder,
    uploadFile,
    renameItem,
    moveItem,
    deleteItem,
    batchDeleteItems,
    batchCreateShares,
    listTrashItems,
    batchRestoreTrashItems,
    restoreTrashItem,
    batchPurgeTrashItems,
    purgeTrashItem,
    listShares,
    createShare,
    createEncryptedPublicShare,
    listCollaboratorShares,
    createCollaboratorShare,
    updateCollaboratorShare,
    removeCollaboratorShare,
    updateShare,
    revokeShare,
    getUsage,
    downloadFile,
    previewFile,
    listFileVersions,
    uploadFileVersion,
    restoreFileVersion,
    cleanupFileVersions,
    getPublicShareMetadata,
    listPublicShareItems,
    downloadPublicShareFile,
    previewPublicShareFile,
    downloadPublicShareItem,
    previewPublicShareItem,
    uploadPublicShareFile,
    listShareAccessLogs,
    listSharedWithMe,
    listIncomingCollaboratorShares,
    respondCollaboratorShare,
    listCollaboratorSharedWithMe,
    listCollaboratorSharedItems,
    createCollaboratorFolder,
    uploadCollaboratorFile,
    downloadCollaboratorFile,
    previewCollaboratorFile,
    saveSharedWithMe,
    removeSharedWithMe
  }
}

function buildEncryptedPublicShareFormData(payload: CreateEncryptedPublicShareRequest): FormData {
  const formData = new FormData()
  formData.append('permission', payload.permission)
  if (payload.expiresAt) {
    formData.append('expiresAt', payload.expiresAt)
  }
  formData.append('password', payload.password)
  formData.append('file', payload.encryptedFile)
  formData.append('e2eeEnabled', 'true')
  formData.append('e2eeAlgorithm', payload.e2ee.algorithm)
  formData.append('e2eeMode', payload.e2ee.mode)
  formData.append('fileName', payload.e2ee.fileName)
  formData.append('contentType', payload.e2ee.contentType)
  formData.append('fileSize', String(payload.e2ee.fileSize))
  return formData
}

function buildFileFormData(
  file: File,
  parentId?: string | null,
  e2ee?: DriveUploadE2eePayload,
): FormData {
  const formData = new FormData()
  formData.append('file', file)
  if (parentId) {
    formData.append('parentId', parentId)
  }
  if (e2ee?.enabled) {
    formData.append('fileName', e2ee.fileName)
    formData.append('contentType', e2ee.contentType)
    formData.append('fileSize', String(e2ee.fileSize))
    formData.append('e2eeEnabled', 'true')
    formData.append('e2eeAlgorithm', e2ee.algorithm)
    formData.append('e2eeRecipientFingerprintsJson', JSON.stringify(e2ee.recipientFingerprints))
  }
  return formData
}

function buildPublicShareHeaders(password?: string): Record<string, string> | undefined {
  if (!password?.trim()) {
    return undefined
  }
  return {
    [PUBLIC_SHARE_PASSWORD_HEADER]: password.trim()
  }
}

async function downloadBlob(
  apiClient: ReturnType<typeof useNuxtApp>['$apiClient'],
  url: string,
  fallbackFileName: string,
  headers?: Record<string, string>
): Promise<DownloadedFile> {
  const response = await apiClient.get<Blob>(url, {
    responseType: 'blob',
    headers
  })
  const header = String(response.headers['content-disposition'] || '')
  return {
    blob: response.data,
    fileName: extractFileName(header) || fallbackFileName
  }
}

async function previewBlob(
  apiClient: ReturnType<typeof useNuxtApp>['$apiClient'],
  url: string,
  fallbackFileName: string,
  headers?: Record<string, string>
): Promise<PreviewedFile> {
  const response = await apiClient.get<Blob>(url, {
    responseType: 'blob',
    headers
  })
  const disposition = String(response.headers['content-disposition'] || '')
  return {
    blob: response.data,
    fileName: extractFileName(disposition) || fallbackFileName,
    mimeType: String(response.headers['content-type'] || 'application/octet-stream'),
    truncated: String(response.headers['x-preview-truncated'] || '').toLowerCase() === 'true'
  }
}

function extractFileName(contentDisposition: string): string | null {
  if (!contentDisposition) {
    return null
  }
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1]).trim()
  }
  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return plainMatch?.[1]?.trim() || null
}
