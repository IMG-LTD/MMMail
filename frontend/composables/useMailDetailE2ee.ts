import { ref } from 'vue'
import { decrypt, decryptKey, readMessage, readPrivateKey } from 'openpgp'
import { useI18n } from '~/composables/useI18n'
import { useSettingsApi } from '~/composables/useSettingsApi'
import type { MailE2eeKeyProfile } from '~/types/api'

export function useMailDetailE2ee() {
  const { t } = useI18n()
  const { fetchMailE2eeKeyProfile } = useSettingsApi()

  const decrypting = ref(false)
  const decryptedBody = ref('')
  const decryptError = ref('')
  const passphrase = ref('')

  async function decryptEncryptedBody(ciphertext: string): Promise<void> {
    if (!passphrase.value.trim()) {
      throw new Error(t('mailWorkspace.detail.e2ee.messages.passphraseRequired'))
    }
    decrypting.value = true
    try {
      const profile = await fetchMailE2eeKeyProfile()
      validateProfile(profile)
      decryptedBody.value = await decryptBody(ciphertext, profile, passphrase.value)
      decryptError.value = ''
      passphrase.value = ''
    } catch (error) {
      decryptedBody.value = ''
      decryptError.value = error instanceof Error ? error.message : t('mailWorkspace.detail.e2ee.messages.decryptFailed')
      throw error
    } finally {
      decrypting.value = false
    }
  }

  function resetDecryptedBody(): void {
    decryptedBody.value = ''
    decryptError.value = ''
    passphrase.value = ''
  }

  return {
    decrypting,
    decryptedBody,
    decryptError,
    passphrase,
    decryptEncryptedBody,
    resetDecryptedBody
  }
}

async function decryptBody(ciphertext: string, profile: MailE2eeKeyProfile, passphrase: string): Promise<string> {
  const privateKey = await readPrivateKey({ armoredKey: profile.encryptedPrivateKeyArmored as string })
  const unlockedKey = await decryptKey({ privateKey, passphrase })
  const message = await readMessage({ armoredMessage: ciphertext })
  const result = await decrypt({
    message,
    decryptionKeys: unlockedKey,
    format: 'utf8'
  })
  return String(result.data || '')
}

function validateProfile(profile: MailE2eeKeyProfile): void {
  if (!profile.enabled || !profile.encryptedPrivateKeyArmored) {
    throw new Error('Current account does not have an encrypted Mail E2EE private key package.')
  }
}
