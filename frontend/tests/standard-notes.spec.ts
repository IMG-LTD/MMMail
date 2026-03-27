import { describe, expect, it } from 'vitest'
import type { StandardNoteSummary } from '../types/standard-notes'
import {
  buildChecklistProgressLabel,
  buildStandardNotesHealthChips,
  normalizeStandardNoteTags,
  resolvePreferredStandardNoteFolderId,
  resolvePreferredStandardNoteId
} from '../utils/standard-notes'

describe('standard notes utils', () => {
  it('prefers route note id when present', () => {
    const notes: StandardNoteSummary[] = [
      { id: 'a', title: '', preview: '', noteType: 'PLAIN_TEXT', tags: [], pinned: false, archived: false, currentVersion: 1, folderId: null, folderName: null, checklistTaskCount: 0, completedChecklistTaskCount: 0, updatedAt: '' },
      { id: 'b', title: '', preview: '', noteType: 'MARKDOWN', tags: [], pinned: true, archived: false, currentVersion: 1, folderId: 'f-1', folderName: 'Focus', checklistTaskCount: 0, completedChecklistTaskCount: 0, updatedAt: '' }
    ]
    expect(resolvePreferredStandardNoteId(notes, 'b', 'a')).toBe('b')
  })

  it('resolves preferred folder selection', () => {
    expect(resolvePreferredStandardNoteFolderId([{ id: 'f-1', name: 'Focus', color: '#C7A57A', description: '', noteCount: 2, checklistTaskCount: 3, completedChecklistTaskCount: 1, updatedAt: '' }], 'f-1')).toBe('f-1')
    expect(resolvePreferredStandardNoteFolderId([], 'missing')).toBe('ALL')
    expect(resolvePreferredStandardNoteFolderId([], 'UNFILED')).toBe('UNFILED')
  })

  it('normalizes tag input', () => {
    expect(normalizeStandardNoteTags('Work, vault, work\nideas')).toEqual(['work', 'vault', 'ideas'])
  })

  it('builds workspace chips', () => {
    expect(buildStandardNotesHealthChips({
      totalNoteCount: 2,
      activeNoteCount: 2,
      pinnedNoteCount: 1,
      archivedNoteCount: 0,
      uniqueTagCount: 3,
      folderCount: 1,
      checklistNoteCount: 1,
      checklistTaskCount: 4,
      completedChecklistTaskCount: 2,
      exportReady: true,
      generatedAt: '2026-03-08T02:00:00'
    })).toEqual(['2 notes', '1 folders', '2/4 tasks done', 'Export ready'])
  })

  it('builds checklist progress labels', () => {
    expect(buildChecklistProgressLabel(0, 0)).toBe('No tasks')
    expect(buildChecklistProgressLabel(4, 2)).toBe('2/4 complete')
  })
})
