import { httpClient } from "@/service/request/http";
import type { ApiResponse } from "@/shared/types/api";

type QueryValue = string | number | boolean | undefined;

interface MailFolderPagePayload {
  items?: MailSummary[];
}

interface MailRecipientTrustApiPayload {
  toEmail?: string;
  fromEmail?: string;
  deliverable?: boolean;
  encryptionReady?: boolean;
  readiness?: string;
  routeCount?: number;
}

export interface MailSummary {
  id: string;
  senderEmail: string | null;
  senderDisplayName?: string | null;
  peerEmail: string;
  subject: string;
  preview: string;
  sentAt: string;
  isRead: boolean;
  unread: boolean;
  hasAttachments?: boolean;
}

export interface MailAttachment {
  id: string;
  fileName: string;
  fileSize: number;
  contentType?: string;
}

export interface MailDetail extends MailSummary {
  body: string;
  attachments: MailAttachment[];
}

export interface MailSenderIdentity {
  emailAddress: string;
  displayName: string;
  source?: string;
}

export interface RecipientTrustState {
  status: "ready" | "warning" | "blocked";
  message?: string;
  toEmail?: string;
  fromEmail?: string;
  deliverable?: boolean;
  encryptionReady?: boolean;
  readiness?: string;
  routeCount?: number;
}

export interface SendMailPayload {
  toEmail: string;
  draftId?: string | number;
  fromEmail?: string;
  subject: string;
  body?: string;
  idempotencyKey?: string;
  labels?: string[];
  scheduledAt?: string;
  e2ee?: Record<string, unknown>;
}

export interface MailFolder {
  key: string;
  label: string;
  unreadCount: number;
}

export interface MailDraftPayload {
  body?: string;
  fromEmail?: string;
  subject?: string;
  toEmail?: string;
}

export interface MailBulkActionPayload {
  action: string;
  messageIds: string[];
}

export interface MailRule {
  id: string;
  name: string;
  enabled: boolean;
}

function normalizeMailSummary(
  payload: Partial<MailSummary> & Record<string, unknown>,
): MailSummary {
  const isRead =
    typeof payload.isRead === "boolean"
      ? payload.isRead
      : typeof payload.unread === "boolean"
        ? !payload.unread
        : false;

  return {
    hasAttachments: typeof payload.hasAttachments === "boolean" ? payload.hasAttachments : false,
    id: String(payload.id || ""),
    isRead,
    peerEmail: typeof payload.peerEmail === "string" ? payload.peerEmail : "",
    preview: typeof payload.preview === "string" ? payload.preview : "",
    senderDisplayName:
      typeof payload.senderDisplayName === "string" ? payload.senderDisplayName : null,
    senderEmail: typeof payload.senderEmail === "string" ? payload.senderEmail : null,
    sentAt: typeof payload.sentAt === "string" ? payload.sentAt : "",
    subject: typeof payload.subject === "string" ? payload.subject : "",
    unread: typeof payload.unread === "boolean" ? payload.unread : !isRead,
  };
}

function normalizeMailDetail(payload: Partial<MailDetail> & Record<string, unknown>): MailDetail {
  const attachments = Array.isArray(payload.attachments)
    ? payload.attachments.map((item) => ({
        contentType: typeof item?.contentType === "string" ? item.contentType : undefined,
        fileName: typeof item?.fileName === "string" ? item.fileName : "",
        fileSize: typeof item?.fileSize === "number" ? item.fileSize : 0,
        id: typeof item?.id === "string" ? item.id : "",
      }))
    : [];

  return {
    ...normalizeMailSummary(payload),
    attachments,
    body: typeof payload.body === "string" ? payload.body : "",
  };
}

function normalizeFolderItems(
  payload: MailFolderPagePayload | MailSummary[] | undefined,
): MailSummary[] {
  const items = Array.isArray(payload)
    ? payload
    : Array.isArray(payload?.items)
      ? payload.items
      : [];

  return items.map((item) =>
    normalizeMailSummary(item as Partial<MailSummary> & Record<string, unknown>),
  );
}

function createIdempotencyKey() {
  return (
    globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`
  );
}

export async function listMailFolder(
  folder: string,
  token: string,
  query: Record<string, QueryValue> = {},
) {
  const response = await httpClient.get<ApiResponse<MailFolderPagePayload | MailSummary[]>>(
    "/api/v2/mail/messages",
    {
      token,
      query: { ...query, folder },
    },
  );
  return {
    ...response,
    data: normalizeFolderItems(response.data),
  };
}

export async function readMailDetail(mailId: string, token: string) {
  const threadId = mailId;
  const response = await httpClient.get<ApiResponse<MailDetail>>(
    `/api/v2/mail/threads/${threadId}`,
    { token },
  );
  return {
    ...response,
    data: normalizeMailDetail(response.data as Partial<MailDetail> & Record<string, unknown>),
  };
}

export function listSenderIdentities(token: string) {
  return httpClient.get<ApiResponse<MailSenderIdentity[]>>("/api/v2/mail/contacts", { token });
}

export async function readRecipientTrustState(toEmail: string, fromEmail: string, token: string) {
  const response = await httpClient.get<ApiResponse<MailRecipientTrustApiPayload>>(
    "/api/v2/mail/contacts",
    {
      token,
      query: { capability: "recipient-trust", fromEmail, toEmail },
    },
  );

  const payload = response.data || {};
  const status: RecipientTrustState["status"] = payload.encryptionReady
    ? "ready"
    : payload.deliverable
      ? "warning"
      : "blocked";

  const message = payload.encryptionReady
    ? "Recipient is ready for encrypted delivery."
    : payload.deliverable
      ? "Recipient can receive mail, but encrypted delivery is not fully ready yet."
      : "Recipient is not ready for authenticated encrypted delivery.";

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
      toEmail: payload.toEmail,
    },
  };
}

export function sendMail(payload: SendMailPayload, token: string) {
  return httpClient.post<void>("/api/v2/mail/send", {
    body: {
      ...payload,
      idempotencyKey: payload.idempotencyKey || createIdempotencyKey(),
      labels: payload.labels || [],
    },
    token,
  });
}

export function listMailFolders(token: string) {
  return httpClient.get<ApiResponse<MailFolder[]>>("/api/v2/mail/folders", { token });
}

export function saveMailDraft(payload: MailDraftPayload, token: string) {
  return httpClient.post<ApiResponse<MailDetail>>("/api/v2/mail/drafts", { body: payload, token });
}

export function bulkActionMailMessages(payload: MailBulkActionPayload, token: string) {
  return httpClient.post<ApiResponse<MailSummary[]>>("/api/v2/mail/messages/bulk-action", {
    body: payload,
    token,
  });
}

export function listMailRules(token: string) {
  return httpClient.get<ApiResponse<MailRule[]>>("/api/v2/mail/rules", { token });
}
