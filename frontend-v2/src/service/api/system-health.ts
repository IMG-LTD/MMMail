import type { ApiResponse } from '@/shared/types/api'
import type { SystemHealthOverview } from '@/shared/types/system-health'
import { httpClient } from '@/service/request/http'

export interface PublicSystemStatus {
  incidentMessage: string | null
  maintenanceWindow: string | null
  status: 'operational' | 'degraded' | 'maintenance' | 'offline'
  updatedAt: string
}

function unwrapResponse<T>(response: ApiResponse<T>) {
  return response.data
}

export async function readSystemHealth(token?: string, scopeHeaders?: Record<string, string>) {
  const response = await httpClient.get<ApiResponse<SystemHealthOverview>>('/api/v1/system/health', { token, scopeHeaders })
  return unwrapResponse(response)
}

export async function readPublicSystemStatus() {
  const response = await httpClient.get<ApiResponse<PublicSystemStatus>>('/api/v2/system/status')
  return unwrapResponse(response)
}
