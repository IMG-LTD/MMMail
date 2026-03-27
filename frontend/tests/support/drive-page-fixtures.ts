import { vi } from 'vitest'
import type {
  DriveBatchActionResult,
  DriveCollaboratorSharedItem,
  DriveFileVersion,
  DriveIncomingCollaboratorShare,
  DriveItem,
  DriveSavedShare,
  DriveShareAccessLog,
  DriveUsage,
  SuiteCollaborationEvent,
} from '~/types/api'

export const navigateToMock = vi.fn(async () => undefined)
export const confirmMock = vi.fn(async () => undefined)
export const messageErrorMock = vi.fn()
export const messageSuccessMock = vi.fn()
export const messageWarningMock = vi.fn()

export const routeState = {
  query: {} as Record<string, unknown>,
}

export const baseItems: DriveItem[] = [
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

export const driveUsage: DriveUsage = {
  fileCount: 1,
  folderCount: 1,
  storageBytes: 4096,
  storageLimitBytes: 10240,
}

export const versionItems: DriveFileVersion[] = [
  {
    id: 'version-1',
    itemId: 'file-1',
    versionNo: 1,
    mimeType: 'text/plain',
    sizeBytes: 4096,
    checksum: 'sha256:version-1',
    createdAt: '2026-03-13T09:06:00',
  },
]

export const collaboratorShares: DriveCollaboratorSharedItem[] = []
export const incomingShares: DriveIncomingCollaboratorShare[] = []
export const savedShares: DriveSavedShare[] = []
export const accessLogs: DriveShareAccessLog[] = []
export const collaborationItems: SuiteCollaborationEvent[] = []
export const batchDeleteResult: DriveBatchActionResult = {
  requestedCount: 2,
  successCount: 2,
  failedCount: 0,
  failedItems: [],
}

export const driveApiMock = {
  listItems: vi.fn(),
  createFolder: vi.fn(),
  uploadFile: vi.fn(),
  renameItem: vi.fn(),
  moveItem: vi.fn(),
  deleteItem: vi.fn(),
  batchDeleteItems: vi.fn(),
  listTrashItems: vi.fn(),
  batchRestoreTrashItems: vi.fn(),
  restoreTrashItem: vi.fn(),
  batchPurgeTrashItems: vi.fn(),
  purgeTrashItem: vi.fn(),
  previewFile: vi.fn(),
  listFileVersions: vi.fn(),
  uploadFileVersion: vi.fn(),
  restoreFileVersion: vi.fn(),
  cleanupFileVersions: vi.fn(),
  getUsage: vi.fn(),
  downloadFile: vi.fn(),
  listShareAccessLogs: vi.fn(),
  listSharedWithMe: vi.fn(),
  listIncomingCollaboratorShares: vi.fn(),
  respondCollaboratorShare: vi.fn(),
  listCollaboratorSharedWithMe: vi.fn(),
  listCollaboratorSharedItems: vi.fn(),
  createCollaboratorFolder: vi.fn(),
  uploadCollaboratorFile: vi.fn(),
  downloadCollaboratorFile: vi.fn(),
  previewCollaboratorFile: vi.fn(),
  removeSharedWithMe: vi.fn(),
}

export const docsApiMock = {
  createNote: vi.fn(async () => ({ id: 'note-1' })),
}

export const sheetsApiMock = {
  createWorkbook: vi.fn(async () => ({ id: 'workbook-1' })),
}

export const suiteApiMock = {
  getCollaborationCenter: vi.fn(),
}

export function resetDriveApiMocks(): void {
  driveApiMock.listItems.mockResolvedValue(baseItems)
  driveApiMock.createFolder.mockResolvedValue(baseItems[0])
  driveApiMock.uploadFile.mockResolvedValue(baseItems[1])
  driveApiMock.renameItem.mockResolvedValue({ ...baseItems[1], name: 'renamed.txt' })
  driveApiMock.moveItem.mockResolvedValue({ ...baseItems[1], parentId: 'folder-1' })
  driveApiMock.deleteItem.mockResolvedValue(undefined)
  driveApiMock.batchDeleteItems.mockResolvedValue(batchDeleteResult)
  driveApiMock.listTrashItems.mockResolvedValue([])
  driveApiMock.batchRestoreTrashItems.mockResolvedValue(batchDeleteResult)
  driveApiMock.restoreTrashItem.mockResolvedValue(undefined)
  driveApiMock.batchPurgeTrashItems.mockResolvedValue(batchDeleteResult)
  driveApiMock.purgeTrashItem.mockResolvedValue(undefined)
  driveApiMock.previewFile.mockResolvedValue({
    blob: new Blob(['preview']),
    fileName: 'release-note.txt',
    mimeType: 'text/plain',
    truncated: false,
  })
  driveApiMock.listFileVersions.mockResolvedValue(versionItems)
  driveApiMock.uploadFileVersion.mockResolvedValue(baseItems[1])
  driveApiMock.restoreFileVersion.mockResolvedValue(baseItems[1])
  driveApiMock.cleanupFileVersions.mockResolvedValue({
    deletedVersions: 1,
    remainingVersions: 1,
    appliedRetentionCount: 5,
    appliedRetentionDays: 30,
  })
  driveApiMock.getUsage.mockResolvedValue(driveUsage)
  driveApiMock.downloadFile.mockResolvedValue({
    blob: new Blob(['download']),
    fileName: 'release-note.txt',
  })
  driveApiMock.listShareAccessLogs.mockResolvedValue(accessLogs)
  driveApiMock.listSharedWithMe.mockResolvedValue(savedShares)
  driveApiMock.listIncomingCollaboratorShares.mockResolvedValue(incomingShares)
  driveApiMock.respondCollaboratorShare.mockResolvedValue(undefined)
  driveApiMock.listCollaboratorSharedWithMe.mockResolvedValue(collaboratorShares)
  driveApiMock.listCollaboratorSharedItems.mockResolvedValue([])
  driveApiMock.createCollaboratorFolder.mockResolvedValue(baseItems[0])
  driveApiMock.uploadCollaboratorFile.mockResolvedValue(baseItems[1])
  driveApiMock.downloadCollaboratorFile.mockResolvedValue({
    blob: new Blob(['download']),
    fileName: 'shared-file.txt',
  })
  driveApiMock.previewCollaboratorFile.mockResolvedValue({
    blob: new Blob(['preview']),
    fileName: 'shared-file.txt',
    mimeType: 'text/plain',
    truncated: false,
  })
  driveApiMock.removeSharedWithMe.mockResolvedValue(undefined)
  suiteApiMock.getCollaborationCenter.mockResolvedValue(collaborationItems)
}

export function setupDrivePageGlobals(): void {
  routeState.query = {}
  ;(globalThis as Record<string, unknown>).useRoute = () => routeState
  ;(globalThis as Record<string, unknown>).navigateTo = navigateToMock
  ;(globalThis as Record<string, unknown>).definePageMeta = () => undefined
  const urlApi = globalThis.URL as typeof URL & {
    createObjectURL?: (object: Blob | MediaSource) => string
    revokeObjectURL?: (url: string) => void
  }
  urlApi.createObjectURL = vi.fn(() => 'blob:drive')
  urlApi.revokeObjectURL = vi.fn()
  vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined)
}
