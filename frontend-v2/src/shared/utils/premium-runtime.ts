import { isHttpRequestError } from '@/service/request/http'

const PREMIUM_STATUS = 403
const PREMIUM_ERROR_PATTERNS = [/entitlement/i, /premium/i, /not supported/i]

export function isPremiumGateError(error: unknown) {
  return isHttpRequestError(error, PREMIUM_STATUS)
    && PREMIUM_ERROR_PATTERNS.some(pattern => pattern.test(error.message))
}

export function resolvePremiumNotice(error: unknown, notice: string) {
  return isPremiumGateError(error)
    ? notice
    : error instanceof Error && error.message
      ? error.message
      : notice
}

export function resolveOptionalRuntimeNotice(results: readonly PromiseSettledResult<unknown>[], notice: string) {
  const rejectedResults = results.filter(isRejectedResult)
  if (!rejectedResults.length) return ''
  const runtimeError = rejectedResults.find(result => !isPremiumGateError(result.reason))
  return runtimeError ? resolvePremiumNotice(runtimeError.reason, notice) : notice
}

function isRejectedResult(result: PromiseSettledResult<unknown>): result is PromiseRejectedResult {
  return result.status === 'rejected'
}
