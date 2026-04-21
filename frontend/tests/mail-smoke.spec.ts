import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import type {
  LabelItem,
  MailAttachment,
  MailDetail,
  MailPage,
  MailSenderIdentity
} from '../types/api'

const navigateToMock = vi.fn()
const routerReplaceMock = vi.fn()
const messageErrorMock = vi.fn()
const messageSuccessMock = vi.fn()
const messageWarningMock = vi.fn()
const fetchMailE2eeKeyProfileMock = vi.fn()
const isDraftAttachmentEncryptionEnabledMock = vi.fn()
const encryptDraftAttachmentMock = vi.fn()
const decryptDownloadedAttachmentMock = vi.fn()
const routeState = {
  params: { id: '42' },
  query: {} as Record<string, unknown>
}

vi.mock('element-plus', () => ({
  ElMessage: {
    error: messageErrorMock,
    success: messageSuccessMock,
    warning: messageWarningMock
  },
  ElMessageBox: {
    prompt: vi.fn(),
    confirm: vi.fn()
  }
}))

const folderStore: Record<string, MailPage> = {}
const mailStore = {
  setFolder: vi.fn((folder: string, page: MailPage) => {
    folderStore[folder] = page
  }),
  getFolder: vi.fn((folder: string) => folderStore[folder]?.items || []),
  updateStats: vi.fn(),
  setCustomFolders: vi.fn()
}

const labelItems: LabelItem[] = [{ id: 1, name: 'ops', color: '#0F6E6E' }]
const senderOptions: MailSenderIdentity[] = [{
  identityId: null,
  orgId: null,
  orgName: null,
  memberId: null,
  emailAddress: 'owner@mmmail.local',
  displayName: 'Owner',
  source: 'PRIMARY',
  status: 'ENABLED',
  defaultIdentity: true
}]

const detailMail: MailDetail = {
  id: '42',
  ownerId: '7',
  senderEmail: 'owner@mmmail.local',
  peerEmail: 'alice@example.com',
  folderType: 'INBOX',
  customFolderId: null,
  customFolderName: null,
  subject: 'Quarterly update',
  preview: 'Preview text',
  body: 'Body line',
  isRead: false,
  isStarred: false,
  isDraft: false,
  sentAt: '2026-03-13T10:20:30Z',
  labels: ['ops'],
  attachments: [{
    id: 'att-1',
    mailId: '42',
    fileName: 'report.pdf',
    contentType: 'application/pdf',
    fileSize: 2048
  }]
}

const mailApiMock = {
  fetchFolder: vi.fn(),
  fetchStats: vi.fn(),
  applyAction: vi.fn(),
  applyBatchAction: vi.fn(),
  undoSend: vi.fn(),
  restoreAllTrash: vi.fn(),
  emptyTrash: vi.fn(),
  restoreAllSpam: vi.fn(),
  emptySpam: vi.fn(),
  snoozeUntil: vi.fn(),
  fetchMailDetail: vi.fn(),
  updateLabels: vi.fn(),
  downloadMailAttachment: vi.fn(),
  sendMail: vi.fn(),
  saveDraft: vi.fn(),
  uploadDraftAttachment: vi.fn(),
  deleteDraftAttachment: vi.fn(),
  listSenderIdentities: vi.fn(),
  fetchRecipientE2eeStatus: vi.fn(async () => ({
    toEmail: 'alice@example.com',
    fromEmail: 'owner@mmmail.local',
    deliverable: false,
    encryptionReady: false,
    readiness: 'UNDELIVERABLE',
    routeCount: 0,
    routes: []
  }))
}

