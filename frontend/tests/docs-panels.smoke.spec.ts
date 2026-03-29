import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import DocsPage from '../pages/docs.vue'
import DocsShareManager from '../components/docs/DocsShareManager.vue'
import DocsSuggestionInbox from '../components/docs/DocsSuggestionInbox.vue'
import type {
  DocsNoteCollaborationOverview,
  DocsNoteDetail,
  DocsNotePresence,
  DocsNoteShare,
  DocsNoteSummary,
} from '../types/api'
import type { DocsNoteSuggestion } from '../types/docs'

const {
  routerReplaceMock,
  messageErrorMock,
  messageSuccessMock,
  messageWarningMock,
  confirmMock,
  docsApiMock,
} = vi.hoisted(() => ({
  routerReplaceMock: vi.fn(),
  messageErrorMock: vi.fn(),
  messageSuccessMock: vi.fn(),
  messageWarningMock: vi.fn(),
  confirmMock: vi.fn(async () => undefined),
  docsApiMock: {
    listNotes: vi.fn(),
    createNote: vi.fn(),
    getNote: vi.fn(),
    updateNote: vi.fn(),
    deleteNote: vi.fn(),
    getCollaborationOverview: vi.fn(),
    createShare: vi.fn(),
    updateSharePermission: vi.fn(),
    revokeShare: vi.fn(),
    createComment: vi.fn(),
    resolveComment: vi.fn(),
    listSuggestions: vi.fn(),
    createSuggestion: vi.fn(),
    acceptSuggestion: vi.fn(),
    rejectSuggestion: vi.fn(),
    listPresence: vi.fn(),
    heartbeatPresence: vi.fn(),
  },
}))

const routeState = {
  query: { noteId: 'note-1' } as Record<string, unknown>,
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
    collaboratorCount: 1,
  },
]

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

let currentCollaborators: DocsNoteShare[] = []
let currentSuggestions: DocsNoteSuggestion[] = []

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

const ElAlert = defineComponent({
  name: 'ElAlert',
  props: { title: { type: String, default: '' } },
  template: '<div class="el-alert-stub"><slot />{{ title }}</div>',
})

