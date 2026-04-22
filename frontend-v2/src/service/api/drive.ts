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

export function listDriveItems(token: string, query: Record<string, string | undefined> = {}) {
  return httpClient.get<ApiResponse<DriveItem[]>>('/api/v1/drive/items', {
    token,
    query
  })
}

export function readDriveUsage(token: string) {
  return httpClient.get<ApiResponse<DriveUsage>>('/api/v1/drive/usage', { token })
}

export function listDriveShares(itemId: string, token: string) {
  return httpClient.get<ApiResponse<DriveShareLink[]>>(`/api/v1/drive/items/${itemId}/shares`, { token })
}
