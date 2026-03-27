import type {
  ApiResponse,
  CreateDocsNoteCommentRequest,
  CreateDocsNoteRequest,
  CreateDocsNoteShareRequest,
  DocsNoteCollaborationOverview,
  DocsNoteComment,
  DocsNoteDetail,
  DocsNotePresence,
  DocsNoteShare,
  DocsNoteSummary,
  DocsNoteSync,
  HeartbeatDocsNotePresenceRequest,
  UpdateDocsNoteRequest
} from '~/types/api'
import type {
  CreateDocsNoteSuggestionRequest,
  DocsNoteSuggestion,
  ResolveDocsNoteSuggestionRequest,
  UpdateDocsNoteSharePermissionRequest
} from '~/types/docs'

export function useDocsApi() {
  const { $apiClient } = useNuxtApp()

  async function listNotes(keyword = '', limit = 100): Promise<DocsNoteSummary[]> {
    const response = await $apiClient.get<ApiResponse<DocsNoteSummary[]>>('/api/v1/docs/notes', {
      params: { keyword, limit }
    })
    return response.data.data
  }

  async function createNote(payload: CreateDocsNoteRequest): Promise<DocsNoteDetail> {
    const response = await $apiClient.post<ApiResponse<DocsNoteDetail>>('/api/v1/docs/notes', payload)
    return response.data.data
  }

  async function getNote(noteId: string): Promise<DocsNoteDetail> {
    const response = await $apiClient.get<ApiResponse<DocsNoteDetail>>(`/api/v1/docs/notes/${noteId}`)
    return response.data.data
  }

  async function updateNote(noteId: string, payload: UpdateDocsNoteRequest): Promise<DocsNoteDetail> {
    const response = await $apiClient.put<ApiResponse<DocsNoteDetail>>(`/api/v1/docs/notes/${noteId}`, payload)
    return response.data.data
  }

  async function deleteNote(noteId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/docs/notes/${noteId}`)
  }

  async function getCollaborationOverview(noteId: string): Promise<DocsNoteCollaborationOverview> {
    const response = await $apiClient.get<ApiResponse<DocsNoteCollaborationOverview>>(`/api/v1/docs/notes/${noteId}/collaboration`)
    return response.data.data
  }

  async function listShares(noteId: string): Promise<DocsNoteShare[]> {
    const response = await $apiClient.get<ApiResponse<DocsNoteShare[]>>(`/api/v1/docs/notes/${noteId}/shares`)
    return response.data.data
  }

  async function createShare(noteId: string, payload: CreateDocsNoteShareRequest): Promise<DocsNoteShare> {
    const response = await $apiClient.post<ApiResponse<DocsNoteShare>>(`/api/v1/docs/notes/${noteId}/shares`, payload)
    return response.data.data
  }

  async function updateSharePermission(
    noteId: string,
    shareId: string,
    payload: UpdateDocsNoteSharePermissionRequest
  ): Promise<DocsNoteShare> {
    const response = await $apiClient.put<ApiResponse<DocsNoteShare>>(`/api/v1/docs/notes/${noteId}/shares/${shareId}`, payload)
    return response.data.data
  }

  async function revokeShare(noteId: string, shareId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/docs/notes/${noteId}/shares/${shareId}`)
  }

  async function listComments(noteId: string, includeResolved = true): Promise<DocsNoteComment[]> {
    const response = await $apiClient.get<ApiResponse<DocsNoteComment[]>>(`/api/v1/docs/notes/${noteId}/comments`, {
      params: { includeResolved }
    })
    return response.data.data
  }

  async function createComment(noteId: string, payload: CreateDocsNoteCommentRequest): Promise<DocsNoteComment> {
    const response = await $apiClient.post<ApiResponse<DocsNoteComment>>(`/api/v1/docs/notes/${noteId}/comments`, payload)
    return response.data.data
  }

  async function resolveComment(noteId: string, commentId: string): Promise<DocsNoteComment> {
    const response = await $apiClient.post<ApiResponse<DocsNoteComment>>(`/api/v1/docs/notes/${noteId}/comments/${commentId}/resolve`)
    return response.data.data
  }

  async function listSuggestions(noteId: string, includeResolved = true): Promise<DocsNoteSuggestion[]> {
    const response = await $apiClient.get<ApiResponse<DocsNoteSuggestion[]>>(`/api/v1/docs/notes/${noteId}/suggestions`, {
      params: { includeResolved }
    })
    return response.data.data
  }

  async function createSuggestion(noteId: string, payload: CreateDocsNoteSuggestionRequest): Promise<DocsNoteSuggestion> {
    const response = await $apiClient.post<ApiResponse<DocsNoteSuggestion>>(`/api/v1/docs/notes/${noteId}/suggestions`, payload)
    return response.data.data
  }

  async function acceptSuggestion(
    noteId: string,
    suggestionId: string,
    payload: ResolveDocsNoteSuggestionRequest
  ): Promise<DocsNoteSuggestion> {
    const response = await $apiClient.post<ApiResponse<DocsNoteSuggestion>>(
      `/api/v1/docs/notes/${noteId}/suggestions/${suggestionId}/accept`,
      payload
    )
    return response.data.data
  }

  async function rejectSuggestion(noteId: string, suggestionId: string): Promise<DocsNoteSuggestion> {
    const response = await $apiClient.post<ApiResponse<DocsNoteSuggestion>>(
      `/api/v1/docs/notes/${noteId}/suggestions/${suggestionId}/reject`
    )
    return response.data.data
  }

  async function listPresence(noteId: string): Promise<DocsNotePresence[]> {
    const response = await $apiClient.get<ApiResponse<DocsNotePresence[]>>(`/api/v1/docs/notes/${noteId}/presence`)
    return response.data.data
  }

  async function heartbeatPresence(noteId: string, payload: HeartbeatDocsNotePresenceRequest): Promise<DocsNotePresence> {
    const response = await $apiClient.post<ApiResponse<DocsNotePresence>>(`/api/v1/docs/notes/${noteId}/presence/heartbeat`, payload)
    return response.data.data
  }

  async function getSync(noteId: string, afterEventId?: number, limit?: number): Promise<DocsNoteSync> {
    const response = await $apiClient.get<ApiResponse<DocsNoteSync>>(`/api/v1/docs/notes/${noteId}/sync`, {
      params: {
        afterEventId: typeof afterEventId === 'number' ? afterEventId : undefined,
        limit: typeof limit === 'number' ? limit : undefined
      }
    })
    return response.data.data
  }

  return {
    listNotes,
    createNote,
    getNote,
    updateNote,
    deleteNote,
    getCollaborationOverview,
    listShares,
    createShare,
    updateSharePermission,
    revokeShare,
    listComments,
    createComment,
    resolveComment,
    listSuggestions,
    createSuggestion,
    acceptSuggestion,
    rejectSuggestion,
    listPresence,
    heartbeatPresence,
    getSync
  }
}
