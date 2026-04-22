import type { ApiResponse } from '@/shared/types/api'
import type { SystemHealthOverview } from '@/shared/types/system-health'
import { httpClient } from '@/service/request/http'

function unwrapResponse<T>(response: ApiResponse<T>) {
  return response.data
}

export async function readSystemHealth(token?: string, scopeHeaders?: Record<string, string>) {
  const response = await httpClient.get<ApiResponse<SystemHealthOverview>>('/api/v1/system/health', { token, scopeHeaders })
  return unwrapResponse(response)
}
