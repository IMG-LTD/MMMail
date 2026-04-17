import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import type { MailSummary } from '../types/api'

const messageSuccessMock = vi.fn()
const messageErrorMock = vi.fn()
const promptMock = vi.fn()
const navigateToMock = vi.fn()
const routeState = {
  query: {} as Record<string, unknown>
}

const stats = {
  folderCounts: {},
  unreadCount: 1,
  starredCount: 1
}

const mailStore = {
  updateStats: vi.fn()
}

const mailApiMock = {
  fetchSearch: vi.fn(),
  fetchStarred: vi.fn(),
  fetchStats: vi.fn(),
  applyAction: vi.fn(),
  applyBatchAction: vi.fn(),
  undoSend: vi.fn(),
  snoozeUntil: vi.fn()
}

const labelApiMock = {
  listLabels: vi.fn()
}

const searchPresetApiMock = {
  listSearchPresets: vi.fn(),
  createSearchPreset: vi.fn(),
  useSearchPreset: vi.fn(),
  updateSearchPreset: vi.fn(),
  pinSearchPreset: vi.fn(),
  unpinSearchPreset: vi.fn(),
  deleteSearchPreset: vi.fn()
}

const searchHistoryApiMock = {
  listSearchHistory: vi.fn(),
  deleteSearchHistoryItem: vi.fn(),
  clearSearchHistory: vi.fn()
}

vi.mock('element-plus', () => ({
  ElMessage: {
    success: messageSuccessMock,
    error: messageErrorMock,
    warning: vi.fn()
  },
  ElMessageBox: {
    prompt: promptMock,
    confirm: vi.fn()
  }
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string) => key
  })
}))

vi.mock('~/composables/useMailApi', () => ({
  useMailApi: () => mailApiMock
}))

vi.mock('~/composables/useLabelApi', () => ({
  useLabelApi: () => labelApiMock
}))

vi.mock('~/composables/useSearchPresetApi', () => ({
  useSearchPresetApi: () => searchPresetApiMock
}))

vi.mock('~/composables/useSearchHistoryApi', () => ({
  useSearchHistoryApi: () => searchHistoryApiMock
}))

vi.mock('~/stores/mail', () => ({
  useMailStore: () => mailStore
}))

