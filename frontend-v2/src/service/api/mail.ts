import { httpClient } from '@/service/request/http'
import type { ApiResponse } from '@/shared/types/api'

type QueryValue = string | number | boolean | undefined

interface MailFolderPagePayload {
  items?: MailSummary[]
}

interface MailRecipientTrustApiPayload {
  toEmail?: string
  fromEmail?: string
  deliverable?: boolean
  encryptionReady?: boolean
  readiness?: string
  routeCount?: number
}

export interface MailSummary {
  id: string
  senderEmail: string | null
  senderDisplayName?: string | null
  peerEmail: string
  subject: string
  preview: string
  sentAt: string
  isRead: boolean
  unread: boolean
  hasAttachments?: boolean
}

export interface MailAttachment {
  id: string
  fileName: string
  fileSize: number
  contentType?: string
}

export interface MailDetail extends MailSummary {
  body: string
  attachments: MailAttachment[]
}

export interface MailSenderIdentity {
  emailAddress: string
  displayName: string
  source?: string
}

export interface RecipientTrustState {
  status: 'ready' | 'warning' | 'blocked'
  message?: string
  toEmail?: string
  fromEmail?: string
  deliverable?: boolean
  encryptionReady?: boolean
  readiness?: string
  routeCount?: number
}

export interface SendMailPayload {
  toEmail: string
  fromEmail?: string
  subject: string
  body?: string
  idempotencyKey?: string
  labels?: string[]
  scheduledAt?: string
  e2ee?: Record<string, unknown>
}

const MAIL_FOLDER_PATHS: Record<string, string> = {
  archive: '/api/v1/mails/archive',
  drafts: '/api/v1/mails/drafts',
  inbox: '/api/v1/mails/inbox',
  outbox: '/api/v1/mails/outbox',
  scheduled: '/api/v1/mails/scheduled',
  search: '/api/v1/mails/search',
  sent: '/api/v1/mails/sent',
  snoozed: '/api/v1/mails/snoozed',
  spam: '/api/v1/mails/spam',
  starred: '/api/v1/mails/starred',
  trash: '/api/v1/mails/trash',
  unread: '/api/v1/mails/unread'
}

function normalizeMailSummary(payload: Partial<MailSummary> & Record<string, unknown>): MailSummary {
  const isRead = typeof payload.isRead === 'boolean'
    ? payload.isRead
    : typeof payload.unread === 'boolean'
      ? !payload.unread
      : false

  return {
    hasAttachments: typeof payload.hasAttachments === 'boolean' ? payload.hasAttachments : false,
    id: String(payload.id || ''),
    isRead,
    peerEmail: typeof payload.peerEmail === 'string' ? payload.peerEmail : '',
    preview: typeof payload.preview === 'string' ? payload.preview : '',
    senderDisplayName: typeof payload.senderDisplayName === 'string' ? payload.senderDisplayName : null,
    senderEmail: typeof payload.senderEmail === 'string' ? payload.senderEmail : null,
    sentAt: typeof payload.sentAt === 'string' ? payload.sentAt : '',
    subject: typeof payload.subject === 'string' ? payload.subject : '',
    unread: typeof payload.unread === 'boolean' ? payload.unread : !isRead
  }
}

function normalizeMailDetail(payload: Partial<MailDetail> & Record<string, unknown>): MailDetail {
  const attachments = Array.isArray(payload.attachments)
    ? payload.attachments.map(item => ({
        contentType: typeof item?.contentType === 'string' ? item.contentType : undefined,
        fileName: typeof item?.fileName === 'string' ? item.fileName : '',
        fileSize: typeof item?.fileSize === 'number' ? item.fileSize : 0,
        id: typeof item?.id === 'string' ? item.id : ''
      }))
    : []

  return {
    ...normalizeMailSummary(payload),
    attachments,
    body: typeof payload.body === 'string' ? payload.body : ''
  }
}

function normalizeFolderItems(payload: MailFolderPagePayload | MailSummary[] | undefined): MailSummary[] {
  const items = Array.isArray(payload)
    ? payload
    : Array.isArray(payload?.items)
      ? payload.items
      : []

  return items.map(item => normalizeMailSummary(item as Partial<MailSummary> & Record<string, unknown>))
}

function createIdempotencyKey() {
  return globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export async function listMailFolder(folder: string, token: string, query: Record<string, QueryValue> = {}) {
  const path = MAIL_FOLDER_PATHS[folder] || `/api/v1/mails/${folder}`
  const response = await httpClient.get<ApiResponse<MailFolderPagePayload | MailSummary[]>>(path, { token, query })
  return {
    ...response,
    data: normalizeFolderItems(response.data)
  }
}

export async function readMailDetail(mailId: string, token: string) {
  const response = await httpClient.get<ApiResponse<MailDetail>>(`/api/v1/mails/${mailId}`, { token })
  return {
    ...response,
    data: normalizeMailDetail(response.data as Partial<MailDetail> & Record<string, unknown>)
  }
}

export function listSenderIdentities(token: string) {
  return httpClient.get<ApiResponse<MailSenderIdentity[]>>('/api/v1/mails/identities', { token })
}

export async function readRecipientTrustState(toEmail: string, fromEmail: string, token: string) {
  const response = await httpClient.get<ApiResponse<MailRecipientTrustApiPayload>>('/api/v1/mails/e2ee-recipient-status', {
    token,
    query: { fromEmail, toEmail }
  })

  const payload = response.data || {}
  const status: RecipientTrustState['status'] = payload.encryptionReady
    ? 'ready'
    : payload.deliverable
      ? 'warning'
      : 'blocked'

  const message = payload.encryptionReady
    ? 'Recipient is ready for encrypted delivery.'
    : payload.deliverable
      ? 'Recipient can receive mail, but encrypted delivery is not fully ready yet.'
      : 'Recipient is not ready for authenticated encrypted delivery.'

  return {
    ...response,
    data: {
      deliverable: payload.deliverable,
      encryptionReady: payload.encryptionReady,
      fromEmail: payload.fromEmail,
      message,
      readiness: payload.readiness,
      routeCount: payload.routeCount,
      status,
      toEmail: payload.toEmail
    }
  }
}

export function sendMail(payload: SendMailPayload, token: string) {
  return httpClient.post<void>('/api/v1/mails/send', {
    body: {
      ...payload,
      idempotencyKey: payload.idempotencyKey || createIdempotencyKey(),
      labels: payload.labels || []
    },
    token
  })
}
