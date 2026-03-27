import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import type { DriveBatchShareResult, DriveItem } from '~/types/api'

const messageErrorMock = vi.fn()
const messageSuccessMock = vi.fn()
const batchCreateSharesMock = vi.fn()
const clipboardWriteTextMock = vi.fn(async () => undefined)

const items: DriveItem[] = [
  {
    id: 'folder-1',
    parentId: null,
    itemType: 'FOLDER',
    name: 'Projects',
    mimeType: null,
    sizeBytes: 0,
    shareCount: 0,
    createdAt: '2026-03-13T09:00:00',
    updatedAt: '2026-03-13T09:00:00',
  },
  {
    id: 'file-1',
    parentId: null,
    itemType: 'FILE',
    name: 'release-note.txt',
    mimeType: 'text/plain',
    sizeBytes: 4096,
    shareCount: 1,
    createdAt: '2026-03-13T09:05:00',
    updatedAt: '2026-03-13T09:05:00',
  },
]

const batchShareResult: DriveBatchShareResult = {
  requestedCount: 2,
  successCount: 2,
  failedCount: 0,
  createdShares: [
    {
      id: 'share-1',
      itemId: 'folder-1',
      token: 'token-folder',
      permission: 'VIEW',
      expiresAt: null,
      status: 'ACTIVE',
      passwordProtected: false,
      createdAt: '2026-03-13T09:10:00',
      updatedAt: '2026-03-13T09:10:00',
    },
    {
      id: 'share-2',
      itemId: 'file-1',
      token: 'token-file',
      permission: 'VIEW',
      expiresAt: null,
      status: 'ACTIVE',
      passwordProtected: true,
      createdAt: '2026-03-13T09:11:00',
      updatedAt: '2026-03-13T09:11:00',
    },
  ],
  failedItems: [],
}

vi.mock('element-plus', () => ({
  ElMessage: {
    error: messageErrorMock,
    success: messageSuccessMock,
  },
}))

vi.mock('~/composables/useDriveApi', () => ({
  useDriveApi: () => ({
    batchCreateShares: batchCreateSharesMock,
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

const ElButton = defineComponent({
  name: 'ElButton',
  emits: ['click'],
  template: '<button v-bind="$attrs" type="button" @click="$emit(\'click\')"><slot /></button>',
})

const ElAlert = defineComponent({
  name: 'ElAlert',
  props: { title: { type: String, default: '' } },
  template: '<div v-bind="$attrs" class="el-alert-stub"><slot />{{ title }}</div>',
})

const ElDialog = defineComponent({
  name: 'ElDialog',
  props: { modelValue: { type: Boolean, default: false } },
  template: '<div v-if="modelValue" class="el-dialog-stub"><slot /><slot name="footer" /></div>',
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
  emits: ['update:modelValue'],
  template: '<select v-bind="$attrs" @change="$emit(\'update:modelValue\', $event.target.value)"><slot /></select>',
})

const ElDatePicker = defineComponent({
  name: 'ElDatePicker',
  props: { modelValue: { type: String, default: '' } },
  emits: ['update:modelValue'],
  template: '<input v-bind="$attrs" type="datetime-local" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)">',
})

async function importBatchShareDialog() {
  return await import('~/components/drive/DriveBatchShareDialog.vue')
}

beforeEach(() => {
  vi.clearAllMocks()
  batchCreateSharesMock.mockResolvedValue(batchShareResult)
  Object.defineProperty(globalThis.navigator, 'clipboard', {
    configurable: true,
    value: {
      writeText: clipboardWriteTextMock,
    },
  })
})

describe('drive batch share dialog', () => {
  it('submits selected items and renders created share links', async () => {
    const { default: DriveBatchShareDialog } = await importBatchShareDialog()
    const wrapper = mount(DriveBatchShareDialog, {
      props: {
        modelValue: true,
        items,
      },
      global: {
        stubs: {
          ElAlert,
          ElButton,
          ElDatePicker,
          ElDialog,
          ElInput,
          ElOption,
          ElSelect,
        },
      },
    })

    await wrapper.get('[data-testid="drive-batch-share-submit"]').trigger('click')
    await flushPromises()

    expect(batchCreateSharesMock).toHaveBeenCalledWith({
      itemIds: ['folder-1', 'file-1'],
      permission: 'VIEW',
      expiresAt: undefined,
      password: undefined,
    })
    expect(wrapper.get('[data-testid="drive-batch-share-result"]').text()).toContain('drive.batch.shareDialog.resultSummary')

    await wrapper.get('[data-testid="drive-batch-share-copy-token-share-1"]').trigger('click')
    await wrapper.get('[data-testid="drive-batch-share-copy-link-share-1"]').trigger('click')
    expect(clipboardWriteTextMock).toHaveBeenCalledWith('token-folder')
    expect(clipboardWriteTextMock).toHaveBeenCalledWith('http://localhost:3000/public/drive/shares/token-folder')
  })

  it('shows visible retry when batch share fails and can recover', async () => {
    batchCreateSharesMock
      .mockRejectedValueOnce(new Error('Batch share exploded'))
      .mockResolvedValue(batchShareResult)

    const { default: DriveBatchShareDialog } = await importBatchShareDialog()
    const wrapper = mount(DriveBatchShareDialog, {
      props: {
        modelValue: true,
        items,
      },
      global: {
        stubs: {
          ElAlert,
          ElButton,
          ElDatePicker,
          ElDialog,
          ElInput,
          ElOption,
          ElSelect,
        },
      },
    })

    await wrapper.get('[data-testid="drive-batch-share-submit"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="drive-batch-share-error"]').text()).toContain('Batch share exploded')

    await wrapper.get('[data-testid="drive-batch-share-error-retry"]').trigger('click')
    await flushPromises()
    expect(batchCreateSharesMock).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-testid="drive-batch-share-result"]').exists()).toBe(true)
  })
})
