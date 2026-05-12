import type { MailDetail, MailDraft, MailSenderIdentity, MailSummary } from './mail-types'

const BYTES_PER_KIB = 1024
const BYTES_PER_MIB = BYTES_PER_KIB * BYTES_PER_KIB

export function resolveRouteString(value: unknown) {
  if (Array.isArray(value)) {
    return resolveRouteString(value[0])
  }

  return typeof value === 'string' ? value : ''
}

export function isEmailLike(value: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
}

export function validateMailDraft(draft: MailDraft) {
  if (!isEmailLike(draft.toEmail.trim())) {
    return 'Enter a valid recipient before sending.'
  }

  if (!draft.subject.trim()) {
    return 'Add a subject before sending.'
  }

  if (!draft.body.trim()) {
    return 'Write a message before sending.'
  }

  return ''
}

export function formatMailTimestamp(value: string) {
  if (!value) {
    return 'Unknown time'
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat(undefined, {
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    month: 'short'
  }).format(parsed)
}

export function formatFileSize(value: number) {
  if (!value) {
    return '0 B'
  }

  if (value >= BYTES_PER_MIB) {
    return `${(value / BYTES_PER_MIB).toFixed(1)} MB`
  }

  if (value >= BYTES_PER_KIB) {
    return `${Math.round(value / BYTES_PER_KIB)} KB`
  }

  return `${value} B`
}

export function resolveMailSender(mail: MailSummary | MailDetail | null) {
  if (!mail) {
    return 'No sender'
  }

  return mail.senderDisplayName || mail.senderEmail || mail.peerEmail || 'Unknown sender'
}

export function resolveIdentityLabel(identity: MailSenderIdentity) {
  return identity.displayName || identity.emailAddress || 'Sender identity'
}

export function createEmptyMailDraft(fromEmail = ''): MailDraft {
  return { body: '', fromEmail, subject: '', toEmail: '' }
}

export function fallbackMailAttachments() {
  return [
    { fileName: 'Project plan.pdf', fileSize: 1240000, id: 'local-attachment-plan' },
    { fileName: 'Meeting notes.docx', fileSize: 856000, id: 'local-attachment-notes' }
  ]
}
