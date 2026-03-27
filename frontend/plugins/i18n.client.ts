import { useSettingsApi } from '~/composables/useSettingsApi'
import { useI18n } from '~/composables/useI18n'
import { useAuthStore } from '~/stores/auth'
import { useSettingsStore } from '~/stores/settings'

export default defineNuxtPlugin(async () => {
  const { initializeLocale, applyProfileLocale } = useI18n()
  const authStore = useAuthStore()
  const settingsStore = useSettingsStore()

  initializeLocale()
  if (!authStore.isAuthenticated) {
    return
  }

  const { fetchProfile } = useSettingsApi()
  try {
    const profile = await fetchProfile()
    settingsStore.setProfile(profile)
    if (authStore.user) {
      authStore.user.displayName = profile.displayName
      authStore.user.mailAddressMode = profile.mailAddressMode
    }
    applyProfileLocale(profile.preferredLocale)
  } catch (error) {
    console.error('Failed to restore locale preference', error)
  }
})
