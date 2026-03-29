import { describe, expect, it } from 'vitest'
import {
  buildDocsRouteQuery,
  extractDocsNoteIdFromRouteQuery,
  extractDocsRouteState,
  filterDocsNoteSummaries,
  resolveDocsVisibleNoteId,
  upsertDocsNoteSummary
} from '../utils/docs-route'
import type { DocsNoteDetail, DocsNoteSummary } from '../types/api'

describe('docs route utilities', () => {
  it('extracts note id from route query variants', () => {
    expect(extractDocsNoteIdFromRouteQuery('note-1')).toBe('note-1')
    expect(extractDocsNoteIdFromRouteQuery(['note-2', 'note-3'])).toBe('note-2')
    expect(extractDocsNoteIdFromRouteQuery('   ')).toBeNull()
    expect(extractDocsNoteIdFromRouteQuery(undefined)).toBeNull()
  })

  it('extracts docs route state with normalized keyword and scope', () => {
    expect(extractDocsRouteState({ noteId: ['note-2'], keyword: '  handoff  ', scope: 'SHARED' })).toEqual({
      noteId: 'note-2',
      keyword: 'handoff',
      scope: 'SHARED'
    })
    expect(extractDocsRouteState({ keyword: '   ', scope: 'INVALID' })).toEqual({
      noteId: null,
      keyword: '',
      scope: 'ALL'
    })
  })

  it('builds docs route query while preserving unrelated filters', () => {
    expect(buildDocsRouteQuery(
      { view: 'compact', noteId: 'old-note' },
      { noteId: 'next-note', keyword: 'handoff', scope: 'SHARED' }
    )).toEqual({
      view: 'compact',
      noteId: 'next-note',
      keyword: 'handoff',
      scope: 'SHARED'
    })
    expect(buildDocsRouteQuery(
      { view: 'compact', scope: 'SHARED', keyword: 'handoff', noteId: 'old-note' },
      { noteId: null, keyword: '', scope: 'ALL' }
    )).toEqual({
      view: 'compact'
    })
  })

  it('filters visible docs notes by scope and resolves fallback selection', () => {
    const notes: DocsNoteSummary[] = [
      {
        id: 'owned-1',
        title: 'Owned note',
        updatedAt: '2026-03-28T11:00:00',
        permission: 'OWNER',
        scope: 'OWNED',
        currentVersion: 1,
        ownerEmail: 'owner@mmmail.local',
        ownerDisplayName: 'Owner',
        collaboratorCount: 0
      },
      {
        id: 'shared-1',
        title: 'Shared note',
        updatedAt: '2026-03-28T12:00:00',
        permission: 'EDIT',
        scope: 'SHARED',
        currentVersion: 3,
        ownerEmail: 'collab@mmmail.local',
        ownerDisplayName: 'Collaborator',
        collaboratorCount: 2
      }
    ]

    expect(filterDocsNoteSummaries(notes, 'ALL')).toEqual(notes)
    expect(filterDocsNoteSummaries(notes, 'OWNED')).toEqual([notes[0]])
    expect(filterDocsNoteSummaries(notes, 'SHARED')).toEqual([notes[1]])
    expect(resolveDocsVisibleNoteId(filterDocsNoteSummaries(notes, 'SHARED'), 'owned-1')).toBe('shared-1')
    expect(resolveDocsVisibleNoteId([], 'owned-1')).toBeNull()
  })

  it('upserts active note summaries using detail payload', () => {
    const existing: DocsNoteSummary[] = [
      {
        id: 'note-1',
        title: 'Older note',
        updatedAt: '2026-03-28T11:00:00',
        permission: 'OWNER',
        scope: 'OWNED',
        currentVersion: 1,
        ownerEmail: 'owner@mmmail.local',
        ownerDisplayName: 'Owner',
        collaboratorCount: 0
      }
    ]
    const detail: DocsNoteDetail = {
      id: 'note-2',
      title: 'Shared note',
      content: 'hello',
      createdAt: '2026-03-28T10:00:00',
      updatedAt: '2026-03-28T12:00:00',
      currentVersion: 3,
      permission: 'EDIT',
      shared: true,
      ownerEmail: 'collab@mmmail.local',
      ownerDisplayName: 'Collaborator',
      collaboratorCount: 2,
      syncCursor: 8,
      syncVersion: 'DOC-8'
    }

    expect(upsertDocsNoteSummary(existing, detail)).toEqual([
      {
        id: 'note-2',
        title: 'Shared note',
        updatedAt: '2026-03-28T12:00:00',
        permission: 'EDIT',
        scope: 'SHARED',
        currentVersion: 3,
        ownerEmail: 'collab@mmmail.local',
        ownerDisplayName: 'Collaborator',
        collaboratorCount: 2
      },
      existing[0]
    ])
  })
})
