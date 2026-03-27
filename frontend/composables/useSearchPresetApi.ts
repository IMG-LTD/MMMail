import type { ApiResponse, CreateSearchPresetRequest, SearchPreset, UpdateSearchPresetRequest } from '~/types/api'

export function useSearchPresetApi() {
  const { $apiClient } = useNuxtApp()

  async function listSearchPresets(): Promise<SearchPreset[]> {
    const response = await $apiClient.get<ApiResponse<SearchPreset[]>>('/api/v1/search-presets')
    return response.data.data
  }

  async function createSearchPreset(payload: CreateSearchPresetRequest): Promise<SearchPreset> {
    const response = await $apiClient.post<ApiResponse<SearchPreset>>('/api/v1/search-presets', payload)
    return response.data.data
  }

  async function useSearchPreset(presetId: string): Promise<SearchPreset> {
    const response = await $apiClient.post<ApiResponse<SearchPreset>>(`/api/v1/search-presets/${presetId}/use`)
    return response.data.data
  }

  async function updateSearchPreset(presetId: string, payload: UpdateSearchPresetRequest): Promise<SearchPreset> {
    const response = await $apiClient.put<ApiResponse<SearchPreset>>(`/api/v1/search-presets/${presetId}`, payload)
    return response.data.data
  }

  async function pinSearchPreset(presetId: string): Promise<SearchPreset> {
    const response = await $apiClient.post<ApiResponse<SearchPreset>>(`/api/v1/search-presets/${presetId}/pin`)
    return response.data.data
  }

  async function unpinSearchPreset(presetId: string): Promise<SearchPreset> {
    const response = await $apiClient.post<ApiResponse<SearchPreset>>(`/api/v1/search-presets/${presetId}/unpin`)
    return response.data.data
  }

  async function deleteSearchPreset(presetId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/search-presets/${presetId}`)
  }

  return {
    listSearchPresets,
    createSearchPreset,
    useSearchPreset,
    updateSearchPreset,
    pinSearchPreset,
    unpinSearchPreset,
    deleteSearchPreset
  }
}
