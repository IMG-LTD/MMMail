import type { DocsNoteDetail, DocsNoteSummary } from '~/types/api'
import type { LocationQuery, LocationQueryRaw, LocationQueryValue } from 'vue-router'

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

export function buildDocsRouteQuery(query: LocationQuery, noteId: string | null): LocationQueryRaw {
  const nextQuery: LocationQueryRaw = { ...query }
  delete nextQuery.noteId
  if (noteId) {
    nextQuery.noteId = noteId
  }
  return nextQuery
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
