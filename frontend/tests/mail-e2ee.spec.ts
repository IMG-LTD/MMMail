import { computed, defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const fetchMailE2eeKeyProfileMock = vi.fn()
const updateMailE2eeKeyProfileMock = vi.fn()
const generateKeyMock = vi.fn()
const readKeyMock = vi.fn()
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
  readKey: readKeyMock
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string) => key
  })
}))

vi.mock('~/composables/useSettingsApi', () => ({
  useSettingsApi: () => ({
    fetchMailE2eeKeyProfile: fetchMailE2eeKeyProfileMock,
    updateMailE2eeKeyProfile: updateMailE2eeKeyProfileMock
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
    generateKeyMock.mockReset()
    readKeyMock.mockReset()
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
