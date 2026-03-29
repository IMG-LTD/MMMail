import type { DocsNoteDetail, DocsNoteSummary } from '~/types/api'
import type { LocationQuery, LocationQueryRaw, LocationQueryValue } from 'vue-router'

export type DocsScopeFilter = 'ALL' | 'OWNED' | 'SHARED'

export interface DocsRouteState {
  noteId: string | null
  keyword: string
  scope: DocsScopeFilter
}

const DEFAULT_DOCS_SCOPE: DocsScopeFilter = 'ALL'
const DOCS_SCOPE_FILTERS: readonly DocsScopeFilter[] = ['ALL', 'OWNED', 'SHARED']

function normalizeRouteQueryValue(value: LocationQueryValue | LocationQueryValue[] | undefined): string | null {
  if (Array.isArray(value)) {
    return normalizeRouteQueryValue(value[0])
  }
  if (typeof value !== 'string') {
    return null
  }
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

export function extractDocsNoteIdFromRouteQuery(value: LocationQueryValue | LocationQueryValue[] | undefined): string | null {
  return normalizeRouteQueryValue(value)
}

function extractDocsKeywordFromRouteQuery(value: LocationQueryValue | LocationQueryValue[] | undefined): string {
  return normalizeRouteQueryValue(value) ?? ''
}

function extractDocsScopeFromRouteQuery(value: LocationQueryValue | LocationQueryValue[] | undefined): DocsScopeFilter {
  const normalized = normalizeRouteQueryValue(value)
  if (!normalized) {
    return DEFAULT_DOCS_SCOPE
  }
  return DOCS_SCOPE_FILTERS.includes(normalized as DocsScopeFilter)
    ? normalized as DocsScopeFilter
    : DEFAULT_DOCS_SCOPE
}

export function extractDocsRouteState(query: LocationQuery): DocsRouteState {
  return {
    noteId: extractDocsNoteIdFromRouteQuery(query.noteId),
    keyword: extractDocsKeywordFromRouteQuery(query.keyword),
    scope: extractDocsScopeFromRouteQuery(query.scope)
  }
}

export function buildDocsRouteQuery(query: LocationQuery, state: DocsRouteState): LocationQueryRaw {
  const nextQuery: LocationQueryRaw = { ...query }
  delete nextQuery.noteId
  delete nextQuery.keyword
  delete nextQuery.scope
  if (state.noteId) {
    nextQuery.noteId = state.noteId
  }
  if (state.keyword) {
    nextQuery.keyword = state.keyword
  }
  if (state.scope !== DEFAULT_DOCS_SCOPE) {
    nextQuery.scope = state.scope
  }
  return nextQuery
}

export function filterDocsNoteSummaries(notes: DocsNoteSummary[], scope: DocsScopeFilter): DocsNoteSummary[] {
  if (scope === DEFAULT_DOCS_SCOPE) {
    return notes
  }
  return notes.filter((note) => note.scope === scope)
}

export function resolveDocsVisibleNoteId(notes: DocsNoteSummary[], activeNoteId: string | null): string | null {
  if (!notes.length) {
    return null
  }
  if (activeNoteId && notes.some((note) => note.id === activeNoteId)) {
    return activeNoteId
  }
  return notes[0].id
}

export function upsertDocsNoteSummary(notes: DocsNoteSummary[], detail: DocsNoteDetail): DocsNoteSummary[] {
  const summary: DocsNoteSummary = {
    id: detail.id,
    title: detail.title,
    updatedAt: detail.updatedAt,
    permission: detail.permission,
    scope: detail.shared ? 'SHARED' : 'OWNED',
    currentVersion: detail.currentVersion,
    ownerEmail: detail.ownerEmail,
    ownerDisplayName: detail.ownerDisplayName,
    collaboratorCount: detail.collaboratorCount
  }
  const next = [summary, ...notes.filter((item) => item.id !== detail.id)]
  next.sort((left, right) => {
    const timeDelta = new Date(right.updatedAt).getTime() - new Date(left.updatedAt).getTime()
    if (timeDelta !== 0) {
      return timeDelta
    }
    return left.title.localeCompare(right.title)
  })
  return next
}
