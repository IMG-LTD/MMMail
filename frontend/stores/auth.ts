import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { AuthPayload, MailAddressMode, UserProfile } from '~/types/api'

const STORAGE_KEY = 'mmmail.auth.session.v1'
const DEFAULT_MAIL_ADDRESS_MODE: MailAddressMode = 'PROTON_ADDRESS'

interface PersistedSession {
  accessToken: string
  user: UserProfile
}

function resolveMailAddressMode(value?: string): MailAddressMode {
  return value === 'EXTERNAL_ACCOUNT' ? 'EXTERNAL_ACCOUNT' : DEFAULT_MAIL_ADDRESS_MODE
}

function normalizeUserProfile(user?: Partial<UserProfile> | null): UserProfile | null {
  if (!user?.email || !user.id || !user.displayName || !user.role) {
    return null
  }
  return {
    id: user.id,
    email: user.email,
    displayName: user.displayName,
    role: user.role,
    mailAddressMode: resolveMailAddressMode(user.mailAddressMode)
  }
}

function canUseLocalStorage(): boolean {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

function readPersistedSession(): PersistedSession | null {
  if (!canUseLocalStorage()) {
    return null
  }
  const raw = window.localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return null
  }
  try {
    const parsed = JSON.parse(raw) as Partial<PersistedSession>
    const user = normalizeUserProfile(parsed.user)
    if (!parsed.accessToken || !user) {
      return null
    }
    return {
      accessToken: parsed.accessToken,
      user
    }
  } catch {
    return null
  }
}

function persistSession(payload: PersistedSession | null): void {
  if (!canUseLocalStorage()) {
    return
  }
  if (!payload) {
    window.localStorage.removeItem(STORAGE_KEY)
    return
  }
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string>('')
  const refreshToken = ref<string>('')
  const user = ref<UserProfile | null>(null)
  const needsSessionRefresh = ref(false)

  const restored = readPersistedSession()
  if (restored) {
    accessToken.value = restored.accessToken
    user.value = restored.user
    needsSessionRefresh.value = true
  }

  const isAuthenticated = computed(() => Boolean(accessToken.value) && Boolean(user.value))
  const hasRefreshToken = computed(() => Boolean(refreshToken.value))

  function persistCurrentSession(): void {
    if (!accessToken.value || !user.value) {
      persistSession(null)
      return
    }
    persistSession({
      accessToken: accessToken.value,
      user: user.value
    })
  }

  function applySession(payload: AuthPayload): void {
    const userProfile = normalizeUserProfile(payload.user)
    if (!userProfile) {
      throw new Error('Invalid auth session payload')
    }
    accessToken.value = payload.accessToken
    refreshToken.value = payload.refreshToken
    user.value = userProfile
    needsSessionRefresh.value = false
    persistCurrentSession()
  }

  function updateUserProfile(profile: Partial<UserProfile>): void {
    if (!user.value) {
      return
    }
    const nextUser = normalizeUserProfile({
      ...user.value,
      ...profile
    })
    if (!nextUser) {
      throw new Error('Invalid user profile update payload')
    }
    user.value = nextUser
    persistCurrentSession()
  }

  function clearSession(): void {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    needsSessionRefresh.value = false
    persistSession(null)
  }

  return {
    accessToken,
    refreshToken,
    user,
    needsSessionRefresh,
    isAuthenticated,
    hasRefreshToken,
    applySession,
    updateUserProfile,
    clearSession
  }
})
