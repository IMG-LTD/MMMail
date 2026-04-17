import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import { axe } from 'vitest-axe'
import MailList from '~/components/business/MailList.vue'
import SuiteReleaseBoundaryPanel from '~/components/suite/SuiteReleaseBoundaryPanel.vue'
import SuiteSectionNav from '~/components/suite/SuiteSectionNav.vue'
import type { MailSummary } from '~/types/api'
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
    locale: { value: 'en-US' },
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
    inheritAttrs: false,
    template: '<button v-bind="$attrs" type="button" @click="$emit(\'click\')"><slot /></button>'
  }),
  ElDropdown: defineComponent({
    name: 'ElDropdown',
    template: '<div class="el-dropdown-stub"><slot /><slot name="dropdown" /></div>'
  }),
  ElDropdownMenu: defineComponent({
    name: 'ElDropdownMenu',
    template: '<div class="el-dropdown-menu-stub"><slot /></div>'
  }),
  ElDropdownItem: defineComponent({
    name: 'ElDropdownItem',
    emits: ['click'],
    template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
  }),
  ElEmpty: defineComponent({
    name: 'ElEmpty',
    props: {
      description: { type: String, default: '' }
    },
    template: '<div>{{ description }}</div>'
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

const triageMail: MailSummary = {
  id: 'mail-1',
  ownerId: 'user-1',
  senderEmail: 'alice@example.com',
  peerEmail: 'alice@example.com',
  folderType: 'INBOX',
  customFolderId: null,
  customFolderName: null,
  subject: 'Quarterly update',
  preview: 'Need your decision',
  isRead: false,
  isStarred: true,
  isDraft: false,
  sentAt: '2026-03-13T10:20:30Z',
  labels: ['ops'],
  senderDisplayName: 'Alice',
  senderType: 'EXTERNAL',
  isImportantContact: true,
  hasAttachments: true,
  replyState: 'AWAITING_ME',
  needsReply: true,
  latestActor: 'OTHER',
  conversationMessageCount: 3
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

  it('keeps the triage mail list free of obvious runtime accessibility violations', async () => {
    const wrapper = mount(MailList, {
      props: {
        title: 'Inbox',
        mails: [triageMail],
        loading: false
      },
      global: {
        stubs
      }
    })

    const selection = wrapper.get('[data-testid="mail-row-select"]')
    await selection.setValue(true)
    await selection.trigger('change')
    await flushPromises()

    const result = await axe(wrapper.element)
    expect(result.violations).toHaveLength(0)
  })
})
