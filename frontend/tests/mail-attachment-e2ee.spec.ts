import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { MailAttachment } from '~/types/api'

const fetchMailE2eeKeyProfileMock = vi.fn()

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string) => key
  })
}))

vi.mock('~/composables/useSettingsApi', () => ({
  useSettingsApi: () => ({
    fetchMailE2eeKeyProfile: fetchMailE2eeKeyProfileMock
  })
}))

describe('mail attachment e2ee composable', () => {
  beforeEach(() => {
    fetchMailE2eeKeyProfileMock.mockReset()
    fetchMailE2eeKeyProfileMock.mockResolvedValue({
      enabled: true,
      fingerprint: 'FINGERPRINT',
      algorithm: 'curve25519Legacy',
      publicKeyArmored: 'PUBLIC',
      encryptedPrivateKeyArmored: 'PRIVATE',
      keyCreatedAt: '2026-04-02T17:20:00'
    })
  })

  it('reports attachment encryption enabled only when profile is ready', async () => {
    const { useMailAttachmentE2ee } = await import('~/composables/useMailAttachmentE2ee')
    const { isDraftAttachmentEncryptionEnabled } = useMailAttachmentE2ee()

    await expect(isDraftAttachmentEncryptionEnabled()).resolves.toBe(true)
    fetchMailE2eeKeyProfileMock.mockResolvedValueOnce({
      enabled: true,
      fingerprint: null,
      algorithm: 'curve25519Legacy',
      publicKeyArmored: 'PUBLIC',
      encryptedPrivateKeyArmored: 'PRIVATE',
      keyCreatedAt: '2026-04-02T17:20:00'
    })
    await expect(isDraftAttachmentEncryptionEnabled()).resolves.toBe(false)
  })

  it('requires passphrase before local decrypt', async () => {
    const { useMailAttachmentE2ee } = await import('~/composables/useMailAttachmentE2ee')
    const { decryptDownloadedAttachment } = useMailAttachmentE2ee()
    const attachment = buildEncryptedAttachment()

    await expect(decryptDownloadedAttachment({
      blob: new Blob(['ciphertext']),
      fileName: 'draft.txt.pgp'
    }, attachment, '')).rejects.toThrow('mailCompose.attachments.e2ee.messages.passphraseRequired')
  })

  it('fails fast when attachment e2ee metadata is missing', async () => {
    const { useMailAttachmentE2ee } = await import('~/composables/useMailAttachmentE2ee')
    const { decryptDownloadedAttachment } = useMailAttachmentE2ee()
    const attachment: MailAttachment = {
      id: 'att-1',
      mailId: '42',
      fileName: 'draft.txt.pgp',
      contentType: 'application/octet-stream',
      fileSize: 8
    }

    await expect(decryptDownloadedAttachment({
      blob: new Blob(['ciphertext']),
      fileName: 'draft.txt.pgp'
    }, attachment, 'valid-passphrase')).rejects.toThrow('Encrypted attachment metadata is required for local decrypt.')
  })
})

function buildEncryptedAttachment(): MailAttachment {
  return {
    id: 'att-1',
    mailId: '42',
    fileName: 'draft.txt.pgp',
    contentType: 'application/octet-stream',
    fileSize: 8,
    e2ee: {
      enabled: true,
      algorithm: 'openpgp',
      recipientFingerprints: ['FINGERPRINT']
    }
  }
}
