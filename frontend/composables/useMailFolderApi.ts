import type { ApiResponse, MailPage } from '~/types/api'
import type { MailFolderNode, MailFolderPayload } from '~/types/mail-folders'

export function useMailFolderApi() {
  const { $apiClient } = useNuxtApp()

  async function listMailFolders(): Promise<MailFolderNode[]> {
    const response = await $apiClient.get<ApiResponse<MailFolderNode[]>>('/api/v1/mail-folders')
    return response.data.data
  }

  async function createMailFolder(payload: MailFolderPayload): Promise<MailFolderNode> {
    const response = await $apiClient.post<ApiResponse<MailFolderNode>>('/api/v1/mail-folders', payload)
    return response.data.data
  }

  async function updateMailFolder(folderId: string, payload: MailFolderPayload): Promise<MailFolderNode> {
    const response = await $apiClient.put<ApiResponse<MailFolderNode>>(`/api/v1/mail-folders/${folderId}`, payload)
    return response.data.data
  }

  async function deleteMailFolder(folderId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/mail-folders/${folderId}`)
  }

  async function fetchMailFolderMessages(folderId: string, page = 1, size = 20, keyword = ''): Promise<MailPage> {
    const response = await $apiClient.get<ApiResponse<MailPage>>(`/api/v1/mail-folders/${folderId}/messages`, {
      params: { page, size, keyword }
    })
    return response.data.data
  }

  return {
    listMailFolders,
    createMailFolder,
    updateMailFolder,
    deleteMailFolder,
    fetchMailFolderMessages
  }
}
