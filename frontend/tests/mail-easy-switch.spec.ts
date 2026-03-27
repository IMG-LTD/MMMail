import { describe, expect, it } from 'vitest'
import type { MailEasySwitchSession } from '../types/mail-easy-switch'
import {
  appendMailEasySwitchMessage,
  buildMailEasySwitchPayload,
  createMailEasySwitchDraft,
  resolveMailEasySwitchMetricTotal,
  resolveMailEasySwitchStatusType
} from '../utils/mail-easy-switch'

describe('mail easy switch utils', () => {
  it('builds an import payload from draft content', () => {
    const draft = createMailEasySwitchDraft()
    draft.sourceEmail = 'legacy@example.com'
    draft.contactsCsv = 'displayName,email\nAlice,alice@example.com'
    draft.importCalendar = true
    draft.calendarIcs = 'BEGIN:VCALENDAR\nEND:VCALENDAR'
    draft.importMail = true
    draft.mailMessages = ['From: test@example.com\n\nhello']

    expect(buildMailEasySwitchPayload(draft)).toEqual({
      provider: 'GOOGLE',
      sourceEmail: 'legacy@example.com',
      importContacts: true,
      mergeContactDuplicates: true,
      contactsCsv: 'displayName,email\nAlice,alice@example.com',
      importCalendar: true,
      calendarIcs: 'BEGIN:VCALENDAR\nEND:VCALENDAR',
      importMail: true,
      mailMessages: ['From: test@example.com\n\nhello'],
      importedMailFolder: 'ARCHIVE'
    })
  })

  it('deduplicates queued eml content', () => {
    const first = appendMailEasySwitchMessage([], 'From: a@example.com\n\nhello')
    const second = appendMailEasySwitchMessage(first, 'From: a@example.com\n\nhello')
    expect(second).toHaveLength(1)
  })

  it('resolves session totals and status color', () => {
    const session: MailEasySwitchSession = {
      id: '1',
      provider: 'GOOGLE',
      sourceEmail: 'legacy@example.com',
      importContacts: true,
      mergeContactDuplicates: true,
      importCalendar: true,
      importMail: true,
      importedMailFolder: 'INBOX',
      status: 'FAILED',
      contactsCreated: 2,
      contactsUpdated: 1,
      contactsSkipped: 0,
      contactsInvalid: 0,
      calendarImported: 3,
      calendarInvalid: 1,
      mailImported: 4,
      mailSkipped: 2,
      mailInvalid: 1,
      errorMessage: 'bad eml',
      createdAt: '2026-03-09T10:00:00',
      completedAt: '2026-03-09T10:02:00'
    }

    expect(resolveMailEasySwitchMetricTotal(session)).toBe(10)
    expect(resolveMailEasySwitchStatusType(session.status)).toBe('danger')
  })
})
