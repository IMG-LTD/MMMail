import { createMessage, decrypt, decryptKey, encrypt, readKey, readMessage, readPrivateKey } from 'openpgp'
import { useI18n } from '~/composables/useI18n'
import { useSettingsApi } from '~/composables/useSettingsApi'
import type {
  MailAttachment,
  MailAttachmentE2ee,
  MailComposeExternalSecureDelivery,
  MailAttachmentE2eePayload,
  MailE2eeKeyProfile
} from '~/types/api'

const ATTACHMENT_E2EE_ALGORITHM = 'openpgp'
const ENCRYPTED_ATTACHMENT_CONTENT_TYPE = 'application/octet-stream'
const DEFAULT_ATTACHMENT_CONTENT_TYPE = 'application/octet-stream'

export interface DraftAttachmentEncryptionResult {
  file: File
  fileName: string
  contentType: string
  fileSize: number
  e2ee: MailAttachmentE2eePayload
}

export interface DownloadedAttachmentPayload {
  blob: Blob
  fileName: string
}

interface AttachmentEncryptionOptions {
  externalSecureDelivery?: MailComposeExternalSecureDelivery | null
}

export function useMailAttachmentE2ee() {
  const { t } = useI18n()
  const { fetchMailE2eeKeyProfile } = useSettingsApi()

  async function isDraftAttachmentEncryptionEnabled(): Promise<boolean> {
    const profile = await fetchMailE2eeKeyProfile()
    return Boolean(profile.enabled && profile.publicKeyArmored && profile.fingerprint)
  }

  async function encryptDraftAttachment(
    file: File,
    options: AttachmentEncryptionOptions = {}
  ): Promise<DraftAttachmentEncryptionResult> {
    const profile = requireEncryptionProfile(await fetchMailE2eeKeyProfile(), t)
    const publicKey = await readKey({ armoredKey: profile.publicKeyArmored as string })
    const message = await createMessage({ binary: new Uint8Array(await file.arrayBuffer()) })
    const passwords = resolveAttachmentPasswords(options.externalSecureDelivery, t)
    const encryptedBinary = await encrypt({
      message,
      encryptionKeys: [publicKey],
      passwords,
      format: 'binary'
    })
    const encryptedBytes = toUint8Array(encryptedBinary)
    const encryptedFileName = `${file.name}.pgp`
    const encryptedFile = new File([toBlobPart(encryptedBytes)], encryptedFileName, {
      type: ENCRYPTED_ATTACHMENT_CONTENT_TYPE
    })
    return {
      file: encryptedFile,
      fileName: file.name,
      contentType: file.type || DEFAULT_ATTACHMENT_CONTENT_TYPE,
      fileSize: file.size,
      e2ee: buildAttachmentE2eePayload(profile.fingerprint as string, file)
    }
  }

  async function decryptDownloadedAttachment(
    payload: DownloadedAttachmentPayload,
    attachment: MailAttachment,
    passphrase: string
  ): Promise<DownloadedAttachmentPayload> {
    const normalizedPassphrase = passphrase.trim()
    if (!normalizedPassphrase) {
      throw new Error(t('mailCompose.attachments.e2ee.messages.passphraseRequired'))
    }
    const metadata = requireAttachmentE2ee(attachment)
    const profile = requireDecryptionProfile(await fetchMailE2eeKeyProfile(), t)
    const privateKey = await readPrivateKey({ armoredKey: profile.encryptedPrivateKeyArmored as string })
    const unlockedKey = await decryptKey({ privateKey, passphrase: normalizedPassphrase })
    const message = await readMessage({ binaryMessage: new Uint8Array(await payload.blob.arrayBuffer()) })
    const decryptedResult = await decrypt({
      message,
      decryptionKeys: unlockedKey,
      format: 'binary'
    })
    const plaintextBytes = toUint8Array(decryptedResult.data)
    return {
      blob: new Blob([toBlobPart(plaintextBytes)], { type: attachment.contentType || DEFAULT_ATTACHMENT_CONTENT_TYPE }),
      fileName: attachment.fileName || payload.fileName
    }
  }

  return {
    isDraftAttachmentEncryptionEnabled,
    encryptDraftAttachment,
    decryptDownloadedAttachment
  }
}

function buildAttachmentE2eePayload(fingerprint: string, file: File): MailAttachmentE2eePayload {
  return {
    algorithm: ATTACHMENT_E2EE_ALGORITHM,
    recipientFingerprints: [fingerprint]
  }
}

function resolveAttachmentPasswords(
  externalSecureDelivery: MailComposeExternalSecureDelivery | null | undefined,
  translate: (key: string) => string
): string[] | undefined {
  if (!externalSecureDelivery?.enabled) {
    return undefined
  }
  const password = externalSecureDelivery.password?.trim() || ''
  if (!password) {
    throw new Error(translate('mailCompose.externalSecure.messages.passwordRequired'))
  }
  return [password]
}

function requireAttachmentE2ee(attachment: MailAttachment): MailAttachmentE2ee {
  const metadata = attachment.e2ee
  if (!metadata?.enabled || !metadata.algorithm) {
    throw new Error('Encrypted attachment metadata is required for local decrypt.')
  }
  return metadata
}

function toUint8Array(value: unknown): Uint8Array {
  if (value instanceof Uint8Array) {
    return value
  }
  throw new Error(`Unsupported OpenPGP binary payload type: ${Object.prototype.toString.call(value)}`)
}

function toBlobPart(bytes: Uint8Array): ArrayBuffer {
  return Uint8Array.from(bytes).buffer
}

function requireEncryptionProfile(
  profile: MailE2eeKeyProfile,
  translate: (key: string) => string
): MailE2eeKeyProfile {
  if (!profile.enabled || !profile.publicKeyArmored || !profile.fingerprint) {
    throw new Error(translate('mailCompose.attachments.e2ee.messages.profileMissing'))
  }
  return profile
}

function requireDecryptionProfile(
  profile: MailE2eeKeyProfile,
  translate: (key: string) => string
): MailE2eeKeyProfile {
  if (!profile.enabled || !profile.encryptedPrivateKeyArmored) {
    throw new Error(translate('mailCompose.attachments.e2ee.messages.privateKeyMissing'))
  }
  return profile
}
