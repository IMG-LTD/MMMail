import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserPreference } from '@/shared/types/api'

const DEFAULT_PREFERENCE: UserPreference = {
  autoSaveSeconds: 15,
  displayName: '',
  driveVersionRetentionCount: 50,
  driveVersionRetentionDays: 365,
  mailAddressMode: 'PROTON_ADDRESS',
  preferredLocale: 'en',
  signature: '',
  timezone: 'UTC',
  undoSendSeconds: 10
}

export const useSettingsStore = defineStore('settings', () => {
  const profile = ref<UserPreference>({ ...DEFAULT_PREFERENCE })

  function setProfile(next: UserPreference) {
    profile.value = { ...next }
  }

  function resetProfile() {
    profile.value = { ...DEFAULT_PREFERENCE }
  }

  return {
    profile,
    resetProfile,
    setProfile
  }
})
