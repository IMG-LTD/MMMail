import type { ApiResponse } from '~/types/api'
import type { CreateMailEasySwitchSessionRequest, MailEasySwitchSession } from '~/types/mail-easy-switch'

export function useMailEasySwitchApi() {
  const { $apiClient } = useNuxtApp()

  async function listSessions(): Promise<MailEasySwitchSession[]> {
    const response = await $apiClient.get<ApiResponse<MailEasySwitchSession[]>>('/api/v1/mail-easy-switch/sessions')
    return response.data.data
  }

  async function createSession(payload: CreateMailEasySwitchSessionRequest): Promise<MailEasySwitchSession> {
    const response = await $apiClient.post<ApiResponse<MailEasySwitchSession>>('/api/v1/mail-easy-switch/sessions', payload)
    return response.data.data
  }

  async function deleteSession(sessionId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/mail-easy-switch/sessions/${sessionId}`)
  }

  return {
    listSessions,
    createSession,
    deleteSession
  }
}
