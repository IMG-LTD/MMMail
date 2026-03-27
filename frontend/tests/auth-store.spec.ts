import { beforeEach, describe, expect, it } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../stores/auth'

const STORAGE_KEY = 'mmmail.auth.session.v1'

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    if (typeof window !== 'undefined' && window.localStorage) {
      window.localStorage.removeItem(STORAGE_KEY)
    }
  })

  it('applies, persists and clears session', () => {
    const store = useAuthStore()
    store.applySession({
      accessToken: 'token-123',
      refreshToken: 'refresh-123',
      user: {
        id: '1',
        email: 'demo@example.com',
        displayName: 'Demo',
        role: 'USER',
        mailAddressMode: 'PROTON_ADDRESS'
      }
    })

    expect(store.isAuthenticated).toBe(true)
    expect(store.needsSessionRefresh).toBe(false)
    expect(store.user?.email).toBe('demo@example.com')
    expect(window.localStorage.getItem(STORAGE_KEY)).toContain('token-123')
    expect(window.localStorage.getItem(STORAGE_KEY)).not.toContain('refresh-123')

    store.clearSession()
    expect(store.isAuthenticated).toBe(false)
    expect(window.localStorage.getItem(STORAGE_KEY)).toBeNull()
  })

  it('hydrates session from localStorage on store bootstrap', () => {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify({
      accessToken: 'token-hydrated',
      user: {
        id: '2',
        email: 'hydrated@example.com',
        displayName: 'Hydrated',
        role: 'USER',
        mailAddressMode: 'EXTERNAL_ACCOUNT'
      }
    }))

    setActivePinia(createPinia())
    const store = useAuthStore()

    expect(store.isAuthenticated).toBe(true)
    expect(store.needsSessionRefresh).toBe(true)
    expect(store.accessToken).toBe('token-hydrated')
    expect(store.user?.email).toBe('hydrated@example.com')
  })

  it('persists user profile updates after hydration', () => {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify({
      accessToken: 'token-hydrated',
      user: {
        id: '2',
        email: 'hydrated@example.com',
        displayName: 'Hydrated',
        role: 'USER',
        mailAddressMode: 'PROTON_ADDRESS'
      }
    }))

    setActivePinia(createPinia())
    const store = useAuthStore()
    store.updateUserProfile({
      displayName: 'Hydrated Next',
      mailAddressMode: 'EXTERNAL_ACCOUNT'
    })

    expect(store.user?.displayName).toBe('Hydrated Next')
    expect(store.user?.mailAddressMode).toBe('EXTERNAL_ACCOUNT')
    expect(window.localStorage.getItem(STORAGE_KEY)).toContain('Hydrated Next')
  })
})