vi.mock('~/composables/useMailApi', () => ({
  useMailApi: () => mailApiMock
}))
vi.mock('~/composables/useLabelApi', () => ({
  useLabelApi: () => ({
    listLabels: vi.fn(async () => labelItems)
  })
}))
vi.mock('~/composables/useMailFolderApi', () => ({
  useMailFolderApi: () => ({
    listMailFolders: vi.fn(async () => [])
  })
}))
vi.mock('~/composables/useSettingsApi', () => ({
  useSettingsApi: () => ({
    fetchProfile: vi.fn(async () => ({
      autoSaveSeconds: 15,
      undoSendSeconds: 0
    })),
    fetchMailE2eeKeyProfile: fetchMailE2eeKeyProfileMock
  })
}))
vi.mock('~/composables/useMailAttachmentE2ee', () => ({
  useMailAttachmentE2ee: () => ({
    isDraftAttachmentEncryptionEnabled: isDraftAttachmentEncryptionEnabledMock,
    encryptDraftAttachment: encryptDraftAttachmentMock,
    decryptDownloadedAttachment: decryptDownloadedAttachmentMock
  })
}))
vi.mock('~/composables/useContactApi', () => ({
  useContactApi: () => ({
    fetchSuggestions: vi.fn(async () => [{ email: 'alice@example.com' }]),
    quickAddContact: vi.fn(async () => undefined)
  })
}))
vi.mock('~/composables/usePassApi', () => ({
  usePassApi: () => ({
    suggestAliasContacts: vi.fn(async () => [])
  })
}))
vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    locale: { value: 'en-US' },
    t: (key: string) => key
  })
}))
vi.mock('~/stores/mail', () => ({
  useMailStore: () => mailStore
}))
vi.mock('~/stores/auth', () => ({
  useAuthStore: () => ({
    user: {
      email: 'owner@mmmail.local',
      displayName: 'Owner'
    }
  })
}))

const elementStubs = {
  ElAlert: defineComponent({
    name: 'ElAlert',
    props: { title: { type: String, default: '' } },
    template: '<div class="el-alert-stub"><slot />{{ title }}</div>'
  }),
  ElButton: defineComponent({
    name: 'ElButton',
    emits: ['click'],
    template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
  }),
  ElEmpty: defineComponent({
    name: 'ElEmpty',
    props: { description: { type: String, default: '' } },
    template: '<div class="el-empty-stub">{{ description }}</div>'
  }),
  ElForm: defineComponent({
    name: 'ElForm',
    template: '<form><slot /></form>'
  }),
  ElFormItem: defineComponent({
    name: 'ElFormItem',
    template: '<div><slot /></div>'
  }),
  ElInput: defineComponent({
    name: 'ElInput',
    props: { modelValue: { type: String, default: '' } },
    emits: ['update:modelValue', 'keyup'],
    template: '<input :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" @keyup="$emit(\'keyup\', $event)">'
  }),
  ElOption: defineComponent({
    name: 'ElOption',
    template: '<option><slot /></option>'
  }),
  ElPagination: defineComponent({
    name: 'ElPagination',
    props: {
      currentPage: { type: Number, default: 1 },
      pageSize: { type: Number, default: 20 }
    },
    emits: ['current-change', 'size-change'],
    template: `
      <div>
        <button data-testid="mail-next-page" type="button" @click="$emit('current-change', currentPage + 1)">next</button>
        <button data-testid="mail-page-size-50" type="button" @click="$emit('size-change', 50)">size</button>
      </div>
    `
  }),
  ElSelect: defineComponent({
    name: 'ElSelect',
    template: '<div><slot /></div>'
  }),
  ElSkeleton: defineComponent({
    name: 'ElSkeleton',
    template: '<div class="el-skeleton-stub"></div>'
  })
}

const mailListStub = defineComponent({
  name: 'MailList',
  props: {
    mails: { type: Array, default: () => [] }
  },
  emits: ['open', 'batch-action'],
  template: `
    <div data-testid="mail-list">
      <span data-testid="mail-list-count">{{ mails.length }}</span>
      <button v-if="mails.length" data-testid="open-mail" type="button" @click="$emit('open', mails[0].id)">open</button>
      <button v-if="mails.length" data-testid="batch-mail" type="button" @click="$emit('batch-action', [mails[0].id], 'MOVE_ARCHIVE')">batch</button>
      <div v-else data-testid="mail-list-empty">empty</div>
    </div>
  `
})

