import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '~/stores/auth'
import { useAuthenticatorApi } from '~/composables/useAuthenticatorApi'
import { useI18n } from '~/composables/useI18n'
import type {
  AuthenticatorSecurityDraft,
  AuthenticatorSecurityPreference,
  UpdateAuthenticatorSecurityRequest
} from '~/types/authenticator-security'
import {
  buildAuthenticatorUnlockDeadline,
  buildAuthenticatorUnlockStorageKey,
  clearAuthenticatorUnlockDeadline,
  createAuthenticatorSecurityDraft,
  isAuthenticatorPinValid,
  isAuthenticatorUnlockDeadlineActive,
  readAuthenticatorUnlockDeadline,
  writeAuthenticatorUnlockDeadline
} from '~/utils/authenticator-security'

export function useAuthenticatorSecurity() {
  const { t } = useI18n()
  const authStore = useAuthStore()
  const {
    getSecurityPreference,
    updateSecurityPreference,
    verifyPin
  } = useAuthenticatorApi()

  const loadingPreference = ref(false)
  const savingPreference = ref(false)
  const verifyingPin = ref(false)
  const preference = ref<AuthenticatorSecurityPreference | null>(null)
  const draft = reactive<AuthenticatorSecurityDraft>(createAuthenticatorSecurityDraft())
  const unlockPin = ref('')
  const unlockedUntil = ref<number | null>(null)
  const lockTimer = ref<ReturnType<typeof setTimeout> | null>(null)

  const storageKey = computed(() => buildAuthenticatorUnlockStorageKey(authStore.user?.id))
  const requiresUnlock = computed(() => {
    if (!preference.value?.pinProtectionEnabled) {
      return false
    }
    return !isAuthenticatorUnlockDeadlineActive(unlockedUntil.value)
  })
  const isUnlocked = computed(() => !requiresUnlock.value)

  async function initializeSecurity(): Promise<void> {
    await refreshSecurity()
  }

  async function refreshSecurity(): Promise<void> {
    loadingPreference.value = true
    try {
      const next = await getSecurityPreference()
      applyPreference(next)
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'authenticator.security.messages.loadFailed'))
    } finally {
      loadingPreference.value = false
    }
  }

  async function saveSecurity(): Promise<boolean> {
    if (requiresUnlock.value) {
      ElMessage.warning(t('authenticator.security.messages.unlockRequired'))
      return false
    }
    const payload = resolveUpdatePayload()
    if (!payload) {
      return false
    }
    savingPreference.value = true
    try {
      const next = await updateSecurityPreference(payload)
      applyPreference(next)
      if (next.pinProtectionEnabled) {
        activateUnlockSession(next.lockTimeoutSeconds)
      } else {
        clearUnlockSession()
      }
      ElMessage.success(t('authenticator.security.messages.settingsSaved'))
      return true
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'authenticator.security.messages.settingsSaveFailed'))
      return false
    } finally {
      savingPreference.value = false
    }
  }

  async function verifyPinAndUnlock(): Promise<boolean> {
    const pin = unlockPin.value.trim()
    if (!isAuthenticatorPinValid(pin)) {
      ElMessage.warning(t('authenticator.security.messages.pinInvalid'))
      return false
    }
    verifyingPin.value = true
    try {
      const result = await verifyPin(pin)
      activateUnlockSession(result.lockTimeoutSeconds)
      unlockPin.value = ''
      ElMessage.success(t('authenticator.security.messages.unlockSuccess'))
      return result.verified
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'authenticator.security.messages.unlockFailed'))
      return false
    } finally {
      verifyingPin.value = false
    }
  }

  function lockNow(): void {
    clearUnlockSession()
    ElMessage.success(t('authenticator.security.messages.lockedNow'))
  }

  function dispose(): void {
    clearLockTimer()
  }

  function applyPreference(next: AuthenticatorSecurityPreference): void {
    preference.value = next
    syncDraft(next)
    restoreUnlockState(next)
  }

  function syncDraft(next: AuthenticatorSecurityPreference): void {
    Object.assign(draft, createAuthenticatorSecurityDraft(next))
    unlockPin.value = ''
  }

  function restoreUnlockState(next: AuthenticatorSecurityPreference): void {
    if (!next.pinProtectionEnabled) {
      clearUnlockSession()
      return
    }
    const storage = resolveSessionStorage()
    if (!storage) {
      unlockedUntil.value = null
      return
    }
    const deadline = readAuthenticatorUnlockDeadline(storage, storageKey.value)
    if (!isAuthenticatorUnlockDeadlineActive(deadline)) {
      clearUnlockSession()
      return
    }
    unlockedUntil.value = deadline
    if (deadline != null) {
      scheduleLock(deadline)
    }
  }

  function activateUnlockSession(lockTimeoutSeconds: number): void {
    const deadline = buildAuthenticatorUnlockDeadline(lockTimeoutSeconds)
    unlockedUntil.value = deadline
    const storage = resolveSessionStorage()
    if (storage) {
      writeAuthenticatorUnlockDeadline(storage, storageKey.value, deadline)
    }
    scheduleLock(deadline)
  }

  function clearUnlockSession(): void {
    clearLockTimer()
    unlockedUntil.value = null
    const storage = resolveSessionStorage()
    if (storage) {
      clearAuthenticatorUnlockDeadline(storage, storageKey.value)
    }
  }

  function clearLockTimer(): void {
    if (!lockTimer.value) {
      return
    }
    clearTimeout(lockTimer.value)
    lockTimer.value = null
  }

  function scheduleLock(deadline: number): void {
    clearLockTimer()
    const remaining = deadline - Date.now()
    if (remaining <= 0) {
      clearUnlockSession()
      return
    }
    lockTimer.value = setTimeout(() => {
      clearUnlockSession()
    }, remaining)
  }

  function resolveUpdatePayload(): UpdateAuthenticatorSecurityRequest | null {
    const pin = draft.pin.trim()
    if (draft.pinProtectionEnabled && !pin && !preference.value?.pinConfigured) {
      ElMessage.warning(t('authenticator.security.messages.pinRequired'))
      return null
    }
    if (pin && !isAuthenticatorPinValid(pin)) {
      ElMessage.warning(t('authenticator.security.messages.pinInvalid'))
      return null
    }
    if (pin && pin !== draft.pinConfirm.trim()) {
      ElMessage.warning(t('authenticator.security.messages.pinConfirmMismatch'))
      return null
    }
    return {
      syncEnabled: draft.syncEnabled,
      encryptedBackupEnabled: draft.encryptedBackupEnabled,
      pinProtectionEnabled: draft.pinProtectionEnabled,
      lockTimeoutSeconds: draft.lockTimeoutSeconds,
      pin: pin || undefined
    }
  }

  function resolveSessionStorage(): Storage | null {
    if (!import.meta.client || typeof window === 'undefined' || !window.sessionStorage) {
      return null
    }
    return window.sessionStorage
  }

  function resolveErrorMessage(error: unknown, fallbackKey: string): string {
    return error instanceof Error && error.message ? error.message : t(fallbackKey)
  }

  return {
    loadingPreference,
    savingPreference,
    verifyingPin,
    preference,
    draft,
    unlockPin,
    requiresUnlock,
    isUnlocked,
    initializeSecurity,
    refreshSecurity,
    saveSecurity,
    verifyPinAndUnlock,
    lockNow,
    dispose
  }
}
