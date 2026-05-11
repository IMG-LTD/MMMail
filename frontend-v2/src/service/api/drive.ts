import { httpClient } from '@/service/request/http'
import type { ApiResponse } from '@/shared/types/api'

export interface DriveItem {
  id: string
  parentId: string | null
  itemType: string
  name: string
  mimeType: string | null
  sizeBytes: number
  shareCount: number
  createdAt: string
  updatedAt: string
}

export interface DriveShareLink {
  id: string
  itemId: string
  token: string
  permission: string
  expiresAt: string | null
  status: string
  passwordProtected: boolean
  createdAt: string
  updatedAt: string
}

export interface DriveUsage {
  fileCount: number
  folderCount: number
  storageBytes: number
  storageLimitBytes: number
}

export interface DriveUploadPayload {
  fileName: string
  parentId?: string | null
  sizeBytes: number
}

export interface DriveFileVersion {
  id: string
  fileId: string
  versionNumber: number
  createdAt: string
}

export function listDriveItems(token: string, query: Record<string, string | undefined> = {}) {
  return httpClient.get<ApiResponse<DriveItem[]>>('/api/v2/drive/files', {
    token,
    query
  })
}

export function readDriveUsage(token: string) {
  return httpClient.get<ApiResponse<DriveUsage>>('/api/v2/drive/storage/summary', { token })
}

export function listDriveShares(itemId: string, token: string) {
  const fileId = itemId
  return httpClient.get<ApiResponse<DriveShareLink[]>>(`/api/v2/drive/files/${fileId}/share`, { token })
}

export function listDriveFolders(token: string) {
  return httpClient.get<ApiResponse<DriveItem[]>>('/api/v2/drive/folders', { token })
}

export function createDriveUpload(payload: DriveUploadPayload, token: string) {
  return httpClient.post<ApiResponse<DriveItem>>('/api/v2/drive/uploads', { body: payload, token })
}

export function readDriveFile(fileId: string, token: string) {
  return httpClient.get<ApiResponse<DriveItem>>(`/api/v2/drive/files/${fileId}`, { token })
}

export function listDriveFileVersions(fileId: string, token: string) {
  return httpClient.get<ApiResponse<DriveFileVersion[]>>(`/api/v2/drive/files/${fileId}/versions`, { token })
}
