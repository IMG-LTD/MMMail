import { createMessage, encrypt, readKey } from 'openpgp'
import { useI18n } from '~/composables/useI18n'
import { useSettingsApi } from '~/composables/useSettingsApi'
import type { DraftRequest, MailComposeDraftRequest, MailComposeExternalSecureDelivery, MailE2eeKeyProfile } from '~/types/api'

const DRAFT_E2EE_ALGORITHM = 'openpgp'

export function useMailDraftE2ee() {
  const { t } = useI18n()
  const { fetchMailE2eeKeyProfile } = useSettingsApi()

  async function buildDraftPayload(payload: MailComposeDraftRequest): Promise<DraftRequest> {
    if (payload.externalSecureDelivery?.enabled) {
      return buildExternalSecureDraftPayload(payload)
    }
    const plainBody = payload.body?.trim() || ''
    if (!plainBody) {
      return stripComposeOnlyFields(payload)
    }
    const profile = await fetchMailE2eeKeyProfile()
    if (!hasEnabledProfile(profile)) {
      return stripComposeOnlyFields(payload)
    }
    const publicKey = await readKey({ armoredKey: profile.publicKeyArmored as string })
    const message = await createMessage({ text: payload.body as string })
    const encryptedBody = await encrypt({
      message,
      encryptionKeys: publicKey,
      format: 'armored'
    })
    return {
      ...stripComposeOnlyFields(payload),
      body: undefined,
      e2ee: {
        encryptedBody,
        algorithm: DRAFT_E2EE_ALGORITHM,
        recipientFingerprints: [profile.fingerprint as string]
      }
    }
  }

  async function buildExternalSecureDraftPayload(payload: MailComposeDraftRequest): Promise<DraftRequest> {
    const profile = requireEnabledProfile(await fetchMailE2eeKeyProfile(), t)
    const externalSecureDelivery = payload.externalSecureDelivery as MailComposeExternalSecureDelivery
    const password = requirePassword(externalSecureDelivery.password, t)
    const externalAccess = {
      mode: 'PASSWORD_PROTECTED' as const,
      passwordHint: normalizeOptional(externalSecureDelivery.passwordHint),
      expiresAt: normalizeOptional(externalSecureDelivery.expiresAt)
    }
    const plainBody = payload.body?.trim() || ''
    if (!plainBody) {
      return {
        ...stripComposeOnlyFields(payload),
        body: undefined,
        e2ee: {
          encryptedBody: '',
          algorithm: DRAFT_E2EE_ALGORITHM,
          recipientFingerprints: [profile.fingerprint as string],
          externalAccess
        }
      }
    }
    const publicKey = await readKey({ armoredKey: profile.publicKeyArmored as string })
    const message = await createMessage({ text: plainBody })
    const encryptedBody = await encrypt({
      message,
      encryptionKeys: [publicKey],
      passwords: [password],
      format: 'armored'
    })
    return {
      ...stripComposeOnlyFields(payload),
      body: undefined,
      e2ee: {
        encryptedBody,
        algorithm: DRAFT_E2EE_ALGORITHM,
        recipientFingerprints: [profile.fingerprint as string],
        externalAccess
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

function requireEnabledProfile(
  profile: MailE2eeKeyProfile,
  translate: (key: string) => string
): MailE2eeKeyProfile {
  if (!hasEnabledProfile(profile)) {
    throw new Error(translate('mailCompose.e2ee.messages.senderKeyMissing'))
  }
  return profile
}

function requirePassword(password: string | undefined, translate: (key: string) => string): string {
  const value = password?.trim() || ''
  if (!value) {
    throw new Error(translate('mailCompose.externalSecure.messages.passwordRequired'))
  }
  return value
}

function normalizeOptional(value: string | undefined): string | undefined {
  const normalized = value?.trim() || ''
  return normalized || undefined
}

function stripComposeOnlyFields(payload: MailComposeDraftRequest): DraftRequest {
  const { externalSecureDelivery: _ignored, ...draftRequest } = payload
  return draftRequest
}
