import { httpClient } from '@/service/request/http'
import type { ApiResponse } from '@/shared/types/api'

export type WorkspaceProductState = 'community' | 'hosted' | 'premium'

export interface WorkspaceSummaryProduct {
  key: string
  label: string
  value: string
  state: WorkspaceProductState
  updatedAt: string | null
}

export interface WorkspaceSummary {
  productCards: WorkspaceSummaryProduct[]
  recommendationCount: number
  systemStatus: string
}

export interface WorkspaceActivityItem {
  id: string
  product: string
  title: string
  occurredAt: string
  actor: string | null
}

export interface WorkspaceTask {
  id: string
  title: string
  completed: boolean
  dueAt: string | null
  product: string
}

export interface PatchWorkspaceTaskPayload {
  completed?: boolean
  title?: string
}

function unwrapResponse<T>(response: ApiResponse<T>) {
  return response.data
}

export async function readWorkspaceSummary(token?: string) {
  const response = await httpClient.get<ApiResponse<WorkspaceSummary>>('/api/v2/workspace/summary', { token })
  return unwrapResponse(response)
}

export async function listWorkspaceActivity(token?: string) {
  const response = await httpClient.get<ApiResponse<WorkspaceActivityItem[]>>('/api/v2/workspace/activity', { token })
  return unwrapResponse(response)
}

export async function listWorkspaceTasks(token?: string) {
  const response = await httpClient.get<ApiResponse<WorkspaceTask[]>>('/api/v2/workspace/tasks', { token })
  return unwrapResponse(response)
}

export async function patchWorkspaceTask(taskId: string, payload: PatchWorkspaceTaskPayload, token?: string) {
  const response = await httpClient.patch<ApiResponse<WorkspaceTask>>(`/api/v2/workspace/tasks/${taskId}`, {
    body: payload,
    token
  })
  return unwrapResponse(response)
}
