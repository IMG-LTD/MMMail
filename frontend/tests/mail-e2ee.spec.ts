import { computed, defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const fetchMailE2eeKeyProfileMock = vi.fn()
const fetchMailE2eeRecoveryPackageMock = vi.fn()
const updateMailE2eeKeyProfileMock = vi.fn()
const updateMailE2eeRecoveryPackageMock = vi.fn()
const generateKeyMock = vi.fn()
const readKeyMock = vi.fn()
const readPrivateKeyMock = vi.fn()
const decryptKeyMock = vi.fn()
const encryptKeyMock = vi.fn()
const messageErrorMock = vi.fn()
const messageSuccessMock = vi.fn()
const messageWarningMock = vi.fn()

vi.mock('element-plus', () => ({
  ElMessage: {
    success: messageSuccessMock,
    warning: messageWarningMock,
    error: messageErrorMock
  }
}))

vi.mock('openpgp', () => ({
  generateKey: generateKeyMock,
  readKey: readKeyMock,
  readPrivateKey: readPrivateKeyMock,
  decryptKey: decryptKeyMock,
  encryptKey: encryptKeyMock
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string) => key
  })
}))

vi.mock('~/composables/useSettingsApi', () => ({
  useSettingsApi: () => ({
    fetchMailE2eeKeyProfile: fetchMailE2eeKeyProfileMock,
    updateMailE2eeKeyProfile: updateMailE2eeKeyProfileMock,
    fetchMailE2eeRecoveryPackage: fetchMailE2eeRecoveryPackageMock,
    updateMailE2eeRecoveryPackage: updateMailE2eeRecoveryPackageMock
  })
}))

vi.mock('~/stores/auth', () => ({
  useAuthStore: () => ({
    user: {
      email: 'alice@example.com',
      displayName: 'Alice'
    }
  })
}))