const ElButton = defineComponent({
  name: 'ElButton',
  props: {
    disabled: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
  },
  emits: ['click'],
  template: '<button type="button" :disabled="disabled || loading" @click="$emit(\'click\')"><slot /></button>',
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
  setup(_, { emit, expose }) {
    const input = ref<HTMLInputElement | null>(null)
    const textarea = ref<HTMLTextAreaElement | null>(null)

    function onInput(event: Event): void {
      emit('update:modelValue', (event.target as HTMLInputElement | HTMLTextAreaElement).value)
    }

    expose({ input, textarea })
    return { input, textarea, onInput }
  },
  template: `
    <textarea
      v-if="type === 'textarea'"
      ref="textarea"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      @input="onInput"
      @select="$emit('select', $event)"
      @mouseup="$emit('mouseup', $event)"
      @keyup="$emit('keyup', $event)"
    />
    <input
      v-else
      ref="input"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      @input="onInput"
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

function setGlobals(): void {
  ;(globalThis as Record<string, unknown>).useRoute = () => routeState
  ;(globalThis as Record<string, unknown>).useRouter = () => ({ replace: routerReplaceMock })
  ;(globalThis as Record<string, unknown>).onBeforeRouteLeave = vi.fn()
}

function buildDetail(content = 'Server owned content'): DocsNoteDetail {
  return {
    id: 'note-1',
    title: 'Owned note',
    content,
    createdAt: '2026-03-28T10:00:00',
    updatedAt: '2026-03-28T11:00:00',
    currentVersion: 2,
    permission: 'OWNER',
    shared: false,
    ownerEmail: 'owner@mmmail.local',
    ownerDisplayName: 'Owner',
    collaboratorCount: currentCollaborators.length,
    syncCursor: 5,
    syncVersion: 'DOC-2',
  }
}

function buildOverview(): DocsNoteCollaborationOverview {
  return {
    generatedAt: '2026-03-28T11:00:00',
    collaborators: currentCollaborators,
    comments: [],
    activeSessions: [presenceRecord],
    syncCursor: 5,
    syncVersion: 'DOC-2',
  }
}

function mountDocsPage() {
  return mount(DocsPage, {
    global: {
      components: {
        DocsShareManager,
        DocsSuggestionInbox,
      },
      stubs: {
        ElAlert,
        ElButton,
        ElEmpty,
        ElInput,
        ElOption,
        ElSelect,
        ElTag,
      },
    },
  })
}

function findButton(wrapper: ReturnType<typeof mount>, text: string, index = 0) {
  return wrapper.findAll('button').filter(button => button.text().includes(text))[index]
}

function configureApiMocks(): void {
  let shareSequence = 2
  let suggestionSequence = 2
  docsApiMock.listNotes.mockResolvedValue(baseNotes)
  docsApiMock.createNote.mockResolvedValue({ id: 'note-created' })
  docsApiMock.getNote.mockImplementation(async () => buildDetail())
  docsApiMock.getCollaborationOverview.mockImplementation(async () => buildOverview())
  docsApiMock.listSuggestions.mockImplementation(async () => currentSuggestions)
  docsApiMock.heartbeatPresence.mockResolvedValue(presenceRecord)
  docsApiMock.listPresence.mockResolvedValue([presenceRecord])
  docsApiMock.createShare.mockImplementation(async (_noteId: string, payload: { collaboratorEmail: string; permission: 'VIEW' | 'EDIT' }) => {
    const created = {
      shareId: `share-${shareSequence++}`,
      collaboratorUserId: `user-${shareSequence}`,
      collaboratorEmail: payload.collaboratorEmail,
      collaboratorDisplayName: payload.collaboratorEmail.split('@')[0],
      permission: payload.permission,
      createdAt: '2026-03-28T11:30:00',
    } satisfies DocsNoteShare
    currentCollaborators = [created, ...currentCollaborators]
    return created
  })
  docsApiMock.updateSharePermission.mockImplementation(async (_noteId: string, shareId: string, payload: { permission: 'VIEW' | 'EDIT' }) => {
    const updated = currentCollaborators.find(item => item.shareId === shareId)
    if (!updated) {
      throw new Error('share not found')
    }
    const next = { ...updated, permission: payload.permission }
    currentCollaborators = currentCollaborators.map(item => item.shareId === shareId ? next : item)
    return next
  })
  docsApiMock.revokeShare.mockImplementation(async (_noteId: string, shareId: string) => {
    currentCollaborators = currentCollaborators.filter(item => item.shareId !== shareId)
  })
  docsApiMock.createSuggestion.mockImplementation(async (_noteId: string, payload: {
    selectionStart: number
    selectionEnd: number
    originalText: string
    replacementText?: string
    baseVersion: number
  }) => {
    const created = {
      suggestionId: `suggestion-${suggestionSequence++}`,
      authorUserId: 'user-1',
      authorEmail: 'owner@mmmail.local',
      authorDisplayName: 'Owner',
      status: 'PENDING',
      selectionStart: payload.selectionStart,
      selectionEnd: payload.selectionEnd,
      originalText: payload.originalText,
      replacementText: payload.replacementText || '',
      baseVersion: payload.baseVersion,
      resolvedByUserId: null,
      resolvedByEmail: null,
      resolvedByDisplayName: null,
      resolvedAt: null,
      createdAt: '2026-03-28T11:40:00',
    } satisfies DocsNoteSuggestion
    currentSuggestions = [created, ...currentSuggestions]
    return created
  })
  docsApiMock.acceptSuggestion.mockImplementation(async (_noteId: string, suggestionId: string) => {
    const updated = currentSuggestions.find(item => item.suggestionId === suggestionId)
    if (!updated) {
      throw new Error('suggestion not found')
    }
    const next = {
      ...updated,
      status: 'ACCEPTED',
      resolvedAt: '2026-03-28T11:45:00',
      resolvedByUserId: 'user-1',
      resolvedByEmail: 'owner@mmmail.local',
      resolvedByDisplayName: 'Owner',
    } satisfies DocsNoteSuggestion
    currentSuggestions = currentSuggestions.map(item => item.suggestionId === suggestionId ? next : item)
    return next
  })
  docsApiMock.rejectSuggestion.mockImplementation(async (_noteId: string, suggestionId: string) => {
    const updated = currentSuggestions.find(item => item.suggestionId === suggestionId)
    if (!updated) {
      throw new Error('suggestion not found')
    }
    const next = {
      ...updated,
      status: 'REJECTED',
      resolvedAt: '2026-03-28T11:46:00',
      resolvedByUserId: 'user-1',
      resolvedByEmail: 'owner@mmmail.local',
      resolvedByDisplayName: 'Owner',
    } satisfies DocsNoteSuggestion
    currentSuggestions = currentSuggestions.map(item => item.suggestionId === suggestionId ? next : item)
    return next
  })
}

beforeEach(() => {
  vi.clearAllMocks()
  currentCollaborators = [
    {
      shareId: 'share-1',
      collaboratorUserId: 'user-2',
      collaboratorEmail: 'existing@mmmail.local',
      collaboratorDisplayName: 'Existing user',
      permission: 'VIEW',
      createdAt: '2026-03-28T11:10:00',
    },
  ]
  currentSuggestions = [
    {
      suggestionId: 'suggestion-1',
      authorUserId: 'user-2',
      authorEmail: 'reviewer@mmmail.local',
      authorDisplayName: 'Reviewer',
      status: 'PENDING',
      selectionStart: 0,
      selectionEnd: 6,
      originalText: 'Server',
      replacementText: 'Draft',
      baseVersion: 2,
      resolvedByUserId: null,
      resolvedByEmail: null,
      resolvedByDisplayName: null,
      resolvedAt: null,
      createdAt: '2026-03-28T11:20:00',
    },
  ]
  routeState.query = { noteId: 'note-1' }
  localStorage.clear()
  setGlobals()
  configureApiMocks()
})

describe('docs panels smoke', () => {
  it('wires share manager create, permission update, and revoke flows through the docs page', async () => {
    const wrapper = mountDocsPage()
    await flushPromises()

    const shareEmailInput = wrapper.find('.share-form input')
    await shareEmailInput.setValue('new-user@mmmail.local')
    await wrapper.find('.share-form select').setValue('VIEW')
    await findButton(wrapper, 'docs.share.add').trigger('click')
    await flushPromises()

    expect(docsApiMock.createShare).toHaveBeenCalledWith('note-1', {
      collaboratorEmail: 'new-user@mmmail.local',
      permission: 'VIEW',
    })
    expect(wrapper.text()).toContain('new-user@mmmail.local')
    expect(messageSuccessMock).toHaveBeenCalledWith('docs.messages.collaboratorAdded')

    const sharePermissionSelect = wrapper.find('.share-list select')
    await sharePermissionSelect.setValue('EDIT')
    await flushPromises()

    expect(docsApiMock.updateSharePermission).toHaveBeenCalledWith('note-1', 'share-2', {
      permission: 'EDIT',
    })
    expect(messageSuccessMock).toHaveBeenCalledWith('docs.messages.sharePermissionUpdated')

    await findButton(wrapper, 'docs.share.revoke').trigger('click')
    await flushPromises()

    expect(confirmMock).toHaveBeenCalledTimes(1)
    expect(docsApiMock.revokeShare).toHaveBeenCalledWith('note-1', 'share-2')
    expect(wrapper.text()).not.toContain('new-user@mmmail.local')
    expect(messageSuccessMock).toHaveBeenCalledWith('docs.messages.collaboratorRevoked')
  })

  it('wires suggestion inbox create, accept, and reject flows through the docs page', async () => {
    const wrapper = mountDocsPage()
    await flushPromises()

    const editorTextarea = wrapper.find('.editor-fields textarea')
    const editorElement = editorTextarea.element as HTMLTextAreaElement
    editorElement.selectionStart = 0
    editorElement.selectionEnd = 6
    await editorTextarea.trigger('select')
    await flushPromises()

    await wrapper.find('.sync-row select').setValue('SUGGEST')
    await flushPromises()

    const suggestionTextarea = wrapper.find('.compose-card textarea')
    await suggestionTextarea.setValue('Updated')
    await flushPromises()
    await findButton(wrapper, 'docs.suggestions.submit').trigger('click')
    await flushPromises()

    expect(docsApiMock.createSuggestion).toHaveBeenCalledWith('note-1', {
      selectionStart: 0,
      selectionEnd: 6,
      originalText: 'Server',
      replacementText: 'Updated',
      baseVersion: 2,
    })
    expect(messageSuccessMock).toHaveBeenCalledWith('docs.messages.suggestionCreated')
    await findButton(wrapper, 'docs.suggestions.accept').trigger('click')
    await flushPromises()

    expect(docsApiMock.acceptSuggestion).toHaveBeenCalledWith('note-1', 'suggestion-2', {
      currentVersion: 2,
    })
    expect(messageSuccessMock).toHaveBeenCalledWith('docs.messages.suggestionAccepted')

    await findButton(wrapper, 'docs.suggestions.reject').trigger('click')
    await flushPromises()

    expect(docsApiMock.rejectSuggestion).toHaveBeenCalledWith('note-1', 'suggestion-1')
    expect(messageSuccessMock).toHaveBeenCalledWith('docs.messages.suggestionRejected')
    expect(wrapper.text()).toContain('docs.suggestions.status.accepted')
    expect(wrapper.text()).toContain('docs.suggestions.status.rejected')
  })
})
