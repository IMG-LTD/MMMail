import type { CreateClientErrorEventRequest } from '~/types/system'

const CATEGORY_WINDOW_ERROR = 'WINDOW_ERROR'
const CATEGORY_UNHANDLED_REJECTION = 'UNHANDLED_REJECTION'
const DEFAULT_METHOD = 'GET'
const DEFAULT_MESSAGE = 'Unhandled client runtime error'

export interface RuntimeErrorTrackingTarget {
  addEventListener(type: string, listener: (event: unknown) => void): void
  removeEventListener(type: string, listener: (event: unknown) => void): void
}

export interface RuntimeErrorTrackingOptions {
  target: RuntimeErrorTrackingTarget
  enabled: () => boolean
  resolvePath: () => string
  report: (payload: CreateClientErrorEventRequest) => Promise<void> | void
}

export function setupRuntimeErrorTracking(options: RuntimeErrorTrackingOptions): () => void {
  const onError = (event: unknown) => {
    if (!options.enabled()) {
      return
    }
    void reportSafely(options, buildWindowErrorPayload(event, options.resolvePath()))
  }
  const onUnhandledRejection = (event: unknown) => {
    if (!options.enabled()) {
      return
    }
    void reportSafely(options, buildUnhandledRejectionPayload(event, options.resolvePath()))
  }

  options.target.addEventListener('error', onError)
  options.target.addEventListener('unhandledrejection', onUnhandledRejection)
  return () => {
    options.target.removeEventListener('error', onError)
    options.target.removeEventListener('unhandledrejection', onUnhandledRejection)
  }
}

export function buildWindowErrorPayload(event: unknown, path: string): CreateClientErrorEventRequest {
  const error = extractErrorLike((event as { error?: unknown } | undefined)?.error ?? event)
  return {
    message: error.message,
    category: CATEGORY_WINDOW_ERROR,
    severity: 'ERROR',
    detail: error.detail,
    path,
    method: DEFAULT_METHOD,
    requestId: error.requestId,
  }
}

export function buildUnhandledRejectionPayload(event: unknown, path: string): CreateClientErrorEventRequest {
  const reason = (event as { reason?: unknown } | undefined)?.reason ?? event
  const error = extractErrorLike(reason)
  return {
    message: error.message,
    category: CATEGORY_UNHANDLED_REJECTION,
    severity: 'ERROR',
    detail: error.detail,
    path,
    method: DEFAULT_METHOD,
    requestId: error.requestId,
  }
}

async function reportSafely(
  options: RuntimeErrorTrackingOptions,
  payload: CreateClientErrorEventRequest,
): Promise<void> {
  try {
    await options.report(payload)
  } catch (error) {
    console.error('Failed to report client runtime error', error, payload)
  }
}

function extractErrorLike(source: unknown): {
  message: string
  detail?: string
  requestId?: string
} {
  if (source instanceof Error) {
    return {
      message: source.message || DEFAULT_MESSAGE,
      detail: source.stack || source.message || DEFAULT_MESSAGE,
      requestId: readRequestId(source),
    }
  }
  if (typeof source === 'string') {
    return {
      message: source || DEFAULT_MESSAGE,
      detail: source || DEFAULT_MESSAGE,
    }
  }
  return {
    message: readMessage(source),
    detail: stringifyDetail(source),
    requestId: readRequestId(source),
  }
}

function readMessage(source: unknown): string {
  if (typeof source === 'object' && source && 'message' in source && typeof source.message === 'string') {
    return source.message || DEFAULT_MESSAGE
  }
  return DEFAULT_MESSAGE
}

function readRequestId(source: unknown): string | undefined {
  if (typeof source === 'object' && source && 'requestId' in source && typeof source.requestId === 'string') {
    return source.requestId
  }
  return undefined
}

function stringifyDetail(source: unknown): string | undefined {
  if (source == null) {
    return undefined
  }
  if (typeof source === 'string') {
    return source
  }
  try {
    return JSON.stringify(source)
  } catch {
    return String(source)
  }
}
