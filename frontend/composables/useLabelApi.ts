import type { ApiResponse, LabelItem } from '~/types/api'

export function useLabelApi() {
  const { $apiClient } = useNuxtApp()

  async function listLabels(): Promise<LabelItem[]> {
    const response = await $apiClient.get<ApiResponse<LabelItem[]>>('/api/v1/labels')
    return response.data.data
  }

  async function createLabel(name: string, color: string): Promise<void> {
    await $apiClient.post('/api/v1/labels', { name, color })
  }

  async function deleteLabel(id: number): Promise<void> {
    await $apiClient.delete(`/api/v1/labels/${id}`)
  }

  return {
    listLabels,
    createLabel,
    deleteLabel
  }
}
