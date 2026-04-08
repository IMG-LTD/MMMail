import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { axe } from 'vitest-axe'
import SuiteReleaseBoundaryPanel from '~/components/suite/SuiteReleaseBoundaryPanel.vue'
import SuiteSectionNav from '~/components/suite/SuiteSectionNav.vue'
import { SUITE_SECTIONS } from '~/utils/suite-sections'

const getPublicSecureLinkMock = vi.fn()
const downloadPublicSecureAttachmentMock = vi.fn()

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn()
  }
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, string | number>) => {
      if (!params) {
        return key
      }
      return Object.entries(params).reduce((result, [paramKey, value]) => {
        return result.replace(`{${paramKey}}`, String(value))
      }, key)
    }
  })
}))

vi.mock('~/composables/useMailApi', () => ({
  useMailApi: () => ({
    getPublicSecureLink: getPublicSecureLinkMock,
    downloadPublicSecureAttachment: downloadPublicSecureAttachmentMock
  })
}))

const stubs = {
  ElTag: defineComponent({
    name: 'ElTag',
    template: '<span><slot /></span>'
  }),
  ElButton: defineComponent({
    name: 'ElButton',
    props: {
      loading: { type: Boolean, default: false }
    },
    emits: ['click'],
    template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
  }),
  ElForm: defineComponent({
    name: 'ElForm',
    template: '<form><slot /></form>'
  }),
  ElFormItem: defineComponent({
    name: 'ElFormItem',
    props: {
      label: { type: String, default: '' }
    },
    template: '<label><span>{{ label }}</span><slot /></label>'
  }),
  ElInput: defineComponent({
    name: 'ElInput',
    props: {
      modelValue: { type: String, default: '' },
      placeholder: { type: String, default: '' }
    },
    emits: ['update:modelValue'],
    template: `
      <input
        :value="modelValue"
        :placeholder="placeholder"
        @input="$emit('update:modelValue', $event.target.value)"
      >
    `
  })
}

describe('runtime a11y gates', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getPublicSecureLinkMock.mockResolvedValue({
      mailId: 'mail-share-1',
      subject: 'Shared secure mail',
      senderEmail: 'sender@mmmail.local',
      recipientEmail: 'external@example.net',
      bodyCiphertext: 'ciphertext-placeholder',
      algorithm: 'openpgp',
      passwordHint: 'hint',
      expiresAt: '2026-04-21T12:00:00',
      attachments: []
    })
    ;(globalThis as typeof globalThis & {
      definePageMeta?: (value: unknown) => void
      useRoute?: () => { params: { token: string } }
    }).definePageMeta = vi.fn()
    ;(globalThis as typeof globalThis & {
      definePageMeta?: (value: unknown) => void
      useRoute?: () => { params: { token: string } }
    }).useRoute = () => ({
      params: {
        token: 'mail-share-token'
      }
    })
  })

  afterEach(() => {
    delete (globalThis as typeof globalThis & { definePageMeta?: unknown }).definePageMeta
    delete (globalThis as typeof globalThis & { useRoute?: unknown }).useRoute
  })

  it('keeps suite section navigation free of obvious runtime accessibility violations', async () => {
    const wrapper = mount(SuiteSectionNav, {
      props: {
        activeSection: 'overview',
        sections: SUITE_SECTIONS
      }
    })

    const result = await axe(wrapper.element)
    expect(result.violations).toHaveLength(0)
  })

  it('keeps the boundary panel free of obvious runtime accessibility violations', async () => {
    const wrapper = mount(SuiteReleaseBoundaryPanel, {
      global: {
        stubs
      }
    })

    const result = await axe(wrapper.element)
    expect(result.violations).toHaveLength(0)
  })

  it('keeps public secure mail share free of obvious runtime accessibility violations', async () => {
    const { default: PublicMailPage } = await import('~/pages/share/mail/[token].vue')
    const wrapper = mount(PublicMailPage, {
      global: {
        stubs
      }
    })

    await flushPromises()

    const result = await axe(wrapper.element)
    expect(result.violations).toHaveLength(0)
  })
})
