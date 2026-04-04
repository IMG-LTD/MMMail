import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'

const messageErrorMock = vi.fn()
const messageSuccessMock = vi.fn()
const messageWarningMock = vi.fn()

const getPublicShareMetadataMock = vi.fn()
const listPublicShareItemsMock = vi.fn()
const downloadPublicShareFileMock = vi.fn()
const previewPublicShareFileMock = vi.fn()
const downloadPublicShareItemMock = vi.fn()
const previewPublicShareItemMock = vi.fn()
const uploadPublicShareFileMock = vi.fn()
const saveSharedWithMeMock = vi.fn()
const decryptPublicShareFileMock = vi.fn()
const navigateToMock = vi.fn(async () => undefined)

vi.mock('element-plus', () => ({
  ElMessage: {
    error: messageErrorMock,
    success: messageSuccessMock,
    warning: messageWarningMock,
  },
}))

vi.mock('~/composables/useDriveApi', () => ({
  useDriveApi: () => ({
    getPublicShareMetadata: getPublicShareMetadataMock,
    listPublicShareItems: listPublicShareItemsMock,
    downloadPublicShareFile: downloadPublicShareFileMock,
    previewPublicShareFile: previewPublicShareFileMock,
    downloadPublicShareItem: downloadPublicShareItemMock,
    previewPublicShareItem: previewPublicShareItemMock,
    uploadPublicShareFile: uploadPublicShareFileMock,
    saveSharedWithMe: saveSharedWithMeMock,
  }),
}))

vi.mock('~/composables/useDriveFileE2ee', () => ({
  useDriveFileE2ee: () => ({
    decryptPublicShareFile: decryptPublicShareFileMock,
  }),
}))

vi.mock('~/stores/auth', () => ({
  useAuthStore: () => ({
    isAuthenticated: false,
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

const ElAlert = defineComponent({
  name: 'ElAlert',
  props: { title: { type: String, default: '' } },
  template: '<div class="el-alert-stub" v-bind="$attrs">{{ title }}<slot /></div>',
})

const ElButton = defineComponent({
  name: 'ElButton',
  emits: ['click'],
  template: '<button v-bind="$attrs" type="button" @click="$emit(\'click\')"><slot /></button>',
})

const ElEmpty = defineComponent({
  name: 'ElEmpty',
  template: '<div class="el-empty-stub"><slot /></div>',
})

const ElInput = defineComponent({
  name: 'ElInput',
  props: { modelValue: { type: String, default: '' } },
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)">',
})

const ElSkeleton = defineComponent({
  name: 'ElSkeleton',
  props: { loading: { type: Boolean, default: false } },
  template: '<div><slot /></div>',
})

const ElTag = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>',
})

const ElTable = defineComponent({
  name: 'ElTable',
  template: '<div class="el-table-stub"><slot /></div>',
})

const ElTableColumn = defineComponent({
  name: 'ElTableColumn',
  template: '<div class="el-table-column-stub"><slot /></div>',
})

async function mountPublicSharePage() {
  const { default: PublicSharePage } = await import('~/pages/public/drive/shares/[token].vue')
  return mount(PublicSharePage, {
    global: {
      directives: {
        loading: {},
      },
      stubs: {
        ElAlert,
        ElButton,
        ElEmpty,
        ElInput,
        ElSkeleton,
        ElTable,
        ElTableColumn,
        ElTag,
      },
    },
  })
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.stubGlobal('useRoute', () => ({
    params: { token: 'share-token' },
    query: {},
  }))
  vi.stubGlobal('navigateTo', navigateToMock)
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)
  const urlApi = globalThis.URL as typeof URL & {
    createObjectURL?: (object: Blob | MediaSource) => string
    revokeObjectURL?: (url: string) => void
  }
  urlApi.createObjectURL = vi.fn(() => 'blob:drive-public-share')
  urlApi.revokeObjectURL = vi.fn()

  getPublicShareMetadataMock.mockResolvedValue({
    shareId: 'share-1',
    token: 'share-token',
    itemId: 'file-1',
    itemType: 'FILE',
    itemName: 'release-note.txt',
    mimeType: 'text/plain',
    sizeBytes: 2048,
    permission: 'VIEW',
    status: 'ACTIVE',
    expiresAt: null,
    passwordProtected: true,
    e2ee: {
      enabled: true,
      algorithm: 'openpgp-password',
    },
  })
  listPublicShareItemsMock.mockResolvedValue([])
  downloadPublicShareFileMock.mockResolvedValue({
    blob: new Blob(['ciphertext']),
    fileName: 'release-note.txt.pgp',
  })
  previewPublicShareFileMock.mockResolvedValue({
    blob: new Blob(['server preview']),
    fileName: 'release-note.txt',
    mimeType: 'text/plain',
    truncated: false,
  })
  downloadPublicShareItemMock.mockResolvedValue({
    blob: new Blob(['nested']),
    fileName: 'nested.txt',
  })
  previewPublicShareItemMock.mockResolvedValue({
    blob: new Blob(['nested preview']),
    fileName: 'nested.txt',
    mimeType: 'text/plain',
    truncated: false,
  })
  uploadPublicShareFileMock.mockResolvedValue(undefined)
  saveSharedWithMeMock.mockResolvedValue(undefined)
  decryptPublicShareFileMock.mockResolvedValue({
    blob: new Blob(['decrypted preview'], { type: 'text/plain' }),
    fileName: 'release-note.txt',
  })
})

describe('drive public share e2ee', () => {
  it('uses download + local decrypt for root preview and download', async () => {
    const wrapper = await mountPublicSharePage()
    await flushPromises()

    await wrapper.get('[data-testid="public-share-password"]').setValue('share-secret')
    await wrapper.get('[data-testid="public-share-unlock"]').trigger('click')
    await flushPromises()

    expect(downloadPublicShareFileMock).toHaveBeenCalledWith('share-token', 'share-secret')
    expect(decryptPublicShareFileMock).toHaveBeenCalledWith(
      expect.objectContaining({ fileName: 'release-note.txt.pgp' }),
      expect.objectContaining({
        id: 'file-1',
        name: 'release-note.txt',
        e2ee: expect.objectContaining({
          enabled: true,
          algorithm: 'openpgp-password',
          recipientFingerprints: [],
        }),
      }),
      'share-secret',
    )
    expect(previewPublicShareFileMock).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('decrypted preview')

    await wrapper.get('[data-testid="public-share-download"]').trigger('click')
    await flushPromises()

    expect(downloadPublicShareFileMock).toHaveBeenCalledTimes(2)
    expect(messageSuccessMock).toHaveBeenCalledWith('drive.publicShare.messages.downloadStarted')
  })
})