const mailComposerStub = defineComponent({
  name: 'MailComposer',
  props: {
    draftId: { type: String, default: '' },
    defaultTo: { type: String, default: '' },
    defaultSubject: { type: String, default: '' },
    defaultBody: { type: String, default: '' },
    attachments: { type: Array, default: () => [] },
    failedUploads: { type: Array, default: () => [] }
  },
  emits: ['upload-attachments', 'retry-attachment', 'send'],
  template: `
    <div>
      <span data-testid="draft-id">{{ draftId }}</span>
      <span data-testid="attachment-count">{{ attachments.length }}</span>
      <span data-testid="failure-count">{{ failedUploads.length }}</span>
      <button
        data-testid="upload"
        type="button"
        @click="$emit('upload-attachments', { files: [{ name: 'upload.txt', size: 6, type: 'text/plain' }], draft: { draftId, toEmail: defaultTo || 'alice@example.com', subject: defaultSubject || 'Quarterly update', body: defaultBody || 'Body line' } })"
      >
        upload
      </button>
      <button
        v-if="failedUploads.length"
        data-testid="retry"
        type="button"
        @click="$emit('retry-attachment', { failureId: failedUploads[0].id, draft: { draftId, toEmail: defaultTo || 'alice@example.com', subject: defaultSubject || 'Quarterly update', body: defaultBody || 'Body line' } })"
      >
        retry
      </button>
      <button
        data-testid="send"
        type="button"
        @click="$emit('send', { draftId, toEmail: defaultTo || 'alice@example.com', subject: defaultSubject || 'Quarterly update', body: defaultBody || 'Body line', idempotencyKey: 'mail-smoke-send', labels: [] })"
      >
        send
      </button>
    </div>
  `
})

function buildPage(page: number, keyword = '', total = 1): MailPage {
  if (!keyword && total > 0) {
    return {
      items: [{
        id: `m-${page}`,
        ownerId: '7',
        senderEmail: page === 1 ? 'alice@example.com' : 'bob@example.com',
        peerEmail: page === 1 ? 'alice@example.com' : 'bob@example.com',
        folderType: 'INBOX',
        customFolderId: null,
        customFolderName: null,
        subject: page === 1 ? 'Inbox mail' : 'Second page mail',
        preview: 'Preview',
        isRead: page !== 1,
        isStarred: false,
        isDraft: false,
        sentAt: `2026-03-13T1${page}:20:30Z`,
        labels: []
      }],
      total,
      page,
      size: 20,
      unread: 1
    }
  }
  return { items: [], total: 0, page, size: 20, unread: 0 }
}

