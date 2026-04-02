import type { ComputedRef } from 'vue'
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { generateKey, readKey } from 'openpgp'
import { useI18n } from '~/composables/useI18n'
import { useSettingsApi } from '~/composables/useSettingsApi'
import type {
  MailE2eeKeyProfile,
  UpdateMailE2eeKeyProfileRequest
} from '~/types/api'

const MIN_PASSPHRASE_LENGTH = 12
const KEY_TYPE = 'ecc'
const KEY_CURVE = 'curve25519Legacy'
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const EMPTY_PROFILE: MailE2eeKeyProfile = {
  enabled: false,
  fingerprint: null,
  algorithm: null,
  publicKeyArmored: null,
  encryptedPrivateKeyArmored: null,
  keyCreatedAt: null
}

interface UseMailE2eeOptions {
  defaultIdentityName: ComputedRef<string>
  defaultIdentityEmail: ComputedRef<string>
}

export function useMailE2ee(options: UseMailE2eeOptions) {
  const { t } = useI18n()
  const { fetchMailE2eeKeyProfile, updateMailE2eeKeyProfile } = useSettingsApi()

  const loadingProfile = ref(false)
  const savingProfile = ref(false)
  const errorMessage = ref('')
  const profile = ref<MailE2eeKeyProfile>({ ...EMPTY_PROFILE })
  const draft = reactive({
    identityName: '',
    identityEmail: '',
    passphrase: '',
    confirmPassphrase: ''
  })

  const hasProfile = computed(() => profile.value.enabled)
  const statusLabel = computed(() => (
    profile.value.enabled ? t('settings.mailE2ee.values.enabled') : t('settings.mailE2ee.values.disabled')
  ))
  const privateKeyLabel = computed(() => (
    profile.value.encryptedPrivateKeyArmored
      ? t('settings.mailE2ee.values.privateKeyStored')
      : t('settings.mailE2ee.values.notConfigured')
  ))
  const createdAtLabel = computed(() => {
    if (!profile.value.keyCreatedAt) {
      return t('settings.mailE2ee.values.notConfigured')
    }
    return new Date(profile.value.keyCreatedAt).toLocaleString()
  })

  async function initializeMailE2ee(): Promise<void> {
    applyIdentityDefaults()
    await refreshMailE2ee()
  }

  async function refreshMailE2ee(): Promise<void> {
    loadingProfile.value = true
    try {
      profile.value = await fetchMailE2eeKeyProfile()
      errorMessage.value = ''
      applyIdentityDefaults()
    } catch (error) {
      handleError(error, 'settings.mailE2ee.messages.loadFailed')
    } finally {
      loadingProfile.value = false
    }
  }

  async function generateAndSaveProfile(): Promise<boolean> {
    if (!validateDraft()) {
      return false
    }
    savingProfile.value = true
    try {
      const payload = await buildEnabledPayload()
      profile.value = await updateMailE2eeKeyProfile(payload)
      errorMessage.value = ''
      clearSensitiveDraft()
      ElMessage.success(t('settings.mailE2ee.messages.profileSaved'))
      return true
    } catch (error) {
      handleError(error, 'settings.mailE2ee.messages.saveFailed')
      return false
    } finally {
      savingProfile.value = false
    }
  }

  async function disableProfile(): Promise<boolean> {
    savingProfile.value = true
    try {
      profile.value = await updateMailE2eeKeyProfile({ enabled: false })
      errorMessage.value = ''
      clearSensitiveDraft()
      ElMessage.success(t('settings.mailE2ee.messages.profileDisabled'))
      return true
    } catch (error) {
      handleError(error, 'settings.mailE2ee.messages.saveFailed')
      return false
    } finally {
      savingProfile.value = false
    }
  }

  function applyIdentityDefaults(): void {
    if (!draft.identityName.trim()) {
      draft.identityName = options.defaultIdentityName.value
    }
    if (!draft.identityEmail.trim()) {
      draft.identityEmail = options.defaultIdentityEmail.value
    }
  }

  function validateDraft(): boolean {
    if (!draft.identityName.trim()) {
      ElMessage.warning(t('settings.mailE2ee.messages.identityNameRequired'))
      return false
    }
    if (!EMAIL_PATTERN.test(draft.identityEmail.trim())) {
      ElMessage.warning(t('settings.mailE2ee.messages.identityEmailRequired'))
      return false
    }
    if (!draft.passphrase.trim()) {
      ElMessage.warning(t('settings.mailE2ee.messages.passphraseRequired'))
      return false
    }
    if (draft.passphrase.trim().length < MIN_PASSPHRASE_LENGTH) {
      ElMessage.warning(t('settings.mailE2ee.messages.passphraseTooShort'))
      return false
    }
    if (draft.passphrase !== draft.confirmPassphrase) {
      ElMessage.warning(t('settings.mailE2ee.messages.passphraseMismatch'))
      return false
    }
    return true
  }

  async function buildEnabledPayload(): Promise<UpdateMailE2eeKeyProfileRequest> {
    const keyCreatedAt = formatKeyCreatedAt(new Date())
    const { privateKey, publicKey } = await generateKey({
      type: KEY_TYPE,
      curve: KEY_CURVE,
      userIDs: [{
        name: draft.identityName.trim(),
        email: draft.identityEmail.trim()
      }],
      passphrase: draft.passphrase,
      format: 'armored'
    })
    const parsedPublicKey = await readKey({ armoredKey: publicKey })
    return {
      enabled: true,
      publicKeyArmored: publicKey,
      encryptedPrivateKeyArmored: privateKey,
      fingerprint: parsedPublicKey.getFingerprint().toUpperCase(),
      algorithm: KEY_CURVE,
      keyCreatedAt
    }
  }

  function clearSensitiveDraft(): void {
    draft.passphrase = ''
    draft.confirmPassphrase = ''
  }

  function handleError(error: unknown, fallbackKey: string): void {
    const message = resolveErrorMessage(error, fallbackKey)
    errorMessage.value = message
    ElMessage.error(message)
  }

  function resolveErrorMessage(error: unknown, fallbackKey: string): string {
    return error instanceof Error && error.message ? error.message : t(fallbackKey)
  }

  function formatKeyCreatedAt(value: Date): string {
    return value.toISOString().slice(0, 19)
  }

  return {
    loadingProfile,
    savingProfile,
    errorMessage,
    profile,
    draft,
    hasProfile,
    statusLabel,
    privateKeyLabel,
    createdAtLabel,
    initializeMailE2ee,
    refreshMailE2ee,
    generateAndSaveProfile,
    disableProfile
  }
}
