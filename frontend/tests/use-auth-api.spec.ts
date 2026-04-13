import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const mockPost = vi.fn()
const fetchProfileMock = vi.fn(async () => ({
  displayName: 'Member',
  preferredLocale: 'en',
  mailAddressMode: 'PROTON_ADDRESS',
}))
const setProfileMock = vi.fn()
const applyProfileLocaleMock = vi.fn()
const clearSessionMock = vi.fn()
const orgAccessClearMock = vi.fn()

const authStore = {
  refreshToken: '',
  applySession: vi.fn(),
  updateUserProfile: vi.fn(),
  clearSession: clearSessionMock,
}

vi.mock('~/stores/auth', () => ({
  useAuthStore: () => authStore,
}))

vi.mock('~/stores/org-access', () => ({
  useOrgAccessStore: () => ({
    clear: orgAccessClearMock,
  }),
}))

vi.mock('~/composables/useSettingsApi', () => ({
  useSettingsApi: () => ({
    fetchProfile: fetchProfileMock,
  }),
}))

vi.mock('~/stores/settings', () => ({
  useSettingsStore: () => ({
    setProfile: setProfileMock,
  }),
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    applyProfileLocale: applyProfileLocaleMock,
  }),
}))

describe('useAuthApi', () => {
  beforeEach(() => {
    mockPost.mockReset()
    fetchProfileMock.mockReset().mockResolvedValue({
      displayName: 'Member',
      preferredLocale: 'en',
      mailAddressMode: 'PROTON_ADDRESS',
    })
    setProfileMock.mockReset()
    applyProfileLocaleMock.mockReset()
    authStore.applySession.mockReset()
    authStore.updateUserProfile.mockReset()
    clearSessionMock.mockReset()
    orgAccessClearMock.mockReset()
    authStore.refreshToken = ''
    vi.stubGlobal('useNuxtApp', () => ({
      $apiClient: {
        post: mockPost,
      },
    }))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('clears org access state when refresh fails without a fallback refresh token', async () => {
    mockPost.mockRejectedValueOnce(new Error('expired'))
    const { useAuthApi } = await import('~/composables/useAuthApi')

    await expect(useAuthApi().refreshSession()).resolves.toBe(false)

    expect(clearSessionMock).toHaveBeenCalledTimes(1)
    expect(orgAccessClearMock).toHaveBeenCalledTimes(1)
  })

  it('clears org access state when refresh and fallback refresh both fail', async () => {
    authStore.refreshToken = 'refresh-token'
    mockPost.mockRejectedValueOnce(new Error('expired')).mockRejectedValueOnce(new Error('fallback-expired'))
    const { useAuthApi } = await import('~/composables/useAuthApi')

    await expect(useAuthApi().refreshSession()).resolves.toBe(false)

    expect(clearSessionMock).toHaveBeenCalledTimes(1)
    expect(orgAccessClearMock).toHaveBeenCalledTimes(1)
    expect(mockPost).toHaveBeenCalledTimes(2)
  })
})
