import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserPreference } from '~/types/api'

const DEFAULT_PREFERENCE: UserPreference = {
  displayName: '',
  signature: '',
  timezone: 'UTC',
  preferredLocale: 'en',
  mailAddressMode: 'PROTON_ADDRESS',
  autoSaveSeconds: 15,
  undoSendSeconds: 10,
  driveVersionRetentionCount: 50,
  driveVersionRetentionDays: 365
}

export const useSettingsStore = defineStore('settings', () => {
  const profile = ref<UserPreference>({ ...DEFAULT_PREFERENCE })

  function setProfile(next: UserPreference): void {
    profile.value = { ...next }
  }

  function resetProfile(): void {
    profile.value = { ...DEFAULT_PREFERENCE }
  }

  return {
    profile,
    setProfile,
    resetProfile
  }
})
