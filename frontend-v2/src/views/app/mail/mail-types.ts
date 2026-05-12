import type { MailAttachment, MailDetail, MailSenderIdentity, MailSummary, RecipientTrustState } from '@/service/api/mail'

export interface MailDraft {
  body: string
  fromEmail: string
  subject: string
  toEmail: string
}

export interface MailComposePanelProps {
  draft: MailDraft
  discardConfirmationOpen: boolean
  identities: MailSenderIdentity[]
  sendError: string
  sending: boolean
  trustCopy: string
  trustState: RecipientTrustState | null
}

export type {
  MailAttachment,
  MailDetail,
  MailSenderIdentity,
  MailSummary,
  RecipientTrustState
}
