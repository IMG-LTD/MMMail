import { request } from '../request';

const MAIL_FOLDER_ACTIONS = {
  archive: 'MOVE_ARCHIVE',
  inbox: 'MOVE_INBOX',
  spam: 'MOVE_SPAM',
  trash: 'MOVE_TRASH'
} as const;

export function listMailFolders() {
  return request<Api.Mail.Folder[]>({ url: '/api/v2/mail/folders' });
}

export function listMailLabels() {
  return request<Api.Mail.Label[]>({ url: '/api/v1/labels' });
}

export function listMailMessages(params: Record<string, string | number | boolean | undefined>) {
  return request<Api.Mail.MessagePage>({ url: '/api/v2/mail/messages', params });
}

export function readMailMessage(messageId: string) {
  return request<Api.Mail.MessageDetail>({ url: `/api/v2/mail/threads/${messageId}` });
}

export function sendMailMessage(data: Api.Mail.SendPayload) {
  return request<void>({
    url: '/api/v2/mail/send',
    method: 'post',
    data
  });
}

export function saveMailDraft(data: Api.Mail.DraftPayload) {
  return request<Api.Mail.MessageDetail>({
    url: '/api/v2/mail/drafts',
    method: 'post',
    data
  });
}

export function bulkActionMailMessages(data: Api.Mail.BulkActionPayload) {
  return request<Api.Mail.MessageSummary[]>({
    url: '/api/v2/mail/messages/bulk-action',
    method: 'post',
    data
  });
}

export function moveMailMessagesToFolder(messageIds: string[], folderKey: string) {
  const action = MAIL_FOLDER_ACTIONS[folderKey.toLowerCase() as keyof typeof MAIL_FOLDER_ACTIONS];

  if (!action) {
    throw new Error(`Unsupported mail drag folder: ${folderKey}`);
  }

  return bulkActionMailMessages({ action, messageIds });
}

export function updateMailLabels(messageId: string, data: Api.Mail.UpdateLabelsPayload) {
  return request<void>({
    url: `/api/v1/mails/${messageId}/labels`,
    method: 'put',
    data
  });
}

export function listMailExternalAccounts() {
  return request<Api.Mail.ExternalAccount[]>({ url: '/api/v1/mail/external-accounts' });
}

export function createMailExternalAccount(data: Api.Mail.ExternalAccountPayload) {
  return request<Api.Mail.ExternalAccount>({
    url: '/api/v1/mail/external-accounts',
    method: 'post',
    data
  });
}

export function readMailExternalAccount(accountId: string) {
  return request<Api.Mail.ExternalAccount>({ url: `/api/v1/mail/external-accounts/${accountId}` });
}

export function updateMailExternalAccount(accountId: string, data: Api.Mail.ExternalAccountPayload) {
  return request<Api.Mail.ExternalAccount>({
    url: `/api/v1/mail/external-accounts/${accountId}`,
    method: 'patch',
    data
  });
}

export function deleteMailExternalAccount(accountId: string) {
  return request<void>({
    url: `/api/v1/mail/external-accounts/${accountId}`,
    method: 'delete'
  });
}

export function testMailExternalAccount(accountId: string) {
  return request<Api.Mail.ExternalAccountTest>({
    url: `/api/v1/mail/external-accounts/${accountId}/test`,
    method: 'post'
  });
}

export function syncMailExternalAccount(accountId: string) {
  return request<Api.Mail.ExternalAccountSync>({
    url: `/api/v1/mail/external-accounts/${accountId}/sync`,
    method: 'post'
  });
}
