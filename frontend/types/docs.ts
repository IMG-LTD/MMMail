export type DocsReviewMode = 'EDIT' | 'SUGGEST'
export type DocsSuggestionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

export interface DocsNoteSuggestion {
  suggestionId: string
  authorUserId: string
  authorEmail: string
  authorDisplayName: string
  status: DocsSuggestionStatus
  selectionStart: number
  selectionEnd: number
  originalText: string
  replacementText: string
  baseVersion: number
  resolvedByUserId: string | null
  resolvedByEmail: string | null
  resolvedByDisplayName: string | null
  resolvedAt: string | null
  createdAt: string
}

export interface CreateDocsNoteSuggestionRequest {
  selectionStart: number
  selectionEnd: number
  originalText: string
  replacementText?: string
  baseVersion: number
}

export interface ResolveDocsNoteSuggestionRequest {
  currentVersion: number
}

export interface UpdateDocsNoteSharePermissionRequest {
  permission: 'VIEW' | 'EDIT'
}
