import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import {
  baseItems,
  confirmMock,
  docsApiMock,
  driveApiMock,
  driveFileE2eeMock,
  messageErrorMock,
  messageSuccessMock,
  messageWarningMock,
  navigateToMock,
  promptMock,
  resetDriveApiMocks,
  setupDrivePageGlobals,
  sheetsApiMock,
  suiteApiMock,
} from './support/drive-page-fixtures'
import { attachFile, mountDrivePage } from './support/drive-page-testkit'

vi.mock('element-plus', () => ({
  ElMessage: {
    error: messageErrorMock,
    success: messageSuccessMock,
    warning: messageWarningMock,
  },
  ElMessageBox: {
    confirm: confirmMock,
    prompt: promptMock,
  },
}))

vi.mock('~/composables/useDriveApi', () => ({
  useDriveApi: () => driveApiMock,
}))

vi.mock('~/composables/useDocsApi', () => ({
  useDocsApi: () => docsApiMock,
}))

vi.mock('~/composables/useDriveFileE2ee', () => ({
  useDriveFileE2ee: () => driveFileE2eeMock,
}))

vi.mock('~/composables/useSheetsApi', () => ({
  useSheetsApi: () => sheetsApiMock,
}))

vi.mock('~/composables/useSuiteApi', () => ({
  useSuiteApi: () => suiteApiMock,
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

beforeEach(() => {
  vi.clearAllMocks()
  resetDriveApiMocks()
  setupDrivePageGlobals()
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('drive smoke', () => {
  it('shows visible recovery for workspace load, upload failure and download failure', async () => {
    driveApiMock.listItems
      .mockRejectedValueOnce(new Error('Workspace offline'))
      .mockResolvedValue(baseItems)
    driveApiMock.uploadFile
      .mockRejectedValueOnce(new Error('Upload failed once'))
      .mockResolvedValue(baseItems[1])
    driveApiMock.downloadFile
      .mockRejectedValueOnce(new Error('Download failed once'))
      .mockResolvedValue({
        blob: new Blob(['download']),
        fileName: 'release-note.txt',
      })

    const wrapper = await mountDrivePage()
    await flushPromises()

    expect(wrapper.get('[data-testid="drive-workspace-error"]').text()).toContain('Workspace offline')
    await wrapper.get('[data-testid="drive-workspace-error-retry"]').trigger('click')
    await flushPromises()
    expect(driveApiMock.listItems).toHaveBeenCalledTimes(2)

    await attachFile(wrapper, '[data-testid="drive-upload-input"]', 'retry.txt', 'retry upload')
    await wrapper.get('[data-testid="drive-upload-submit"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="drive-operation-error"]').text()).toContain('Upload failed once')

    await wrapper.get('[data-testid="drive-operation-error-retry"]').trigger('click')
    await flushPromises()
    expect(driveApiMock.uploadFile).toHaveBeenCalledTimes(2)

    await wrapper.get('[data-testid="drive-action-download-file-1"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="drive-operation-error"]').text()).toContain('Download failed once')

    await wrapper.get('[data-testid="drive-operation-error-retry"]').trigger('click')
    await flushPromises()
    expect(driveApiMock.downloadFile).toHaveBeenCalledTimes(2)
  })

  it('covers folder, batch share, rename, move, versions, delete and share flows', async () => {
    const wrapper = await mountDrivePage()
    await flushPromises()

    expect(wrapper.get('[data-testid="drive-usage-storage"]').text()).toContain('4.00 KB')

    await wrapper.get('[data-testid="drive-folder-name"]').setValue('Ops')
    await wrapper.get('[data-testid="drive-folder-create"]').trigger('click')
    await flushPromises()
    expect(driveApiMock.createFolder).toHaveBeenCalledWith({
      name: 'Ops',
      parentId: null,
    })

    await wrapper.get('[data-testid="drive-action-open-folder-1"]').trigger('click')
    await flushPromises()
    expect(driveApiMock.listItems).toHaveBeenLastCalledWith(expect.objectContaining({
      parentId: 'folder-1',
    }))

    await wrapper.get('[data-testid="drive-search-input"]').setValue('release')
    await wrapper.get('[data-testid="drive-search-submit"]').trigger('click')
    await flushPromises()
    expect(driveApiMock.listItems).toHaveBeenLastCalledWith(expect.objectContaining({
      keyword: 'release',
    }))

    await wrapper.get('[data-testid="table-select-folder-1"]').setValue(true)
    await wrapper.get('[data-testid="table-select-file-1"]').setValue(true)
    await wrapper.get('[data-testid="drive-batch-share"]').trigger('click')
    expect(wrapper.get('[data-testid="drive-batch-share-dialog-stub"]').text()).toContain('2')

    await wrapper.get('[data-testid="drive-action-rename-file-1"]').trigger('click')
    await wrapper.get('[data-testid="drive-rename-input"]').setValue('renamed.txt')
    await wrapper.get('[data-testid="drive-rename-submit"]').trigger('click')
    await flushPromises()
    expect(driveApiMock.renameItem).toHaveBeenCalledWith('file-1', 'renamed.txt')

    await wrapper.get('[data-testid="drive-action-move-file-1"]').trigger('click')
    await wrapper.get('[data-testid="drive-move-select"]').setValue('folder-1')
    await wrapper.get('[data-testid="drive-move-submit"]').trigger('click')
    await flushPromises()
    expect(driveApiMock.moveItem).toHaveBeenCalledWith('file-1', { parentId: 'folder-1' })

    await wrapper.get('[data-testid="drive-action-versions-file-1"]').trigger('click')
    await flushPromises()
    expect(driveApiMock.listFileVersions).toHaveBeenCalledWith('file-1', 100)

    await attachFile(wrapper, '[data-testid="drive-version-upload-input"]', 'v2.txt', 'version 2')
    await wrapper.get('[data-testid="drive-version-upload-submit"]').trigger('click')
    await flushPromises()
    expect(driveApiMock.uploadFileVersion).toHaveBeenCalledWith('file-1', expect.any(File))

    await wrapper.get('[data-testid="drive-version-restore-version-1"]').trigger('click')
    await flushPromises()
    expect(driveApiMock.restoreFileVersion).toHaveBeenCalledWith('file-1', 'version-1')

    await wrapper.get('[data-testid="drive-action-shares-file-1"]').trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="drive-share-drawer-stub"]').text()).toContain('file-1')

    await wrapper.get('[data-testid="drive-action-delete-file-1"]').trigger('click')
    await flushPromises()
    expect(driveApiMock.deleteItem).toHaveBeenCalledWith('file-1')

    expect(navigateToMock).not.toHaveBeenCalled()
  })
})
