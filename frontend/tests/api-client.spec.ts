import { AxiosHeaders, type InternalAxiosRequestConfig } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const mocked = vi.hoisted(() => {
  function makeClient() {
    const requestHandlers: Array<(request: InternalAxiosRequestConfig) => InternalAxiosRequestConfig> = []
    const responseErrorHandlers: Array<(error: unknown) => unknown> = []
    return {
      requestHandlers,
      responseErrorHandlers,
      interceptors: {
        request: {
          use: vi.fn((handler: (request: InternalAxiosRequestConfig) => InternalAxiosRequestConfig) => {
            requestHandlers.push(handler)
            return 0
          }),
        },
        response: {
          use: vi.fn((_onFulfilled?: unknown, onRejected?: (error: unknown) => unknown) => {
            if (onRejected) {
              responseErrorHandlers.push(onRejected)
            }
            return 0
          }),
        },
      },
      post: vi.fn(),
      request: vi.fn(),
    }
  }

  const clients: ReturnType<typeof makeClient>[] = []
  const queue: ReturnType<typeof makeClient>[] = []

  return {
    authStore: {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      clearSession: vi.fn(),
    },
    orgAccessStore: {
      activeOrgId: 'org-123',
      activeScope: null,
      clear: vi.fn(),
    },
    router: {
      currentRoute: { value: { path: '/', fullPath: '/' } },
      replace: vi.fn(),
    },
    runtimeConfig: {
      public: {
        apiBase: '/api',
        authCsrfCookieName: 'MMMAIL_CSRF_TOKEN',
      },
    },
    clients,
    resetClients() {
      clients.length = 0
      queue.length = 0
      const primaryClient = makeClient()
      const refreshClient = makeClient()
      clients.push(primaryClient, refreshClient)
      queue.push(primaryClient, refreshClient)
    },
    nextClient() {
      const client = queue.shift()
      if (!client) {
        throw new Error('No mock API client prepared')
      }
      return client
    },
  }
})

vi.mock('~/utils/request', () => ({
  createApiClient: () => mocked.nextClient(),
}))

vi.mock('~/stores/auth', () => ({
  useAuthStore: () => mocked.authStore,
}))

vi.mock('~/stores/org-access', () => ({
  useOrgAccessStore: () => mocked.orgAccessStore,
}))

const isAuthExpiredResponseMock = vi.fn((status?: number, code?: unknown) => false)

vi.mock('~/utils/auth-error', () => ({
  isAuthExpiredResponse: (status?: number, code?: unknown) => isAuthExpiredResponseMock(status, code),
}))

vi.mock('~/utils/org-access-recovery', () => ({
  buildMailAddressBlockedQuery: () => ({}),
  buildTwoFactorBlockedQuery: () => ({}),
  ACCOUNT_MAIL_ADDRESS_REQUIRED_CODE: 4701,
  ORG_TWO_FACTOR_REQUIRED_CODE: 4702,
}))

vi.mock('~/utils/org-product-access', () => ({
  resolveProductKeyFromApiPath: () => null,
  resolveProductKeyFromPath: () => null,
}))

async function loadPlugin() {
  vi.resetModules()
  mocked.resetClients()
  const { default: plugin } = await import('~/plugins/api-client')
  return plugin as () => unknown
}

function runPrimaryRequestInterceptor(request: Partial<InternalAxiosRequestConfig>) {
  const interceptor = mocked.clients[0].requestHandlers[0]
  if (!interceptor) {
    throw new Error('Primary request interceptor was not registered')
  }
  return interceptor({ headers: {}, method: 'get', ...request } as InternalAxiosRequestConfig)
}

async function runPrimaryResponseErrorInterceptor(error: unknown) {
  const interceptor = mocked.clients[0].responseErrorHandlers[0]
  if (!interceptor) {
    throw new Error('Primary response error interceptor was not registered')
  }
  return interceptor(error)
}

describe('api client org scope headers', () => {
  beforeEach(() => {
    mocked.authStore.accessToken = 'access-token'
    mocked.authStore.refreshToken = 'refresh-token'
    mocked.authStore.clearSession.mockReset()
    mocked.orgAccessStore.activeOrgId = 'org-123'
    mocked.orgAccessStore.activeScope = null
    mocked.orgAccessStore.clear.mockReset()
    mocked.router.currentRoute.value = { path: '/', fullPath: '/' }
    mocked.router.replace.mockReset()
    isAuthExpiredResponseMock.mockReset()
    isAuthExpiredResponseMock.mockReturnValue(false)
    vi.stubGlobal('defineNuxtPlugin', (plugin: unknown) => plugin)
    vi.stubGlobal('useRuntimeConfig', () => mocked.runtimeConfig)
    vi.stubGlobal('useRouter', () => mocked.router)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it.each([
    '/api/v1/auth/refresh',
    '/api/v1/auth/login',
    '/api/v1/auth/logout',
  ])('omits org scope headers on auth requests for %s', async (url) => {
    const plugin = await loadPlugin()

    plugin()

    const request = runPrimaryRequestInterceptor({
      url,
      method: 'post',
    })
    const headers = AxiosHeaders.from(request.headers || {})

    expect(headers.get('Authorization')).toBe('Bearer access-token')
    expect(headers.get('X-MMMAIL-ORG-ID')).toBeUndefined()
  })

  it('keeps org scope headers on non-auth requests', async () => {
    const plugin = await loadPlugin()

    plugin()

    const request = runPrimaryRequestInterceptor({
      url: '/api/v1/mails',
      method: 'get',
    })
    const headers = AxiosHeaders.from(request.headers || {})

    expect(headers.get('Authorization')).toBe('Bearer access-token')
    expect(headers.get('X-MMMAIL-ORG-ID')).toBe('org-123')
  })

  it('clears org access state when auth expires and refresh recovery fails', async () => {
    const plugin = await loadPlugin()
    mocked.clients[1].post.mockRejectedValueOnce(new Error('expired'))
    isAuthExpiredResponseMock.mockReturnValue(true)

    plugin()

    const error = {
      status: 401,
      code: 'UNAUTHORIZED',
      config: {
        url: '/api/v1/mails',
        method: 'get',
        headers: {},
      },
      message: 'Request failed with status code 401',
    }

    await expect(runPrimaryResponseErrorInterceptor(error)).rejects.toBe(error)

    expect(mocked.clients[1].post).toHaveBeenCalledWith('/api/v1/auth/refresh', { refreshToken: 'refresh-token' })
    expect(mocked.authStore.clearSession).toHaveBeenCalled()
    expect(mocked.orgAccessStore.clear).toHaveBeenCalledTimes(1)
  })
})
