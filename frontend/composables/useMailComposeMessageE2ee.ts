import { createMessage, encrypt, readKey } from 'openpgp'
import { useI18n } from '~/composables/useI18n'
import { useSettingsApi } from '~/composables/useSettingsApi'
import type {
  MailBodyE2eePayload,
  MailE2eeKeyProfile,
  MailE2eeRecipientRouteStatus,
  MailE2eeRecipientStatus,
  SendMailRequest
} from '~/types/api'

const MESSAGE_E2EE_ALGORITHM = 'openpgp'

export function useMailComposeMessageE2ee() {
  const { t } = useI18n()
  const { fetchMailE2eeKeyProfile } = useSettingsApi()

  async function buildSendPayload(
    payload: SendMailRequest,
    recipientStatus: MailE2eeRecipientStatus | null
  ): Promise<SendMailRequest> {
    if (!recipientStatus?.encryptionReady) {
      return payload
    }
    const senderProfile = await fetchMailE2eeKeyProfile()
    validateSenderProfile(senderProfile)
    const e2eePayload = await encryptBody(payload.body, senderProfile, recipientStatus.routes)
    return {
      ...payload,
      body: undefined,
      e2ee: e2eePayload
    }
  }

  async function encryptBody(
    body: string | undefined,
    senderProfile: MailE2eeKeyProfile,
    routes: MailE2eeRecipientRouteStatus[]
  ): Promise<MailBodyE2eePayload> {
    const plainBody = requireBody(body)
    const routeKeys = await resolveRouteKeys(routes)
    const senderKey = await readKey({ armoredKey: senderProfile.publicKeyArmored as string })
    const encryptionKeys = [...routeKeys.map(item => item.key), senderKey]
    const message = await createMessage({ text: plainBody })
    const encryptedBody = await encrypt({
      message,
      encryptionKeys,
      format: 'armored'
    })
    return {
      encryptedBody,
      algorithm: MESSAGE_E2EE_ALGORITHM,
      recipientFingerprints: [...routeKeys.map(item => item.fingerprint), senderProfile.fingerprint as string]
    }
  }

  async function resolveRouteKeys(
    routes: MailE2eeRecipientRouteStatus[]
  ): Promise<Array<{ fingerprint: string, key: Awaited<ReturnType<typeof readKey>> }>> {
    if (routes.length === 0) {
      throw new Error(t('mailCompose.e2ee.messages.routeUnavailable'))
    }
    return Promise.all(routes.map(async (route) => {
      if (!route.publicKeyArmored || !route.fingerprint) {
        throw new Error(t('mailCompose.e2ee.messages.routeKeyMissing'))
      }
      return {
        fingerprint: route.fingerprint,
        key: await readKey({ armoredKey: route.publicKeyArmored })
      }
    }))
  }

  function validateSenderProfile(profile: MailE2eeKeyProfile): void {
    if (!profile.enabled || !profile.publicKeyArmored || !profile.fingerprint) {
      throw new Error(t('mailCompose.e2ee.messages.senderKeyMissing'))
    }
  }

  function requireBody(body: string | undefined): string {
    const value = body?.trim() || ''
    if (!value) {
      throw new Error(t('mailCompose.e2ee.messages.bodyRequired'))
    }
    return value
  }

  return {
    buildSendPayload
  }
}
