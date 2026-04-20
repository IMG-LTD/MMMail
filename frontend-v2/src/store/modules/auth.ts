import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { AuthPayload, MailAddressMode, UserProfile } from '@/shared/types/api'

const STORAGE_KEY = 'mmmail.auth.session.v1'
const DEFAULT_MAIL_ADDRESS_MODE: MailAddressMode = 'PROTON_ADDRESS'

interface PersistedSession {
  accessToken: string
  user: UserProfile
}

function canUseLocalStorage() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

function resolveMailAddressMode(value?: string): MailAddressMode {
  return value === 'EXTERNAL_ACCOUNT' ? 'EXTERNAL_ACCOUNT' : DEFAULT_MAIL_ADDRESS_MODE
}

function normalizeUserProfile(user?: Partial<UserProfile> | null): UserProfile | null {
  if (!user?.displayName || !user.email || !user.id || !user.role) {
    return null
  }

  return {
    displayName: user.displayName,
    email: user.email,
    id: user.id,
    mailAddressMode: resolveMailAddressMode(user.mailAddressMode),
    role: user.role
  }
}

function readPersistedSession(): PersistedSession | null {
  if (!canUseLocalStorage()) {
    return null
  }

  const rawSession = window.localStorage.getItem(STORAGE_KEY)
  if (!rawSession) {
    return null
  }

  try {
    const parsedSession = JSON.parse(rawSession) as Partial<PersistedSession>
    const user = normalizeUserProfile(parsedSession.user)

    if (!parsedSession.accessToken || !user) {
      return null
    }

    return { accessToken: parsedSession.accessToken, user }
  } catch {
    return null
  }
}

function persistSession(payload: PersistedSession | null) {
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
  const restoredSession = readPersistedSession()
  const accessToken = ref(restoredSession?.accessToken || '')
  const refreshToken = ref('')
  const softAuthLocked = ref(false)
  const user = ref<UserProfile | null>(restoredSession?.user || null)
  const needsSessionRefresh = ref(Boolean(restoredSession))

  const isAuthenticated = computed(() => Boolean(accessToken.value) && Boolean(user.value))
  const hasRefreshToken = computed(() => Boolean(refreshToken.value))

  function persistCurrentSession() {
    if (!accessToken.value || !user.value) {
      persistSession(null)
      return
    }

    persistSession({
      accessToken: accessToken.value,
      user: user.value
    })
  }

  function applySession(payload: AuthPayload) {
    const normalizedUser = normalizeUserProfile(payload.user)
    if (!normalizedUser) {
      throw new Error('Invalid auth session payload')
    }

    accessToken.value = payload.accessToken
    refreshToken.value = payload.refreshToken
    softAuthLocked.value = false
    user.value = normalizedUser
    needsSessionRefresh.value = false
    persistCurrentSession()
  }

  function updateUserProfile(profile: Partial<UserProfile>) {
    if (!user.value) {
      return
    }

    const nextUser = normalizeUserProfile({ ...user.value, ...profile })
    if (!nextUser) {
      throw new Error('Invalid user profile update payload')
    }

    user.value = nextUser
    persistCurrentSession()
  }

  function setSoftAuthLocked(value: boolean) {
    softAuthLocked.value = value
  }

  function clearSession() {
    accessToken.value = ''
    refreshToken.value = ''
    softAuthLocked.value = false
    user.value = null
    needsSessionRefresh.value = false
    persistSession(null)
  }

  return {
    accessToken,
    applySession,
    clearSession,
    hasRefreshToken,
    isAuthenticated,
    needsSessionRefresh,
    refreshToken,
    setSoftAuthLocked,
    softAuthLocked,
    updateUserProfile,
    user
  }
})
