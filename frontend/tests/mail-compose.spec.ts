import { describe, expect, it } from 'vitest'
import type { MailDetail } from '../types/api'
import {
  buildForwardBody,
  buildMailComposeQuery,
  buildReplyBody,
  prefixMailSubject
} from '../utils/mail-compose'

const baseMail: MailDetail = {
  id: '42',
  ownerId: '7',
  senderEmail: 'owner@mmmail.local',
  peerEmail: 'alice@example.com',
  folderType: 'INBOX',
  customFolderId: null,
  customFolderName: null,
  subject: 'Quarterly update',
  preview: 'First line',
  body: 'First line\nSecond line',
  isRead: false,
  isStarred: false,
  isDraft: false,
  sentAt: '2026-03-13T10:20:30Z',
  labels: [],
  attachments: []
}

describe('mail compose utils', () => {
  it('avoids duplicate reply and forward prefixes', () => {
    expect(prefixMailSubject('Quarterly update', 'reply')).toBe('Re: Quarterly update')
    expect(prefixMailSubject('Re: Quarterly update', 'reply')).toBe('Re: Quarterly update')
    expect(prefixMailSubject('Fwd: Quarterly update', 'forward')).toBe('Fwd: Quarterly update')
  })

  it('builds deterministic reply and forward bodies', () => {
    expect(buildReplyBody(baseMail)).toContain('On 2026-03-13 10:20:30 UTC, alice@example.com wrote:')
    expect(buildReplyBody(baseMail)).toContain('> First line\n> Second line')

    const forwardBody = buildForwardBody(baseMail)
    expect(forwardBody).toContain('---------- Forwarded message ---------')
    expect(forwardBody).toContain('Subject: Quarterly update')
    expect(forwardBody).toContain('First line\nSecond line')
  })

  it('builds compose query for reply, forward and draft edit', () => {
    expect(buildMailComposeQuery('reply', baseMail)).toEqual({
      to: 'alice@example.com',
      subject: 'Re: Quarterly update',
      body: '\n\nOn 2026-03-13 10:20:30 UTC, alice@example.com wrote:\n> First line\n> Second line'
    })

    expect(buildMailComposeQuery('forward', baseMail)).toEqual({
      subject: 'Fwd: Quarterly update',
      body: '\n\n---------- Forwarded message ---------\nFrom: alice@example.com\nDate: 2026-03-13 10:20:30 UTC\nSubject: Quarterly update\n\nFirst line\nSecond line'
    })

    expect(buildMailComposeQuery('draft', {
      ...baseMail,
      isDraft: true,
      folderType: 'DRAFTS'
    })).toEqual({
      draftId: '42'
    })
  })
})
