import { httpClient } from '@/service/request/http'
import type { ApiResponse } from '@/shared/types/api'

export type LabsModuleMaturity = 'beta' | 'ga' | 'preview'

export interface LabsModule {
  key: string
  label: string
  description: string
  enabled: boolean
  maturity: LabsModuleMaturity
  premium: boolean
  hosted: boolean
  updatedAt: string | null
}

export interface LabsModuleSettings {
  enabled: boolean
  feedbackEnabled: boolean
  rolloutPercent: number
}

function unwrapResponse<T>(response: ApiResponse<T>) {
  return response.data
}

export async function listLabsModules(token: string) {
  const response = await httpClient.get<ApiResponse<LabsModule[]>>('/api/v2/labs/modules', { token })
  return unwrapResponse(response)
}

export async function readLabsModule(moduleKey: string, token: string) {
  const response = await httpClient.get<ApiResponse<LabsModule>>(`/api/v2/labs/modules/${moduleKey}`, { token })
  return unwrapResponse(response)
}

export async function patchLabsModuleSettings(moduleKey: string, body: Partial<LabsModuleSettings>, token: string) {
  const response = await httpClient.patch<ApiResponse<LabsModuleSettings>>(`/api/v2/labs/modules/${moduleKey}/settings`, {
    body,
    token
  })
  return unwrapResponse(response)
}
