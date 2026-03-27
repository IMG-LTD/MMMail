import { describe, expect, it } from 'vitest'
import {
  buildChecklistProgressLabel,
  buildStandardNoteTypeOptions,
  buildStandardNotesHealthChips,
  formatStandardNoteType,
  normalizeStandardNoteTags,
  resolvePreferredStandardNoteFolderId,
  resolvePreferredStandardNoteId
} from '../utils/standard-notes'

describe('standard notes business utils', () => {
  it('builds v77 health chips', () => {
    expect(buildStandardNotesHealthChips({
      totalNoteCount: 3,
      activeNoteCount: 3,
      pinnedNoteCount: 1,
      archivedNoteCount: 0,
      uniqueTagCount: 4,
      folderCount: 2,
      checklistNoteCount: 1,
      checklistTaskCount: 5,
      completedChecklistTaskCount: 3,
      exportReady: true,
      generatedAt: '2026-03-08T11:00:00'
    })).toEqual([
      '3 notes',
      '2 folders',
      '3/5 tasks done',
      'Export ready'
    ])
  })

  it('resolves note and folder preference', () => {
    expect(resolvePreferredStandardNoteId([
      { id: 'n1', title: 'A', preview: '', noteType: 'PLAIN_TEXT', tags: [], pinned: false, archived: false, currentVersion: 1, folderId: null, folderName: null, checklistTaskCount: 0, completedChecklistTaskCount: 0, updatedAt: '' },
      { id: 'n2', title: 'B', preview: '', noteType: 'CHECKLIST', tags: [], pinned: true, archived: false, currentVersion: 2, folderId: 'f1', folderName: 'Focus', checklistTaskCount: 2, completedChecklistTaskCount: 1, updatedAt: '' }
    ], 'n2', 'n1')).toBe('n2')

    expect(resolvePreferredStandardNoteFolderId([
      { id: 'f1', name: 'Focus', color: '#C7A57A', description: 'Daily work', noteCount: 1, checklistTaskCount: 2, completedChecklistTaskCount: 1, updatedAt: '' }
    ], 'f1')).toBe('f1')
    expect(resolvePreferredStandardNoteFolderId([], 'UNFILED')).toBe('UNFILED')
  })

  it('formats checklist helpers', () => {
    expect(buildChecklistProgressLabel(0, 0)).toBe('No tasks')
    expect(buildChecklistProgressLabel(5, 3)).toBe('3/5 complete')
    expect(formatStandardNoteType('CHECKLIST')).toBe('Checklist')
  })

  it('builds translated note type options', () => {
    const tMock = (key: string) => {
      if (key === 'standardNotes.type.plainText') {
        return '纯文本'
      }
      if (key === 'standardNotes.type.markdown') {
        return 'Markdown'
      }
      return '清单'
    }

    expect(buildStandardNoteTypeOptions()).toEqual([
      { label: 'Plain text', value: 'PLAIN_TEXT' },
      { label: 'Markdown', value: 'MARKDOWN' },
      { label: 'Checklist', value: 'CHECKLIST' }
    ])
    expect(buildStandardNoteTypeOptions(tMock)).toEqual([
      { label: '纯文本', value: 'PLAIN_TEXT' },
      { label: 'Markdown', value: 'MARKDOWN' },
      { label: '清单', value: 'CHECKLIST' }
    ])
  })

  it('normalizes tags', () => {
    expect(normalizeStandardNoteTags(' Ops, vault, ops\nprivate ')).toEqual(['ops', 'vault', 'private'])
  })
})
