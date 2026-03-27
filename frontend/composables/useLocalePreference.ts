import { ElMessage } from 'element-plus'
import type { SupportedLocale } from '~/constants/i18n'
import { useSettingsApi } from '~/composables/useSettingsApi'
import { useI18n } from '~/composables/useI18n'
import { useAuthStore } from '~/stores/auth'
import { useSettingsStore } from '~/stores/settings'
import type { UserPreference } from '~/types/api'

function hasLoadedProfile(profile: UserPreference): boolean {
  return profile.displayName.trim().length > 0
}

export function useLocalePreference() {
  const syncing = useState<boolean>('mmmail.locale.syncing', () => false)
  const authStore = useAuthStore()
  const settingsStore = useSettingsStore()
  const { fetchProfile, updateProfile } = useSettingsApi()
  const { rememberLocaleSelection, setLocale, t } = useI18n()

  async function resolveProfile(): Promise<UserPreference> {
    if (hasLoadedProfile(settingsStore.profile)) {
      return settingsStore.profile
    }
    const profile = await fetchProfile()
    settingsStore.setProfile(profile)
    return profile
  }

  async function applyLocaleSelection(locale: SupportedLocale): Promise<void> {
    rememberLocaleSelection(locale)
    setLocale(locale)
    if (!authStore.isAuthenticated || syncing.value) {
      return
    }

    syncing.value = true
    try {
      const profile = await resolveProfile()
      const updatedProfile = await updateProfile({
        ...profile,
        preferredLocale: locale
      })
      settingsStore.setProfile(updatedProfile)
    } catch (error) {
      const message = error instanceof Error ? error.message : t('topbar.localeSyncFailed')
      ElMessage.error(message)
    } finally {
      syncing.value = false
    }
  }

  return {
    syncing,
    applyLocaleSelection
  }
}
