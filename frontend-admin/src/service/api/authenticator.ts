import { request } from '../request';

export function listAuthenticatorEntries() {
  return request<Api.Authenticator.Entry[]>({ url: '/api/v1/authenticator/entries' });
}

export function createAuthenticatorEntry(data: Api.Authenticator.EntryPayload) {
  return request<Api.Authenticator.Entry>({
    url: '/api/v1/authenticator/entries',
    method: 'post',
    data
  });
}

export function readAuthenticatorEntry(entryId: string) {
  return request<Api.Authenticator.Entry>({
    url: `/api/v1/authenticator/entries/${entryId}`
  });
}

export function updateAuthenticatorEntry(entryId: string, data: Api.Authenticator.EntryPayload) {
  return request<Api.Authenticator.Entry>({
    url: `/api/v1/authenticator/entries/${entryId}`,
    method: 'put',
    data
  });
}

export function deleteAuthenticatorEntry(entryId: string) {
  return request<void>({
    url: `/api/v1/authenticator/entries/${entryId}`,
    method: 'delete'
  });
}

export function generateAuthenticatorCode(entryId: string) {
  return request<Api.Authenticator.Code>({
    url: `/api/v1/authenticator/entries/${entryId}/code`,
    method: 'post'
  });
}

export function readAuthenticatorSecurity() {
  return request<Api.Authenticator.Security>({ url: '/api/v1/authenticator/security' });
}

export function importAuthenticatorEntries(data: Api.Authenticator.ImportPayload) {
  return request<Api.Authenticator.ImportResult>({
    url: '/api/v1/authenticator/import',
    method: 'post',
    data
  });
}

export function exportAuthenticatorEntries() {
  return request<Api.Authenticator.ExportResult>({ url: '/api/v1/authenticator/export' });
}

export function exportAuthenticatorBackup(data: Api.Authenticator.BackupExportPayload) {
  return request<Api.Authenticator.Backup>({
    url: '/api/v1/authenticator/backup/export',
    method: 'post',
    data
  });
}

export function importAuthenticatorBackup(data: Api.Authenticator.BackupImportPayload) {
  return request<Api.Authenticator.ImportResult>({
    url: '/api/v1/authenticator/backup/import',
    method: 'post',
    data
  });
}

export function updateAuthenticatorSecurity(data: Api.Authenticator.SecurityPayload) {
  return request<Api.Authenticator.Security>({
    url: '/api/v1/authenticator/security',
    method: 'put',
    data
  });
}

export function verifyAuthenticatorPin(data: Api.Authenticator.PinPayload) {
  return request<Api.Authenticator.PinVerification>({
    url: '/api/v1/authenticator/security/verify-pin',
    method: 'post',
    data
  });
}

export function importAuthenticatorQrImage(data: Api.Authenticator.QrImagePayload) {
  return request<Api.Authenticator.ImportResult>({
    url: '/api/v1/authenticator/import/qr-image',
    method: 'post',
    data
  });
}
