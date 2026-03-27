import { describe, expect, it } from 'vitest'
import type { MailFilter } from '../types/mail-filters'
import {
  buildMailFilterPayload,
  createMailFilterDraft,
  describeMailFilterActions,
  describeMailFilterConditions
} from '../utils/mail-filters'
import { messages } from '../locales'
import { translate } from '../utils/i18n'

function t(key: string, params?: Record<string, string | number>): string {
  return translate(messages, 'en', key, params)
}

describe('mail filters utils', () => {
  it('builds a normalized filter payload', () => {
    const draft = createMailFilterDraft()
    draft.name = ' Ops archive '
    draft.senderContains = ' alerts@example.com '
    draft.subjectContains = ' Billing '
    draft.targetFolder = 'ARCHIVE'
    draft.labels = ['ops']
    draft.markRead = true

    expect(buildMailFilterPayload(draft)).toEqual({
      name: 'Ops archive',
      senderContains: 'alerts@example.com',
      subjectContains: 'Billing',
      targetFolder: 'ARCHIVE',
      labels: ['ops'],
      markRead: true,
      enabled: true
    })
  })

  it('rejects missing conditions and actions', () => {
    const draft = createMailFilterDraft()
    draft.name = 'Only name'
    expect(() => buildMailFilterPayload(draft)).toThrowError('conditions')

    draft.senderContains = 'alerts@example.com'
    expect(() => buildMailFilterPayload(draft)).toThrowError('actions')
  })

  it('describes filter conditions and actions', () => {
    const filter: MailFilter = {
      id: '1',
      name: 'Ops archive',
      enabled: true,
      senderContains: 'alerts@example.com',
      subjectContains: 'Billing',
      keywordContains: null,
      targetFolder: 'ARCHIVE',
      targetCustomFolderId: null,
      targetCustomFolderName: null,
      labels: ['ops', 'billing'],
      markRead: true,
      createdAt: '2026-03-10T10:00:00',
      updatedAt: '2026-03-10T10:00:00'
    }

    expect(describeMailFilterConditions(filter, t)).toBe('Sender has “alerts@example.com” · Subject has “Billing”')
    expect(describeMailFilterActions(filter, t)).toBe('Move to Archive · Apply labels: ops, billing · Mark as read')
  })

  it('describes custom folder actions', () => {
    const filter: MailFilter = {
      id: '2',
      name: 'Route to projects',
      enabled: true,
      senderContains: null,
      subjectContains: null,
      keywordContains: 'roadmap',
      targetFolder: null,
      targetCustomFolderId: 'folder-1',
      targetCustomFolderName: 'Projects',
      labels: [],
      markRead: false,
      createdAt: '2026-03-10T10:00:00',
      updatedAt: '2026-03-10T10:00:00'
    }

    expect(describeMailFilterActions(filter, t)).toBe('Move to Projects')
  })
})
