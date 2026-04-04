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
  promptMock,
  resetDriveApiMocks,
  setupDrivePageGlobals,
  sheetsApiMock,
  suiteApiMock,
  versionItems,
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

const encryptedItem = {
  ...baseItems[1],
  e2ee: {
    enabled: true,
    algorithm: 'openpgp',
    recipientFingerprints: ['DRIVE-SELF-FP'],
  },
}

beforeEach(() => {
  vi.clearAllMocks()
  resetDriveApiMocks()
  setupDrivePageGlobals()
  driveApiMock.listItems.mockResolvedValue([baseItems[0], encryptedItem])
  driveApiMock.uploadFile.mockResolvedValue(encryptedItem)
  driveApiMock.uploadFileVersion.mockResolvedValue(encryptedItem)
  driveApiMock.listFileVersions.mockResolvedValue([
    {
      ...versionItems[0],
      e2ee: encryptedItem.e2ee,
    },
  ])
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('drive e2ee', () => {
  it('encrypts owner uploads and new versions before calling drive api', async () => {
    const encryptedUpload = {
      file: new File(['ciphertext'], 'release-note.txt.pgp', { type: 'application/octet-stream' }),
      e2ee: {
        enabled: true as const,
        algorithm: 'openpgp',
        recipientFingerprints: ['DRIVE-SELF-FP'],
        fileName: 'release-note.txt',
        contentType: 'text/plain',
        fileSize: 12,
      },
    }
    driveFileE2eeMock.isDriveFileEncryptionEnabled.mockResolvedValue(true)
    driveFileE2eeMock.encryptOwnedFile.mockResolvedValue(encryptedUpload)

    const wrapper = await mountDrivePage()
    await flushPromises()

    await attachFile(wrapper, '[data-testid="drive-upload-input"]', 'release-note.txt', 'hello world!')
    await wrapper.get('[data-testid="drive-upload-submit"]').trigger('click')
    await flushPromises()

    expect(driveFileE2eeMock.encryptOwnedFile).toHaveBeenCalledWith(expect.any(File))
    expect(driveApiMock.uploadFile).toHaveBeenCalledWith(
      encryptedUpload.file,
      null,
      encryptedUpload.e2ee,
    )

    await wrapper.get('[data-testid="drive-action-versions-file-1"]').trigger('click')
    await flushPromises()
    await attachFile(wrapper, '[data-testid="drive-version-upload-input"]', 'release-note.txt', 'version 2!')
    await wrapper.get('[data-testid="drive-version-upload-submit"]').trigger('click')
    await flushPromises()

    expect(driveApiMock.uploadFileVersion).toHaveBeenCalledWith(
      'file-1',
      encryptedUpload.file,
      encryptedUpload.e2ee,
    )
  })

  it('decrypts encrypted owner files locally and keeps batch-share blocked for e2ee items', async () => {
    driveApiMock.downloadFile.mockResolvedValue({
      blob: new Blob(['ciphertext']),
      fileName: 'release-note.txt.pgp',
    })
    driveFileE2eeMock.decryptOwnedFile.mockResolvedValue({
      blob: new Blob(['decrypted preview'], { type: 'text/plain' }),
      fileName: 'release-note.txt',
    })
    promptMock.mockResolvedValue({ value: 'drive-passphrase' })

    const wrapper = await mountDrivePage()
    await flushPromises()

    expect(wrapper.text()).toContain('drive.table.badges.e2ee')

    await wrapper.get('[data-testid="drive-action-preview-file-1"]').trigger('click')
    await flushPromises()
    expect(driveApiMock.previewFile).not.toHaveBeenCalled()
    expect(driveApiMock.downloadFile).toHaveBeenCalledWith('file-1')
    expect(driveFileE2eeMock.decryptOwnedFile).toHaveBeenCalledWith(
      expect.objectContaining({ fileName: 'release-note.txt.pgp' }),
      encryptedItem,
      'drive-passphrase',
    )
    expect(wrapper.text()).toContain('decrypted preview')

    await wrapper.get('[data-testid="drive-action-download-file-1"]').trigger('click')
    await flushPromises()
    expect(driveApiMock.downloadFile).toHaveBeenCalledTimes(2)

    await wrapper.get('[data-testid="drive-action-shares-file-1"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="drive-share-drawer-stub"]').exists()).toBe(true)

    await wrapper.get('[data-testid="table-select-file-1"]').setValue(true)
    await wrapper.get('[data-testid="drive-batch-share"]').trigger('click')
    await flushPromises()
    expect(messageWarningMock).toHaveBeenCalledWith('drive.messages.e2eeShareUnavailable')
    expect(wrapper.find('[data-testid="drive-batch-share-dialog-stub"]').exists()).toBe(false)
  })
})
