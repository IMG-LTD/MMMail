import type { ApiResponse } from '~/types/api'
import type {
  CreateClientErrorEventRequest,
  SystemHealthOverview,
} from '~/types/system'

export function useSystemApi() {
  const { $apiClient } = useNuxtApp()

  async function fetchSystemHealth(): Promise<SystemHealthOverview> {
    const response = await $apiClient.get<ApiResponse<SystemHealthOverview>>('/api/v1/system/health')
    return response.data.data
  }

  async function reportClientError(payload: CreateClientErrorEventRequest): Promise<void> {
    await $apiClient.post('/api/v1/system/errors/client', payload)
  }

  return {
    fetchSystemHealth,
    reportClientError,
  }
}
