import type { ApiResponse, AuditEvent } from '~/types/api'

export function useAuditApi() {
  const { $apiClient } = useNuxtApp()

  async function fetchEvents(): Promise<AuditEvent[]> {
    const response = await $apiClient.get<ApiResponse<AuditEvent[]>>('/api/v1/audit/events')
    return response.data.data
  }

  return {
    fetchEvents
  }
}