function resetMailApiMocks(): void {
  fetchMailE2eeKeyProfileMock.mockReset()
  isDraftAttachmentEncryptionEnabledMock.mockReset()
  encryptDraftAttachmentMock.mockReset()
  decryptDownloadedAttachmentMock.mockReset()
  Object.values(mailApiMock).forEach((value) => value.mockReset())
  fetchMailE2eeKeyProfileMock.mockResolvedValue({
    enabled: false,
    fingerprint: null,
    algorithm: null,
    publicKeyArmored: null,
    encryptedPrivateKeyArmored: null,
    keyCreatedAt: null
  })
  isDraftAttachmentEncryptionEnabledMock.mockResolvedValue(false)
  encryptDraftAttachmentMock.mockImplementation(async (file: File) => ({
    file,
    fileName: file.name,
    contentType: file.type || 'application/octet-stream',
    fileSize: file.size,
    e2ee: {
      algorithm: 'openpgp',
      recipientFingerprints: ['PROFILE'],
      envelope: {
        contentFormat: 'openpgp-binary',
        originalFileName: file.name,
        originalContentType: file.type || 'application/octet-stream',
        originalFileSize: file.size
      }
    }
  }))
  decryptDownloadedAttachmentMock.mockImplementation(async (downloaded: { blob: Blob, fileName: string }) => downloaded)
  mailApiMock.fetchStats.mockResolvedValue({ folderCounts: {}, unreadCount: 1, starredCount: 0 })
  mailApiMock.applyAction.mockResolvedValue({ affected: 1, stats: { folderCounts: {}, unreadCount: 0, starredCount: 0 } })
  mailApiMock.applyBatchAction.mockResolvedValue({ affected: 1, stats: { folderCounts: {}, unreadCount: 0, starredCount: 0 } })
  mailApiMock.undoSend.mockResolvedValue(undefined)
  mailApiMock.restoreAllTrash.mockResolvedValue({ affected: 0, stats: { folderCounts: {}, unreadCount: 0, starredCount: 0 } })
  mailApiMock.emptyTrash.mockResolvedValue({ affected: 0, stats: { folderCounts: {}, unreadCount: 0, starredCount: 0 } })
  mailApiMock.restoreAllSpam.mockResolvedValue({ affected: 0, stats: { folderCounts: {}, unreadCount: 0, starredCount: 0 } })
  mailApiMock.emptySpam.mockResolvedValue({ affected: 0, stats: { folderCounts: {}, unreadCount: 0, starredCount: 0 } })
  mailApiMock.snoozeUntil.mockResolvedValue({ affected: 1, stats: { folderCounts: {}, unreadCount: 0, starredCount: 0 } })
  mailApiMock.fetchMailDetail.mockResolvedValue(detailMail)
  mailApiMock.updateLabels.mockResolvedValue(undefined)
  mailApiMock.downloadMailAttachment.mockResolvedValue({ blob: new Blob(['report']), fileName: 'report.pdf' })
  mailApiMock.sendMail.mockResolvedValue(undefined)
  mailApiMock.saveDraft.mockResolvedValue('42')
  mailApiMock.uploadDraftAttachment.mockResolvedValue({
    id: 'att-2',
    mailId: '42',
    fileName: 'upload.txt',
    contentType: 'text/plain',
    fileSize: 12
  } satisfies MailAttachment)
  mailApiMock.deleteDraftAttachment.mockResolvedValue(undefined)
  mailApiMock.listSenderIdentities.mockResolvedValue(senderOptions)
}

function findButton(wrapper: VueWrapper<unknown>, selector: string) {
  return wrapper.get(selector)
}

async function importFolderMailbox() {
  return await import('../components/business/FolderMailbox.vue')
}

async function importMailDetailPage() {
  return await import('../pages/mail/[id].vue')
}

async function importComposePage() {
  return await import('../pages/compose.vue')
}

beforeEach(() => {
  vi.clearAllMocks()
  resetMailApiMocks()
  routeState.params = { id: '42' }
  routeState.query = {}
  folderStore.INBOX = buildPage(1)
  ;(globalThis as Record<string, unknown>).useRoute = () => routeState
  ;(globalThis as Record<string, unknown>).useRouter = () => ({ replace: routerReplaceMock })
  ;(globalThis as Record<string, unknown>).navigateTo = navigateToMock
  ;(globalThis as Record<string, unknown>).useHead = () => undefined
  ;(globalThis as Record<string, unknown>).definePageMeta = () => undefined
  const urlApi = globalThis.URL as typeof URL & {
    createObjectURL?: (object: Blob | MediaSource) => string
    revokeObjectURL?: (url: string) => void
  }
  urlApi.createObjectURL = vi.fn(() => 'blob:mail')
  urlApi.revokeObjectURL = vi.fn()
})

