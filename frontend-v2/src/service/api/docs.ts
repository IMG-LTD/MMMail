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

export interface DocsComment {
  id: string
  body: string
  authorEmail: string
  createdAt: string
}

export interface DocsVersion {
  id: string
  version: number
  createdAt: string
  authorEmail: string
}

export interface DocsSharePayload {
  email: string
  permission: DocsPermission
}

export function listDocsNotes(token: string, keyword = '') {
  return httpClient.get<ApiResponse<DocsNoteSummary[]>>('/api/v2/docs', {
    token,
    query: { keyword, limit: 100 }
  })
}

export function readDocsNote(noteId: string, token: string) {
  return httpClient.get<ApiResponse<DocsNoteDetail>>(`/api/v2/docs/${noteId}`, { token })
}

export function updateDocsNote(noteId: string, body: Record<string, unknown>, token: string) {
  return httpClient.patch<ApiResponse<DocsNoteDetail>>(`/api/v2/docs/${noteId}`, { body, token })
}

export function createDocsNote(body: Record<string, unknown>, token: string) {
  return httpClient.post<ApiResponse<DocsNoteDetail>>('/api/v2/docs', { body, token })
}

export function listDocsComments(noteId: string, token: string) {
  return httpClient.get<ApiResponse<DocsComment[]>>(`/api/v2/docs/${noteId}/comments`, { token })
}

export function createDocsComment(noteId: string, body: Record<string, unknown>, token: string) {
  return httpClient.post<ApiResponse<DocsComment>>(`/api/v2/docs/${noteId}/comments`, { body, token })
}

export function listDocsVersions(noteId: string, token: string) {
  return httpClient.get<ApiResponse<DocsVersion[]>>(`/api/v2/docs/${noteId}/versions`, { token })
}

export function shareDocsNote(noteId: string, body: DocsSharePayload, token: string) {
  return httpClient.post<ApiResponse<DocsNoteDetail>>(`/api/v2/docs/${noteId}/share`, { body, token })
}
