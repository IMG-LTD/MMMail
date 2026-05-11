import { httpClient } from '@/service/request/http'
import type { ApiResponse } from '@/shared/types/api'

export interface EntitlementRequestOptions {
  scopeHeaders?: Record<string, string>
  token: string
}

export interface EntitlementState {
  key: string
  label: string
  state: 'available' | 'locked' | 'enabled'
  requiredPlan: string | null
}

export interface EntitlementMatrix {
  community: string[]
  premium: string[]
  hosted: string[]
}

function unwrapResponse<T>(response: ApiResponse<T>) {
  return response.data
}

export async function listEntitlements(options: EntitlementRequestOptions) {
  const response = await httpClient.get<ApiResponse<EntitlementState[]>>('/api/v2/entitlements', options)
  return unwrapResponse(response)
}

export async function readEntitlementMatrix(options: EntitlementRequestOptions) {
  const response = await httpClient.get<ApiResponse<EntitlementMatrix>>('/api/v2/entitlements/matrix', options)
  return unwrapResponse(response)
}
