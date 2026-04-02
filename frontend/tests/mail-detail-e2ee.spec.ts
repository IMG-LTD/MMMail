import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { encrypt, generateKey, readKey } from 'openpgp'

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

let keyMaterial: KeyMaterial
let encryptedBody = ''

describe('mail detail e2ee decrypt', () => {
  beforeAll(async () => {
    keyMaterial = await createKeyMaterial()
    encryptedBody = await createEncryptedBody('Encrypted body line', keyMaterial)
  })

  beforeEach(() => {
    fetchMailE2eeKeyProfileMock.mockReset()
    fetchMailE2eeKeyProfileMock.mockResolvedValue({
      enabled: true,
      fingerprint: keyMaterial.fingerprint,
      algorithm: 'curve25519Legacy',
      publicKeyArmored: keyMaterial.publicKey,
      encryptedPrivateKeyArmored: keyMaterial.privateKey,
      keyCreatedAt: '2026-04-01T21:00:00'
    })
  })

  it('decrypts encrypted body locally with passphrase', async () => {
    const wrapper = await mountHost()
    wrapper.vm.passphrase = keyMaterial.passphrase
    await wrapper.vm.decryptEncryptedBody(encryptedBody)

    expect(wrapper.vm.decryptedBody).toBe('Encrypted body line')
    expect(wrapper.vm.decryptError).toBe('')
    expect(wrapper.vm.passphrase).toBe('')
  })
})

async function mountHost() {
  const { useMailDetailE2ee } = await import('~/composables/useMailDetailE2ee')
  const Host = defineComponent({
    setup() {
      return useMailDetailE2ee()
    },
    template: '<div />'
  })
  return mount(Host)
}

async function createKeyMaterial(): Promise<KeyMaterial> {
  const passphrase = 'detail-passphrase-123'
  const { publicKey, privateKey } = await generateKey({
    type: 'ecc',
    curve: 'curve25519Legacy',
    userIDs: [{ name: 'Detail Owner', email: 'detail@mmmail.local' }],
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

async function createEncryptedBody(text: string, material: KeyMaterial): Promise<string> {
  const message = await createMessage(text)
  const publicKey = await readKey({ armoredKey: material.publicKey })
  return encrypt({
    message,
    encryptionKeys: publicKey,
    format: 'armored'
  })
}

async function createMessage(text: string) {
  const { createMessage } = await import('openpgp')
  return createMessage({ text })
}
