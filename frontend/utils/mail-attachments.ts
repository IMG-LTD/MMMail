import type { MailAttachment } from '~/types/api'

export const MAX_MAIL_ATTACHMENT_BYTES = 20 * 1024 * 1024

const BLOCKED_EXTENSIONS = new Set([
  'apk', 'app', 'bat', 'cmd', 'com', 'cpl', 'dll', 'exe', 'hta',
  'jar', 'js', 'jse', 'lnk', 'msi', 'msix', 'pif', 'ps1', 'reg',
  'scr', 'sh', 'vb', 'vbe', 'vbs', 'ws', 'wsc', 'wsf', 'wsh'
])

export interface UploadableMailAttachment {
  name: string
  size: number
}

export function validateMailAttachmentFile(file: UploadableMailAttachment): void {
  const normalizedName = file.name.trim()
  if (!normalizedName) {
    throw new Error('Attachment file name is required')
  }
  if (file.size <= 0) {
    throw new Error('Attachment file is empty')
  }
  if (file.size > MAX_MAIL_ATTACHMENT_BYTES) {
    throw new Error('Attachment exceeds 20MB limit')
  }
  const extension = resolveFileExtension(normalizedName)
  if (extension && BLOCKED_EXTENSIONS.has(extension)) {
    throw new Error('Attachment type is not allowed')
  }
}

export function formatMailAttachmentSize(size: number): string {
  if (size >= 1024 * 1024) {
    return `${(size / (1024 * 1024)).toFixed(1)} MB`
  }
  if (size >= 1024) {
    return `${Math.round(size / 1024)} KB`
  }
  return `${size} B`
}

export function upsertMailAttachment(list: MailAttachment[], attachment: MailAttachment): MailAttachment[] {
  const next = list.filter(item => item.id !== attachment.id)
  next.push(attachment)
  return next
}

export function buildMailAttachmentFailureId(file: UploadableMailAttachment): string {
  return `${file.name}:${file.size}`
}

function resolveFileExtension(fileName: string): string {
  const dotIndex = fileName.lastIndexOf('.')
  if (dotIndex < 0 || dotIndex === fileName.length - 1) {
    return ''
  }
  return fileName.slice(dotIndex + 1).toLowerCase()
}
