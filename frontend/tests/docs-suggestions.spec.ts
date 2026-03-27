import { describe, expect, it } from 'vitest'
import type { DocsNoteSuggestion } from '../types/docs'
import { buildDocsSuggestionCounts, getDocsSuggestionTagType, needsDocsDetailRefresh } from '../utils/docs-suggestions'

const suggestions: DocsNoteSuggestion[] = [
  {
    suggestionId: '1',
    authorUserId: 'u1',
    authorEmail: 'author@mmmail.local',
    authorDisplayName: 'Author',
    status: 'PENDING',
    selectionStart: 0,
    selectionEnd: 5,
    originalText: 'hello',
    replacementText: 'private',
    baseVersion: 1,
    resolvedByUserId: null,
    resolvedByEmail: null,
    resolvedByDisplayName: null,
    resolvedAt: null,
    createdAt: '2026-03-09T16:00:00'
  },
  {
    suggestionId: '2',
    authorUserId: 'u2',
    authorEmail: 'owner@mmmail.local',
    authorDisplayName: 'Owner',
    status: 'ACCEPTED',
    selectionStart: 6,
    selectionEnd: 11,
    originalText: 'world',
    replacementText: 'suite',
    baseVersion: 2,
    resolvedByUserId: 'u3',
    resolvedByEmail: 'resolver@mmmail.local',
    resolvedByDisplayName: 'Resolver',
    resolvedAt: '2026-03-09T16:01:00',
    createdAt: '2026-03-09T16:00:30'
  },
  {
    suggestionId: '3',
    authorUserId: 'u2',
    authorEmail: 'owner@mmmail.local',
    authorDisplayName: 'Owner',
    status: 'REJECTED',
    selectionStart: 0,
    selectionEnd: 4,
    originalText: 'text',
    replacementText: '',
    baseVersion: 2,
    resolvedByUserId: 'u3',
    resolvedByEmail: 'resolver@mmmail.local',
    resolvedByDisplayName: 'Resolver',
    resolvedAt: '2026-03-09T16:02:00',
    createdAt: '2026-03-09T16:01:30'
  }
]

describe('docs suggestions utilities', () => {
  it('builds counts by suggestion status', () => {
    expect(buildDocsSuggestionCounts(suggestions)).toEqual({
      all: 3,
      pending: 1,
      accepted: 1,
      rejected: 1
    })
  })

  it('maps suggestion statuses to tag types', () => {
    expect(getDocsSuggestionTagType('PENDING')).toBe('warning')
    expect(getDocsSuggestionTagType('ACCEPTED')).toBe('success')
    expect(getDocsSuggestionTagType('REJECTED')).toBe('info')
  })

  it('marks events that should refresh the active docs detail', () => {
    expect(needsDocsDetailRefresh('DOCS_NOTE_UPDATE')).toBe(true)
    expect(needsDocsDetailRefresh('DOCS_NOTE_SUGGEST_ACCEPT')).toBe(true)
    expect(needsDocsDetailRefresh('DOCS_NOTE_SHARE_PERMISSION_UPDATE')).toBe(true)
    expect(needsDocsDetailRefresh('DOCS_NOTE_SUGGEST_REJECT')).toBe(false)
  })
})
