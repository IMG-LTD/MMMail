import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import type { ConversationDetail, ConversationSummary, LabelItem, MailDetail, MailSummary, MailboxStats } from '../types/api'
import { messages } from '../locales'
import { translate } from '../utils/i18n'

const routeState = {
  params: { id: 'mail-42' },
  query: {}
}

const navigateToMock = vi.fn()
const messageSuccessMock = vi.fn()
const messageWarningMock = vi.fn()
const messageErrorMock = vi.fn()
const confirmMock = vi.fn()

const stats: MailboxStats = {
  folderCounts: {
    INBOX: 1,
    SENT: 0,
    DRAFTS: 0,
    OUTBOX: 0,
    ARCHIVE: 0,
    SPAM: 0,
    TRASH: 0,
    SCHEDULED: 0,
    SNOOZED: 0
  },
  unreadCount: 1,
  starredCount: 0
}

const labels: LabelItem[] = [{ id: 1, name: 'ops', color: '#409eff' }]
const mailDetail: MailDetail = {
  id: 'mail-42',
  ownerId: 'user-1',
  senderEmail: 'owner@mmmail.local',
  peerEmail: 'alice@example.com',
  folderType: 'INBOX',
  customFolderId: null,
  customFolderName: null,
  subject: 'Quarterly update',
  preview: 'Latest summary',
  body: 'Body line',
  isRead: false,
  isStarred: false,
  isDraft: false,
  sentAt: '2026-03-13T10:20:30Z',
  labels: ['ops'],
  attachments: [{
    id: 'att-1',
    mailId: 'mail-42',
    fileName: 'report.pdf',
    contentType: 'application/pdf',
    fileSize: 2048
  }]
}

const conversationMessages: MailSummary[] = [{
  id: 'mail-42',
  ownerId: 'user-1',
  peerEmail: 'alice@example.com',
  senderEmail: 'owner@mmmail.local',
  subject: 'Quarterly update',
  preview: 'Latest summary',
  folderType: 'INBOX',
  customFolderId: null,
  customFolderName: null,
  isRead: false,
  isStarred: false,
  isDraft: false,
  sentAt: '2026-03-13T10:20:30Z',
  labels: []
}]

const conversationList: ConversationSummary[] = [{
  conversationId: 'conv-1',
  subject: 'Quarterly update',
  participants: ['alice@example.com', 'owner@mmmail.local'],
  messageCount: 2,
  unreadCount: 1,
  latestAt: '2026-03-13T10:20:30Z'
}]

const conversationDetail: ConversationDetail = {
  conversationId: 'conv-1',
  subject: 'Quarterly update',
  messages: [
    ...conversationMessages,
    {
      ...conversationMessages[0],
      id: 'mail-43',
      preview: 'Second summary',
      sentAt: '2026-03-13T11:20:30Z'
    }
  ]
}

const mailApiMock = {
  fetchMailDetail: vi.fn(),
  applyAction: vi.fn(),
  updateLabels: vi.fn(),
  fetchStats: vi.fn(),
  downloadMailAttachment: vi.fn(),
  applyConversationAction: vi.fn()
}

const conversationApiMock = {
  fetchConversations: vi.fn(),
  fetchConversationDetail: vi.fn()
}

const mailStore = {
  updateStats: vi.fn()
}

vi.mock('element-plus', () => ({
  ElMessage: {
    success: messageSuccessMock,
    warning: messageWarningMock,
    error: messageErrorMock
  },
  ElMessageBox: {
    confirm: confirmMock
  }
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    locale: { value: 'en' },
    t: (key: string, params?: Record<string, string | number>) => translate(messages, 'en', key, params)
  })
}))

vi.mock('~/composables/useMailApi', () => ({
  useMailApi: () => mailApiMock
}))

vi.mock('~/composables/useConversationApi', () => ({
  useConversationApi: () => conversationApiMock
}))

vi.mock('~/composables/useLabelApi', () => ({
  useLabelApi: () => ({
    listLabels: vi.fn(async () => labels)
  })
}))

vi.mock('~/composables/useContactApi', () => ({
  useContactApi: () => ({
    quickAddContact: vi.fn(async () => undefined)
  })
}))

vi.mock('~/stores/mail', () => ({
  useMailStore: () => mailStore
}))

vi.stubGlobal('useRoute', () => routeState)
vi.stubGlobal('navigateTo', navigateToMock)
vi.stubGlobal('useHead', vi.fn())

