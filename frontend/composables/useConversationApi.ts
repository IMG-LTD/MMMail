import type { ApiResponse, ConversationDetail, ConversationPage, SystemMailFolder } from '~/types/api'

export function useConversationApi() {
  const { $apiClient } = useNuxtApp()

  async function fetchConversations(
    page = 1,
    size = 20,
    keyword = '',
    folder?: SystemMailFolder | ''
  ): Promise<ConversationPage> {
    const response = await $apiClient.get<ApiResponse<ConversationPage>>('/api/v1/mails/conversations', {
      params: {
        page,
        size,
        keyword,
        folder: folder || undefined
      }
    })
    return response.data.data
  }

  async function fetchConversationDetail(conversationId: string): Promise<ConversationDetail> {
    const response = await $apiClient.get<ApiResponse<ConversationDetail>>(`/api/v1/mails/conversations/${conversationId}`)
    return response.data.data
  }

  return {
    fetchConversations,
    fetchConversationDetail
  }
}
