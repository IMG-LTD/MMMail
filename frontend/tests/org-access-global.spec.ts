import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const authState = {
  isAuthenticated: false,
  needsSessionRefresh: false,
  user: null,
}

const clearMock = vi.fn()
const ensureLoadedMock = vi.fn(async () => undefined)
const navigateToMock = vi.fn(async (to: unknown) => to)
const defineNuxtRouteMiddlewareMock = vi.fn((middleware: unknown) => middleware)

vi.mock('~/stores/auth', () => ({
  useAuthStore: () => authState,
}))

vi.mock('~/stores/org-access', () => ({
  useOrgAccessStore: () => ({
    clear: clearMock,
    ensureLoaded: ensureLoadedMock,
    isProductEnabled: () => true,
    activeOrgId: '',
    activeScope: null,
  }),
}))

const loadMiddleware = async () => {
  vi.resetModules()
  const { default: middleware } = await import('~/middleware/org-access.global')
  return middleware as (to: { path: string; fullPath: string }) => Promise<unknown>
}

describe('org access global middleware', () => {
  beforeEach(() => {
    authState.isAuthenticated = false
    authState.needsSessionRefresh = false
    authState.user = null
    clearMock.mockReset()
    ensureLoadedMock.mockReset().mockResolvedValue(undefined)
    navigateToMock.mockReset().mockImplementation(async (to: unknown) => to)
    defineNuxtRouteMiddlewareMock.mockReset().mockImplementation((middleware: unknown) => middleware)
    vi.stubGlobal('defineNuxtRouteMiddleware', defineNuxtRouteMiddlewareMock)
    vi.stubGlobal('navigateTo', navigateToMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it.each([
    { path: '/', fullPath: '/' },
    { path: '/login', fullPath: '/login' },
    { path: '/register', fullPath: '/register' },
    { path: '/boundary', fullPath: '/boundary' },
    { path: '/product-access-blocked', fullPath: '/product-access-blocked?productKey=DOCS' },
    { path: '/share/pass/public-token', fullPath: '/share/pass/public-token' },
  ])('clears stale org access state for signed-out visitors on $path without redirecting', async (to) => {
    const middleware = await loadMiddleware()

    await middleware(to)

    expect(clearMock).toHaveBeenCalledTimes(1)
    expect(ensureLoadedMock).not.toHaveBeenCalled()
    expect(navigateToMock).not.toHaveBeenCalled()
  })

  it('lets restored sessions stay on / so the landing page can refresh first', async () => {
    authState.isAuthenticated = true
    authState.needsSessionRefresh = true
    const middleware = await loadMiddleware()

    await middleware({ path: '/', fullPath: '/' })

    expect(clearMock).not.toHaveBeenCalled()
    expect(ensureLoadedMock).not.toHaveBeenCalled()
    expect(navigateToMock).not.toHaveBeenCalled()
  })
})
