import type {
  ApiResponse,
  ConversationAction,
  DraftRequest,
  MailAttachment,
  MailActionResult,
  MailId,
  MailPage,
  MailDetail,
  MailSenderIdentity,
  MailboxStats,
  SearchMailParams,
  SystemMailFolder,
  SendMailRequest
} from '~/types/api'

const FOLDER_PATH_MAP: Record<Lowercase<SystemMailFolder>, string> = {
  inbox: 'inbox',
  sent: 'sent',
  drafts: 'drafts',
  outbox: 'outbox',
  archive: 'archive',
  spam: 'spam',
  trash: 'trash',
  scheduled: 'scheduled',
  snoozed: 'snoozed'
}

export function useMailApi() {
  const { $apiClient } = useNuxtApp()

  async function fetchFolder(
    folder: Lowercase<SystemMailFolder>,
    page = 1,
    size = 20,
    keyword = ''
  ): Promise<MailPage> {
    const response = await $apiClient.get<ApiResponse<MailPage>>(`/api/v1/mails/${FOLDER_PATH_MAP[folder]}`, {
      params: { page, size, keyword }
    })
    return response.data.data
  }

  async function fetchSearch(params: SearchMailParams): Promise<MailPage> {
    const response = await $apiClient.get<ApiResponse<MailPage>>('/api/v1/mails/search', { params })
    return response.data.data
  }

  async function fetchUnread(page = 1, size = 20, keyword = ''): Promise<MailPage> {
    const response = await $apiClient.get<ApiResponse<MailPage>>('/api/v1/mails/unread', {
      params: { page, size, keyword }
    })
    return response.data.data
  }

  async function fetchStats(): Promise<MailboxStats> {
    const response = await $apiClient.get<ApiResponse<MailboxStats>>('/api/v1/mails/stats')
    return response.data.data
  }

  async function fetchMailDetail(mailId: MailId): Promise<MailDetail> {
    const response = await $apiClient.get<ApiResponse<MailDetail>>(`/api/v1/mails/${mailId}`)
    return response.data.data
  }

  async function listSenderIdentities(): Promise<MailSenderIdentity[]> {
    const response = await $apiClient.get<ApiResponse<MailSenderIdentity[]>>('/api/v1/mails/identities')
    return response.data.data
  }

  async function sendMail(payload: SendMailRequest): Promise<void> {
    await $apiClient.post('/api/v1/mails/send', payload)
  }

  async function undoSend(mailId: MailId): Promise<void> {
    await $apiClient.post(`/api/v1/mails/${mailId}/undo-send`)
  }

  async function saveDraft(payload: DraftRequest): Promise<MailId> {
    const response = await $apiClient.post<ApiResponse<{ draftId: MailId }>>('/api/v1/mails/drafts', payload)
    return response.data.data.draftId
  }

  async function uploadDraftAttachment(draftId: MailId, file: File): Promise<MailAttachment> {
    const formData = new FormData()
    formData.append('file', file)
    const response = await $apiClient.post<ApiResponse<{ draftId: MailId, attachment: MailAttachment }>>(
      `/api/v1/mails/drafts/${draftId}/attachments`,
      formData
    )
    return response.data.data.attachment
  }

  async function deleteDraftAttachment(draftId: MailId, attachmentId: MailId): Promise<void> {
    await $apiClient.delete(`/api/v1/mails/drafts/${draftId}/attachments/${attachmentId}`)
  }

  async function downloadMailAttachment(mailId: MailId, attachmentId: MailId): Promise<{ blob: Blob, fileName: string }> {
    const response = await $apiClient.get<Blob>(`/api/v1/mails/${mailId}/attachments/${attachmentId}/download`, {
      responseType: 'blob'
    })
    return {
      blob: response.data,
      fileName: extractFileName(String(response.headers['content-disposition'] || '')) || `mail-attachment-${attachmentId}`
    }
  }

  async function applyAction(mailId: MailId, action: string): Promise<MailActionResult> {
    const response = await $apiClient.post<ApiResponse<MailActionResult>>(`/api/v1/mails/${mailId}/actions`, { action })
    return response.data.data
  }

  async function applyBatchAction(mailIds: MailId[], action: string): Promise<MailActionResult> {
    const response = await $apiClient.post<ApiResponse<MailActionResult>>('/api/v1/mails/actions/batch', {
      mailIds,
      action
    })
    return response.data.data
  }

  async function applyConversationAction(conversationId: string, action: ConversationAction): Promise<MailActionResult> {
    const response = await $apiClient.post<ApiResponse<MailActionResult>>(
      `/api/v1/mails/conversations/${conversationId}/actions`,
      { action }
    )
    return response.data.data
  }

  async function updateLabels(mailId: MailId, labels: string[]): Promise<void> {
    await $apiClient.put(`/api/v1/mails/${mailId}/labels`, { labels })
  }

  async function restoreAllTrash(): Promise<MailActionResult> {
    const response = await $apiClient.post<ApiResponse<MailActionResult>>('/api/v1/mails/trash/restore-all')
    return response.data.data
  }

  async function emptyTrash(): Promise<MailActionResult> {
    const response = await $apiClient.post<ApiResponse<MailActionResult>>('/api/v1/mails/trash/empty')
    return response.data.data
  }

  async function restoreAllSpam(): Promise<MailActionResult> {
    const response = await $apiClient.post<ApiResponse<MailActionResult>>('/api/v1/mails/spam/restore-all')
    return response.data.data
  }

  async function emptySpam(): Promise<MailActionResult> {
    const response = await $apiClient.post<ApiResponse<MailActionResult>>('/api/v1/mails/spam/empty')
    return response.data.data
  }

  async function snoozeUntil(mailId: MailId, untilAt: string): Promise<MailActionResult> {
    const response = await $apiClient.post<ApiResponse<MailActionResult>>(`/api/v1/mails/${mailId}/snooze`, { untilAt })
    return response.data.data
  }

  async function fetchStarred(page = 1, size = 20, keyword = ''): Promise<MailPage> {
    const response = await $apiClient.get<ApiResponse<MailPage>>('/api/v1/mails/starred', {
      params: { page, size, keyword }
    })
    return response.data.data
  }

  return {
    fetchFolder,
    fetchSearch,
    fetchUnread,
    fetchStats,
    fetchMailDetail,
    listSenderIdentities,
    sendMail,
    undoSend,
    saveDraft,
    uploadDraftAttachment,
    deleteDraftAttachment,
    downloadMailAttachment,
    applyAction,
    applyBatchAction,
    applyConversationAction,
    updateLabels,
    fetchStarred,
    restoreAllTrash,
    emptyTrash,
    restoreAllSpam,
    emptySpam,
    snoozeUntil
  }
}

function extractFileName(contentDisposition: string): string | null {
  if (!contentDisposition) {
    return null
  }
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1]).trim()
  }
  const plainMatch = contentDisposition.match(/filename=\"?([^\";]+)\"?/i)
  return plainMatch?.[1]?.trim() || null
}
