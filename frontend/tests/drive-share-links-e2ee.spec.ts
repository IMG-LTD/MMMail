import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import type { DriveItem } from '~/types/api'

const messageErrorMock = vi.fn()
const messageSuccessMock = vi.fn()
const messageWarningMock = vi.fn()
const promptMock = vi.fn()

const listSharesMock = vi.fn()
const createShareMock = vi.fn()
const createEncryptedPublicShareMock = vi.fn()
const updateShareMock = vi.fn()
const revokeShareMock = vi.fn()
const downloadFileMock = vi.fn()
const listCollaboratorSharesMock = vi.fn()
const createCollaboratorShareMock = vi.fn()
const updateCollaboratorShareMock = vi.fn()
const removeCollaboratorShareMock = vi.fn()

const encryptPublicShareFileMock = vi.fn()

vi.mock('element-plus', () => ({
  ElMessage: {
    error: messageErrorMock,
    success: messageSuccessMock,
    warning: messageWarningMock,
  },
  ElMessageBox: {
    prompt: promptMock,
  },
}))

vi.mock('~/composables/useDriveApi', () => ({
  useDriveApi: () => ({
    listShares: listSharesMock,
    createShare: createShareMock,
    createEncryptedPublicShare: createEncryptedPublicShareMock,
    updateShare: updateShareMock,
    revokeShare: revokeShareMock,
    downloadFile: downloadFileMock,
    listCollaboratorShares: listCollaboratorSharesMock,
    createCollaboratorShare: createCollaboratorShareMock,
    updateCollaboratorShare: updateCollaboratorShareMock,
    removeCollaboratorShare: removeCollaboratorShareMock,
  }),
}))

