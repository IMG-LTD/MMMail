import type { StandardNoteFolder, StandardNoteSummary, StandardNotesOverview, StandardNoteType } from '~/types/standard-notes'

type TranslateFn = (key: string, params?: Record<string, string | number>) => string

function translateMessage(t: TranslateFn | undefined, key: string, fallback: string, params?: Record<string, string | number>): string {
  if (t) {
    return t(key, params)
  }
  if (!params) {
    return fallback
  }
  return Object.entries(params).reduce((message, [paramKey, value]) => {
    return message.replaceAll(`{${paramKey}}`, String(value))
  }, fallback)
}

export const STANDARD_NOTE_TYPE_OPTIONS: Array<{ label: string, value: StandardNoteType }> = [
  { label: 'Plain text', value: 'PLAIN_TEXT' },
  { label: 'Markdown', value: 'MARKDOWN' },
  { label: 'Checklist', value: 'CHECKLIST' }
]

export function buildStandardNoteTypeOptions(t?: TranslateFn): Array<{ label: string, value: StandardNoteType }> {
  return STANDARD_NOTE_TYPE_OPTIONS.map((option) => ({
    label: formatStandardNoteType(option.value, t),
    value: option.value
  }))
}

export function resolvePreferredStandardNoteId(notes: StandardNoteSummary[], routeNoteId: string | null, fallbackNoteId = ''): string {
  if (routeNoteId && notes.some((note) => note.id === routeNoteId)) {
    return routeNoteId
  }
  if (fallbackNoteId && notes.some((note) => note.id === fallbackNoteId)) {
    return fallbackNoteId
  }
  return notes[0]?.id || ''
}

export function resolvePreferredStandardNoteFolderId(folders: StandardNoteFolder[], routeFolderId: string | null): string {
  if (!routeFolderId) {
    return 'ALL'
  }
  if (routeFolderId === 'UNFILED') {
    return 'UNFILED'
  }
  return folders.some((folder) => folder.id === routeFolderId) ? routeFolderId : 'ALL'
}

export function normalizeStandardNoteTags(input: string): string[] {
  if (!input.trim()) {
    return []
  }
  return Array.from(new Set(
    input
      .split(/[\n,]/)
      .map((item) => item.trim().toLowerCase())
      .filter(Boolean)
  ))
}

export function buildStandardNotesHealthChips(overview: StandardNotesOverview | null, t?: TranslateFn): string[] {
  if (!overview) {
    return [
      translateMessage(t, 'standardNotes.health.loading', 'Loading workspace'),
      translateMessage(t, 'standardNotes.health.foldersPending', 'Folders pending'),
      translateMessage(t, 'standardNotes.health.tasksPending', 'Tasks pending'),
      translateMessage(t, 'standardNotes.health.exportStandby', 'Export standby')
    ]
  }
  return [
    overview.totalNoteCount > 0
      ? translateMessage(t, 'standardNotes.health.notesCount', '{count} notes', { count: overview.totalNoteCount })
      : translateMessage(t, 'standardNotes.health.noNotes', 'No notes yet'),
    overview.folderCount > 0
      ? translateMessage(t, 'standardNotes.health.folderCount', '{count} folders', { count: overview.folderCount })
      : translateMessage(t, 'standardNotes.health.foldersPending', 'Folders pending'),
    overview.checklistTaskCount > 0
      ? translateMessage(
          t,
          'standardNotes.health.tasksDone',
          '{completed}/{total} tasks done',
          { completed: overview.completedChecklistTaskCount, total: overview.checklistTaskCount }
        )
      : translateMessage(t, 'standardNotes.health.tasksPending', 'Tasks pending'),
    overview.exportReady
      ? translateMessage(t, 'standardNotes.health.exportReady', 'Export ready')
      : translateMessage(t, 'standardNotes.health.exportStandby', 'Export standby')
  ]
}

export function formatStandardNoteType(noteType: StandardNoteType, t?: TranslateFn): string {
  if (!t) {
    return STANDARD_NOTE_TYPE_OPTIONS.find((item) => item.value === noteType)?.label || noteType
  }
  if (noteType === 'PLAIN_TEXT') {
    return t('standardNotes.type.plainText')
  }
  if (noteType === 'MARKDOWN') {
    return t('standardNotes.type.markdown')
  }
  return t('standardNotes.type.checklist')
}

export function buildChecklistProgressLabel(taskCount: number, completedTaskCount: number, t?: TranslateFn): string {
  if (taskCount === 0) {
    return translateMessage(t, 'standardNotes.progress.noTasks', 'No tasks')
  }
  return translateMessage(t, 'standardNotes.progress.complete', '{completed}/{total} complete', {
    completed: completedTaskCount,
    total: taskCount
  })
}

export function buildFolderFilterLabel(folderId: string, t?: TranslateFn): string {
  if (folderId === 'ALL') {
    return translateMessage(t, 'standardNotes.collections.allNotes', 'All notes')
  }
  if (folderId === 'UNFILED') {
    return translateMessage(t, 'standardNotes.collections.unfiled', 'Unfiled')
  }
  return translateMessage(t, 'standardNotes.filter.folder', 'Folder')
}
