import type { MailSummary } from '../types/api'

export interface MailListSignal {
  key: string
  value?: string | number
}

export interface MailListModel {
  senderName: string
  avatarText: string
  subjectText: string
  previewText: string
  identitySignals: MailListSignal[]
  taskSignals: MailListSignal[]
  conversationSignals: MailListSignal[]
}

function normalizeDisplayValue(value: string | null | undefined): string {
  return value?.trim() || ''
}

function getFirstVisibleCharacter(value: string): string {
  return Array.from(value.trim())[0] || '?'
}

function isOutgoingFolder(folderType: MailSummary['folderType']): boolean {
  return folderType === 'DRAFTS' || folderType === 'OUTBOX' || folderType === 'SENT' || folderType === 'SCHEDULED'
}

function resolveDisplayIdentity(mail: MailSummary): string {
  if (isOutgoingFolder(mail.folderType)) {
    return normalizeDisplayValue(mail.peerEmail)
      || normalizeDisplayValue(mail.senderDisplayName)
      || normalizeDisplayValue(mail.senderEmail)
  }

  return normalizeDisplayValue(mail.senderDisplayName)
    || normalizeDisplayValue(mail.senderEmail)
    || normalizeDisplayValue(mail.peerEmail)
}

export function buildMailListModel(mail: MailSummary): MailListModel {
  const senderName = resolveDisplayIdentity(mail)
  const avatarText = getFirstVisibleCharacter(senderName).toUpperCase()
  const identitySignals: MailListSignal[] = []
  const taskSignals: MailListSignal[] = []
  const conversationSignals: MailListSignal[] = []

  if (!isOutgoingFolder(mail.folderType) && mail.isImportantContact) {
    identitySignals.push({ key: 'importantContact' })
  }

  if (!isOutgoingFolder(mail.folderType) && mail.senderType) {
    identitySignals.push({ key: `senderType:${mail.senderType}` })
  }

  if (mail.needsReply) {
    taskSignals.push({ key: 'needsReply' })
  }

  if (mail.isStarred) {
    taskSignals.push({ key: 'starred' })
  }

  if (mail.hasAttachments) {
    conversationSignals.push({ key: 'attachments' })
  }

  if (mail.replyState && mail.replyState !== 'NONE') {
    conversationSignals.push({ key: `replyState:${mail.replyState}` })
  }

  if (mail.latestActor) {
    conversationSignals.push({ key: `latestActor:${mail.latestActor}` })
  }

  if (typeof mail.conversationMessageCount === 'number' && mail.conversationMessageCount > 1) {
    conversationSignals.push({ key: 'messageCount', value: mail.conversationMessageCount })
  }

  return {
    senderName,
    avatarText,
    subjectText: mail.subject,
    previewText: mail.preview,
    identitySignals,
    taskSignals,
    conversationSignals
  }
}
