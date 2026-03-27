import type { ApiResponse } from '~/types/api'
import type {
  MailFilter,
  MailFilterPayload,
  MailFilterPreview,
  MailFilterPreviewRequest
} from '~/types/mail-filters'

export function useMailFilterApi() {
  const { $apiClient } = useNuxtApp()

  async function listMailFilters(): Promise<MailFilter[]> {
    const response = await $apiClient.get<ApiResponse<MailFilter[]>>('/api/v1/mail-filters')
    return response.data.data
  }

  async function createMailFilter(payload: MailFilterPayload): Promise<MailFilter> {
    const response = await $apiClient.post<ApiResponse<MailFilter>>('/api/v1/mail-filters', payload)
    return response.data.data
  }

  async function updateMailFilter(filterId: string, payload: MailFilterPayload): Promise<MailFilter> {
    const response = await $apiClient.put<ApiResponse<MailFilter>>(`/api/v1/mail-filters/${filterId}`, payload)
    return response.data.data
  }

  async function deleteMailFilter(filterId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/mail-filters/${filterId}`)
  }

  async function previewMailFilter(payload: MailFilterPreviewRequest): Promise<MailFilterPreview> {
    const response = await $apiClient.post<ApiResponse<MailFilterPreview>>('/api/v1/mail-filters/preview', payload)
    return response.data.data
  }

  return {
    listMailFilters,
    createMailFilter,
    updateMailFilter,
    deleteMailFilter,
    previewMailFilter
  }
}