vi.mock('~/composables/useDriveFileE2ee', () => ({
  useDriveFileE2ee: () => ({
    encryptPublicShareFile: encryptPublicShareFileMock,
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

const ElCheckbox = defineComponent({
  name: 'ElCheckbox',
  props: { modelValue: { type: Boolean, default: false } },
  emits: ['update:modelValue'],
  template: '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)">',
})

const ElDatePicker = defineComponent({
  name: 'ElDatePicker',
  props: { modelValue: { type: String, default: '' } },
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" type="datetime-local" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)">',
})

const ElDialog = defineComponent({
  name: 'ElDialog',
  props: { modelValue: { type: Boolean, default: false } },
  template: '<div v-if="modelValue" class="el-dialog-stub"><slot /><slot name="footer" /></div>',
})

const ElDrawer = defineComponent({
  name: 'ElDrawer',
  props: { modelValue: { type: Boolean, default: false } },
  emits: ['update:modelValue', 'closed'],
  template: '<div v-if="modelValue" class="el-drawer-stub"><slot /></div>',
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

const ElOption = defineComponent({
  name: 'ElOption',
  props: {
    label: { type: String, default: '' },
    value: { type: String, default: '' },
  },
  template: '<option :value="value">{{ label || value }}</option>',
})

const ElSelect = defineComponent({
  name: 'ElSelect',
  props: { modelValue: { type: String, default: '' } },
  emits: ['update:modelValue', 'change'],
  template: '<select v-bind="$attrs" :value="modelValue" @change="$emit(\'update:modelValue\', $event.target.value); $emit(\'change\', $event.target.value)"><slot /></select>',
})

const ElTable = defineComponent({
  name: 'ElTable',
  template: '<div class="el-table-stub"><slot /></div>',
})

const ElTableColumn = defineComponent({
  name: 'ElTableColumn',
  template: '<div class="el-table-column-stub"></div>',
})

const ElTag = defineComponent({
  name: 'ElTag',
  template: '<span class="el-tag-stub"><slot /></span>',
})

const encryptedItem: DriveItem = {
  id: 'file-e2ee-1',
  parentId: null,
  itemType: 'FILE',
  name: 'release-note.txt',
  mimeType: 'text/plain',
  sizeBytes: 2048,
  shareCount: 0,
  createdAt: '2026-03-20T10:00:00',
  updatedAt: '2026-03-20T10:00:00',
  e2ee: {
    enabled: true,
    algorithm: 'openpgp',
    recipientFingerprints: ['DRIVE-SELF-FP'],
  },
}

async function mountDrawer() {
  const { default: DriveShareLinksDrawer } = await import('~/components/drive/DriveShareLinksDrawer.vue')
  return mount(DriveShareLinksDrawer, {
    props: {
      modelValue: true,
      item: encryptedItem,
    },
    global: {
      directives: {
        loading: {
          mounted() {
            return undefined
          },
          updated() {
            return undefined
          },
        },
      },
      stubs: {
        ElAlert,
        ElButton,
        ElCheckbox,
        ElDatePicker,
        ElDialog,
        ElDrawer,
        ElEmpty,
        ElInput,
        ElOption,
        ElSelect,
        ElTable,
        ElTableColumn,
        ElTag,
      },
    },
  })
}

beforeEach(() => {
  vi.clearAllMocks()
  listSharesMock.mockResolvedValue([])
  listCollaboratorSharesMock.mockResolvedValue([])
  createShareMock.mockResolvedValue(undefined)
  createEncryptedPublicShareMock.mockResolvedValue({ id: 'share-1' })
  updateShareMock.mockResolvedValue(undefined)
  revokeShareMock.mockResolvedValue(undefined)
  downloadFileMock.mockResolvedValue({
    blob: new Blob(['ciphertext']),
    fileName: 'release-note.txt.pgp',
  })
  promptMock.mockResolvedValue({ value: 'owner-passphrase' })
  encryptPublicShareFileMock.mockResolvedValue({
    file: new File(['shared-ciphertext'], 'release-note.txt.pgp', { type: 'application/octet-stream' }),
    e2ee: {
      enabled: true,
      algorithm: 'openpgp-password',
      mode: 'PASSWORD',
      fileName: 'release-note.txt',
      contentType: 'text/plain',
      fileSize: 2048,
    },
  })
})

describe('drive share links drawer e2ee', () => {
  it('creates encrypted public share with local decrypt + password re-encrypt flow', async () => {
    const wrapper = await mountDrawer()
    await flushPromises()

    expect(wrapper.find('[data-testid="drive-share-collaborator-disabled"]').exists()).toBe(true)
    expect(listCollaboratorSharesMock).not.toHaveBeenCalled()

    await wrapper.get('input[placeholder="drive.shareDrawer.e2ee.passwordPlaceholder"]').setValue('share-secret')
    await wrapper.get('[data-testid="drive-share-create-public"]').trigger('click')
    await flushPromises()

    expect(downloadFileMock).toHaveBeenCalledWith('file-e2ee-1')
    expect(encryptPublicShareFileMock).toHaveBeenCalledWith(
      expect.objectContaining({ fileName: 'release-note.txt.pgp' }),
      encryptedItem,
      'owner-passphrase',
      'share-secret',
    )
    expect(createEncryptedPublicShareMock).toHaveBeenCalledWith(
      'file-e2ee-1',
      expect.objectContaining({
        permission: 'VIEW',
        password: 'share-secret',
      }),
    )
    expect(createShareMock).not.toHaveBeenCalled()
    expect(messageSuccessMock).toHaveBeenCalledWith('drive.messages.shareCreated')
  })

  it('rejects encrypted public share creation when password is missing', async () => {
    const wrapper = await mountDrawer()
    await flushPromises()

    await wrapper.get('[data-testid="drive-share-create-public"]').trigger('click')
    await flushPromises()

    expect(messageErrorMock).toHaveBeenCalledWith('drive.shareDrawer.e2ee.passwordRequired')
    expect(downloadFileMock).not.toHaveBeenCalled()
    expect(createEncryptedPublicShareMock).not.toHaveBeenCalled()
    expect(createShareMock).not.toHaveBeenCalled()
  })
})
