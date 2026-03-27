const AUTH_EXPIRED_CODES = new Set([10002, 10003, 20004])

export function isAuthExpiredResponse(status?: number, code?: number): boolean {
  if (status === 401) {
    return true
  }
  if (status === 400 && code === 20004) {
    return true
  }
  return status === 403 && (code == null || AUTH_EXPIRED_CODES.has(code))
}