describe('mail smoke', () => {
  it('covers list navigation, search, pagination and bulk actions', async () => {
    mailApiMock.fetchFolder
      .mockResolvedValueOnce(buildPage(1))
      .mockResolvedValueOnce(buildPage(1))
      .mockResolvedValueOnce(buildPage(1))
      .mockResolvedValueOnce(buildPage(2))

    const { default: FolderMailbox } = await importFolderMailbox()
    const wrapper = mount(FolderMailbox, {
      props: { titleKey: 'nav.inbox', folder: 'inbox' },
      global: { stubs: { ...elementStubs, MailList: mailListStub } }
    })

    await flushPromises()
    expect(mailApiMock.fetchFolder).toHaveBeenCalledWith('inbox', 1, 20, '', {})

    await wrapper.get('[data-testid="mail-search-input"]').setValue('release')
    await wrapper.get('[data-testid="mail-search-button"]').trigger('click')
    await flushPromises()
    expect(mailApiMock.fetchFolder).toHaveBeenNthCalledWith(2, 'inbox', 1, 20, 'release', {})

    await wrapper.get('[data-testid="open-mail"]').trigger('click')
    expect(navigateToMock).toHaveBeenCalledWith('/mail/m-1')

    await wrapper.get('[data-testid="batch-mail"]').trigger('click')
    await flushPromises()
    expect(mailApiMock.applyBatchAction).toHaveBeenCalledWith(['m-1'], 'MOVE_ARCHIVE')

    await wrapper.get('[data-testid="mail-next-page"]').trigger('click')
    await flushPromises()
    expect(mailApiMock.fetchFolder).toHaveBeenLastCalledWith('inbox', 2, 20, 'release', {})
  })

  it('shows empty state and visible error when search reload fails', async () => {
    mailApiMock.fetchFolder
      .mockResolvedValueOnce(buildPage(1))
      .mockResolvedValueOnce(buildPage(1, 'missing', 0))
      .mockRejectedValueOnce(new Error('Search failed'))

    const { default: FolderMailbox } = await importFolderMailbox()
    const wrapper = mount(FolderMailbox, {
      props: { titleKey: 'nav.inbox', folder: 'inbox' },
      global: { stubs: { ...elementStubs, MailList: mailListStub } }
    })

    await flushPromises()
    await wrapper.get('[data-testid="mail-search-input"]').setValue('missing')
    await wrapper.get('[data-testid="mail-search-button"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="mail-list-empty"]').text()).toContain('empty')

    await wrapper.get('[data-testid="mail-search-input"]').setValue('broken')
    await wrapper.get('[data-testid="mail-search-button"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="mail-load-error"]').text()).toContain('Search failed')
    expect(messageErrorMock).toHaveBeenCalledWith('Search failed')
  })

  it('passes inbox triage filters through FolderMailbox reloads', async () => {
    mailApiMock.fetchFolder
      .mockResolvedValueOnce(buildPage(1))
      .mockResolvedValueOnce(buildPage(1))
      .mockResolvedValueOnce(buildPage(1))

    const { default: FolderMailbox } = await importFolderMailbox()
    const wrapper = mount(FolderMailbox, {
      props: { titleKey: 'nav.inbox', folder: 'inbox' },
      global: { stubs: { ...elementStubs, MailList: mailListStub } }
    })

    await flushPromises()
    expect(wrapper.find('[data-testid="mail-filter-unread"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="mail-filter-needsReply"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="mail-filter-starred"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="mail-filter-attachments"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="mail-filter-importantContact"]').exists()).toBe(true)

    await wrapper.get('[data-testid="mail-filter-needsReply"]').trigger('click')
    await flushPromises()
    expect(mailApiMock.fetchFolder).toHaveBeenLastCalledWith('inbox', 1, 20, '', {
      needsReply: true
    })

    await wrapper.get('[data-testid="mail-search-input"]').setValue('vip')
    await wrapper.get('[data-testid="mail-search-button"]').trigger('click')
    await flushPromises()
    expect(mailApiMock.fetchFolder).toHaveBeenLastCalledWith('inbox', 1, 20, 'vip', {
      needsReply: true
    })
  })

  it('does not render inbox triage filters for non-inbox folders', async () => {
    mailApiMock.fetchFolder.mockResolvedValueOnce(buildPage(1))

    const { default: FolderMailbox } = await importFolderMailbox()
    const wrapper = mount(FolderMailbox, {
      props: { titleKey: 'nav.sent', folder: 'sent' },
      global: { stubs: { ...elementStubs, MailList: mailListStub } }
    })

    await flushPromises()
    expect(wrapper.find('[data-testid="mail-filter-needsReply"]').exists()).toBe(false)
    expect(mailApiMock.fetchFolder).toHaveBeenCalledWith('sent', 1, 20, '', {})
  })

  it('redirects legacy mail detail routes to the conversation workspace', async () => {
    const { default: MailDetailPage } = await importMailDetailPage()
    mount(MailDetailPage, {
      global: { stubs: elementStubs }
    })

    await flushPromises()
    expect(navigateToMock).toHaveBeenCalledWith('/conversations/42')
    expect(mailApiMock.fetchMailDetail).not.toHaveBeenCalled()
    expect(mailApiMock.downloadMailAttachment).not.toHaveBeenCalled()
  })

  it('restores the same draft and retries failed attachment upload', async () => {
    routeState.query = { draftId: '42' }
    isDraftAttachmentEncryptionEnabledMock.mockResolvedValue(true)
    mailApiMock.fetchMailDetail.mockResolvedValueOnce({
      ...detailMail,
      isDraft: true,
      folderType: 'DRAFTS'
    })
    encryptDraftAttachmentMock.mockResolvedValue({
      file: new File(['ciphertext'], 'upload.txt.pgp', { type: 'application/octet-stream' }),
      fileName: 'upload.txt',
      contentType: 'text/plain',
      fileSize: 6,
      e2ee: {
        algorithm: 'openpgp',
        recipientFingerprints: ['A1B2C3']
      }
    })
    mailApiMock.uploadDraftAttachment
      .mockRejectedValueOnce(new Error('Attachment upload failed'))
      .mockResolvedValueOnce({
        id: 'att-2',
        mailId: '42',
        fileName: 'upload.txt',
        contentType: 'text/plain',
        fileSize: 12
      } satisfies MailAttachment)

    const { default: ComposePage } = await importComposePage()
    const wrapper = mount(ComposePage, {
      global: { stubs: { ...elementStubs, MailComposer: mailComposerStub } }
    })

    await flushPromises()
    await flushPromises()
    expect(mailApiMock.fetchMailDetail).toHaveBeenCalledWith('42')
    expect(wrapper.get('[data-testid="attachment-count"]').text()).toBe('1')

    await wrapper.get('[data-testid="upload"]').trigger('click')
    await flushPromises()
    expect(mailApiMock.uploadDraftAttachment).toHaveBeenCalledWith('42', expect.objectContaining({
      fileName: 'upload.txt',
      contentType: 'text/plain',
      fileSize: 6,
      e2ee: expect.objectContaining({
        algorithm: 'openpgp',
        recipientFingerprints: ['A1B2C3']
      })
    }))
    expect(wrapper.get('[data-testid="failure-count"]').text()).toBe('1')
    expect(wrapper.get('[data-testid="mail-compose-error"]').text()).toContain('Attachment upload failed')
    expect(mailApiMock.saveDraft).not.toHaveBeenCalled()

    await wrapper.get('[data-testid="retry"]').trigger('click')
    await flushPromises()
    expect(encryptDraftAttachmentMock).toHaveBeenCalledTimes(2)
    expect(mailApiMock.uploadDraftAttachment).toHaveBeenCalledTimes(2)
    expect(wrapper.get('[data-testid="attachment-count"]').text()).toBe('2')
    expect(wrapper.get('[data-testid="failure-count"]').text()).toBe('0')
    expect(routerReplaceMock).not.toHaveBeenCalled()
  })

  it('shows send failure instead of hiding compose errors', async () => {
    mailApiMock.sendMail.mockRejectedValueOnce(new Error('Send failed'))

    const { default: ComposePage } = await importComposePage()
    const wrapper = mount(ComposePage, {
      global: { stubs: { ...elementStubs, MailComposer: mailComposerStub } }
    })

    await flushPromises()
    await wrapper.get('[data-testid="send"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="mail-compose-error"]').text()).toContain('Send failed')
    expect(messageErrorMock).toHaveBeenCalledWith('Send failed')
  })
})
