function decodeBase64Url(input: string): string {
  if (!input) {
    return ''
  }
  const normalized = input.replace(/-/g, '+').replace(/_/g, '/')
  const padding = normalized.length % 4 === 0 ? '' : '='.repeat(4 - (normalized.length % 4))
  const decoder = globalThis.atob
  if (typeof decoder !== 'function') {
    return ''
  }
  try {
    return decoder(normalized + padding)
  } catch {
    return ''
  }
}

export function resolveSessionIdFromAccessToken(accessToken: string): string {
  if (!accessToken) {
    return ''
  }
  const parts = accessToken.split('.')
  if (parts.length !== 3) {
    return ''
  }
  const payloadText = decodeBase64Url(parts[1])
  if (!payloadText) {
    return ''
  }
  try {
    const payload = JSON.parse(payloadText) as { sessionId?: unknown }
    if (typeof payload.sessionId === 'number') {
      return String(payload.sessionId)
    }
    if (typeof payload.sessionId === 'string') {
      return payload.sessionId
    }
    return ''
  } catch {
    return ''
  }
}