const elementStubs = {
  ElButton: defineComponent({
    name: 'ElButton',
    emits: ['click'],
    template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
  }),
  ElInput: defineComponent({
    name: 'ElInput',
    props: {
      modelValue: { type: String, default: '' }
    },
    emits: ['update:modelValue', 'keyup'],
    template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" @keyup="$emit(\'keyup\', $event)">'
  })
}

const mailListStub = defineComponent({
  name: 'MailList',
  props: {
    mails: {
      type: Array,
      default: () => []
    }
  },
  emits: ['undo', 'custom-snooze'],
  template: `
    <div data-testid="mail-list-stub">
      <button v-if="mails.length" data-testid="mail-list-undo" type="button" @click="$emit('undo', mails[0].id)">undo</button>
      <button v-if="mails.length" data-testid="mail-list-custom-snooze" type="button" @click="$emit('custom-snooze', mails[0].id)">custom snooze</button>
    </div>
  `
})

const searchPanelStubs = {
  SearchWorkspacePanel: defineComponent({
    name: 'SearchWorkspacePanel',
    template: '<div data-testid="search-workspace-panel" />'
  }),
  SearchHistoryPanel: defineComponent({
    name: 'SearchHistoryPanel',
    template: '<div data-testid="search-history-panel" />'
  }),
  SearchSavedPresetsPanel: defineComponent({
    name: 'SearchSavedPresetsPanel',
    template: '<div data-testid="search-saved-presets-panel" />'
  }),
  SearchPresetEditDialog: defineComponent({
    name: 'SearchPresetEditDialog',
    template: '<div data-testid="search-preset-edit-dialog" />'
  })
}

function buildMail(overrides: Partial<MailSummary> = {}): MailSummary {
  return {
    id: 'mail-1',
    ownerId: 'owner-1',
    senderEmail: 'alice@example.com',
    peerEmail: 'alice@example.com',
    folderType: 'INBOX',
    customFolderId: null,
    customFolderName: null,
    subject: 'Mail subject',
    preview: 'Mail preview',
    isRead: false,
    isStarred: true,
    isDraft: false,
    sentAt: '2026-04-17T08:00:00Z',
    labels: [],
    ...overrides
  }
}

async function importSearchPage() {
  return await import('../pages/search.vue')
}

async function importStarredPage() {
  return await import('../pages/starred.vue')
}

async function mountSearchPage() {
  const { default: SearchPage } = await importSearchPage()
  return mount(SearchPage, {
    global: {
      stubs: {
        ...elementStubs,
        ...searchPanelStubs,
        MailList: mailListStub
      }
    }
  })
}

async function mountStarredPage() {
  const { default: StarredPage } = await importStarredPage()
  return mount(StarredPage, {
    global: {
      stubs: {
        ...elementStubs,
        MailList: mailListStub
      }
    }
  })
}

beforeEach(() => {
  vi.clearAllMocks()
  routeState.query = {}
  ;(globalThis as Record<string, unknown>).useRoute = () => routeState
  ;(globalThis as Record<string, unknown>).navigateTo = navigateToMock
  ;(globalThis as Record<string, unknown>).useHead = () => undefined
  ;(globalThis as Record<string, unknown>).definePageMeta = () => undefined

  mailApiMock.fetchSearch.mockResolvedValue({
    items: [buildMail()],
    total: 1,
    page: 1,
    size: 30,
    unread: 1
  })
  mailApiMock.fetchStarred.mockResolvedValue({
    items: [buildMail()],
    total: 1,
    page: 1,
    size: 30,
    unread: 1
  })
  mailApiMock.fetchStats.mockResolvedValue(stats)
  mailApiMock.applyAction.mockResolvedValue({ affected: 1, stats })
  mailApiMock.applyBatchAction.mockResolvedValue({ affected: 1, stats })
  mailApiMock.undoSend.mockResolvedValue(undefined)
  mailApiMock.snoozeUntil.mockResolvedValue({ affected: 1, stats })

  labelApiMock.listLabels.mockResolvedValue([])
  searchPresetApiMock.listSearchPresets.mockResolvedValue([])
  searchPresetApiMock.createSearchPreset.mockResolvedValue(undefined)
  searchPresetApiMock.useSearchPreset.mockResolvedValue(undefined)
  searchPresetApiMock.updateSearchPreset.mockResolvedValue(undefined)
  searchPresetApiMock.pinSearchPreset.mockResolvedValue(undefined)
  searchPresetApiMock.unpinSearchPreset.mockResolvedValue(undefined)
  searchPresetApiMock.deleteSearchPreset.mockResolvedValue(undefined)
  searchHistoryApiMock.listSearchHistory.mockResolvedValue([])
  searchHistoryApiMock.deleteSearchHistoryItem.mockResolvedValue(undefined)
  searchHistoryApiMock.clearSearchHistory.mockResolvedValue(undefined)

  promptMock.mockResolvedValue({ value: '2026-04-18T10:00:00Z' })
})

describe('mail list page wiring', () => {
  it('wires undo send from search results for outbox rows', async () => {
    mailApiMock.fetchSearch.mockResolvedValue({
      items: [buildMail({ id: 'outbox-mail', folderType: 'OUTBOX', isStarred: false })],
      total: 1,
      page: 1,
      size: 30,
      unread: 0
    })

    const wrapper = await mountSearchPage()
    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="mail-list-undo"]').trigger('click')
    await flushPromises()
    await flushPromises()

    expect(mailApiMock.undoSend).toHaveBeenCalledWith('outbox-mail')
    expect(messageSuccessMock).toHaveBeenCalledWith('mailbox.messages.undoSuccess')
    expect(mailApiMock.fetchSearch).toHaveBeenCalledTimes(2)
    expect(mailApiMock.fetchSearch).toHaveBeenLastCalledWith({
      keyword: '',
      folder: undefined,
      unread: null,
      starred: null,
      from: undefined,
      to: undefined,
      label: undefined,
      page: 1,
      size: 30
    })
  })

  it('wires undo send from starred results', async () => {
    mailApiMock.fetchStarred.mockResolvedValue({
      items: [buildMail({ id: 'starred-outbox-mail', folderType: 'OUTBOX', isStarred: true })],
      total: 1,
      page: 1,
      size: 30,
      unread: 0
    })

    const wrapper = await mountStarredPage()
    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="mail-list-undo"]').trigger('click')
    await flushPromises()
    await flushPromises()

    expect(mailApiMock.undoSend).toHaveBeenCalledWith('starred-outbox-mail')
    expect(messageSuccessMock).toHaveBeenCalledWith('mailbox.messages.undoSuccess')
    expect(mailApiMock.fetchStarred).toHaveBeenCalledTimes(2)
    expect(mailApiMock.fetchStarred).toHaveBeenLastCalledWith(1, 30, '')
  })

  it('wires custom snooze from starred results', async () => {
    mailApiMock.fetchStarred.mockResolvedValue({
      items: [buildMail({ id: 'starred-inbox-mail', folderType: 'INBOX', isStarred: true })],
      total: 1,
      page: 1,
      size: 30,
      unread: 1
    })

    const wrapper = await mountStarredPage()
    await flushPromises()
    await flushPromises()

    await wrapper.get('[data-testid="mail-list-custom-snooze"]').trigger('click')
    await flushPromises()
    await flushPromises()

    expect(promptMock).toHaveBeenCalledTimes(1)
    expect(mailApiMock.snoozeUntil).toHaveBeenCalledWith('starred-inbox-mail', '2026-04-18T10:00:00Z')
    expect(messageSuccessMock).toHaveBeenCalledWith('mailbox.messages.customSnoozeSuccess')
    expect(mailApiMock.fetchStarred).toHaveBeenCalledTimes(2)
    expect(mailApiMock.fetchStarred).toHaveBeenLastCalledWith(1, 30, '')
  })
})
