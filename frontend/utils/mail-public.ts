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
