import type { MailDetail } from '~/types/api'

const REPLY_PREFIX = 'Re: '
const FORWARD_PREFIX = 'Fwd: '
const NO_SUBJECT = '(no subject)'

type ComposeMode = 'reply' | 'forward' | 'draft'

function normalizedSubject(subject: string): string {
  const value = subject.trim()
  return value || NO_SUBJECT
}

function normalizePrefix(prefix: string): RegExp {
  return new RegExp(`^${prefix.replace(':', '\\:')}\\s*`, 'i')
}

function quoteMailBody(body: string): string {
  return body
    .split('\n')
    .map((line) => `> ${line}`)
    .join('\n')
}

function formatQuotedTime(sentAt: string): string {
  const date = new Date(sentAt)
  if (Number.isNaN(date.getTime())) {
    return sentAt
  }
  return date.toISOString().replace('.000Z', ' UTC').replace('T', ' ')
}

export function prefixMailSubject(subject: string, mode: Exclude<ComposeMode, 'draft'>): string {
  const prefix = mode === 'reply' ? REPLY_PREFIX : FORWARD_PREFIX
  const value = normalizedSubject(subject)
  if (normalizePrefix(prefix).test(value)) {
    return value
  }
  return `${prefix}${value}`
}

export function buildReplyBody(mail: Pick<MailDetail, 'body' | 'sentAt' | 'peerEmail'>): string {
  return `\n\nOn ${formatQuotedTime(mail.sentAt)}, ${mail.peerEmail} wrote:\n${quoteMailBody(mail.body)}`
}

export function buildForwardBody(mail: Pick<MailDetail, 'subject' | 'body' | 'sentAt' | 'peerEmail'>): string {
  const header = [
    '---------- Forwarded message ---------',
    `From: ${mail.peerEmail}`,
    `Date: ${formatQuotedTime(mail.sentAt)}`,
    `Subject: ${normalizedSubject(mail.subject)}`
  ]
  return `\n\n${header.join('\n')}\n\n${mail.body}`
}

export function buildMailComposeQuery(mode: ComposeMode, mail: MailDetail): Record<string, string> {
  if (mode === 'draft') {
    return { draftId: mail.id }
  }

  if (mode === 'reply') {
    return {
      to: mail.peerEmail,
      subject: prefixMailSubject(mail.subject, 'reply'),
      body: buildReplyBody(mail)
    }
  }

  return {
    subject: prefixMailSubject(mail.subject, 'forward'),
    body: buildForwardBody(mail)
  }
}
