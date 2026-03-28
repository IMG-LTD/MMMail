import { describe, expect, it } from 'vitest'
import { buildDocsRouteQuery, extractDocsNoteIdFromRouteQuery, upsertDocsNoteSummary } from '../utils/docs-route'
import type { DocsNoteDetail, DocsNoteSummary } from '../types/api'

describe('docs route utilities', () => {
  it('extracts note id from route query variants', () => {
    expect(extractDocsNoteIdFromRouteQuery('note-1')).toBe('note-1')
    expect(extractDocsNoteIdFromRouteQuery(['note-2', 'note-3'])).toBe('note-2')
    expect(extractDocsNoteIdFromRouteQuery('   ')).toBeNull()
    expect(extractDocsNoteIdFromRouteQuery(undefined)).toBeNull()
  })

  it('builds docs route query while preserving unrelated filters', () => {
    expect(buildDocsRouteQuery({ scope: 'SHARED', noteId: 'old-note' }, 'next-note')).toEqual({
      scope: 'SHARED',
      noteId: 'next-note'
    })
    expect(buildDocsRouteQuery({ scope: 'ALL', keyword: 'handoff', noteId: 'old-note' }, null)).toEqual({
      scope: 'ALL',
      keyword: 'handoff'
    })
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
