import { createMessage, encrypt, readKey } from 'openpgp'
import { useSettingsApi } from '~/composables/useSettingsApi'
import type { DraftRequest, MailE2eeKeyProfile } from '~/types/api'

const DRAFT_E2EE_ALGORITHM = 'openpgp'

export function useMailDraftE2ee() {
  const { fetchMailE2eeKeyProfile } = useSettingsApi()

  async function buildDraftPayload(payload: DraftRequest): Promise<DraftRequest> {
    const plainBody = payload.body?.trim() || ''
    if (!plainBody) {
      return payload
    }
    const profile = await fetchMailE2eeKeyProfile()
    if (!hasEnabledProfile(profile)) {
      return payload
    }
    const publicKey = await readKey({ armoredKey: profile.publicKeyArmored as string })
    const message = await createMessage({ text: payload.body as string })
    const encryptedBody = await encrypt({
      message,
      encryptionKeys: publicKey,
      format: 'armored'
    })
    return {
      ...payload,
      body: undefined,
      e2ee: {
        encryptedBody,
        algorithm: DRAFT_E2EE_ALGORITHM,
        recipientFingerprints: [profile.fingerprint as string]
      }
    }
  }

  return {
    buildDraftPayload
  }
}

function hasEnabledProfile(profile: MailE2eeKeyProfile): boolean {
  return Boolean(
    profile.enabled
      && profile.publicKeyArmored
      && profile.fingerprint
  )
}
