import { httpClient } from '@/service/request/http'
import type { ApiResponse } from '@/shared/types/api'

export type DocsPermission = 'OWNER' | 'VIEW' | 'EDIT'
export type DocsScope = 'OWNED' | 'SHARED'

export interface DocsNoteSummary {
  id: string
  title: string
  updatedAt: string
  permission: DocsPermission
  scope: DocsScope
  currentVersion: number
  ownerEmail: string
  ownerDisplayName: string
  collaboratorCount: number
}

export interface DocsNoteDetail {
  id: string
  title: string
  content: string
  createdAt: string
  updatedAt: string
  currentVersion: number
  permission: DocsPermission
  shared: boolean
  ownerEmail: string
  ownerDisplayName: string
  collaboratorCount: number
  syncCursor: number
  syncVersion: string
}

export function listDocsNotes(token: string, keyword = '') {
  return httpClient.get<ApiResponse<DocsNoteSummary[]>>('/api/v1/docs/notes', {
    token,
    query: { keyword, limit: 100 }
  })
}

export function readDocsNote(noteId: string, token: string) {
  return httpClient.get<ApiResponse<DocsNoteDetail>>(`/api/v1/docs/notes/${noteId}`, { token })
}

export function updateDocsNote(noteId: string, body: Record<string, unknown>, token: string) {
  return httpClient.put<ApiResponse<DocsNoteDetail>>(`/api/v1/docs/notes/${noteId}`, { body, token })
}
