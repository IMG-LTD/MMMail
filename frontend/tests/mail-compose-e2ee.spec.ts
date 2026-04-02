import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { decrypt, decryptKey, generateKey, readKey, readMessage, readPrivateKey } from 'openpgp'

const fetchRecipientE2eeStatusMock = vi.fn()
const fetchMailE2eeKeyProfileMock = vi.fn()

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string) => key
  })
}))

vi.mock('~/composables/useMailApi', () => ({
  useMailApi: () => ({
    fetchRecipientE2eeStatus: fetchRecipientE2eeStatusMock
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
let receiverKey: KeyMaterial

describe('mail compose e2ee readiness', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    fetchRecipientE2eeStatusMock.mockReset()
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  it('loads recipient readiness after debounce', async () => {
    fetchRecipientE2eeStatusMock.mockResolvedValue({
      toEmail: 'alice@example.com',
      fromEmail: 'sender@example.com',
      deliverable: true,
      encryptionReady: true,
      readiness: 'READY',
      routeCount: 1,
      routes: [{
        targetEmail: 'alice@example.com',
        forwardToEmail: 'alice@example.com',
        keyAvailable: true,
        fingerprint: 'ABCD',
        algorithm: 'curve25519Legacy',
        publicKeyArmored: 'PUBLIC_KEY'
      }]
    })

    const wrapper = await mountHost()
    wrapper.vm.scheduleRecipientE2eeRefresh('alice@example.com', 'sender@example.com')
    await vi.advanceTimersByTimeAsync(250)
    await flushPromises()

    expect(fetchRecipientE2eeStatusMock).toHaveBeenCalledWith('alice@example.com', 'sender@example.com')
    expect(wrapper.vm.recipientE2eeStatus?.readiness).toBe('READY')
    expect(wrapper.vm.recipientE2eeError).toBe('')
  })

  it('clears readiness state for invalid recipient address', async () => {
    const wrapper = await mountHost()
    wrapper.vm.scheduleRecipientE2eeRefresh('invalid-address', 'sender@example.com')
    await vi.advanceTimersByTimeAsync(250)
    await flushPromises()

    expect(fetchRecipientE2eeStatusMock).not.toHaveBeenCalled()
    expect(wrapper.vm.recipientE2eeStatus).toBeNull()
    expect(wrapper.vm.recipientE2eeError).toBe('')
  })

  it('surfaces lookup errors without hiding them', async () => {
    fetchRecipientE2eeStatusMock.mockRejectedValue(new Error('lookup failed'))

    const wrapper = await mountHost()
    wrapper.vm.scheduleRecipientE2eeRefresh('alice@example.com', 'sender@example.com')
    await vi.advanceTimersByTimeAsync(250)
    await flushPromises()

    expect(wrapper.vm.recipientE2eeStatus).toBeNull()
    expect(wrapper.vm.recipientE2eeError).toBe('lookup failed')
  })
})

describe('mail compose message e2ee send', () => {
  beforeAll(async () => {
    senderKey = await createKeyMaterial('sender@mmmail.local', 'sender-passphrase-123')
    receiverKey = await createKeyMaterial('receiver@mmmail.local', 'receiver-passphrase-123')
  })

  beforeEach(() => {
    fetchMailE2eeKeyProfileMock.mockReset()
    fetchMailE2eeKeyProfileMock.mockResolvedValue({
      enabled: true,
      fingerprint: senderKey.fingerprint,
      algorithm: 'curve25519Legacy',
      publicKeyArmored: senderKey.publicKey,
      encryptedPrivateKeyArmored: senderKey.privateKey,
      keyCreatedAt: '2026-04-01T21:00:00'
    })
  })

  it('encrypts body for ready routes and removes plaintext from send payload', async () => {
    const { useMailComposeMessageE2ee } = await import('~/composables/useMailComposeMessageE2ee')
    const { buildSendPayload } = useMailComposeMessageE2ee()

    const result = await buildSendPayload({
      toEmail: 'receiver@mmmail.local',
      fromEmail: 'sender@mmmail.local',
      subject: 'Encrypted subject',
      body: 'Encrypted compose body',
      idempotencyKey: 'compose-e2ee-send',
      labels: []
    }, {
      toEmail: 'receiver@mmmail.local',
      fromEmail: 'sender@mmmail.local',
      deliverable: true,
      encryptionReady: true,
      readiness: 'READY',
      routeCount: 1,
      routes: [{
        targetEmail: 'receiver@mmmail.local',
        forwardToEmail: 'receiver@mmmail.local',
        keyAvailable: true,
        fingerprint: receiverKey.fingerprint,
        algorithm: 'curve25519Legacy',
        publicKeyArmored: receiverKey.publicKey
      }]
    })

    expect(result.body).toBeUndefined()
    expect(result.e2ee?.algorithm).toBe('openpgp')
    expect(result.e2ee?.recipientFingerprints).toEqual([receiverKey.fingerprint, senderKey.fingerprint])

    const plainText = await decryptCiphertext(result.e2ee?.encryptedBody as string, senderKey)
    expect(plainText).toBe('Encrypted compose body')
  })
})

async function mountHost() {
  const { useMailComposeE2ee } = await import('~/composables/useMailComposeE2ee')
  const Host = defineComponent({
    setup() {
      return useMailComposeE2ee()
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
