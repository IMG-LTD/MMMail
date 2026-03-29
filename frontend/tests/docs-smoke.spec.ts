import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import DocsPage from '../pages/docs.vue'
import type {
  DocsNoteCollaborationOverview,
  DocsNoteDetail,
  DocsNotePresence,
  DocsNoteSummary,
} from '../types/api'

const {
  routerReplaceMock,
  messageErrorMock,
  messageSuccessMock,
  messageWarningMock,
  confirmMock,
  parseDocsImportFileMock,
  downloadDocsExportMock,
  docsApiMock,
} = vi.hoisted(() => ({
  routerReplaceMock: vi.fn(),
  messageErrorMock: vi.fn(),
  messageSuccessMock: vi.fn(),
  messageWarningMock: vi.fn(),
  confirmMock: vi.fn(async () => undefined),
  parseDocsImportFileMock: vi.fn(),
  downloadDocsExportMock: vi.fn(),
  docsApiMock: {
    listNotes: vi.fn(),
    createNote: vi.fn(),
    getNote: vi.fn(),
    updateNote: vi.fn(),
    deleteNote: vi.fn(),
    getCollaborationOverview: vi.fn(),
    listShares: vi.fn(),
    createShare: vi.fn(),
    updateSharePermission: vi.fn(),
    revokeShare: vi.fn(),
    listComments: vi.fn(),
    createComment: vi.fn(),
    resolveComment: vi.fn(),
    listSuggestions: vi.fn(),
    createSuggestion: vi.fn(),
    acceptSuggestion: vi.fn(),
    rejectSuggestion: vi.fn(),
    listPresence: vi.fn(),
    heartbeatPresence: vi.fn(),
    getSync: vi.fn(),
  },
}))

const routeState = {
  query: {} as Record<string, unknown>,
}

const baseNotes: DocsNoteSummary[] = [
  {
    id: 'note-1',
    title: 'Owned note',
    updatedAt: '2026-03-28T11:00:00',
    permission: 'OWNER',
    scope: 'OWNED',
    currentVersion: 2,
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    collaboratorCount: 0,
  },
  {
    id: 'note-2',
    title: 'Shared note',
    updatedAt: '2026-03-28T12:00:00',
    permission: 'EDIT',
    scope: 'SHARED',
    currentVersion: 3,
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    collaboratorCount: 1,
  },
]

const createdNote: DocsNoteSummary = {
  id: 'note-3',
  title: 'New note',
  updatedAt: '2026-03-28T13:00:00',
  permission: 'OWNER',
  scope: 'OWNED',
  currentVersion: 1,
  ownerEmail: 'owner@mmmail.local',
  ownerDisplayName: 'Owner',
  collaboratorCount: 0,
}

function buildDetail(summary: DocsNoteSummary, content = 'Server content'): DocsNoteDetail {
  return {
    id: summary.id,
    title: summary.title,
    content,
    createdAt: '2026-03-28T10:00:00',
    updatedAt: summary.updatedAt,
    currentVersion: summary.currentVersion,
    permission: summary.permission,
    shared: summary.scope === 'SHARED',
    ownerEmail: summary.ownerEmail,
    ownerDisplayName: summary.ownerDisplayName,
    collaboratorCount: summary.collaboratorCount,
    syncCursor: 5,
    syncVersion: `DOC-${summary.currentVersion}`,
  }
}

const presenceRecord: DocsNotePresence = {
  presenceId: 'presence-1',
  userId: 'user-1',
  email: 'owner@mmmail.local',
  displayName: 'Owner',
  sessionId: 'session-1',
  permission: 'OWNER',
  activeMode: 'EDIT',
  lastHeartbeatAt: '2026-03-28T11:00:00',
}

const overview: DocsNoteCollaborationOverview = {
  generatedAt: '2026-03-28T11:00:00',
  collaborators: [],
  comments: [],
  activeSessions: [presenceRecord],
  syncCursor: 5,
  syncVersion: 'DOC-2',
}

vi.mock('element-plus', () => ({
  ElMessage: {
    error: messageErrorMock,
    success: messageSuccessMock,
    warning: messageWarningMock,
  },
  ElMessageBox: {
    confirm: confirmMock,
  },
}))