const stubs = {
  ElButton: defineComponent({
    name: 'ElButton',
    template: '<button><slot /></button>'
  }),
  ElForm: defineComponent({
    name: 'ElForm',
    template: '<form><slot /></form>'
  }),
  ElFormItem: defineComponent({
    name: 'ElFormItem',
    props: { label: { type: String, default: '' } },
    template: '<label><span>{{ label }}</span><slot /></label>'
  }),
  ElInput: defineComponent({
    name: 'ElInput',
    props: { modelValue: { type: String, default: '' }, placeholder: { type: String, default: '' } },
    template: '<input :value="modelValue" :placeholder="placeholder">'
  }),
  ElSelect: defineComponent({
    name: 'ElSelect',
    props: { placeholder: { type: String, default: '' } },
    template: '<div><span>{{ placeholder }}</span><slot /></div>'
  }),
  ElOption: defineComponent({
    name: 'ElOption',
    props: { label: { type: String, default: '' } },
    template: '<option>{{ label }}</option>'
  }),
  ElTable: defineComponent({
    name: 'ElTable',
    props: { data: { type: Array, default: () => [] }, emptyText: { type: String, default: '' } },
    template: '<div><slot /></div>'
  }),
  ElTableColumn: defineComponent({
    name: 'ElTableColumn',
    props: { label: { type: String, default: '' } },
    template: '<div><span>{{ label }}</span><slot :row="{ conversationId: \'conv-1\', participants: [\'alice@example.com\'] }" /></div>'
  }),
  ElPagination: defineComponent({
    name: 'ElPagination',
    template: '<div class="pagination-stub" />'
  }),
  ElSkeleton: defineComponent({
    name: 'ElSkeleton',
    template: '<div class="skeleton-stub" />'
  }),
  ElEmpty: defineComponent({
    name: 'ElEmpty',
    props: { description: { type: String, default: '' } },
    template: '<div class="empty-stub">{{ description }}</div>'
  })
}

async function importLegacyMailDetailPage() {
  const module = await import('../pages/mail/[id].vue')
  return module.default
}

async function importLegacyConversationsPage() {
  const module = await import('../pages/conversations/index.vue')
  return module.default
}

async function importConversationDetailPage() {
  const module = await import('../pages/conversations/[id].vue')
  return module.default
}

describe('mail workspace i18n', () => {
  beforeEach(() => {
    routeState.params = { id: 'mail-42' }
    routeState.query = {}
    navigateToMock.mockReset()
    messageSuccessMock.mockReset()
    messageWarningMock.mockReset()
    messageErrorMock.mockReset()
    confirmMock.mockReset()
    confirmMock.mockResolvedValue(undefined)
    mailApiMock.fetchMailDetail.mockReset()
    mailApiMock.applyAction.mockReset()
    mailApiMock.updateLabels.mockReset()
    mailApiMock.fetchStats.mockReset()
    mailApiMock.downloadMailAttachment.mockReset()
    mailApiMock.applyConversationAction.mockReset()
    conversationApiMock.fetchConversations.mockReset()
    conversationApiMock.fetchConversationDetail.mockReset()
    mailStore.updateStats.mockReset()

    mailApiMock.fetchMailDetail.mockResolvedValue(mailDetail)
    mailApiMock.fetchStats.mockResolvedValue(stats)
    mailApiMock.updateLabels.mockResolvedValue(undefined)
    mailApiMock.applyAction.mockResolvedValue({ affected: 1, stats })
    mailApiMock.downloadMailAttachment.mockResolvedValue({
      fileName: 'report.pdf',
      blob: new Blob(['ok'], { type: 'application/pdf' })
    })
    mailApiMock.applyConversationAction.mockResolvedValue({ affected: 2, stats })
    conversationApiMock.fetchConversations.mockResolvedValue({
      items: conversationList,
      total: 1,
      page: 1,
      size: 20
    })
    conversationApiMock.fetchConversationDetail.mockResolvedValue(conversationDetail)
  })

  it('redirects legacy mail detail routes to conversations', async () => {
    const MailDetailPage = await importLegacyMailDetailPage()
    mount(MailDetailPage, {
      global: {
        stubs,
        directives: { loading: {} }
      }
    })

    await flushPromises()
    expect(navigateToMock).toHaveBeenCalledWith('/conversations/mail-42')
    expect(mailApiMock.fetchMailDetail).not.toHaveBeenCalled()
  })

  it('redirects legacy conversation list routes to inbox', async () => {
    const LegacyConversationsPage = await importLegacyConversationsPage()
    mount(LegacyConversationsPage, {
      global: {
        stubs,
        directives: { loading: {} }
      }
    })

    await flushPromises()
    expect(navigateToMock).toHaveBeenCalledWith('/inbox')
    expect(conversationApiMock.fetchConversations).not.toHaveBeenCalled()
  })

  it('renders localized conversation detail actions', async () => {
    routeState.params = { id: 'conv-1' }
    const ConversationDetailPage = await importConversationDetailPage()
    const detailWrapper = mount(ConversationDetailPage, {
      global: {
        stubs,
        directives: { loading: {} }
      }
    })

    await flushPromises()
    expect(detailWrapper.text()).toContain('Back')
    expect(detailWrapper.text()).toContain('2 messages')
    expect(detailWrapper.text()).toContain('Mark all read')
    expect(detailWrapper.text()).toContain('Open Mail')
  })
})
