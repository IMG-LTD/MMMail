import type {
  CreateStandardNoteFolderRequest,
  CreateStandardNoteRequest,
  StandardNoteDetail,
  StandardNoteFolder,
  StandardNoteSummary,
  StandardNotesApiResponse,
  StandardNotesExport,
  StandardNotesOverview,
  StandardNoteType,
  ToggleStandardNoteChecklistItemRequest,
  UpdateStandardNoteFolderRequest,
  UpdateStandardNoteRequest
} from '~/types/standard-notes'

export function useStandardNotesApi() {
  const { $apiClient } = useNuxtApp()

  async function getOverview(): Promise<StandardNotesOverview> {
    const response = await $apiClient.get<StandardNotesApiResponse<StandardNotesOverview>>('/api/v1/standard-notes/overview')
    return response.data.data
  }

  async function listFolders(): Promise<StandardNoteFolder[]> {
    const response = await $apiClient.get<StandardNotesApiResponse<StandardNoteFolder[]>>('/api/v1/standard-notes/folders')
    return response.data.data
  }

  async function createFolder(payload: CreateStandardNoteFolderRequest): Promise<StandardNoteFolder> {
    const response = await $apiClient.post<StandardNotesApiResponse<StandardNoteFolder>>('/api/v1/standard-notes/folders', payload)
    return response.data.data
  }

  async function updateFolder(folderId: string, payload: UpdateStandardNoteFolderRequest): Promise<StandardNoteFolder> {
    const response = await $apiClient.put<StandardNotesApiResponse<StandardNoteFolder>>(`/api/v1/standard-notes/folders/${folderId}`, payload)
    return response.data.data
  }

  async function deleteFolder(folderId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/standard-notes/folders/${folderId}`)
  }

  async function listNotes(params: { keyword?: string, includeArchived?: boolean, noteType?: StandardNoteType | 'ALL', tag?: string, folderId?: string, limit?: number } = {}): Promise<StandardNoteSummary[]> {
    const response = await $apiClient.get<StandardNotesApiResponse<StandardNoteSummary[]>>('/api/v1/standard-notes/notes', {
      params: {
        keyword: params.keyword || undefined,
        includeArchived: params.includeArchived ? true : undefined,
        noteType: params.noteType && params.noteType !== 'ALL' ? params.noteType : undefined,
        tag: params.tag || undefined,
        folderId: params.folderId && params.folderId !== 'ALL' ? params.folderId : undefined,
        limit: params.limit || undefined
      }
    })
    return response.data.data
  }

  async function createNote(payload: CreateStandardNoteRequest): Promise<StandardNoteDetail> {
    const response = await $apiClient.post<StandardNotesApiResponse<StandardNoteDetail>>('/api/v1/standard-notes/notes', payload)
    return response.data.data
  }

  async function getNote(noteId: string): Promise<StandardNoteDetail> {
    const response = await $apiClient.get<StandardNotesApiResponse<StandardNoteDetail>>(`/api/v1/standard-notes/notes/${noteId}`)
    return response.data.data
  }

  async function updateNote(noteId: string, payload: UpdateStandardNoteRequest): Promise<StandardNoteDetail> {
    const response = await $apiClient.put<StandardNotesApiResponse<StandardNoteDetail>>(`/api/v1/standard-notes/notes/${noteId}`, payload)
    return response.data.data
  }

  async function toggleChecklistItem(noteId: string, itemIndex: number, payload: ToggleStandardNoteChecklistItemRequest): Promise<StandardNoteDetail> {
    const response = await $apiClient.post<StandardNotesApiResponse<StandardNoteDetail>>(
      `/api/v1/standard-notes/notes/${noteId}/checklist-items/${itemIndex}/toggle`,
      payload
    )
    return response.data.data
  }

  async function deleteNote(noteId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/standard-notes/notes/${noteId}`)
  }

  async function exportWorkspace(): Promise<StandardNotesExport> {
    const response = await $apiClient.get<StandardNotesApiResponse<StandardNotesExport>>('/api/v1/standard-notes/export')
    return response.data.data
  }

  return {
    getOverview,
    listFolders,
    createFolder,
    updateFolder,
    deleteFolder,
    listNotes,
    createNote,
    getNote,
    updateNote,
    toggleChecklistItem,
    deleteNote,
    exportWorkspace
  }
}
