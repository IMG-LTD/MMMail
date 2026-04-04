import type { Ref } from 'vue'
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { decryptKey, encryptKey, readPrivateKey } from 'openpgp'
import { useI18n } from '~/composables/useI18n'
import { useSettingsApi } from '~/composables/useSettingsApi'
import type {
  MailE2eeKeyProfile,
  MailE2eeRecoveryPackage,
  UpdateMailE2eeKeyProfileRequest
} from '~/types/api'

const MIN_PASSPHRASE_LENGTH = 12

const EMPTY_RECOVERY: MailE2eeRecoveryPackage = {
  enabled: false,
  encryptedPrivateKeyArmored: null,
  updatedAt: null
}

interface UseMailE2eeRecoveryOptions {
  profile: Ref<MailE2eeKeyProfile>
}

export function useMailE2eeRecovery(options: UseMailE2eeRecoveryOptions) {
  const { t } = useI18n()
  const {
    fetchMailE2eeRecoveryPackage,
    updateMailE2eeRecoveryPackage,
    updateMailE2eeKeyProfile
  } = useSettingsApi()

  const loadingRecovery = ref(false)
  const savingRecovery = ref(false)
  const recoveryErrorMessage = ref('')
  const recovery = ref<MailE2eeRecoveryPackage>({ ...EMPTY_RECOVERY })
  const recoveryDraft = reactive({
    currentPassphrase: '',
    recoveryPassphrase: '',
    confirmRecoveryPassphrase: ''
  })
  const restoreDraft = reactive({
    recoveryPassphrase: '',
    nextPassphrase: '',
    confirmNextPassphrase: ''
  })

  const hasRecovery = computed(() => recovery.value.enabled)
  const recoveryStatusLabel = computed(() => (
    hasRecovery.value
      ? t('settings.mailE2ee.recovery.values.enabled')
      : t('settings.mailE2ee.recovery.values.disabled')
  ))
  const recoveryUpdatedAtLabel = computed(() => {
    if (!recovery.value.updatedAt) {
      return t('settings.mailE2ee.values.notConfigured')
    }
    return new Date(recovery.value.updatedAt).toLocaleString()
  })

  async function initializeRecovery(): Promise<void> {
    await refreshRecovery()
  }

  async function refreshRecovery(): Promise<void> {
    loadingRecovery.value = true
    try {
      recovery.value = await fetchMailE2eeRecoveryPackage()
      recoveryErrorMessage.value = ''
    } catch (error) {
      handleError(error, 'settings.mailE2ee.recovery.messages.loadFailed')
    } finally {
      loadingRecovery.value = false
    }
  }

  async function saveRecoveryPackage(): Promise<boolean> {
    const currentProfile = requireProfile()
    if (!validateRecoveryDraft()) {
      return false
    }
    savingRecovery.value = true
    try {
      const encryptedPrivateKeyArmored = await buildRecoveryPackage(currentProfile)
      recovery.value = await updateMailE2eeRecoveryPackage({ enabled: true, encryptedPrivateKeyArmored })
      clearRecoveryDraft()
      recoveryErrorMessage.value = ''
      ElMessage.success(t('settings.mailE2ee.recovery.messages.saved'))
      return true
    } catch (error) {
      handleError(error, 'settings.mailE2ee.recovery.messages.saveFailed')
      return false
    } finally {
      savingRecovery.value = false
    }
  }

  async function disableRecoveryPackage(): Promise<boolean> {
    savingRecovery.value = true
    try {
      recovery.value = await updateMailE2eeRecoveryPackage({ enabled: false })
      clearRecoveryDraft()
      clearRestoreDraft()
      recoveryErrorMessage.value = ''
      ElMessage.success(t('settings.mailE2ee.recovery.messages.disabled'))
      return true
    } catch (error) {
      handleError(error, 'settings.mailE2ee.recovery.messages.saveFailed')
      return false
    } finally {
      savingRecovery.value = false
    }
  }

  async function restoreFromRecoveryPackage(): Promise<boolean> {
    const currentProfile = requireProfile()
    const storedRecovery = requireRecoveryPackage()
    if (!validateRestoreDraft()) {
      return false
    }
    savingRecovery.value = true
    try {
      const payload = await buildRestoredProfilePayload(currentProfile, storedRecovery.encryptedPrivateKeyArmored as string)
      options.profile.value = await updateMailE2eeKeyProfile(payload)
      clearRestoreDraft()
      recoveryErrorMessage.value = ''
      ElMessage.success(t('settings.mailE2ee.recovery.messages.restored'))
      return true
    } catch (error) {
      handleError(error, 'settings.mailE2ee.recovery.messages.restoreFailed')
      return false
    } finally {
      savingRecovery.value = false
    }
  }

  return {
    loadingRecovery,
    savingRecovery,
    recoveryErrorMessage,
    recovery,
    recoveryDraft,
    restoreDraft,
    hasRecovery,
    recoveryStatusLabel,
    recoveryUpdatedAtLabel,
    initializeRecovery,
    refreshRecovery,
    saveRecoveryPackage,
    disableRecoveryPackage,
    restoreFromRecoveryPackage
  }

  function requireProfile(): MailE2eeKeyProfile {
    const profile = options.profile.value
    if (!profile.enabled || !profile.encryptedPrivateKeyArmored || !profile.publicKeyArmored || !profile.fingerprint || !profile.algorithm) {
      throw new Error(t('settings.mailE2ee.recovery.messages.profileRequired'))
    }
    return profile
  }

  function requireRecoveryPackage(): MailE2eeRecoveryPackage {
    if (!recovery.value.enabled || !recovery.value.encryptedPrivateKeyArmored) {
      throw new Error(t('settings.mailE2ee.recovery.messages.unavailable'))
    }
    return recovery.value
  }

  function validateRecoveryDraft(): boolean {
    if (!recoveryDraft.currentPassphrase.trim()) {
      ElMessage.warning(t('settings.mailE2ee.recovery.messages.currentPassphraseRequired'))
      return false
    }
    return validateNewPassphrase(
      recoveryDraft.recoveryPassphrase,
      recoveryDraft.confirmRecoveryPassphrase,
      'settings.mailE2ee.recovery.messages.passphraseRequired',
      'settings.mailE2ee.recovery.messages.passphraseTooShort',
      'settings.mailE2ee.recovery.messages.passphraseMismatch'
    )
  }

  function validateRestoreDraft(): boolean {
    if (!restoreDraft.recoveryPassphrase.trim()) {
      ElMessage.warning(t('settings.mailE2ee.recovery.messages.restorePassphraseRequired'))
      return false
    }
    return validateNewPassphrase(
      restoreDraft.nextPassphrase,
      restoreDraft.confirmNextPassphrase,
      'settings.mailE2ee.recovery.messages.nextPassphraseRequired',
      'settings.mailE2ee.recovery.messages.nextPassphraseTooShort',
      'settings.mailE2ee.recovery.messages.nextPassphraseMismatch'
    )
  }

  function validateNewPassphrase(
    passphrase: string,
    confirmation: string,
    requiredKey: string,
    tooShortKey: string,
    mismatchKey: string
  ): boolean {
    if (!passphrase.trim()) {
      ElMessage.warning(t(requiredKey))
      return false
    }
    if (passphrase.trim().length < MIN_PASSPHRASE_LENGTH) {
      ElMessage.warning(t(tooShortKey))
      return false
    }
    if (passphrase !== confirmation) {
      ElMessage.warning(t(mismatchKey))
      return false
    }
    return true
  }

  async function buildRecoveryPackage(profile: MailE2eeKeyProfile): Promise<string> {
    const privateKey = await readPrivateKey({ armoredKey: profile.encryptedPrivateKeyArmored as string })
    const unlockedKey = await decryptKey({
      privateKey,
      passphrase: recoveryDraft.currentPassphrase
    })
    const encryptedKey = await encryptKey({
      privateKey: unlockedKey,
      passphrase: recoveryDraft.recoveryPassphrase
    })
    return encryptedKey.armor()
  }

  async function buildRestoredProfilePayload(
    profile: MailE2eeKeyProfile,
    recoveryCiphertext: string
  ): Promise<UpdateMailE2eeKeyProfileRequest> {
    const recoveryKey = await readPrivateKey({ armoredKey: recoveryCiphertext })
    const unlockedKey = await decryptKey({
      privateKey: recoveryKey,
      passphrase: restoreDraft.recoveryPassphrase
    })
    const encryptedKey = await encryptKey({
      privateKey: unlockedKey,
      passphrase: restoreDraft.nextPassphrase
    })
    return {
      enabled: true,
      publicKeyArmored: profile.publicKeyArmored as string,
      encryptedPrivateKeyArmored: encryptedKey.armor(),
      fingerprint: profile.fingerprint as string,
      algorithm: profile.algorithm || undefined,
      keyCreatedAt: profile.keyCreatedAt || undefined
    }
  }

  function clearRecoveryDraft(): void {
    recoveryDraft.currentPassphrase = ''
    recoveryDraft.recoveryPassphrase = ''
    recoveryDraft.confirmRecoveryPassphrase = ''
  }

  function clearRestoreDraft(): void {
    restoreDraft.recoveryPassphrase = ''
    restoreDraft.nextPassphrase = ''
    restoreDraft.confirmNextPassphrase = ''
  }

  function handleError(error: unknown, fallbackKey: string): void {
    const message = error instanceof Error && error.message ? error.message : t(fallbackKey)
    recoveryErrorMessage.value = message
    ElMessage.error(message)
  }
}
