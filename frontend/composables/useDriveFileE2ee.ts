import { createMessage, decrypt, decryptKey, encrypt, readKey, readMessage, readPrivateKey } from 'openpgp'
import { useI18n } from '~/composables/useI18n'
import { useSettingsApi } from '~/composables/useSettingsApi'
import type {
  DriveEncryptedPublicShareE2eePayload,
  DriveItem,
  DriveUploadE2eePayload,
  MailE2eeKeyProfile,
} from '~/types/api'

const DRIVE_E2EE_ALGORITHM = 'openpgp'
const DRIVE_PUBLIC_SHARE_E2EE_ALGORITHM = 'openpgp-password'
const ENCRYPTED_FILE_CONTENT_TYPE = 'application/octet-stream'
const DEFAULT_FILE_CONTENT_TYPE = 'application/octet-stream'

export interface DriveEncryptedUpload {
  file: File
  e2ee: DriveUploadE2eePayload
}

export interface DriveDownloadedPayload {
  blob: Blob
  fileName: string
}

export interface DriveEncryptedPublicShare {
  file: File
  e2ee: DriveEncryptedPublicShareE2eePayload
}

export function useDriveFileE2ee() {
  const { t } = useI18n()
  const { fetchMailE2eeKeyProfile } = useSettingsApi()

  async function isDriveFileEncryptionEnabled(): Promise<boolean> {
    const profile = await fetchMailE2eeKeyProfile()
    return Boolean(profile.enabled && profile.publicKeyArmored && profile.fingerprint)
  }

  async function encryptOwnedFile(file: File): Promise<DriveEncryptedUpload> {
    const profile = requireEncryptionProfile(await fetchMailE2eeKeyProfile(), t)
    const publicKey = await readKey({ armoredKey: profile.publicKeyArmored as string })
    const message = await createMessage({ binary: new Uint8Array(await file.arrayBuffer()) })
    const encryptedBinary = await encrypt({
      message,
      encryptionKeys: publicKey,
      format: 'binary',
    })
    const encryptedBytes = toUint8Array(encryptedBinary)
    return {
      file: new File([toBlobPart(encryptedBytes)], `${file.name}.pgp`, {
        type: ENCRYPTED_FILE_CONTENT_TYPE,
      }),
      e2ee: {
        enabled: true,
        algorithm: DRIVE_E2EE_ALGORITHM,
        recipientFingerprints: [profile.fingerprint as string],
        fileName: file.name,
        contentType: file.type || DEFAULT_FILE_CONTENT_TYPE,
        fileSize: file.size,
      },
    }
  }

  async function decryptOwnedFile(
    payload: DriveDownloadedPayload,
    item: DriveItem,
    passphrase: string,
  ): Promise<DriveDownloadedPayload> {
    const normalizedPassphrase = requirePassphrase(passphrase, t)
    const metadata = requireDriveE2ee(item)
    const profile = requireDecryptionProfile(await fetchMailE2eeKeyProfile(), t)
    const privateKey = await readPrivateKey({ armoredKey: profile.encryptedPrivateKeyArmored as string })
    const unlockedKey = await decryptKey({ privateKey, passphrase: normalizedPassphrase })
    const message = await readMessage({ binaryMessage: new Uint8Array(await payload.blob.arrayBuffer()) })
    const decryptedResult = await decrypt({
      message,
      decryptionKeys: unlockedKey,
      format: 'binary',
    })
    const plaintextBytes = toUint8Array(decryptedResult.data)
    return {
      blob: new Blob([toBlobPart(plaintextBytes)], { type: item.mimeType || DEFAULT_FILE_CONTENT_TYPE }),
      fileName: item.name || payload.fileName || resolveEncryptedName(metadata),
    }
  }

  async function encryptPublicShareFile(
    payload: DriveDownloadedPayload,
    item: DriveItem,
    ownerPassphrase: string,
    sharePassword: string,
  ): Promise<DriveEncryptedPublicShare> {
    const normalizedSharePassword = requireSharePassword(sharePassword, t)
    const decryptedFile = await decryptOwnedFile(payload, item, ownerPassphrase)
    const message = await createMessage({ binary: new Uint8Array(await decryptedFile.blob.arrayBuffer()) })
    const encryptedBinary = await encrypt({
      message,
      passwords: [normalizedSharePassword],
      format: 'binary',
    })
    const encryptedBytes = toUint8Array(encryptedBinary)
    return {
      file: new File([toBlobPart(encryptedBytes)], `${decryptedFile.fileName}.pgp`, {
        type: ENCRYPTED_FILE_CONTENT_TYPE,
      }),
      e2ee: {
        enabled: true,
        algorithm: DRIVE_PUBLIC_SHARE_E2EE_ALGORITHM,
        mode: 'PASSWORD',
        fileName: decryptedFile.fileName,
        contentType: item.mimeType || DEFAULT_FILE_CONTENT_TYPE,
        fileSize: decryptedFile.blob.size,
      },
    }
  }

  async function decryptPublicShareFile(
    payload: DriveDownloadedPayload,
    item: DriveItem,
    sharePassword: string,
  ): Promise<DriveDownloadedPayload> {
    requireDriveE2ee(item)
    const normalizedSharePassword = requireSharePassword(sharePassword, t)
    const message = await readMessage({ binaryMessage: new Uint8Array(await payload.blob.arrayBuffer()) })
    const decryptedResult = await decrypt({
      message,
      passwords: [normalizedSharePassword],
      format: 'binary',
    })
    const plaintextBytes = toUint8Array(decryptedResult.data)
    return {
      blob: new Blob([toBlobPart(plaintextBytes)], { type: item.mimeType || DEFAULT_FILE_CONTENT_TYPE }),
      fileName: item.name || payload.fileName || resolveEncryptedName(item.e2ee),
    }
  }

  return {
    isDriveFileEncryptionEnabled,
    encryptOwnedFile,
    decryptOwnedFile,
    encryptPublicShareFile,
    decryptPublicShareFile,
  }
}

function resolveEncryptedName(item: DriveItem['e2ee']): string {
  return item?.enabled ? 'drive-file' : 'drive-file'
}

function requireDriveE2ee(item: DriveItem) {
  if (!item.e2ee?.enabled || !item.e2ee.algorithm) {
    throw new Error('Encrypted Drive file metadata is required for local decrypt.')
  }
  return item.e2ee
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
  translate: (key: string) => string,
): MailE2eeKeyProfile {
  if (!profile.enabled || !profile.publicKeyArmored || !profile.fingerprint) {
    throw new Error(translate('drive.messages.e2eeProfileMissing'))
  }
  return profile
}

function requireDecryptionProfile(
  profile: MailE2eeKeyProfile,
  translate: (key: string) => string,
): MailE2eeKeyProfile {
  if (!profile.enabled || !profile.encryptedPrivateKeyArmored) {
    throw new Error(translate('drive.messages.e2eePrivateKeyMissing'))
  }
  return profile
}

function requirePassphrase(passphrase: string, translate: (key: string) => string): string {
  const value = passphrase.trim()
  if (!value) {
    throw new Error(translate('drive.messages.e2eePassphraseRequired'))
  }
  return value
}

function requireSharePassword(password: string, translate: (key: string) => string): string {
  const value = password.trim()
  if (!value) {
    throw new Error(translate('drive.shareDrawer.e2ee.passwordRequired'))
  }
  return value
}