vi.mock('~/composables/useDocsApi', () => ({
  useDocsApi: () => docsApiMock,
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

vi.mock('~/stores/auth', () => ({
  useAuthStore: () => ({
    accessToken: 'access-token',
  }),
}))

vi.mock('~/utils/auth-session', () => ({
  resolveSessionIdFromAccessToken: () => 'session-1',
}))

vi.mock('~/composables/useDocsSyncStream', () => ({
  useDocsSyncStream: () => ({
    status: ref('IDLE'),
    errorMessage: ref(''),
    connect: vi.fn(),
    disconnect: vi.fn(),
    reconnect: vi.fn(),
    lastCursor: ref(0),
  }),
}))

vi.mock('~/utils/docs-transfer', () => ({
  parseDocsImportFile: parseDocsImportFileMock,
  downloadDocsExport: downloadDocsExportMock,
}))

const ElAlert = defineComponent({
  name: 'ElAlert',
  props: { title: { type: String, default: '' } },
  template: '<div class="el-alert-stub"><slot />{{ title }}</div>',
})

const ElButton = defineComponent({
  name: 'ElButton',
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
})

const ElEmpty = defineComponent({
  name: 'ElEmpty',
  props: { description: { type: String, default: '' } },
  template: '<div class="el-empty-stub">{{ description }}</div>',
})

const ElInput = defineComponent({
  name: 'ElInput',
  props: {
    modelValue: { type: String, default: '' },
    type: { type: String, default: 'text' },
    placeholder: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
    readonly: { type: Boolean, default: false },
  },
  emits: ['update:modelValue', 'keyup', 'select', 'mouseup'],
  template: `
    <textarea
      v-if="type === 'textarea'"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      @input="$emit('update:modelValue', $event.target.value)"
      @select="$emit('select', $event)"
      @mouseup="$emit('mouseup', $event)"
      @keyup="$emit('keyup', $event)"
    />
    <input
      v-else
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      @input="$emit('update:modelValue', $event.target.value)"
      @keyup="$emit('keyup', $event)"
    />
  `,
})

const ElOption = defineComponent({
  name: 'ElOption',
  props: {
    label: { type: String, default: '' },
    value: { type: String, default: '' },
  },
  template: '<option :value="value">{{ label }}</option>',
})

const ElSelect = defineComponent({
  name: 'ElSelect',
  props: { modelValue: { type: String, default: '' } },
  emits: ['update:modelValue', 'change'],
  methods: {
    onChange(event: Event) {
      const value = (event.target as HTMLSelectElement).value
      this.$emit('update:modelValue', value)
      this.$emit('change', value)
    },
  },
  template: '<select :value="modelValue" @change="onChange"><slot /></select>',
})

const ElTag = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>',
})

const DocsShareManager = defineComponent({
  name: 'DocsShareManager',
  template: '<div class="docs-share-manager-stub"></div>',
})

const DocsSuggestionInbox = defineComponent({
  name: 'DocsSuggestionInbox',
  template: '<div class="docs-suggestion-inbox-stub"></div>',
})

function setGlobals(): void {
  ;(globalThis as Record<string, unknown>).useRoute = () => routeState
  ;(globalThis as Record<string, unknown>).useRouter = () => ({ replace: routerReplaceMock })
  ;(globalThis as Record<string, unknown>).onBeforeRouteLeave = vi.fn()
}

function configureApiMocks(): void {
  docsApiMock.createNote.mockResolvedValue({ id: createdNote.id })
  docsApiMock.listNotes.mockResolvedValue(baseNotes)
  docsApiMock.getNote.mockImplementation(async (noteId: string) => {
    const note = [...baseNotes, createdNote].find(item => item.id === noteId) ?? baseNotes[0]
    if (noteId === 'note-1') {
      return buildDetail(note, 'Server owned content')
    }
    if (noteId === 'note-2') {
      return buildDetail(note, 'Server shared content')
    }
    return buildDetail(note, 'Created note content')
  })
  docsApiMock.getCollaborationOverview.mockResolvedValue(overview)
  docsApiMock.listSuggestions.mockResolvedValue([])
  docsApiMock.heartbeatPresence.mockResolvedValue(presenceRecord)
  docsApiMock.listPresence.mockResolvedValue([presenceRecord])
}

function findButton(wrapper: ReturnType<typeof mount>, text: string) {
  return wrapper.findAll('button').find(button => button.text().includes(text))
}

function findNoteTile(wrapper: ReturnType<typeof mount>, title: string) {
  return wrapper.findAll('.note-tile').find(tile => tile.text().includes(title))
}

function findTitleInput(wrapper: ReturnType<typeof mount>) {
  return wrapper.find('input[placeholder="docs.editor.titlePlaceholder"]')
}

function findSearchInput(wrapper: ReturnType<typeof mount>) {
  return wrapper.find('input[placeholder="docs.search.placeholder"]')
}

beforeEach(() => {
  vi.clearAllMocks()
  routeState.query = {}
  localStorage.clear()
  setGlobals()
  configureApiMocks()
})

describe('docs smoke', () => {
  it('restores local drafts, guards unsaved note switching, and syncs route filters', async () => {
    routeState.query = { noteId: 'note-1', keyword: 'Owned', scope: 'OWNED' }
    localStorage.setItem('mmmail.docs.draft.note-1', JSON.stringify({
      noteId: 'note-1',
      title: 'Recovered title',
      content: 'Recovered content',
      baseVersion: 2,
      savedAt: '2026-03-28T12:00:00',
    }))

    const wrapper = mount(DocsPage, {
      global: {
        stubs: {
          ElAlert,
          ElButton,
          ElEmpty,
          ElInput,
          ElOption,
          ElSelect,
          ElTag,
          DocsShareManager,
          DocsSuggestionInbox,
        },
      },
    })
    await flushPromises()

    expect(docsApiMock.listNotes).toHaveBeenCalledWith('Owned', 200)
    expect(wrapper.text()).toContain('docs.draftRecovery.restore')

    const restoreButton = findButton(wrapper, 'docs.draftRecovery.restore')
    expect(restoreButton).toBeTruthy()
    await restoreButton!.trigger('click')
    await flushPromises()

    const titleInput = findTitleInput(wrapper)
    const editorTextarea = wrapper.find('textarea')
    expect(titleInput.exists()).toBe(true)
    expect((titleInput.element as HTMLInputElement).value).toBe('Recovered title')
    expect((editorTextarea.element as HTMLTextAreaElement).value).toBe('Recovered content')

    await findSearchInput(wrapper).setValue('')
    await wrapper.findAll('select')[0]!.setValue('ALL')
    await findButton(wrapper, 'docs.search.action')!.trigger('click')
    await flushPromises()
    expect(docsApiMock.listNotes).toHaveBeenLastCalledWith('', 200)

    confirmMock.mockRejectedValueOnce(new Error('cancel'))
    await findNoteTile(wrapper, 'Shared note')!.trigger('click')
    await flushPromises()
    expect(confirmMock).toHaveBeenCalledTimes(1)
    expect(docsApiMock.getNote).toHaveBeenCalledTimes(1)

    confirmMock.mockResolvedValueOnce(undefined)
    await findNoteTile(wrapper, 'Shared note')!.trigger('click')
    await flushPromises()
    expect(docsApiMock.getNote).toHaveBeenCalledWith('note-2')
  })

  it('keeps the active draft when zero-result filters are cancelled', async () => {
    routeState.query = { noteId: 'note-1', keyword: '', scope: 'ALL' }
    docsApiMock.listNotes.mockImplementation(async (keyword: string) => (keyword === 'Missing' ? [] : baseNotes))

    const wrapper = mount(DocsPage, {
      global: {
        stubs: {
          ElAlert,
          ElButton,
          ElEmpty,
          ElInput,
          ElOption,
          ElSelect,
          ElTag,
          DocsShareManager,
          DocsSuggestionInbox,
        },
      },
    })
    await flushPromises()

    const titleInput = findTitleInput(wrapper)
    await titleInput.setValue('Unsaved title')
    await flushPromises()

    confirmMock.mockRejectedValueOnce(new Error('cancel'))
    await findSearchInput(wrapper).setValue('Missing')
    await findButton(wrapper, 'docs.search.action')!.trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledTimes(1)
    expect((titleInput.element as HTMLInputElement).value).toBe('Unsaved title')
    expect(findNoteTile(wrapper, 'Owned note')).toBeTruthy()
    expect(docsApiMock.listNotes).toHaveBeenLastCalledWith('Missing', 200)
  })

  it('wires import and export actions through docs transfer helpers', async () => {
    parseDocsImportFileMock.mockResolvedValue({
      title: 'Imported runbook',
      content: 'Imported body',
      format: 'MARKDOWN',
    })

    const wrapper = mount(DocsPage, {
      global: {
        stubs: {
          ElAlert,
          ElButton,
          ElEmpty,
          ElInput,
          ElOption,
          ElSelect,
          ElTag,
          DocsShareManager,
          DocsSuggestionInbox,
        },
      },
    })
    await flushPromises()

    const importInput = wrapper.find('input[type="file"]')
    const file = new File(['# Imported runbook'], 'import.md', { type: 'text/markdown' })
    Object.defineProperty(importInput.element, 'files', {
      configurable: true,
      value: [file],
    })
    await importInput.trigger('change')
    await flushPromises()

    expect(parseDocsImportFileMock).toHaveBeenCalled()
    expect(findSearchInput(wrapper).exists()).toBe(true)
    expect((findTitleInput(wrapper).element as HTMLInputElement).value).toBe('Imported runbook')
    expect(messageSuccessMock).toHaveBeenCalledWith(expect.stringContaining('docs.messages.importReady'))

    const exportMarkdownButton = findButton(wrapper, 'docs.actions.exportMarkdown')
    const exportTextButton = findButton(wrapper, 'docs.actions.exportText')
    expect(exportMarkdownButton).toBeTruthy()
    expect(exportTextButton).toBeTruthy()

    await exportMarkdownButton!.trigger('click')
    await exportTextButton!.trigger('click')
    expect(downloadDocsExportMock).toHaveBeenNthCalledWith(1, 'Imported runbook', 'Imported body', 'MARKDOWN')
    expect(downloadDocsExportMock).toHaveBeenNthCalledWith(2, 'Imported runbook', 'Imported body', 'TEXT')
  })

  it('guards imports behind unsaved-change confirmation before replacing the editor', async () => {
    parseDocsImportFileMock.mockResolvedValue({
      title: 'Imported runbook',
      content: 'Imported body',
      format: 'MARKDOWN',
    })

    const wrapper = mount(DocsPage, {
      global: {
        stubs: {
          ElAlert,
          ElButton,
          ElEmpty,
          ElInput,
          ElOption,
          ElSelect,
          ElTag,
          DocsShareManager,
          DocsSuggestionInbox,
        },
      },
    })
    await flushPromises()

    await findTitleInput(wrapper).setValue('Unsaved import title')
    await flushPromises()

    const importInput = wrapper.find('input[type="file"]')
    const file = new File(['# Imported runbook'], 'import.md', { type: 'text/markdown' })

    confirmMock.mockRejectedValueOnce(new Error('cancel'))
    Object.defineProperty(importInput.element, 'files', {
      configurable: true,
      value: [file],
    })
    await importInput.trigger('change')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledTimes(1)
    expect(parseDocsImportFileMock).not.toHaveBeenCalled()
    expect((findTitleInput(wrapper).element as HTMLInputElement).value).toBe('Unsaved import title')

    confirmMock.mockResolvedValueOnce(undefined)
    Object.defineProperty(importInput.element, 'files', {
      configurable: true,
      value: [file],
    })
    await importInput.trigger('change')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledTimes(2)
    expect(parseDocsImportFileMock).toHaveBeenCalledTimes(1)
    expect((findTitleInput(wrapper).element as HTMLInputElement).value).toBe('Imported runbook')
    expect(messageSuccessMock).toHaveBeenCalledWith(expect.stringContaining('docs.messages.importReady'))
  })

  it('guards workspace refresh behind unsaved-change confirmation before reloading the editor', async () => {
    const wrapper = mount(DocsPage, {
      global: {
        stubs: {
          ElAlert,
          ElButton,
          ElEmpty,
          ElInput,
          ElOption,
          ElSelect,
          ElTag,
          DocsShareManager,
          DocsSuggestionInbox,
        },
      },
    })
    await flushPromises()

    const titleInput = findTitleInput(wrapper)
    await titleInput.setValue('Unsaved refresh title')
    await flushPromises()

    docsApiMock.getNote.mockClear()
    docsApiMock.getCollaborationOverview.mockClear()
    docsApiMock.listSuggestions.mockClear()
    docsApiMock.listNotes.mockClear()

    confirmMock.mockRejectedValueOnce(new Error('cancel'))
    await findButton(wrapper, 'docs.actions.refreshWorkspace')!.trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledTimes(1)
    expect(docsApiMock.getNote).not.toHaveBeenCalled()
    expect(docsApiMock.getCollaborationOverview).not.toHaveBeenCalled()
    expect(docsApiMock.listSuggestions).not.toHaveBeenCalled()
    expect(docsApiMock.listNotes).not.toHaveBeenCalled()
    expect((titleInput.element as HTMLInputElement).value).toBe('Unsaved refresh title')

    confirmMock.mockResolvedValueOnce(undefined)
    await findButton(wrapper, 'docs.actions.refreshWorkspace')!.trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledTimes(2)
    expect(docsApiMock.getNote).toHaveBeenCalledWith('note-1')
    expect(docsApiMock.getCollaborationOverview).toHaveBeenCalledWith('note-1')
    expect(docsApiMock.listSuggestions).toHaveBeenCalledWith('note-1', true)
    expect(docsApiMock.listNotes).toHaveBeenCalledWith('', 200)
  })

  it('loads created notes through the select flow after creation', async () => {
    docsApiMock.listNotes
      .mockResolvedValueOnce(baseNotes)
      .mockResolvedValueOnce([createdNote, ...baseNotes])

    const wrapper = mount(DocsPage, {
      global: {
        stubs: {
          ElAlert,
          ElButton,
          ElEmpty,
          ElInput,
          ElOption,
          ElSelect,
          ElTag,
          DocsShareManager,
          DocsSuggestionInbox,
        },
      },
    })
    await flushPromises()

    await findButton(wrapper, 'docs.actions.newNote')!.trigger('click')
    await flushPromises()

    expect(docsApiMock.createNote).toHaveBeenCalledTimes(1)
    expect(docsApiMock.getNote).toHaveBeenCalledWith(createdNote.id)
    expect((findTitleInput(wrapper).element as HTMLInputElement).value).toBe(createdNote.title)
    expect(routerReplaceMock).toHaveBeenCalledWith(expect.objectContaining({
      path: '/docs',
      query: expect.objectContaining({ noteId: createdNote.id }),
    }))
  })

  it('rejects file import changes when no active note is selected', async () => {
    docsApiMock.listNotes.mockResolvedValueOnce([])

    const wrapper = mount(DocsPage, {
      global: {
        stubs: {
          ElAlert,
          ElButton,
          ElEmpty,
          ElInput,
          ElOption,
          ElSelect,
          ElTag,
          DocsShareManager,
          DocsSuggestionInbox,
        },
      },
    })
    await flushPromises()

    const importInput = wrapper.find('input[type="file"]')
    const file = new File(['# Orphan import'], 'orphan.md', { type: 'text/markdown' })
    Object.defineProperty(importInput.element, 'files', {
      configurable: true,
      value: [file],
    })
    await importInput.trigger('change')
    await flushPromises()

    expect(parseDocsImportFileMock).not.toHaveBeenCalled()
    expect(messageWarningMock).toHaveBeenCalledWith('docs.messages.selectNote')
  })

  it('falls back to the nearest accessible note when a deep-linked note becomes unavailable', async () => {
    routeState.query = { noteId: 'note-2' }
    docsApiMock.getNote.mockImplementation(async (noteId: string) => {
      if (noteId === 'note-2') {
        throw { status: 404, message: 'gone' }
      }
      const note = baseNotes.find(item => item.id === noteId) ?? baseNotes[0]
      return buildDetail(note, 'Recovered owned content')
    })

    const wrapper = mount(DocsPage, {
      global: {
        stubs: {
          ElAlert,
          ElButton,
          ElEmpty,
          ElInput,
          ElOption,
          ElSelect,
          ElTag,
          DocsShareManager,
          DocsSuggestionInbox,
        },
      },
    })
    await flushPromises()

    expect(messageWarningMock).toHaveBeenCalledWith('docs.messages.noteUnavailable')
    expect(docsApiMock.getNote).toHaveBeenCalledWith('note-2')
    expect(docsApiMock.getNote).toHaveBeenCalledWith('note-1')
    expect((findTitleInput(wrapper).element as HTMLInputElement).value).toBe('Owned note')
    expect(routerReplaceMock).toHaveBeenCalledWith(expect.objectContaining({
      path: '/docs',
      query: expect.objectContaining({ noteId: 'note-1' }),
    }))
  })

  it('falls back to the nearest accessible note when a clicked note becomes unavailable', async () => {
    docsApiMock.getNote.mockImplementation(async (noteId: string) => {
      if (noteId === 'note-2') {
        throw { status: 404, message: 'gone' }
      }
      const note = baseNotes.find(item => item.id === noteId) ?? baseNotes[0]
      return buildDetail(note, 'Recovered owned content')
    })

    const wrapper = mount(DocsPage, {
      global: {
        stubs: {
          ElAlert,
          ElButton,
          ElEmpty,
          ElInput,
          ElOption,
          ElSelect,
          ElTag,
          DocsShareManager,
          DocsSuggestionInbox,
        },
      },
    })
    await flushPromises()
    messageWarningMock.mockClear()
    docsApiMock.getNote.mockClear()

    await findNoteTile(wrapper, 'Shared note')!.trigger('click')
    await flushPromises()

    expect(messageWarningMock).toHaveBeenCalledWith('docs.messages.noteUnavailable')
    expect(docsApiMock.getNote).toHaveBeenCalledWith('note-2')
    expect(docsApiMock.getNote).toHaveBeenCalledWith('note-1')
    expect((findTitleInput(wrapper).element as HTMLInputElement).value).toBe('Owned note')
    expect(routerReplaceMock).toHaveBeenCalledWith(expect.objectContaining({
      path: '/docs',
      query: expect.objectContaining({ noteId: 'note-1' }),
    }))
  })

})
