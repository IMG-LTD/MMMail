import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const authState = {
  isAuthenticated: false,
  needsSessionRefresh: false,
}

const refreshSessionMock = vi.fn(async () => false)
const navigateToMock = vi.fn(async (to: string) => to)
const defineNuxtRouteMiddlewareMock = vi.fn((middleware: unknown) => middleware)

vi.mock('~/stores/auth', () => ({
  useAuthStore: () => authState,
}))

vi.mock('~/composables/useAuthApi', () => ({
  useAuthApi: () => ({ refreshSession: refreshSessionMock }),
}))

const loadMiddleware = async () => {
  vi.resetModules()
  const { default: middleware } = await import('~/middleware/auth.global')
  return middleware as (to: { path: string }) => Promise<unknown>
}

describe('auth global middleware', () => {
  beforeEach(() => {
    authState.isAuthenticated = false
    authState.needsSessionRefresh = false
    refreshSessionMock.mockReset().mockResolvedValue(false)
    navigateToMock.mockReset().mockImplementation(async (to: string) => to)
    defineNuxtRouteMiddlewareMock.mockReset().mockImplementation((middleware: unknown) => middleware)
    vi.stubGlobal('defineNuxtRouteMiddleware', defineNuxtRouteMiddlewareMock)
    vi.stubGlobal('navigateTo', navigateToMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('allows signed-out visitors to access / without refreshing the session', async () => {
    const middleware = await loadMiddleware()

    await middleware({ path: '/' })

    expect(refreshSessionMock).not.toHaveBeenCalled()
    expect(navigateToMock).not.toHaveBeenCalled()
  })

  it('redirects signed-out visitors on protected routes when session refresh fails', async () => {
    const middleware = await loadMiddleware()

    await expect(middleware({ path: '/inbox' })).resolves.toBe('/login')
    expect(refreshSessionMock).toHaveBeenCalledTimes(1)
    expect(navigateToMock).toHaveBeenCalledWith('/login')
  })
})
