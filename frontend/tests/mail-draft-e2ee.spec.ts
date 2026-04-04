import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { decrypt, decryptKey, generateKey, readMessage, readPrivateKey, readKey } from 'openpgp'
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

interface KeyMaterial {
  publicKey: string
  privateKey: string
  fingerprint: string
  passphrase: string
}

let senderKey: KeyMaterial

describe('mail draft e2ee', () => {
  beforeAll(async () => {
    senderKey = await createKeyMaterial('draft-owner@mmmail.local', 'draft-owner-passphrase')
  })

  beforeEach(() => {
    fetchMailE2eeKeyProfileMock.mockReset()
    fetchMailE2eeKeyProfileMock.mockResolvedValue({
      enabled: true,
      fingerprint: senderKey.fingerprint,
      algorithm: 'curve25519Legacy',
      publicKeyArmored: senderKey.publicKey,
      encryptedPrivateKeyArmored: senderKey.privateKey,
      keyCreatedAt: '2026-04-02T15:30:00'
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('encrypts draft body for the current sender key', async () => {
    const { useMailDraftE2ee } = await import('~/composables/useMailDraftE2ee')
    const { buildDraftPayload } = useMailDraftE2ee()

    const result = await buildDraftPayload({
      toEmail: 'target@example.com',
      subject: 'Encrypted draft',
      body: 'Draft body secret'
    })

    expect(result.body).toBeUndefined()
    expect(result.e2ee?.algorithm).toBe('openpgp')
    expect(result.e2ee?.recipientFingerprints).toEqual([senderKey.fingerprint])

    const decryptedBody = await decryptCiphertext(result.e2ee?.encryptedBody as string, senderKey)
    expect(decryptedBody).toBe('Draft body secret')
  })

  it('decrypts encrypted draft body locally with the current sender key', async () => {
    const encryptedBody = await encryptForSelf('Locked draft body', senderKey.publicKey)
    const wrapper = await mountDraftDecryptHost()

    wrapper.vm.passphrase = senderKey.passphrase
    await wrapper.vm.decryptEncryptedBody(encryptedBody)

    expect(wrapper.vm.decryptedBody).toBe('Locked draft body')
    expect(wrapper.vm.decryptError).toBe('')
    expect(wrapper.vm.passphrase).toBe('')
  })

  it('encrypts draft attachment and decrypts it locally with metadata', async () => {
    const { useMailAttachmentE2ee } = await vi.importActual<typeof import('~/composables/useMailAttachmentE2ee')>(
      '~/composables/useMailAttachmentE2ee'
    )
    const { encryptDraftAttachment, decryptDownloadedAttachment } = useMailAttachmentE2ee()
    const file = new File(['draft attachment body'], 'draft.txt', { type: 'text/plain' })

    const encrypted = await encryptDraftAttachment(file)
    expect(encrypted.fileName).toBe('draft.txt')
    expect(encrypted.file.name).toBe('draft.txt.pgp')
    expect(encrypted.contentType).toBe('text/plain')
    expect(encrypted.fileSize).toBe(file.size)
    expect(encrypted.e2ee.algorithm).toBe('openpgp')
    expect(encrypted.e2ee.recipientFingerprints).toEqual([senderKey.fingerprint])

    const attachment: MailAttachment = {
      id: 'att-1',
      mailId: '42',
      fileName: encrypted.fileName,
      contentType: encrypted.contentType,
      fileSize: encrypted.fileSize,
      e2ee: {
        enabled: true,
        algorithm: encrypted.e2ee.algorithm,
        recipientFingerprints: encrypted.e2ee.recipientFingerprints
      }
    }
    const decrypted = await decryptDownloadedAttachment({
      blob: encrypted.file,
      fileName: encrypted.fileName
    }, attachment, senderKey.passphrase)
    expect(decrypted.fileName).toBe('draft.txt')
    expect(await decrypted.blob.text()).toBe('draft attachment body')
  })
})

async function mountDraftDecryptHost() {
  const { useMailDetailE2ee } = await import('~/composables/useMailDetailE2ee')
  const Host = defineComponent({
    setup() {
      return useMailDetailE2ee()
    },
    template: '<div />'
  })
  return mount(Host)
}

async function createKeyMaterial(email: string, passphrase: string): Promise<KeyMaterial> {
  const { publicKey, privateKey } = await generateKey({
    type: 'ecc',
    curve: 'curve25519Legacy',
    userIDs: [{ name: email, email }],
    passphrase,
    format: 'armored'
  })
  const publicKeyObject = await readKey({ armoredKey: publicKey })
  return {
    publicKey,
    privateKey,
    fingerprint: publicKeyObject.getFingerprint().toUpperCase(),
    passphrase
  }
}

async function encryptForSelf(text: string, publicKeyArmored: string): Promise<string> {
  const publicKey = await readKey({ armoredKey: publicKeyArmored })
  const { createMessage, encrypt } = await import('openpgp')
  const message = await createMessage({ text })
  return await encrypt({
    message,
    encryptionKeys: publicKey,
    format: 'armored'
  })
}

async function decryptCiphertext(ciphertext: string, material: KeyMaterial): Promise<string> {
  const privateKey = await readPrivateKey({ armoredKey: material.privateKey })
  const unlockedKey = await decryptKey({ privateKey, passphrase: material.passphrase })
  const message = await readMessage({ armoredMessage: ciphertext })
  const result = await decrypt({
    message,
    decryptionKeys: unlockedKey,
    format: 'utf8'
  })
  return String(result.data || '')
}
