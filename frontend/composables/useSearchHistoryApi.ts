import type { ApiResponse, SearchHistoryItem } from '~/types/api'

export function useSearchHistoryApi() {
  const { $apiClient } = useNuxtApp()

  async function listSearchHistory(): Promise<SearchHistoryItem[]> {
    const response = await $apiClient.get<ApiResponse<SearchHistoryItem[]>>('/api/v1/search-history')
    return response.data.data
  }

  async function deleteSearchHistoryItem(historyId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/search-history/${historyId}`)
  }

  async function clearSearchHistory(): Promise<void> {
    await $apiClient.delete('/api/v1/search-history')
  }

  return {
    listSearchHistory,
    deleteSearchHistoryItem,
    clearSearchHistory
  }
}
