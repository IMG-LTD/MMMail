import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { createMessage, encrypt } from 'openpgp'

const messageErrorMock = vi.fn()
const messageSuccessMock = vi.fn()
const getPublicSecureLinkMock = vi.fn()

vi.mock('element-plus', () => ({
  ElMessage: {
    error: messageErrorMock,
    success: messageSuccessMock,
  },
}))

vi.mock('~/composables/useMailApi', () => ({
  useMailApi: () => ({
    getPublicSecureLink: getPublicSecureLinkMock,
  }),
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, string | number>) => {
      if (!params) {
        return key
      }
      return Object.entries(params).reduce(
        (result, [paramKey, value]) => result.replace(`{${paramKey}}`, String(value)),
        key,
      )
    },
  }),
}))

const ElButton = defineComponent({
  name: 'ElButton',
  props: { loading: { type: Boolean, default: false } },
  emits: ['click'],
  template: '<button v-bind="$attrs" type="button" @click="$emit(\'click\')"><slot /></button>',
})

const ElForm = defineComponent({
  name: 'ElForm',
  template: '<form><slot /></form>',
})

const ElFormItem = defineComponent({
  name: 'ElFormItem',
  props: { label: { type: String, default: '' } },
  template: '<label><span>{{ label }}</span><slot /></label>',
})

const ElInput = defineComponent({
  name: 'ElInput',
  props: {
    modelValue: { type: String, default: '' },
    placeholder: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  template: `
    <input
      v-bind="$attrs"
      :value="modelValue"
      :placeholder="placeholder"
      @input="$emit('update:modelValue', $event.target.value)"
    >
  `,
})

async function mountPublicMailPage() {
  const { default: PublicMailPage } = await import('~/pages/share/mail/[token].vue')
  return mount(PublicMailPage, {
    global: {
      stubs: {
        ElButton,
        ElForm,
        ElFormItem,
        ElInput,
      },
    },
  })
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.stubGlobal('useRoute', () => ({
    params: { token: 'mail-share-token' },
  }))
  vi.stubGlobal('definePageMeta', vi.fn())
})

describe('mail public share', () => {
  it('loads secure link metadata and decrypts ciphertext locally with password', async () => {
    const password = 'ExternalPass@123'
    const message = await createMessage({ text: 'External mail plaintext' })
    const ciphertext = await encrypt({
      message,
      passwords: [password],
      format: 'armored',
    })
    getPublicSecureLinkMock.mockResolvedValue({
      mailId: 'mail-1',
      subject: 'Shared secure mail',
      senderEmail: 'sender@mmmail.local',
      recipientEmail: 'external@example.net',
      bodyCiphertext: ciphertext,
      algorithm: 'openpgp',
      passwordHint: 'shared out-of-band',
      expiresAt: '2026-04-21T12:00:00',
    })

    const wrapper = await mountPublicMailPage()
    await flushPromises()

    expect(getPublicSecureLinkMock).toHaveBeenCalledWith('mail-share-token')
    await wrapper.get('[data-testid="mail-public-password"]').setValue(password)
    await wrapper.get('[data-testid="mail-public-unlock"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Shared secure mail')
    expect(wrapper.text()).toContain('External mail plaintext')
    expect(messageSuccessMock).toHaveBeenCalledWith('mailPublicShare.messages.decryptSuccess')
  })

  it('maps expired secure link errors to the public error copy', async () => {
    getPublicSecureLinkMock.mockRejectedValue(new Error('Mail secure link has expired'))

    const wrapper = await mountPublicMailPage()
    await flushPromises()

    expect(wrapper.text()).toContain('mailPublicShare.errors.expired')
    expect(messageErrorMock).not.toHaveBeenCalled()
  })
})
