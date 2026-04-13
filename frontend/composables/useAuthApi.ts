import type { ApiResponse, AuthPayload, UserSession } from '~/types/api'
import { useAuthStore } from '~/stores/auth'
import { useSettingsApi } from '~/composables/useSettingsApi'
import { useI18n } from '~/composables/useI18n'
import { useSettingsStore } from '~/stores/settings'
import { useOrgAccessStore } from '~/stores/org-access'

export function useAuthApi() {
  const { $apiClient } = useNuxtApp()
  const authStore = useAuthStore()
  const orgAccessStore = useOrgAccessStore()
  const settingsStore = useSettingsStore()
  const { fetchProfile } = useSettingsApi()
  const { applyProfileLocale } = useI18n()

  async function syncProfilePreference(): Promise<void> {
    try {
      const profile = await fetchProfile()
      settingsStore.setProfile(profile)
      authStore.updateUserProfile({
        displayName: profile.displayName,
        mailAddressMode: profile.mailAddressMode
      })
      applyProfileLocale(profile.preferredLocale)
    } catch (error) {
      console.error('Failed to sync profile preference after auth', error)
    }
  }

  async function login(email: string, password: string): Promise<void> {
    const payload = { email, password }
    const response = await $apiClient.post<ApiResponse<AuthPayload>>('/api/v1/auth/login', payload)
    authStore.applySession(response.data.data)
    await syncProfilePreference()
  }

  async function register(email: string, password: string, displayName: string): Promise<void> {
    const payload = { email, password, displayName }
    const response = await $apiClient.post<ApiResponse<AuthPayload>>('/api/v1/auth/register', payload)
    authStore.applySession(response.data.data)
    await syncProfilePreference()
  }

  async function refreshSession(): Promise<boolean> {
    try {
      const response = await $apiClient.post<ApiResponse<AuthPayload>>('/api/v1/auth/refresh', {})
      authStore.applySession(response.data.data)
      await syncProfilePreference()
      return true
    } catch {
      if (!authStore.refreshToken) {
        orgAccessStore.clear()
        authStore.clearSession()
        return false
      }
      try {
        const fallbackResponse = await $apiClient.post<ApiResponse<AuthPayload>>('/api/v1/auth/refresh', {
          refreshToken: authStore.refreshToken
        })
        authStore.applySession(fallbackResponse.data.data)
        await syncProfilePreference()
        return true
      } catch {
        orgAccessStore.clear()
        authStore.clearSession()
        return false
      }
    }
  }

  async function logoutAll(): Promise<void> {
    await $apiClient.post('/api/v1/auth/logout-all')
    authStore.clearSession()
  }

  async function listSessions(): Promise<UserSession[]> {
    const response = await $apiClient.get<ApiResponse<UserSession[]>>('/api/v1/auth/sessions')
    return response.data.data
  }

  async function revokeSession(sessionId: string): Promise<void> {
    await $apiClient.post(`/api/v1/auth/sessions/${sessionId}/revoke`)
  }

  return {
    login,
    register,
    refreshSession,
    logoutAll,
    listSessions,
    revokeSession
  }
}
