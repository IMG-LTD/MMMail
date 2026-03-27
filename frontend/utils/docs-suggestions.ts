import type { DocsNoteSuggestion, DocsSuggestionStatus } from '~/types/docs'

const DETAIL_REFRESH_EVENT_TYPES = new Set([
  'DOCS_NOTE_UPDATE',
  'DOCS_NOTE_SHARE_ADD',
  'DOCS_NOTE_SHARE_PERMISSION_UPDATE',
  'DOCS_NOTE_SHARE_REMOVE',
  'DOCS_NOTE_SUGGEST_ACCEPT'
])

export function buildDocsSuggestionCounts(items: DocsNoteSuggestion[]) {
  return items.reduce(
    (counts, item) => {
      counts.all += 1
      if (item.status === 'PENDING') counts.pending += 1
      if (item.status === 'ACCEPTED') counts.accepted += 1
      if (item.status === 'REJECTED') counts.rejected += 1
      return counts
    },
    { all: 0, pending: 0, accepted: 0, rejected: 0 }
  )
}

export function getDocsSuggestionTagType(status: DocsSuggestionStatus): 'warning' | 'success' | 'info' {
  if (status === 'PENDING') return 'warning'
  if (status === 'ACCEPTED') return 'success'
  return 'info'
}

export function needsDocsDetailRefresh(eventType: string): boolean {
  return DETAIL_REFRESH_EVENT_TYPES.has(eventType)
}
