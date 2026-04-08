import { decrypt, readMessage } from 'openpgp'

const DEFAULT_PUBLIC_ATTACHMENT_CONTENT_TYPE = 'application/octet-stream'

export function resolveMailPublicSecureLinkErrorKey(message: string): string | null {
  if (message.includes('Mail secure link is not found')) {
    return 'mailPublicShare.errors.notFound'
  }
  if (message.includes('Mail secure link has been revoked')) {
    return 'mailPublicShare.errors.revoked'
  }
  if (message.includes('Mail secure link has expired')) {
    return 'mailPublicShare.errors.expired'
  }
  return null
}

export function formatMailPublicTime(value: string | null | undefined): string {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}

export function requireMailPublicPassword(password: string): string {
  const normalized = password.trim()
  if (!normalized) {
    throw new Error('mailPublicShare.messages.passwordRequired')
  }
  return normalized
}

export async function decryptMailPublicBody(ciphertext: string, password: string): Promise<string> {
  const message = await readMessage({ armoredMessage: ciphertext })
  const result = await decrypt({
    message,
    passwords: [password],
    format: 'utf8'
  })
  return String(result.data || '')
}

export async function decryptMailPublicAttachmentBlob(
  encryptedBlob: Blob,
  contentType: string | null | undefined,
  password: string
): Promise<Blob> {
  const message = await readMessage({ binaryMessage: new Uint8Array(await encryptedBlob.arrayBuffer()) })
  const result = await decrypt({
    message,
    passwords: [password],
    format: 'binary'
  })
  return new Blob([toArrayBuffer(result.data)], { type: contentType || DEFAULT_PUBLIC_ATTACHMENT_CONTENT_TYPE })
}

export function triggerMailPublicDownload(blob: Blob, fileName: string): void {
  const downloadUrl = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = downloadUrl
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(downloadUrl)
}

function toArrayBuffer(value: unknown): ArrayBuffer {
  if (value instanceof Uint8Array) {
    return Uint8Array.from(value).buffer
  }
  throw new Error(`Unsupported OpenPGP binary payload type: ${Object.prototype.toString.call(value)}`)
}
