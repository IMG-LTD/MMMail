import type { AuthenticatorImportFormat } from '~/types/api'

const JSON_MIME_TYPE = 'application/json;charset=utf-8'
const BACKUP_MIME_TYPE = 'application/octet-stream;charset=utf-8'

export function normalizeAuthenticatorPortabilityContent(value: string): string {
  return value.replace(/\r\n/g, '\n').trim()
}

export function normalizeAuthenticatorImportFormat(value: string | null | undefined): AuthenticatorImportFormat {
  if (value === 'OTPAUTH_URI' || value === 'MMMAIL_JSON') {
    return value
  }
  return 'AUTO'
}

export function buildAuthenticatorDownloadMimeType(kind: 'export' | 'backup'): string {
  return kind === 'backup' ? BACKUP_MIME_TYPE : JSON_MIME_TYPE
}

export function isBackupPassphraseConfirmed(passphrase: string, confirm: string): boolean {
  return passphrase.trim().length >= 8 && passphrase === confirm
}

export async function readAuthenticatorPortabilityFile(file: File): Promise<string> {
  const content = await file.text()
  return normalizeAuthenticatorPortabilityContent(content)
}

export async function readAuthenticatorQrImageFile(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      if (typeof reader.result !== 'string') {
        reject(new Error('Failed to read authenticator QR image'))
        return
      }
      resolve(reader.result)
    }
    reader.onerror = () => {
      reject(reader.error ?? new Error('Failed to read authenticator QR image'))
    }
    reader.readAsDataURL(file)
  })
}
