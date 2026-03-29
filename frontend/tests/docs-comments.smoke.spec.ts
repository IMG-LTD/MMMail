import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import DocsPage from '../pages/docs.vue'
import type {
  DocsNoteCollaborationOverview,
  DocsNoteComment,
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

let currentComments: DocsNoteComment[] = []

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
    collaboratorCount: 1,
    syncCursor: 5,
    syncVersion: 'DOC-2',
  }
}

function buildOverview(): DocsNoteCollaborationOverview {
  return {
    generatedAt: '2026-03-28T11:00:00',
    collaborators: [],
    comments: currentComments,
    activeSessions: [presenceRecord],
    syncCursor: 5,
    syncVersion: 'DOC-2',
  }
}

function configureApiMocks(): void {
  let commentSequence = 2
  docsApiMock.listNotes.mockResolvedValue(baseNotes)
  docsApiMock.createNote.mockResolvedValue({ id: 'note-created' })
  docsApiMock.getNote.mockImplementation(async () => buildDetail())
  docsApiMock.getCollaborationOverview.mockImplementation(async () => buildOverview())
  docsApiMock.listSuggestions.mockResolvedValue([])
  docsApiMock.heartbeatPresence.mockResolvedValue(presenceRecord)
  docsApiMock.listPresence.mockResolvedValue([presenceRecord])
  docsApiMock.createComment.mockImplementation(async (_noteId: string, payload: { excerpt?: string; content: string }) => {
    const created = {
      commentId: `comment-${commentSequence++}`,
      authorUserId: 'user-1',
      authorEmail: 'owner@mmmail.local',
      authorDisplayName: 'Owner',
      excerpt: payload.excerpt || null,
      content: payload.content,
      resolved: false,
      resolvedAt: null,
      createdAt: '2026-03-28T11:30:00',
    } satisfies DocsNoteComment
    currentComments = [created, ...currentComments]
    return created
  })
  docsApiMock.resolveComment.mockImplementation(async (_noteId: string, commentId: string) => {
    const updated = currentComments.find(item => item.commentId === commentId)
    if (!updated) {
      throw new Error('comment not found')
    }
    const next = {
      ...updated,
      resolved: true,
      resolvedAt: '2026-03-28T11:35:00',
    } satisfies DocsNoteComment
    currentComments = currentComments.map(item => item.commentId === commentId ? next : item)
    return next
  })
}

function mountDocsPage() {
  return mount(DocsPage, {
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
}

function findButton(wrapper: ReturnType<typeof mount>, text: string, index = 0) {
  return wrapper.findAll('button').filter(button => button.text().includes(text))[index]
}

beforeEach(() => {
  vi.clearAllMocks()
  currentComments = [
    {
      commentId: 'comment-1',
      authorUserId: 'user-2',
      authorEmail: 'reviewer@mmmail.local',
      authorDisplayName: 'Reviewer',
      excerpt: 'Server',
      content: 'Please tighten this sentence',
      resolved: false,
      resolvedAt: null,
      createdAt: '2026-03-28T11:20:00',
    },
  ]
  routeState.query = { noteId: 'note-1' }
  localStorage.clear()
  setGlobals()
  configureApiMocks()
})

describe('docs comments smoke', () => {
  it('wires comment creation and resolution through the docs page', async () => {
    const wrapper = mountDocsPage()
    await flushPromises()

    const editorTextarea = wrapper.find('.editor-fields textarea')
    const editorElement = editorTextarea.element as HTMLTextAreaElement
    editorElement.selectionStart = 0
    editorElement.selectionEnd = 6
    await editorTextarea.trigger('select')
    await flushPromises()

    const commentTextarea = wrapper.find('.comment-compose textarea')
    await commentTextarea.setValue('Looks good after revision')
    await flushPromises()
    await findButton(wrapper, 'docs.comments.add').trigger('click')
    await flushPromises()

    expect(docsApiMock.createComment).toHaveBeenCalledWith('note-1', {
      excerpt: 'Server',
      content: 'Looks good after revision',
    })
    expect(wrapper.text()).toContain('Looks good after revision')
    expect(messageSuccessMock).toHaveBeenCalledWith('docs.messages.commentAdded')

    await findButton(wrapper, 'docs.comments.resolve').trigger('click')
    await flushPromises()

    expect(docsApiMock.resolveComment).toHaveBeenCalledWith('note-1', 'comment-2')
    expect(messageSuccessMock).toHaveBeenCalledWith('docs.messages.commentResolved')
    expect(wrapper.text()).toContain('docs.comments.resolvedAt')
  })
})
