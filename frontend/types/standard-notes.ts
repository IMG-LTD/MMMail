import type { ApiResponse } from '~/types/api'

export type StandardNoteType = 'PLAIN_TEXT' | 'MARKDOWN' | 'CHECKLIST'

export interface StandardNotesOverview {
  totalNoteCount: number
  activeNoteCount: number
  pinnedNoteCount: number
  archivedNoteCount: number
  uniqueTagCount: number
  folderCount: number
  checklistNoteCount: number
  checklistTaskCount: number
  completedChecklistTaskCount: number
  exportReady: boolean
  generatedAt: string
}

export interface StandardNoteChecklistItem {
  itemIndex: number
  text: string
  completed: boolean
}

export interface StandardNoteFolder {
  id: string
  name: string
  color: string
  description?: string | null
  noteCount: number
  checklistTaskCount: number
  completedChecklistTaskCount: number
  updatedAt: string
}

export interface StandardNoteSummary {
  id: string
  title: string
  preview: string
  noteType: StandardNoteType
  tags: string[]
  pinned: boolean
  archived: boolean
  currentVersion: number
  folderId?: string | null
  folderName?: string | null
  checklistTaskCount: number
  completedChecklistTaskCount: number
  updatedAt: string
}

export interface StandardNoteDetail extends StandardNoteSummary {
  content: string
  checklistItems: StandardNoteChecklistItem[]
  createdAt: string
}

export interface StandardNotesFilterState {
  keyword: string
  includeArchived: boolean
  noteType: StandardNoteType | 'ALL'
  tag: string
}

export interface StandardNotesEditorState {
  title: string
  content: string
  noteType: StandardNoteType
  tagInput: string
  folderId: string
  pinned: boolean
  archived: boolean
  currentVersion: number
  createdAt: string
  updatedAt: string
}

export interface StandardNotesExport {
  fileName: string
  format: string
  exportedAt: string
  overview: StandardNotesOverview
  folders: StandardNoteFolder[]
  notes: StandardNoteDetail[]
}

export interface CreateStandardNoteRequest {
  title: string
  content?: string
  noteType?: StandardNoteType
  tags?: string[]
  folderId?: string
  pinned?: boolean
}

export interface UpdateStandardNoteRequest {
  title: string
  content?: string
  currentVersion: number
  noteType?: StandardNoteType
  tags?: string[]
  folderId?: string
  pinned?: boolean
  archived?: boolean
}

export interface CreateStandardNoteFolderRequest {
  name: string
  color?: string
  description?: string
}

export interface UpdateStandardNoteFolderRequest extends CreateStandardNoteFolderRequest {
}

export interface ToggleStandardNoteChecklistItemRequest {
  currentVersion: number
  completed: boolean
}

export type StandardNotesApiResponse<T> = ApiResponse<T>