describe('mail e2ee foundation', () => {
  beforeEach(() => {
    fetchMailE2eeKeyProfileMock.mockReset()
    updateMailE2eeKeyProfileMock.mockReset()
    fetchMailE2eeRecoveryPackageMock.mockReset()
    updateMailE2eeRecoveryPackageMock.mockReset()
    generateKeyMock.mockReset()
    readKeyMock.mockReset()
    readPrivateKeyMock.mockReset()
    decryptKeyMock.mockReset()
    encryptKeyMock.mockReset()
    messageErrorMock.mockReset()
    messageSuccessMock.mockReset()
    messageWarningMock.mockReset()
    fetchMailE2eeKeyProfileMock.mockResolvedValue({
      enabled: false,
      fingerprint: null,
      algorithm: null,
      publicKeyArmored: null,
      encryptedPrivateKeyArmored: null,
      keyCreatedAt: null
    })
    fetchMailE2eeRecoveryPackageMock.mockResolvedValue({
      enabled: false,
      encryptedPrivateKeyArmored: null,
      updatedAt: null
    })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('generates and saves a key profile through the composable flow', async () => {
    generateKeyMock.mockResolvedValue({
      publicKey: 'PUBLIC_KEY',
      privateKey: 'PRIVATE_KEY'
    })
    readKeyMock.mockResolvedValue({
      getFingerprint: () => 'abcd1234abcd1234abcd1234abcd1234abcd1234'
    })
    updateMailE2eeKeyProfileMock.mockImplementation(async (payload) => ({
      enabled: payload.enabled,
      fingerprint: payload.fingerprint || null,
      algorithm: payload.algorithm || null,
      publicKeyArmored: payload.publicKeyArmored || null,
      encryptedPrivateKeyArmored: payload.encryptedPrivateKeyArmored || null,
      keyCreatedAt: payload.keyCreatedAt || null
    }))

    const wrapper = await mountHost()
    wrapper.vm.draft.identityName = 'Alice'
    wrapper.vm.draft.identityEmail = 'alice@example.com'
    wrapper.vm.draft.passphrase = 'very-secure-passphrase'
    wrapper.vm.draft.confirmPassphrase = 'very-secure-passphrase'

    await wrapper.vm.generateAndSaveProfile()
    await flushPromises()

    expect(generateKeyMock).toHaveBeenCalledTimes(1)
    expect(readKeyMock).toHaveBeenCalledWith({ armoredKey: 'PUBLIC_KEY' })
    expect(updateMailE2eeKeyProfileMock).toHaveBeenCalledTimes(1)
    expect(updateMailE2eeKeyProfileMock.mock.calls[0]?.[0]).toMatchObject({
      enabled: true,
      publicKeyArmored: 'PUBLIC_KEY',
      encryptedPrivateKeyArmored: 'PRIVATE_KEY',
      algorithm: 'curve25519Legacy',
      fingerprint: 'ABCD1234ABCD1234ABCD1234ABCD1234ABCD1234'
    })
    expect(wrapper.vm.profile.enabled).toBe(true)
    expect(messageSuccessMock).toHaveBeenCalledWith('settings.mailE2ee.messages.profileSaved')
  })

  it('shows validation warning when passphrase confirmation mismatches', async () => {
    const wrapper = await mountHost()
    wrapper.vm.draft.identityName = 'Alice'
    wrapper.vm.draft.identityEmail = 'alice@example.com'
    wrapper.vm.draft.passphrase = 'very-secure-passphrase'
    wrapper.vm.draft.confirmPassphrase = 'mismatch'

    const result = await wrapper.vm.generateAndSaveProfile()

    expect(result).toBe(false)
    expect(generateKeyMock).not.toHaveBeenCalled()
    expect(messageWarningMock).toHaveBeenCalledWith('settings.mailE2ee.messages.passphraseMismatch')
  })

  it('renders panel status and wires generate action', async () => {
    generateKeyMock.mockResolvedValue({
      publicKey: 'PUBLIC_KEY',
      privateKey: 'PRIVATE_KEY'
    })
    readKeyMock.mockResolvedValue({
      getFingerprint: () => 'abcd1234abcd1234abcd1234abcd1234abcd1234'
    })
    updateMailE2eeKeyProfileMock.mockImplementation(async (payload) => ({
      enabled: payload.enabled,
      fingerprint: payload.fingerprint || null,
      algorithm: payload.algorithm || null,
      publicKeyArmored: payload.publicKeyArmored || null,
      encryptedPrivateKeyArmored: payload.encryptedPrivateKeyArmored || null,
      keyCreatedAt: payload.keyCreatedAt || null
    }))

    const { default: SettingsMailE2eePanel } = await import('~/components/settings/SettingsMailE2eePanel.vue')
    const wrapper = mount(SettingsMailE2eePanel, {
      global: {
        directives: {
          loading: {}
        },
        stubs: {
          ElAlert: {
            props: ['title', 'description'],
            template: '<div><span>{{ title }}</span><span>{{ description }}</span></div>'
          },
          ElButton: {
            props: ['disabled', 'loading'],
            emits: ['click'],
            template: '<button :disabled="disabled" @click="$emit(`click`)"><slot /></button>'
          },
          ElForm: { template: '<form><slot /></form>' },
          ElFormItem: { template: '<div><slot /></div>' },
          ElInput: {
            props: ['modelValue'],
            emits: ['update:modelValue'],
            template: '<input :value="modelValue" @input="$emit(`update:modelValue`, $event.target.value)" />'
          }
        }
      }
    })
    await flushPromises()

    expect(wrapper.get('[data-testid="settings-mail-e2ee-status"]').text()).toContain('settings.mailE2ee.values.disabled')
    await wrapper.get('[data-testid="settings-mail-e2ee-passphrase"]').setValue('very-secure-passphrase')
    await wrapper.get('[data-testid="settings-mail-e2ee-confirm-passphrase"]').setValue('very-secure-passphrase')
    await wrapper.get('[data-testid="settings-mail-e2ee-generate"]').trigger('click')
    await flushPromises()
    expect(generateKeyMock).toHaveBeenCalledTimes(1)
    expect(wrapper.get('[data-testid="settings-mail-e2ee-status"]').text()).toContain('settings.mailE2ee.values.enabled')
    await wrapper.get('[data-testid="settings-mail-e2ee-disable"]').trigger('click')
    await flushPromises()
    expect(updateMailE2eeKeyProfileMock).toHaveBeenLastCalledWith({ enabled: false })
  })

  it('creates and restores a recovery package through the recovery composable', async () => {
    fetchMailE2eeKeyProfileMock.mockResolvedValue({
      enabled: true,
      fingerprint: 'ABCD1234ABCD1234ABCD1234ABCD1234ABCD1234',
      algorithm: 'curve25519Legacy',
      publicKeyArmored: 'PUBLIC_KEY',
      encryptedPrivateKeyArmored: 'PRIVATE_KEY',
      keyCreatedAt: '2026-04-02T15:40:00'
    })
    readPrivateKeyMock.mockResolvedValue({ key: 'PRIVATE_KEY' })
    decryptKeyMock.mockResolvedValue({ key: 'UNLOCKED_KEY' })
    encryptKeyMock
      .mockResolvedValueOnce({ armor: () => 'RECOVERY_PACKAGE' })
      .mockResolvedValueOnce({ armor: () => 'RESTORED_PRIVATE_KEY' })
    updateMailE2eeRecoveryPackageMock.mockResolvedValue({
      enabled: true,
      encryptedPrivateKeyArmored: 'RECOVERY_PACKAGE',
      updatedAt: '2026-04-02T15:41:00'
    })
    updateMailE2eeKeyProfileMock.mockResolvedValue({
      enabled: true,
      fingerprint: 'ABCD1234ABCD1234ABCD1234ABCD1234ABCD1234',
      algorithm: 'curve25519Legacy',
      publicKeyArmored: 'PUBLIC_KEY',
      encryptedPrivateKeyArmored: 'RESTORED_PRIVATE_KEY',
      keyCreatedAt: '2026-04-02T15:40:00'
    })

    const wrapper = await mountRecoveryHost()
    wrapper.vm.recoveryDraft.currentPassphrase = 'current-passphrase'
    wrapper.vm.recoveryDraft.recoveryPassphrase = 'recovery-passphrase'
    wrapper.vm.recoveryDraft.confirmRecoveryPassphrase = 'recovery-passphrase'

    const saved = await wrapper.vm.saveRecoveryPackage()
    await flushPromises()

    expect(saved).toBe(true)
    expect(updateMailE2eeRecoveryPackageMock).toHaveBeenCalledWith({
      enabled: true,
      encryptedPrivateKeyArmored: 'RECOVERY_PACKAGE'
    })

    wrapper.vm.restoreDraft.recoveryPassphrase = 'recovery-passphrase'
    wrapper.vm.restoreDraft.nextPassphrase = 'new-daily-passphrase'
    wrapper.vm.restoreDraft.confirmNextPassphrase = 'new-daily-passphrase'

    const restored = await wrapper.vm.restoreFromRecoveryPackage()
    await flushPromises()

    expect(restored).toBe(true)
    expect(updateMailE2eeKeyProfileMock).toHaveBeenCalledWith({
      enabled: true,
      publicKeyArmored: 'PUBLIC_KEY',
      encryptedPrivateKeyArmored: 'RESTORED_PRIVATE_KEY',
      fingerprint: 'ABCD1234ABCD1234ABCD1234ABCD1234ABCD1234',
      algorithm: 'curve25519Legacy',
      keyCreatedAt: '2026-04-02T15:40:00'
    })
  })
})

async function mountHost() {
  const { useMailE2ee } = await import('~/composables/useMailE2ee')
  const Host = defineComponent({
    setup() {
      return useMailE2ee({
        defaultIdentityName: computed(() => 'Alice'),
        defaultIdentityEmail: computed(() => 'alice@example.com')
      })
    },
    template: '<div />'
  })

  const wrapper = mount(Host)
  await flushPromises()
  return wrapper
}

async function mountRecoveryHost() {
  const { useMailE2ee } = await import('~/composables/useMailE2ee')
  const { useMailE2eeRecovery } = await import('~/composables/useMailE2eeRecovery')
  const Host = defineComponent({
    setup() {
      const foundation = useMailE2ee({
        defaultIdentityName: computed(() => 'Alice'),
        defaultIdentityEmail: computed(() => 'alice@example.com')
      })
      return {
        ...foundation,
        ...useMailE2eeRecovery({
          profile: foundation.profile
        })
      }
    },
    template: '<div />'
  })

  const wrapper = mount(Host)
  await wrapper.vm.initializeMailE2ee()
  await wrapper.vm.initializeRecovery()
  await flushPromises()
  return wrapper
}
