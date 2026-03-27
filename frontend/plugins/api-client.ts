import { AxiosHeaders, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import type { ApiResponse, AuthPayload } from '~/types/api'
import { createApiClient } from '~/utils/request'
import type { ApiClientError } from '~/utils/request'
import { isAuthExpiredResponse } from '~/utils/auth-error'
import { useAuthStore } from '~/stores/auth'
import { useOrgAccessStore } from '~/stores/org-access'
import {
  ACCOUNT_MAIL_ADDRESS_REQUIRED_CODE,
  buildMailAddressBlockedQuery,
  buildTwoFactorBlockedQuery,
  ORG_TWO_FACTOR_REQUIRED_CODE
} from '~/utils/org-access-recovery'
import { resolveProductKeyFromApiPath, resolveProductKeyFromPath } from '~/utils/org-product-access'

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  __retried?: boolean
}

const REDIRECT_BYPASS_PATHS = new Set(['/authenticator', '/product-access-blocked'])

export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()
  const authStore = useAuthStore()
  const orgAccessStore = useOrgAccessStore()
  const router = useRouter()
  const client = createApiClient(config.public.apiBase) as AxiosInstance
  const refreshClient = createApiClient(config.public.apiBase) as AxiosInstance
  let refreshPromise: Promise<boolean> | null = null
  const csrfCookieName = String(config.public.authCsrfCookieName || 'MMMAIL_CSRF_TOKEN')

  function readCookie(name: string): string {
    if (typeof document === 'undefined') {
      return ''
    }
    const escapedName = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const matcher = new RegExp(`(?:^|; )${escapedName}=([^;]*)`)
    const matched = document.cookie.match(matcher)
    return matched ? decodeURIComponent(matched[1]) : ''
  }

  function withRequestHeaders(
    request: InternalAxiosRequestConfig,
    options: { includeAccessToken: boolean }
  ): InternalAxiosRequestConfig {
    const headers = AxiosHeaders.from(request.headers || {})

    if (options.includeAccessToken && authStore.accessToken) {
      headers.set('Authorization', `Bearer ${authStore.accessToken}`)
    }

    if (options.includeAccessToken && orgAccessStore.activeOrgId) {
      headers.set('X-MMMAIL-ORG-ID', orgAccessStore.activeOrgId)
    }

    const method = String(request.method || 'get').toLowerCase()
    if (['post', 'put', 'patch', 'delete'].includes(method)) {
      const csrfToken = readCookie(csrfCookieName)
      if (csrfToken) {
        headers.set('X-MMMAIL-CSRF', csrfToken)
      }
    }

    request.headers = headers
    return request
  }

  client.interceptors.request.use((request) => withRequestHeaders(request, { includeAccessToken: true }))
  refreshClient.interceptors.request.use((request) => withRequestHeaders(request, { includeAccessToken: false }))

  async function redirectToTwoFactorBlocked(requestConfig?: RetryableRequestConfig): Promise<void> {
    const currentRoute = router.currentRoute.value
    if (!orgAccessStore.activeOrgId || REDIRECT_BYPASS_PATHS.has(currentRoute.path)) {
      return
    }

    const productKey = resolveProductKeyFromPath(currentRoute.path) || resolveProductKeyFromApiPath(resolveRequestPath(requestConfig?.url))
    if (!productKey) {
      return
    }

    await router.replace({
      path: '/product-access-blocked',
      query: buildTwoFactorBlockedQuery({
        from: currentRoute.fullPath,
        orgId: orgAccessStore.activeOrgId,
        orgName: orgAccessStore.activeScope?.orgName || '',
        productKey
      })
    })
  }

  async function redirectToMailAddressBlocked(requestConfig?: RetryableRequestConfig): Promise<void> {
    const currentRoute = router.currentRoute.value
    if (REDIRECT_BYPASS_PATHS.has(currentRoute.path)) {
      return
    }

    const productKey = resolveProductKeyFromPath(currentRoute.path) || resolveProductKeyFromApiPath(resolveRequestPath(requestConfig?.url))
    if (!productKey) {
      return
    }

    await router.replace({
      path: '/product-access-blocked',
      query: buildMailAddressBlockedQuery({
        from: currentRoute.fullPath,
        productKey
      })
    })
  }

  async function refreshAccessToken(): Promise<boolean> {
    if (!refreshPromise) {
      const currentRefreshToken = authStore.refreshToken
      const payload = currentRefreshToken ? { refreshToken: currentRefreshToken } : {}
      refreshPromise = refreshClient.post<ApiResponse<AuthPayload>>('/api/v1/auth/refresh', payload)
        .then((response) => {
          authStore.applySession(response.data.data)
          return true
        })
        .catch(() => {
          authStore.clearSession()
          return false
        })
        .finally(() => {
          refreshPromise = null
        })
    }

    return refreshPromise
  }

  client.interceptors.response.use(
    (response) => response,
    async (error: unknown) => {
      const normalizedError = error as ApiClientError
      const status = normalizedError.status
      const authExpired = isAuthExpiredResponse(status, normalizedError.code)
      const requestConfig = normalizedError.config as RetryableRequestConfig | undefined
      const isRefreshRequest = Boolean(requestConfig?.url?.includes('/api/v1/auth/refresh'))

      if (normalizedError.code === ORG_TWO_FACTOR_REQUIRED_CODE && !isRefreshRequest) {
        await redirectToTwoFactorBlocked(requestConfig)
      }
      if (normalizedError.code === ACCOUNT_MAIL_ADDRESS_REQUIRED_CODE && !isRefreshRequest) {
        await redirectToMailAddressBlocked(requestConfig)
      }

      if (
        authExpired
        && requestConfig
        && !requestConfig.__retried
        && !isRefreshRequest
      ) {
        requestConfig.__retried = true
        const refreshed = await refreshAccessToken()
        if (refreshed && authStore.accessToken) {
          const headers = AxiosHeaders.from(requestConfig.headers || {})
          headers.set('Authorization', `Bearer ${authStore.accessToken}`)
          requestConfig.headers = headers
          return client.request(requestConfig)
        }
      }

      if (authExpired || (error instanceof Error && error.message.toLowerCase().includes('unauthorized'))) {
        authStore.clearSession()
      }
      return Promise.reject(error)
    }
  )

  return {
    provide: {
      apiClient: client
    }
  }
})

function resolveRequestPath(url?: string): string {
  if (!url) {
    return ''
  }
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return new URL(url).pathname
  }
  return url
}
