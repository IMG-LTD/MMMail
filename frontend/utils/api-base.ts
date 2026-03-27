const LOCAL_HOSTS = new Set(['127.0.0.1', 'localhost'])

export function resolveApiBase(apiBase: string, locationHostname = readLocationHostname()): string {
  const normalizedBase = new URL(apiBase).toString()
  const baseUrl = new URL(normalizedBase)
  if (!LOCAL_HOSTS.has(baseUrl.hostname) || !LOCAL_HOSTS.has(locationHostname) || baseUrl.hostname === locationHostname) {
    return normalizedBase
  }
  baseUrl.hostname = locationHostname
  return baseUrl.toString()
}

function readLocationHostname(): string {
  if (typeof window === 'undefined') {
    return ''
  }
  return window.location.hostname
}
