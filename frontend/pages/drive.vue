<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
import DriveBatchShareDialog from '~/components/drive/DriveBatchShareDialog.vue'
import DriveCollaborationLaunchpad from '~/components/drive/DriveCollaborationLaunchpad.vue'
import DriveCollaboratorSharedPanel from '~/components/drive/DriveCollaboratorSharedPanel.vue'
import DriveErrorBanner from '~/components/drive/DriveErrorBanner.vue'
import DriveIncomingSharesPanel from '~/components/drive/DriveIncomingSharesPanel.vue'
import DriveShareLinksDrawer from '~/components/drive/DriveShareLinksDrawer.vue'
import DriveSharedWithMePanel from '~/components/drive/DriveSharedWithMePanel.vue'
import type {
  DriveBatchActionResult,
  DriveCollaboratorSharedItem,
  DriveFileVersion,
  DriveIncomingCollaboratorShare,
  DriveItem,
  DriveItemType,
  DriveSavedShare,
  DrivePreviewKind,
  DriveShareAccessLog,
  DriveTrashItem,
  DriveUsage,
  DriveVersionCleanupResult,
  SuiteCollaborationEvent
} from '~/types/api'
import { useDocsApi } from '~/composables/useDocsApi'
import { useDriveApi } from '~/composables/useDriveApi'
import { useI18n } from '~/composables/useI18n'
import { useSheetsApi } from '~/composables/useSheetsApi'
import { useSuiteApi } from '~/composables/useSuiteApi'
import {
  DRIVE_SHARE_ACCESS_ACTION_OPTIONS,
  DRIVE_SHARE_ACCESS_STATUS_OPTIONS,
  getDriveShareAccessActionI18nKey,
  getDriveShareAccessStatusI18nKey
} from '~/utils/drive-share'
import { countPendingDriveIncomingShares } from '~/utils/drive-collaboration'

const route = useRoute()
const { t } = useI18n()

const loading = ref(false)
const mutating = ref(false)
const batchMutating = ref(false)
const uploading = ref(false)
const downloadingItemId = ref('')
const batchShareDialogVisible = ref(false)
const itemTableRef = ref<TableInstance>()
const trashTableRef = ref<TableInstance>()

const items = ref<DriveItem[]>([])
const selectedItemIds = ref<string[]>([])
const usage = ref<DriveUsage | null>(null)
const currentParentId = ref<string | null>(null)
const keyword = ref('')
const itemTypeFilter = ref<DriveItemType | ''>('')

const trail = ref<Array<{ id: string | null; name: string }>>([{ id: null, name: t('drive.root') }])
const knownFolders = ref<Record<string, string>>({})

const createFolderName = ref('')
const uploadInputRef = ref<HTMLInputElement | null>(null)
const uploadFileRef = ref<File | null>(null)
const uploadFileName = ref('')
const dragOver = ref(false)

const renameDialogVisible = ref(false)
const renameForm = reactive({
  itemId: '',
  name: ''
})

const moveDialogVisible = ref(false)
const moveForm = reactive({
  itemId: '',
  parentId: ''
})

const previewDrawerVisible = ref(false)
const previewLoading = ref(false)
const previewTarget = ref<DriveItem | null>(null)
const previewKind = ref<DrivePreviewKind>('UNSUPPORTED')
const previewText = ref('')
const previewBlobUrl = ref('')
const previewMimeType = ref('')
const previewTruncated = ref(false)

const versionDrawerVisible = ref(false)
const versionLoading = ref(false)
const versionUploadLoading = ref(false)
const versionCleanupLoading = ref(false)
const versionMutatingId = ref('')
const activeVersionItem = ref<DriveItem | null>(null)
const versionItems = ref<DriveFileVersion[]>([])
const lastVersionCleanupResult = ref<DriveVersionCleanupResult | null>(null)
const versionUploadInputRef = ref<HTMLInputElement | null>(null)
const versionUploadFileRef = ref<File | null>(null)
const versionUploadFileName = ref('')

const shareDrawerVisible = ref(false)
const activeShareItem = ref<DriveItem | null>(null)

const trashDrawerVisible = ref(false)
const trashLoading = ref(false)
const trashMutatingId = ref('')
const trashItems = ref<DriveTrashItem[]>([])
const selectedTrashIds = ref<string[]>([])

const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuItem = ref<DriveItem | null>(null)

const accessLogDrawerVisible = ref(false)
const workspaceView = ref<'MY_FILES' | 'SHARED_WITH_ME'>(route.query.view === 'shared-with-me' ? 'SHARED_WITH_ME' : 'MY_FILES')
const collaboratorIncomingLoading = ref(false)
const collaboratorIncomingItems = ref<DriveIncomingCollaboratorShare[]>([])
const collaboratorSharedLoading = ref(false)
const collaboratorSharedItems = ref<DriveCollaboratorSharedItem[]>([])
const collaboratorMutationId = ref('')
const collaboratorWorkspaceLoading = ref(false)
const collaboratorWorkspaceMutating = ref(false)
const collaboratorUploading = ref(false)
const collaboratorDownloadingItemId = ref('')
const activeCollaboratorShare = ref<DriveCollaboratorSharedItem | null>(null)
const collaboratorWorkspaceItems = ref<DriveItem[]>([])
const collaboratorParentId = ref<string | null>(null)
const collaboratorTrail = ref<Array<{ id: string | null; name: string }>>([])
const collaboratorFolderName = ref('')
const collaboratorUploadInputRef = ref<HTMLInputElement | null>(null)
const collaboratorUploadFileRef = ref<File | null>(null)
const collaboratorUploadFileName = ref('')
const sharedWithMeLoading = ref(false)
const sharedWithMeRemovingId = ref('')
const sharedWithMeItems = ref<DriveSavedShare[]>([])
const accessLogLoading = ref(false)
const accessLogs = ref<DriveShareAccessLog[]>([])
const accessLogFilters = reactive({
  action: '',
  accessStatus: '',
  limit: 50
})

const creatingDoc = ref(false)
const creatingSheet = ref(false)
const collaborationLoading = ref(false)
const collaborationItems = ref<SuiteCollaborationEvent[]>([])

const {
  listItems,
  createFolder,
  uploadFile,
  renameItem,
  moveItem,
  deleteItem,
  batchDeleteItems,
  listTrashItems,
  batchRestoreTrashItems,
  restoreTrashItem,
  batchPurgeTrashItems,
  purgeTrashItem,
  previewFile,
  listFileVersions,
  uploadFileVersion,
  restoreFileVersion,
  cleanupFileVersions,
  getUsage,
  downloadFile,
  listShareAccessLogs,
  listSharedWithMe,
  listIncomingCollaboratorShares,
  respondCollaboratorShare,
  listCollaboratorSharedWithMe,
  listCollaboratorSharedItems,
  createCollaboratorFolder,
  uploadCollaboratorFile,
  downloadCollaboratorFile,
  previewCollaboratorFile,
  removeSharedWithMe
} = useDriveApi()

const { createNote } = useDocsApi()
const { createWorkbook } = useSheetsApi()
const { getCollaborationCenter } = useSuiteApi()

const currentFolderLabel = computed(() => trail.value[trail.value.length - 1]?.name || t('drive.root'))
const currentWorkspaceDescription = computed(() => {
  if (workspaceView.value === 'SHARED_WITH_ME') {
    return activeCollaboratorShare.value
      ? t('drive.collaboration.workspace.subtitle', { name: activeCollaboratorShare.value.itemName })
      : t('drive.sharedWithMe.subtitle')
  }
  return t('drive.currentFolder', { name: currentFolderLabel.value })
})
const driveViewOptions = computed(() => ([
  { label: t('drive.views.myFiles'), value: 'MY_FILES' },
  { label: t('drive.views.sharedWithMe'), value: 'SHARED_WITH_ME' }
]))
const folderItems = computed(() => items.value.filter(item => item.itemType === 'FOLDER'))
const fileItems = computed(() => items.value.filter(item => item.itemType === 'FILE'))
const storagePercent = computed(() => {
  if (!usage.value || usage.value.storageLimitBytes <= 0) {
    return 0
  }
  return Math.min(100, Math.round((usage.value.storageBytes / usage.value.storageLimitBytes) * 100))
})
const previewTitle = computed(() => previewTarget.value?.name || t('drive.preview.title'))
const hasSelectedItems = computed(() => selectedItemIds.value.length > 0)
const hasSelectedTrashItems = computed(() => selectedTrashIds.value.length > 0)
const pendingCollaboratorIncomingCount = computed(() => countPendingDriveIncomingShares(collaboratorIncomingItems.value))
const activeCollaboratorWorkspaceLabel = computed(() => collaboratorTrail.value[collaboratorTrail.value.length - 1]?.name || activeCollaboratorShare.value?.itemName || t('drive.root'))
const canEditCollaboratorWorkspace = computed(() => activeCollaboratorShare.value?.permission === 'EDIT' && Boolean(activeCollaboratorShare.value?.available))
const currentViewSummary = computed(() => t('drive.usage.currentView', {
  items: items.value.length,
  folders: folderItems.value.length,
  files: fileItems.value.length
}))

const accessLogActionOptions = computed(() => DRIVE_SHARE_ACCESS_ACTION_OPTIONS.map((action) => ({
  value: action,
  label: t(getDriveShareAccessActionI18nKey(action))
})))
const accessLogStatusOptions = computed(() => DRIVE_SHARE_ACCESS_STATUS_OPTIONS.map((status) => ({
  value: status,
  label: t(getDriveShareAccessStatusI18nKey(status))
})))
const selectedItems = computed(() => items.value.filter((item) => selectedItemIds.value.includes(item.id)))

interface DriveAlertState {
  title: string
  message: string
  retry: () => Promise<void>
}

const workspaceAlert = ref<DriveAlertState | null>(null)
const operationAlert = ref<DriveAlertState | null>(null)

const moveTargetOptions = computed(() => {
  const options: Array<{ label: string; value: string }> = [{ label: t('drive.root'), value: '' }]
  const dedupe = new Set<string>([''])

  for (const crumb of trail.value) {
    if (crumb.id && !dedupe.has(crumb.id)) {
      options.push({ label: t('drive.moveTargets.path', { name: crumb.name }), value: crumb.id })
      dedupe.add(crumb.id)
    }
  }

  for (const item of items.value) {
    if (item.itemType === 'FOLDER' && !dedupe.has(item.id)) {
      options.push({ label: t('drive.moveTargets.current', { name: item.name }), value: item.id })
      dedupe.add(item.id)
    }
  }

  for (const [id, name] of Object.entries(knownFolders.value)) {
    if (!dedupe.has(id)) {
      options.push({ label: t('drive.moveTargets.known', { name }), value: id })
      dedupe.add(id)
    }
  }

  return options
})

function getItemTypeLabel(itemType: DriveItemType): string {
  return itemType === 'FOLDER' ? t('drive.search.types.folder') : t('drive.search.types.file')
}

function getAccessLogActionLabel(action: string): string {
  return t(getDriveShareAccessActionI18nKey(action))
}

function getAccessLogStatusLabel(status: string): string {
  return t(getDriveShareAccessStatusI18nKey(status))
}

function getAccessLogStatusTagType(status: string): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'ALLOW') {
    return 'success'
  }
  if (['DENY_INVALID_TOKEN', 'DENY_REVOKED', 'DENY_FILE_MISSING', 'DENY_PASSWORD_INVALID'].includes(status)) {
    return 'danger'
  }
  if (['DENY_RATE_LIMIT', 'DENY_EXPIRED', 'DENY_PASSWORD_REQUIRED'].includes(status)) {
    return 'warning'
  }
  return 'info'
}

function resolveErrorMessage(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}

function setWorkspaceAlert(error: unknown, fallback: string, retry: () => Promise<void>): void {
  workspaceAlert.value = {
    title: t('drive.errors.workspaceTitle'),
    message: resolveErrorMessage(error, fallback),
    retry
  }
}

function setOperationAlert(error: unknown, fallback: string, retry: () => Promise<void>): void {
  operationAlert.value = {
    title: t('drive.errors.operationTitle'),
    message: resolveErrorMessage(error, fallback),
    retry
  }
}

async function retryWorkspaceAlert(): Promise<void> {
  if (!workspaceAlert.value) {
    return
  }
  const retry = workspaceAlert.value.retry
  workspaceAlert.value = null
  await retry()
}

async function retryOperationAlert(): Promise<void> {
  if (!operationAlert.value) {
    return
  }
  const retry = operationAlert.value.retry
  operationAlert.value = null
  await retry()
}

function registerFolders(rows: DriveItem[]): void {
  const next = { ...knownFolders.value }
  for (const row of rows) {
    if (row.itemType === 'FOLDER') {
      next[row.id] = row.name
    }
  }
  knownFolders.value = next
}

function onItemSelectionChange(rows: DriveItem[]): void {
  selectedItemIds.value = rows.map(row => row.id)
}

function onTrashSelectionChange(rows: DriveTrashItem[]): void {
  selectedTrashIds.value = rows.map(row => row.id)
}

function clearItemSelection(): void {
  selectedItemIds.value = []
  itemTableRef.value?.clearSelection()
}

function clearTrashSelection(): void {
  selectedTrashIds.value = []
  trashTableRef.value?.clearSelection()
}

function summarizeBatchResult(action: string, result: DriveBatchActionResult): void {
  if (result.failedCount === 0) {
    ElMessage.success(t('drive.messages.batchCompleted', {
      action,
      success: result.successCount,
      requested: result.requestedCount
    }))
    return
  }
  const firstFailure = result.failedItems[0]
  const details = firstFailure
    ? t('drive.messages.firstFailure', { itemId: firstFailure.itemId, reason: firstFailure.reason })
    : ''
  ElMessage.warning(t('drive.messages.batchPartial', {
    action,
    success: result.successCount,
    failed: result.failedCount,
    details
  }))
}

async function onBatchDeleteSelected(): Promise<void> {
  if (!hasSelectedItems.value) {
    ElMessage.warning(t('drive.messages.selectAtLeastOneItem'))
    return
  }
  try {
    await ElMessageBox.confirm(
      t('drive.messages.batchDeleteConfirm', { count: selectedItemIds.value.length }),
      t('drive.messages.batchDeleteTitle'),
      {
        type: 'warning',
        confirmButtonText: t('common.actions.delete'),
        cancelButtonText: t('common.actions.cancel')
      }
    )
  } catch {
    return
  }

  batchMutating.value = true
  try {
    const result = await batchDeleteItems(selectedItemIds.value)
    summarizeBatchResult(t('drive.batch.deleteAction'), result)
    clearItemSelection()
    closeContextMenu()
    await Promise.all([loadWorkspace(), trashDrawerVisible.value ? loadTrashItemsData() : Promise.resolve()])
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.batchDeleteFailed'))
  } finally {
    batchMutating.value = false
  }
}

async function onBatchRestoreTrash(): Promise<void> {
  if (!hasSelectedTrashItems.value) {
    ElMessage.warning(t('drive.messages.selectAtLeastOneTrashItem'))
    return
  }
  batchMutating.value = true
  try {
    const result = await batchRestoreTrashItems(selectedTrashIds.value)
    summarizeBatchResult(t('drive.trash.batchRestoreAction'), result)
    clearTrashSelection()
    await Promise.all([loadTrashItemsData(), loadWorkspace()])
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.batchRestoreFailed'))
  } finally {
    batchMutating.value = false
  }
}

async function onBatchPurgeTrash(): Promise<void> {
  if (!hasSelectedTrashItems.value) {
    ElMessage.warning(t('drive.messages.selectAtLeastOneTrashItem'))
    return
  }
  try {
    await ElMessageBox.confirm(
      t('drive.messages.batchPurgeConfirm', { count: selectedTrashIds.value.length }),
      t('drive.messages.batchPurgeTitle'),
      {
        type: 'warning',
        confirmButtonText: t('drive.trash.purge'),
        cancelButtonText: t('common.actions.cancel')
      }
    )
  } catch {
    return
  }
  batchMutating.value = true
  try {
    const result = await batchPurgeTrashItems(selectedTrashIds.value)
    summarizeBatchResult(t('drive.trash.batchPurgeAction'), result)
    clearTrashSelection()
    await Promise.all([loadTrashItemsData(), loadWorkspace()])
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.batchPurgeFailed'))
  } finally {
    batchMutating.value = false
  }
}

function closeContextMenu(): void {
  contextMenuVisible.value = false
  contextMenuItem.value = null
}

function onRowContextMenu(event: MouseEvent, item: DriveItem): void {
  event.preventDefault()
  const menuWidth = 220
  const menuHeight = 280
  const maxX = typeof window === 'undefined' ? event.clientX : window.innerWidth - menuWidth
  const maxY = typeof window === 'undefined' ? event.clientY : window.innerHeight - menuHeight
  contextMenuX.value = Math.max(12, Math.min(event.clientX, maxX))
  contextMenuY.value = Math.max(12, Math.min(event.clientY, maxY))
  contextMenuItem.value = item
  contextMenuVisible.value = true
}

function onContextMenuAction(action: string): void {
  const item = contextMenuItem.value
  closeContextMenu()
  if (!item) {
    return
  }
  if (action === 'open') {
    void onOpenFolder(item)
    return
  }
  if (action === 'download') {
    void onDownload(item)
    return
  }
  if (action === 'preview') {
    void onPreview(item)
    return
  }
  if (action === 'versions') {
    void openVersions(item)
    return
  }
  if (action === 'shares') {
    void openShares(item)
    return
  }
  if (action === 'rename') {
    openRename(item)
    return
  }
  if (action === 'move') {
    openMove(item)
    return
  }
  if (action === 'delete') {
    void onDelete(item)
  }
}

function isEditableTarget(target: EventTarget | null): boolean {
  const element = target as HTMLElement | null
  if (!element) {
    return false
  }
  const tagName = element.tagName.toLowerCase()
  if (tagName === 'input' || tagName === 'textarea' || tagName === 'select') {
    return true
  }
  if (element.isContentEditable) {
    return true
  }
  return Boolean(element.closest('.el-input') || element.closest('.el-textarea') || element.closest('[contenteditable="true"]'))
}

function selectAllVisibleItems(): void {
  if (!items.value.length) {
    return
  }
  itemTableRef.value?.clearSelection()
  for (const row of items.value) {
    itemTableRef.value?.toggleRowSelection(row, true)
  }
  selectedItemIds.value = items.value.map(row => row.id)
}

function onDriveKeydown(event: KeyboardEvent): void {
  if (isEditableTarget(event.target)) {
    return
  }
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'a') {
    event.preventDefault()
    selectAllVisibleItems()
    return
  }
  if (event.key === 'Delete' && hasSelectedItems.value && !batchMutating.value) {
    event.preventDefault()
    void onBatchDeleteSelected()
    return
  }
  if (event.key === 'Escape') {
    if (contextMenuVisible.value) {
      closeContextMenu()
    }
    if (hasSelectedItems.value) {
      clearItemSelection()
    }
  }
}

function formatBytes(bytes: number): string {
  const value = Math.max(0, bytes)
  if (value < 1024) {
    return `${value} B`
  }
  const units = ['KB', 'MB', 'GB', 'TB']
  let scaled = value / 1024
  let index = 0
  while (scaled >= 1024 && index < units.length - 1) {
    scaled /= 1024
    index += 1
  }
  return `${scaled.toFixed(scaled >= 10 ? 1 : 2)} ${units[index]}`
}

function formatTime(value: string | null): string {
  return value || '-'
}

function buildCollaborativeTitle(kind: 'docs' | 'sheets'): string {
  const prefix = kind === 'docs' ? t('drive.launcher.defaultDocTitle') : t('drive.launcher.defaultSheetTitle')
  return `${prefix} ${new Date().toLocaleString()}`
}

async function loadCollaborationLaunchpad(): Promise<void> {
  collaborationLoading.value = true
  try {
    const center = await getCollaborationCenter(16)
    collaborationItems.value = center.items.filter((item) => ['DOCS', 'DRIVE', 'SHEETS'].includes(item.productCode))
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.loadCollaborationFailed'))
  } finally {
    collaborationLoading.value = false
  }
}

async function onRefreshDashboard(): Promise<void> {
  await Promise.all([
    loadWorkspace(),
    loadSharedWithMe(),
    loadCollaboratorIncomingShares(),
    loadCollaboratorSharedEntries(),
    loadCollaborationLaunchpad()
  ])
  await refreshCollaboratorWorkspaceIfNeeded()
}

async function onCreateDoc(): Promise<void> {
  creatingDoc.value = true
  try {
    const note = await createNote({ title: buildCollaborativeTitle('docs'), content: '' })
    ElMessage.success(t('drive.messages.docCreated'))
    await loadCollaborationLaunchpad()
    await navigateTo(`/docs?noteId=${note.id}`)
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.docCreateFailed'))
  } finally {
    creatingDoc.value = false
  }
}

async function onCreateSheet(): Promise<void> {
  creatingSheet.value = true
  try {
    const workbook = await createWorkbook({ title: buildCollaborativeTitle('sheets') })
    ElMessage.success(t('drive.messages.sheetCreated'))
    await loadCollaborationLaunchpad()
    await navigateTo(`/sheets?workbookId=${workbook.id}`)
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.sheetCreateFailed'))
  } finally {
    creatingSheet.value = false
  }
}

async function onOpenLaunchpadItem(item: SuiteCollaborationEvent): Promise<void> {
  await navigateTo(item.routePath || '/drive')
}

async function loadWorkspace(): Promise<void> {
  loading.value = true
  try {
    const [nextItems, nextUsage] = await Promise.all([
      listItems({
        parentId: currentParentId.value,
        keyword: keyword.value.trim(),
        itemType: itemTypeFilter.value,
        limit: 200
      }),
      getUsage()
    ])
    items.value = nextItems
    usage.value = nextUsage
    registerFolders(nextItems)
    clearItemSelection()
    closeContextMenu()
    workspaceAlert.value = null
  } catch (error) {
    setWorkspaceAlert(error, t('drive.messages.loadWorkspaceFailed'), loadWorkspace)
    ElMessage.error(resolveErrorMessage(error, t('drive.messages.loadWorkspaceFailed')))
  } finally {
    loading.value = false
  }
}

async function loadSharedWithMe(): Promise<void> {
  sharedWithMeLoading.value = true
  try {
    sharedWithMeItems.value = await listSharedWithMe()
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.sharedWithMe.messages.loadFailed'))
  } finally {
    sharedWithMeLoading.value = false
  }
}

async function loadCollaboratorIncomingShares(): Promise<void> {
  collaboratorIncomingLoading.value = true
  try {
    collaboratorIncomingItems.value = await listIncomingCollaboratorShares()
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.collaboration.incoming.messages.loadFailed'))
  } finally {
    collaboratorIncomingLoading.value = false
  }
}

async function loadCollaboratorSharedEntries(): Promise<void> {
  collaboratorSharedLoading.value = true
  try {
    collaboratorSharedItems.value = await listCollaboratorSharedWithMe()
    syncActiveCollaboratorShare()
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.collaboration.shared.messages.loadFailed'))
  } finally {
    collaboratorSharedLoading.value = false
  }
}

function syncActiveCollaboratorShare(): void {
  if (!activeCollaboratorShare.value) {
    return
  }
  const next = collaboratorSharedItems.value.find((item) => item.shareId === activeCollaboratorShare.value?.shareId) || null
  activeCollaboratorShare.value = next
}

async function refreshCollaboratorWorkspaceIfNeeded(): Promise<void> {
  if (!activeCollaboratorShare.value?.available) {
    collaboratorWorkspaceItems.value = []
    return
  }
  await loadCollaboratorWorkspace()
}

async function onOpenSharedWithMe(item: DriveSavedShare): Promise<void> {
  await navigateTo(`/public/drive/shares/${item.token}`)
}

async function onRemoveSharedWithMe(item: DriveSavedShare): Promise<void> {
  try {
    await ElMessageBox.confirm(
      t('drive.sharedWithMe.messages.removeConfirm', { name: item.itemName }),
      t('drive.sharedWithMe.actions.remove'),
      {
        type: 'warning',
        confirmButtonText: t('drive.sharedWithMe.actions.remove'),
        cancelButtonText: t('common.actions.cancel')
      }
    )
  } catch {
    return
  }
  sharedWithMeRemovingId.value = item.id
  try {
    await removeSharedWithMe(item.id)
    ElMessage.success(t('drive.sharedWithMe.messages.removed'))
    await loadSharedWithMe()
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.sharedWithMe.messages.removeFailed'))
  } finally {
    sharedWithMeRemovingId.value = ''
  }
}

async function onRespondCollaboratorIncoming(payload: { shareId: string; response: 'ACCEPT' | 'DECLINE' }): Promise<void> {
  collaboratorMutationId.value = payload.shareId
  try {
    const updated = await respondCollaboratorShare(payload.shareId, { response: payload.response })
    await Promise.all([loadCollaboratorIncomingShares(), loadCollaboratorSharedEntries()])
    if (payload.response === 'ACCEPT') {
      const matched = collaboratorSharedItems.value.find((item) => item.shareId === updated.shareId)
      if (matched) {
        await openCollaboratorWorkspace(matched)
      }
    }
    ElMessage.success(t('drive.collaboration.incoming.messages.updated'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.collaboration.incoming.messages.updateFailed'))
  } finally {
    collaboratorMutationId.value = ''
  }
}

async function onOpenIncomingCollaboratorShare(item: DriveIncomingCollaboratorShare): Promise<void> {
  const matched = collaboratorSharedItems.value.find((entry) => entry.shareId === item.shareId)
  if (matched) {
    await openCollaboratorWorkspace(matched)
  }
}

async function openCollaboratorWorkspace(item: DriveCollaboratorSharedItem): Promise<void> {
  if (!item.available) {
    return
  }
  activeCollaboratorShare.value = item
  collaboratorParentId.value = null
  collaboratorTrail.value = [{ id: null, name: item.itemName }]
  await loadCollaboratorWorkspace()
}

async function loadCollaboratorWorkspace(): Promise<void> {
  if (!activeCollaboratorShare.value?.available) {
    collaboratorWorkspaceItems.value = []
    return
  }
  collaboratorWorkspaceLoading.value = true
  try {
    collaboratorWorkspaceItems.value = await listCollaboratorSharedItems({
      shareId: activeCollaboratorShare.value.shareId,
      parentId: collaboratorParentId.value,
      limit: 200
    })
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.collaboration.workspace.messages.loadFailed'))
  } finally {
    collaboratorWorkspaceLoading.value = false
  }
}

async function onOpenCollaboratorFolder(folder: DriveItem): Promise<void> {
  if (folder.itemType !== 'FOLDER') {
    return
  }
  collaboratorParentId.value = folder.id
  const existing = collaboratorTrail.value.findIndex((node) => node.id === folder.id)
  if (existing >= 0) {
    collaboratorTrail.value = collaboratorTrail.value.slice(0, existing + 1)
  } else {
    collaboratorTrail.value = [...collaboratorTrail.value, { id: folder.id, name: folder.name }]
  }
  await loadCollaboratorWorkspace()
}

async function onNavigateCollaborator(index: number): Promise<void> {
  const target = collaboratorTrail.value[index]
  if (!target) {
    return
  }
  collaboratorTrail.value = collaboratorTrail.value.slice(0, index + 1)
  collaboratorParentId.value = target.id
  await loadCollaboratorWorkspace()
}

function triggerCollaboratorUploadPicker(): void {
  collaboratorUploadInputRef.value?.click()
}

function setCollaboratorUploadFile(file: File | null): void {
  collaboratorUploadFileRef.value = file
  collaboratorUploadFileName.value = file?.name || ''
}

function onCollaboratorUploadFileSelected(event: Event): void {
  const input = event.target as HTMLInputElement
  setCollaboratorUploadFile(input.files?.[0] || null)
}

function clearCollaboratorUploadSelection(): void {
  setCollaboratorUploadFile(null)
  if (collaboratorUploadInputRef.value) {
    collaboratorUploadInputRef.value.value = ''
  }
}

async function onCreateCollaboratorWorkspaceFolder(): Promise<void> {
  if (!activeCollaboratorShare.value?.available) {
    return
  }
  const name = collaboratorFolderName.value.trim()
  if (!name) {
    ElMessage.warning(t('drive.collaboration.workspace.messages.folderNameRequired'))
    return
  }
  collaboratorWorkspaceMutating.value = true
  try {
    await createCollaboratorFolder(activeCollaboratorShare.value.shareId, {
      name,
      parentId: collaboratorParentId.value
    })
    collaboratorFolderName.value = ''
    ElMessage.success(t('drive.collaboration.workspace.messages.folderCreated'))
    await Promise.all([loadCollaboratorWorkspace(), loadCollaboratorSharedEntries()])
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.collaboration.workspace.messages.folderCreateFailed'))
  } finally {
    collaboratorWorkspaceMutating.value = false
  }
}

async function onUploadCollaboratorWorkspaceFile(): Promise<void> {
  if (collaboratorUploading.value) {
    return
  }
  if (!activeCollaboratorShare.value?.available) {
    return
  }
  if (!collaboratorUploadFileRef.value) {
    ElMessage.warning(t('drive.collaboration.workspace.messages.fileRequired'))
    return
  }
  collaboratorUploading.value = true
  try {
    await uploadCollaboratorFile(
      activeCollaboratorShare.value.shareId,
      collaboratorUploadFileRef.value,
      collaboratorParentId.value
    )
    ElMessage.success(t('drive.collaboration.workspace.messages.fileUploaded'))
    clearCollaboratorUploadSelection()
    await loadCollaboratorWorkspace()
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.collaboration.workspace.messages.fileUploadFailed'))
  } finally {
    collaboratorUploading.value = false
  }
}

async function onSearch(): Promise<void> {
  await loadWorkspace()
}

async function onOpenFolder(folder: DriveItem): Promise<void> {
  if (folder.itemType !== 'FOLDER') {
    return
  }
  currentParentId.value = folder.id
  const existing = trail.value.findIndex(node => node.id === folder.id)
  if (existing >= 0) {
    trail.value = trail.value.slice(0, existing + 1)
  } else {
    trail.value = [...trail.value, { id: folder.id, name: folder.name }]
  }
  await loadWorkspace()
}

async function onNavigate(index: number): Promise<void> {
  const target = trail.value[index]
  if (!target) {
    return
  }
  trail.value = trail.value.slice(0, index + 1)
  currentParentId.value = target.id
  await loadWorkspace()
}

async function onCreateFolder(): Promise<void> {
  const name = createFolderName.value.trim()
  if (!name) {
    ElMessage.warning(t('drive.messages.folderNameRequired'))
    return
  }

  mutating.value = true
  try {
    await createFolder({
      name,
      parentId: currentParentId.value
    })
    createFolderName.value = ''
    operationAlert.value = null
    ElMessage.success(t('drive.messages.folderCreated'))
    await loadWorkspace()
  } catch (error) {
    setOperationAlert(error, t('drive.messages.createFolderFailed'), onCreateFolder)
    ElMessage.error(resolveErrorMessage(error, t('drive.messages.createFolderFailed')))
  } finally {
    mutating.value = false
  }
}

function triggerUploadPicker(): void {
  uploadInputRef.value?.click()
}

function setUploadFile(file: File | null): void {
  uploadFileRef.value = file
  uploadFileName.value = file?.name || ''
}

function onUploadFileSelected(event: Event): void {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] || null
  setUploadFile(file)
}

function clearUploadSelection(): void {
  setUploadFile(null)
  if (uploadInputRef.value) {
    uploadInputRef.value.value = ''
  }
}

function onUploadDragOver(event: DragEvent): void {
  event.preventDefault()
  if (uploading.value) {
    return
  }
  dragOver.value = true
}

function onUploadDragLeave(event: DragEvent): void {
  event.preventDefault()
  dragOver.value = false
}

async function onUploadDrop(event: DragEvent): Promise<void> {
  event.preventDefault()
  dragOver.value = false
  if (uploading.value) {
    return
  }
  const file = event.dataTransfer?.files?.[0]
  if (!file) {
    return
  }
  setUploadFile(file)
  await onUploadFile()
}

async function onUploadFile(): Promise<void> {
  if (uploading.value) {
    return
  }
  if (!uploadFileRef.value) {
    ElMessage.warning(t('drive.messages.fileRequired'))
    return
  }

  uploading.value = true
  try {
    await uploadFile(uploadFileRef.value, currentParentId.value)
    operationAlert.value = null
    ElMessage.success(t('drive.messages.fileUploaded'))
    clearUploadSelection()
    await loadWorkspace()
  } catch (error) {
    setOperationAlert(error, t('drive.messages.uploadFailed'), onUploadFile)
    ElMessage.error(resolveErrorMessage(error, t('drive.messages.uploadFailed')))
  } finally {
    uploading.value = false
  }
}

function openRename(item: DriveItem): void {
  renameForm.itemId = item.id
  renameForm.name = item.name
  renameDialogVisible.value = true
}

async function submitRename(): Promise<void> {
  const name = renameForm.name.trim()
  if (!renameForm.itemId || !name) {
    ElMessage.warning(t('drive.messages.renameRequired'))
    return
  }

  mutating.value = true
  try {
    await renameItem(renameForm.itemId, name)
    renameDialogVisible.value = false
    operationAlert.value = null
    ElMessage.success(t('drive.messages.renamed'))
    await loadWorkspace()
  } catch (error) {
    setOperationAlert(error, t('drive.messages.renameFailed'), submitRename)
    ElMessage.error(resolveErrorMessage(error, t('drive.messages.renameFailed')))
  } finally {
    mutating.value = false
  }
}

function openMove(item: DriveItem): void {
  moveForm.itemId = item.id
  moveForm.parentId = item.parentId || ''
  moveDialogVisible.value = true
}

async function submitMove(): Promise<void> {
  if (!moveForm.itemId) {
    return
  }

  mutating.value = true
  try {
    await moveItem(moveForm.itemId, {
      parentId: moveForm.parentId || null
    })
    moveDialogVisible.value = false
    operationAlert.value = null
    ElMessage.success(t('drive.messages.moved'))
    await loadWorkspace()
  } catch (error) {
    setOperationAlert(error, t('drive.messages.moveFailed'), submitMove)
    ElMessage.error(resolveErrorMessage(error, t('drive.messages.moveFailed')))
  } finally {
    mutating.value = false
  }
}

async function onDelete(item: DriveItem): Promise<void> {
  try {
    await ElMessageBox.confirm(t('drive.messages.deleteConfirm', { name: item.name }), t('drive.messages.deleteTitle'), {
      type: 'warning',
      confirmButtonText: t('common.actions.delete'),
      cancelButtonText: t('common.actions.cancel')
    })
  } catch {
    return
  }

  mutating.value = true
  try {
    await deleteItem(item.id)
    operationAlert.value = null
    ElMessage.success(t('drive.messages.deleted'))
    await loadWorkspace()
  } catch (error) {
    setOperationAlert(error, t('drive.messages.deleteFailed'), () => onDelete(item))
    ElMessage.error(resolveErrorMessage(error, t('drive.messages.deleteFailed')))
  } finally {
    mutating.value = false
  }
}

async function loadTrashItemsData(): Promise<void> {
  trashLoading.value = true
  try {
    trashItems.value = await listTrashItems(100)
    clearTrashSelection()
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.loadTrashFailed'))
  } finally {
    trashLoading.value = false
  }
}

async function openTrashDrawer(): Promise<void> {
  trashDrawerVisible.value = true
  await loadTrashItemsData()
}

async function onRestoreTrash(item: DriveTrashItem): Promise<void> {
  trashMutatingId.value = item.id
  try {
    await restoreTrashItem(item.id)
    ElMessage.success(t('drive.messages.restored'))
    await Promise.all([loadTrashItemsData(), loadWorkspace()])
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.restoreFailed'))
  } finally {
    trashMutatingId.value = ''
  }
}

async function onPurgeTrash(item: DriveTrashItem): Promise<void> {
  try {
    await ElMessageBox.confirm(t('drive.messages.purgeConfirm', { name: item.name }), t('drive.messages.purgeTitle'), {
      type: 'warning',
      confirmButtonText: t('drive.trash.purge'),
      cancelButtonText: t('common.actions.cancel')
    })
  } catch {
    return
  }
  trashMutatingId.value = item.id
  try {
    await purgeTrashItem(item.id)
    ElMessage.success(t('drive.messages.purged'))
    await Promise.all([loadTrashItemsData(), loadWorkspace()])
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.purgeFailed'))
  } finally {
    trashMutatingId.value = ''
  }
}

function onCloseTrashDrawer(): void {
  trashItems.value = []
  trashMutatingId.value = ''
  clearTrashSelection()
}

async function loadAccessLogs(): Promise<void> {
  accessLogLoading.value = true
  try {
    accessLogs.value = await listShareAccessLogs({
      action: accessLogFilters.action || undefined,
      accessStatus: accessLogFilters.accessStatus || undefined,
      limit: accessLogFilters.limit
    })
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.loadAccessLogsFailed'))
  } finally {
    accessLogLoading.value = false
  }
}

async function openAccessLogDrawer(): Promise<void> {
  accessLogDrawerVisible.value = true
  await loadAccessLogs()
}

async function onApplyAccessLogFilters(): Promise<void> {
  await loadAccessLogs()
}

function onCloseAccessLogDrawer(): void {
  accessLogs.value = []
}

async function loadVersionItems(itemId: string): Promise<void> {
  versionLoading.value = true
  try {
    versionItems.value = await listFileVersions(itemId, 100)
    operationAlert.value = null
  } catch (error) {
    setOperationAlert(error, t('drive.messages.loadVersionsFailed'), () => loadVersionItems(itemId))
    ElMessage.error(resolveErrorMessage(error, t('drive.messages.loadVersionsFailed')))
  } finally {
    versionLoading.value = false
  }
}

function triggerVersionUploadPicker(): void {
  versionUploadInputRef.value?.click()
}

function setVersionUploadFile(file: File | null): void {
  versionUploadFileRef.value = file
  versionUploadFileName.value = file?.name || ''
}

function onVersionUploadFileSelected(event: Event): void {
  const input = event.target as HTMLInputElement
  setVersionUploadFile(input.files?.[0] || null)
}

function clearVersionUploadSelection(): void {
  setVersionUploadFile(null)
  if (versionUploadInputRef.value) {
    versionUploadInputRef.value.value = ''
  }
}

async function openVersions(item: DriveItem): Promise<void> {
  if (item.itemType !== 'FILE') {
    return
  }
  activeVersionItem.value = item
  versionDrawerVisible.value = true
  lastVersionCleanupResult.value = null
  clearVersionUploadSelection()
  await loadVersionItems(item.id)
}

async function onUploadNewVersion(): Promise<void> {
  if (versionUploadLoading.value) {
    return
  }
  if (!activeVersionItem.value) {
    return
  }
  if (!versionUploadFileRef.value) {
    ElMessage.warning(t('drive.messages.versionFileRequired'))
    return
  }
  versionUploadLoading.value = true
  try {
    await uploadFileVersion(activeVersionItem.value.id, versionUploadFileRef.value)
    operationAlert.value = null
    ElMessage.success(t('drive.messages.versionUploaded'))
    clearVersionUploadSelection()
    await Promise.all([loadVersionItems(activeVersionItem.value.id), loadWorkspace()])
  } catch (error) {
    setOperationAlert(error, t('drive.messages.versionUploadFailed'), onUploadNewVersion)
    ElMessage.error(resolveErrorMessage(error, t('drive.messages.versionUploadFailed')))
  } finally {
    versionUploadLoading.value = false
  }
}

async function onRestoreVersion(version: DriveFileVersion): Promise<void> {
  if (!activeVersionItem.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      t('drive.messages.versionRestoreConfirm', { version: version.versionNo, name: activeVersionItem.value.name }),
      t('drive.messages.versionRestoreTitle'),
      {
        type: 'warning',
        confirmButtonText: t('drive.versions.restore'),
        cancelButtonText: t('common.actions.cancel')
      }
    )
  } catch {
    return
  }
  versionMutatingId.value = version.id
  try {
    await restoreFileVersion(activeVersionItem.value.id, version.id)
    operationAlert.value = null
    ElMessage.success(t('drive.messages.versionRestored'))
    await Promise.all([loadVersionItems(activeVersionItem.value.id), loadWorkspace()])
  } catch (error) {
    setOperationAlert(error, t('drive.messages.versionRestoreFailed'), () => onRestoreVersion(version))
    ElMessage.error(resolveErrorMessage(error, t('drive.messages.versionRestoreFailed')))
  } finally {
    versionMutatingId.value = ''
  }
}

async function onCleanupVersions(): Promise<void> {
  if (!activeVersionItem.value) {
    return
  }
  versionCleanupLoading.value = true
  try {
    const result = await cleanupFileVersions(activeVersionItem.value.id)
    lastVersionCleanupResult.value = result
    ElMessage.success(t('drive.messages.versionCleanupCompleted', { deleted: result.deletedVersions, remaining: result.remainingVersions }))
    await Promise.all([loadVersionItems(activeVersionItem.value.id), loadWorkspace()])
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.versionCleanupFailed'))
  } finally {
    versionCleanupLoading.value = false
  }
}

function onCloseVersionDrawer(): void {
  versionItems.value = []
  activeVersionItem.value = null
  lastVersionCleanupResult.value = null
  versionCleanupLoading.value = false
  versionMutatingId.value = ''
  clearVersionUploadSelection()
}

function resolvePreviewKind(mimeType: string): DrivePreviewKind {
  const normalized = mimeType.toLowerCase()
  if (
    normalized.startsWith('text/')
    || normalized.includes('json')
    || normalized.includes('xml')
  ) {
    return 'TEXT'
  }
  if (normalized.startsWith('image/')) {
    return 'IMAGE'
  }
  if (normalized === 'application/pdf') {
    return 'PDF'
  }
  return 'UNSUPPORTED'
}

function clearPreviewState(): void {
  previewTarget.value = null
  previewKind.value = 'UNSUPPORTED'
  previewText.value = ''
  previewMimeType.value = ''
  previewTruncated.value = false
  if (previewBlobUrl.value) {
    URL.revokeObjectURL(previewBlobUrl.value)
    previewBlobUrl.value = ''
  }
}

function onClosePreviewDrawer(): void {
  clearPreviewState()
}

async function showPreview(
  item: DriveItem,
  loader: () => Promise<{ blob: Blob; mimeType: string; truncated: boolean }>
): Promise<void> {
  clearPreviewState()
  previewTarget.value = item
  previewDrawerVisible.value = true
  previewLoading.value = true
  try {
    const file = await loader()
    previewMimeType.value = file.mimeType
    previewKind.value = resolvePreviewKind(file.mimeType)
    previewTruncated.value = file.truncated
    if (previewKind.value === 'TEXT') {
      previewText.value = await file.blob.text()
      return
    }
    if (previewKind.value === 'IMAGE' || previewKind.value === 'PDF') {
      previewBlobUrl.value = URL.createObjectURL(file.blob)
      return
    }
  } catch (error) {
    ElMessage.error((error as Error).message || t('drive.messages.previewFailed'))
    previewDrawerVisible.value = false
    clearPreviewState()
  } finally {
    previewLoading.value = false
  }
}

async function onPreview(item: DriveItem): Promise<void> {
  if (item.itemType !== 'FILE') {
    return
  }
  await showPreview(item, () => previewFile(item.id))
}

async function onPreviewCollaboratorFile(item: DriveItem): Promise<void> {
  if (!activeCollaboratorShare.value || item.itemType !== 'FILE') {
    return
  }
  await showPreview(item, () => previewCollaboratorFile(activeCollaboratorShare.value!.shareId, item.id))
}

async function openShares(item: DriveItem): Promise<void> {
  activeShareItem.value = item
  shareDrawerVisible.value = true
}

async function onShareChanged(): Promise<void> {
  await Promise.all([
    loadWorkspace(),
    loadCollaboratorIncomingShares(),
    loadCollaboratorSharedEntries(),
    accessLogDrawerVisible.value ? loadAccessLogs() : Promise.resolve()
  ])
}

async function downloadAndSave(
  loader: () => Promise<{ blob: Blob; fileName: string }>,
  loadingId: { value: string },
  itemId: string,
  retry: () => Promise<void>
): Promise<void> {
  loadingId.value = itemId
  try {
    const file = await loader()
    saveBlob(file.blob, file.fileName)
    operationAlert.value = null
    ElMessage.success(t('drive.messages.downloadStarted'))
  } catch (error) {
    setOperationAlert(error, t('drive.messages.downloadFailed'), retry)
    ElMessage.error(resolveErrorMessage(error, t('drive.messages.downloadFailed')))
  } finally {
    loadingId.value = ''
  }
}

async function onDownload(item: DriveItem): Promise<void> {
  if (item.itemType !== 'FILE') {
    return
  }
  await downloadAndSave(() => downloadFile(item.id), downloadingItemId, item.id, () => onDownload(item))
}

async function onDownloadCollaboratorFile(item: DriveItem): Promise<void> {
  if (!activeCollaboratorShare.value || item.itemType !== 'FILE') {
    return
  }
  await downloadAndSave(
    () => downloadCollaboratorFile(activeCollaboratorShare.value!.shareId, item.id),
    collaboratorDownloadingItemId,
    item.id,
    () => onDownloadCollaboratorFile(item)
  )
}

function onOpenBatchShare(): void {
  if (!hasSelectedItems.value) {
    ElMessage.warning(t('drive.messages.selectAtLeastOneItem'))
    return
  }
  batchShareDialogVisible.value = true
}

async function onBatchShareChanged(): Promise<void> {
  operationAlert.value = null
  await loadWorkspace()
}

function saveBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  URL.revokeObjectURL(url)
}

function onCloseShareDrawer(): void {
  activeShareItem.value = null
}

function onShareDrawerVisibilityChange(value: boolean): void {
  if (!value) {
    onCloseShareDrawer()
  }
}

onMounted(() => {
  window.addEventListener('keydown', onDriveKeydown)
  window.addEventListener('click', closeContextMenu)
  window.addEventListener('scroll', closeContextMenu, true)
  void onRefreshDashboard()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onDriveKeydown)
  window.removeEventListener('click', closeContextMenu)
  window.removeEventListener('scroll', closeContextMenu, true)
  closeContextMenu()
  clearPreviewState()
})
</script>

<template>
  <div class="mm-page">
    <section class="mm-card panel">
      <DriveCollaborationLaunchpad
        :items="collaborationItems"
        :loading="collaborationLoading"
        :creating-doc="creatingDoc"
        :creating-sheet="creatingSheet"
        @create-doc="onCreateDoc"
        @create-sheet="onCreateSheet"
        @open-item="onOpenLaunchpadItem"
      />

      <div class="head-row">
        <div>
          <h1 class="mm-section-title">{{ t('page.drive.title') }}</h1>
          <p class="muted">{{ currentWorkspaceDescription }}</p>
        </div>
        <div class="head-actions">
          <el-button @click="openAccessLogDrawer">{{ t('drive.actions.accessLogs') }}</el-button>
          <el-button @click="openTrashDrawer">{{ t('drive.actions.trash') }}</el-button>
          <el-button :loading="loading || collaborationLoading" @click="onRefreshDashboard">{{ t('common.actions.refresh') }}</el-button>
        </div>
      </div>

      <div class="view-switch-row">
        <el-segmented v-model="workspaceView" :options="driveViewOptions" />
      </div>

      <template v-if="workspaceView === 'MY_FILES'">
      <DriveErrorBanner
        v-if="workspaceAlert"
        test-id="drive-workspace-error"
        :title="workspaceAlert.title"
        :message="workspaceAlert.message"
        @retry="retryWorkspaceAlert"
        @dismiss="workspaceAlert = null"
      />

      <DriveErrorBanner
        v-if="operationAlert"
        test-id="drive-operation-error"
        :title="operationAlert.title"
        :message="operationAlert.message"
        @retry="retryOperationAlert"
        @dismiss="operationAlert = null"
      />

      <el-breadcrumb separator="/" class="breadcrumb">
        <el-breadcrumb-item
          v-for="(node, index) in trail"
          :key="`${node.id || 'root'}-${index}`"
        >
          <a class="crumb-link" @click.prevent="onNavigate(index)">{{ node.name }}</a>
        </el-breadcrumb-item>
      </el-breadcrumb>

      <div class="toolbar">
        <el-input
          v-model="keyword"
          data-testid="drive-search-input"
          :placeholder="t('drive.search.placeholder')"
          clearable
          @keyup.enter="onSearch"
        />
        <el-select v-model="itemTypeFilter" :placeholder="t('drive.search.typePlaceholder')" clearable>
          <el-option :label="t('drive.search.types.folder')" value="FOLDER" />
          <el-option :label="t('drive.search.types.file')" value="FILE" />
        </el-select>
        <el-button data-testid="drive-search-submit" type="primary" @click="onSearch">{{ t('drive.search.action') }}</el-button>
      </div>

      <div class="create-row">
        <el-input v-model="createFolderName" data-testid="drive-folder-name" :placeholder="t('drive.folder.placeholder')" />
        <el-button data-testid="drive-folder-create" type="success" :loading="mutating" @click="onCreateFolder">{{ t('drive.folder.create') }}</el-button>
      </div>

      <input
        ref="uploadInputRef"
        data-testid="drive-upload-input"
        class="hidden-file-input"
        type="file"
        @change="onUploadFileSelected"
      >
      <div
        class="upload-zone"
        :class="{ 'upload-zone--active': dragOver }"
        @dragover="onUploadDragOver"
        @dragleave="onUploadDragLeave"
        @drop="onUploadDrop"
      >
        <p class="upload-tip">
          {{ t('drive.upload.tip') }}
        </p>
        <div class="upload-row">
          <el-input :model-value="uploadFileName" readonly :placeholder="t('drive.upload.placeholder')" />
          <el-button data-testid="drive-upload-pick" @click="triggerUploadPicker">{{ t('drive.upload.chooseFile') }}</el-button>
          <el-button data-testid="drive-upload-submit" type="success" :loading="uploading" @click="onUploadFile">{{ t('drive.upload.submit') }}</el-button>
          <el-button :disabled="!uploadFileName" @click="clearUploadSelection">{{ t('common.actions.clear') }}</el-button>
        </div>
      </div>

      <div class="batch-row">
        <div class="batch-info">
          <el-tag type="info">{{ t('drive.batch.selected', { count: selectedItemIds.length }) }}</el-tag>
          <span class="muted">{{ t('drive.batch.shortcuts') }}</span>
        </div>
        <div class="batch-actions">
          <el-button data-testid="drive-batch-share" type="primary" plain :disabled="!hasSelectedItems" @click="onOpenBatchShare">
            {{ t('drive.batch.shareSelected') }}
          </el-button>
          <el-button data-testid="drive-batch-delete" type="danger" plain :loading="batchMutating" :disabled="!hasSelectedItems" @click="onBatchDeleteSelected">
            {{ t('drive.batch.deleteSelected') }}
          </el-button>
          <el-button :disabled="!hasSelectedItems" @click="clearItemSelection">{{ t('drive.batch.clearSelection') }}</el-button>
        </div>
      </div>

      <el-table
        ref="itemTableRef"
        :data="items"
        v-loading="loading"
        row-key="id"
        style="width: 100%"
        @selection-change="onItemSelectionChange"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column :label="t('drive.table.columns.name')" min-width="220">
          <template #default="scope">
            <div class="name-cell" @contextmenu.prevent="onRowContextMenu($event, scope.row)">
              <el-tag :type="scope.row.itemType === 'FOLDER' ? 'success' : 'info'" size="small">
                {{ getItemTypeLabel(scope.row.itemType) }}
              </el-tag>
              <span>{{ scope.row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('drive.table.columns.size')" min-width="120">
          <template #default="scope">
            <span>{{ scope.row.itemType === 'FILE' ? formatBytes(scope.row.sizeBytes) : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('drive.table.columns.shares')" prop="shareCount" width="90" />
        <el-table-column :label="t('drive.table.columns.updatedAt')" min-width="180">
          <template #default="scope">
            <span>{{ formatTime(scope.row.updatedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('drive.table.columns.actions')" width="480">
          <template #default="scope">
            <el-button
              v-if="scope.row.itemType === 'FOLDER'"
              :data-testid="`drive-action-open-${scope.row.id}`"
              type="primary"
              text
              @click="onOpenFolder(scope.row)"
            >
              {{ t('drive.table.actions.open') }}
            </el-button>
            <template v-else>
              <el-button
                :data-testid="`drive-action-download-${scope.row.id}`"
                type="primary"
                text
                :loading="downloadingItemId === scope.row.id"
                @click="onDownload(scope.row)"
              >
                {{ t('drive.table.actions.download') }}
              </el-button>
              <el-button :data-testid="`drive-action-preview-${scope.row.id}`" type="primary" text @click="onPreview(scope.row)">{{ t('drive.table.actions.preview') }}</el-button>
              <el-button :data-testid="`drive-action-versions-${scope.row.id}`" type="primary" text @click="openVersions(scope.row)">{{ t('drive.table.actions.versions') }}</el-button>
            </template>
            <el-button :data-testid="`drive-action-shares-${scope.row.id}`" type="primary" text @click="openShares(scope.row)">{{ t('drive.table.actions.shares') }}</el-button>
            <el-button :data-testid="`drive-action-rename-${scope.row.id}`" type="warning" text @click="openRename(scope.row)">{{ t('common.actions.rename') }}</el-button>
            <el-button :data-testid="`drive-action-move-${scope.row.id}`" type="warning" text @click="openMove(scope.row)">{{ t('drive.table.actions.move') }}</el-button>
            <el-button :data-testid="`drive-action-delete-${scope.row.id}`" type="danger" text @click="onDelete(scope.row)">{{ t('common.actions.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      </template>

      <template v-else>
        <DriveIncomingSharesPanel
          :items="collaboratorIncomingItems"
          :loading="collaboratorIncomingLoading"
          :mutation-id="collaboratorMutationId"
          @respond="onRespondCollaboratorIncoming"
          @open="onOpenIncomingCollaboratorShare"
        />

        <DriveCollaboratorSharedPanel
          :items="collaboratorSharedItems"
          :loading="collaboratorSharedLoading"
          :active-share-id="activeCollaboratorShare?.shareId || ''"
          @open="openCollaboratorWorkspace"
        />

        <section v-if="activeCollaboratorShare" class="mm-card collaborator-workspace">
          <div class="head-row collaborator-workspace__head">
            <div>
              <p class="muted">{{ t('drive.collaboration.workspace.owner', { name: activeCollaboratorShare.ownerDisplayName || activeCollaboratorShare.ownerEmail }) }}</p>
              <h2 class="mm-section-title">{{ activeCollaboratorWorkspaceLabel }}</h2>
            </div>
            <div class="head-actions">
              <el-tag size="small" effect="plain">
                {{ activeCollaboratorShare.permission === 'EDIT' ? t('docs.share.edit') : t('docs.share.view') }}
              </el-tag>
              <el-tag :type="activeCollaboratorShare.available ? 'success' : 'danger'" effect="plain">
                {{ activeCollaboratorShare.available ? t('drive.collaboration.shared.actions.opened') : t('drive.collaboration.status.revoked') }}
              </el-tag>
            </div>
          </div>

          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item
              v-for="(node, index) in collaboratorTrail"
              :key="`${node.id || 'share-root'}-${index}`"
            >
              <a class="crumb-link" @click.prevent="onNavigateCollaborator(index)">{{ node.name }}</a>
            </el-breadcrumb-item>
          </el-breadcrumb>

          <div v-if="canEditCollaboratorWorkspace" class="create-row">
            <el-input
              v-model="collaboratorFolderName"
              :placeholder="t('drive.collaboration.workspace.folderPlaceholder')"
            />
            <el-button
              type="success"
              :loading="collaboratorWorkspaceMutating"
              @click="onCreateCollaboratorWorkspaceFolder"
            >
              {{ t('drive.collaboration.workspace.createFolder') }}
            </el-button>
          </div>

          <input
            ref="collaboratorUploadInputRef"
            class="hidden-file-input"
            type="file"
            @change="onCollaboratorUploadFileSelected"
          >
          <div v-if="canEditCollaboratorWorkspace" class="upload-row collaborator-upload-row">
            <el-input
              :model-value="collaboratorUploadFileName"
              readonly
              :placeholder="t('drive.collaboration.workspace.filePlaceholder')"
            />
            <el-button @click="triggerCollaboratorUploadPicker">{{ t('drive.upload.chooseFile') }}</el-button>
            <el-button type="success" :loading="collaboratorUploading" @click="onUploadCollaboratorWorkspaceFile">
              {{ t('drive.collaboration.workspace.uploadFile') }}
            </el-button>
            <el-button :disabled="!collaboratorUploadFileName" @click="clearCollaboratorUploadSelection">{{ t('common.actions.clear') }}</el-button>
          </div>

          <el-table
            :data="collaboratorWorkspaceItems"
            v-loading="collaboratorWorkspaceLoading"
            row-key="id"
            style="width: 100%"
          >
            <el-table-column :label="t('drive.table.columns.name')" min-width="220">
              <template #default="scope">
                <div class="name-cell">
                  <el-tag :type="scope.row.itemType === 'FOLDER' ? 'success' : 'info'" size="small">
                    {{ getItemTypeLabel(scope.row.itemType) }}
                  </el-tag>
                  <span>{{ scope.row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('drive.table.columns.size')" min-width="120">
              <template #default="scope">
                <span>{{ scope.row.itemType === 'FILE' ? formatBytes(scope.row.sizeBytes) : '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="t('drive.table.columns.updatedAt')" min-width="180">
              <template #default="scope">
                <span>{{ formatTime(scope.row.updatedAt) }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="t('drive.table.columns.actions')" width="320">
              <template #default="scope">
                <el-button
                  v-if="scope.row.itemType === 'FOLDER'"
                  type="primary"
                  text
                  @click="onOpenCollaboratorFolder(scope.row)"
                >
                  {{ t('drive.table.actions.open') }}
                </el-button>
                <template v-else>
                  <el-button
                    type="primary"
                    text
                    :loading="collaboratorDownloadingItemId === scope.row.id"
                    @click="onDownloadCollaboratorFile(scope.row)"
                  >
                    {{ t('drive.table.actions.download') }}
                  </el-button>
                  <el-button type="primary" text @click="onPreviewCollaboratorFile(scope.row)">
                    {{ t('drive.table.actions.preview') }}
                  </el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <DriveSharedWithMePanel
          :items="sharedWithMeItems"
          :loading="sharedWithMeLoading"
          :removing-id="sharedWithMeRemovingId"
          @open="onOpenSharedWithMe"
          @remove="onRemoveSharedWithMe"
        />
      </template>
    </section>

    <section v-if="workspaceView === 'MY_FILES'" class="mm-card panel usage-panel">
      <h2 class="mm-section-title">{{ t('drive.usage.title') }}</h2>
      <div v-if="usage" class="usage-grid">
        <article class="usage-card" data-testid="drive-usage-storage">
          <div class="usage-title">{{ t('drive.usage.storage') }}</div>
          <div class="usage-value">{{ formatBytes(usage.storageBytes) }} / {{ formatBytes(usage.storageLimitBytes) }}</div>
          <el-progress :percentage="storagePercent" :stroke-width="12" />
        </article>
        <article class="usage-card">
          <div class="usage-title">{{ t('drive.usage.folderCount') }}</div>
          <div class="usage-value">{{ usage.folderCount }}</div>
        </article>
        <article class="usage-card">
          <div class="usage-title">{{ t('drive.usage.fileCount') }}</div>
          <div class="usage-value">{{ usage.fileCount }}</div>
        </article>
      </div>
      <p class="muted">
        {{ currentViewSummary }}
      </p>
    </section>

    <el-dialog v-model="renameDialogVisible" :title="t('drive.dialogs.rename.title')" width="420px">
      <el-input v-model="renameForm.name" data-testid="drive-rename-input" :placeholder="t('drive.dialogs.rename.placeholder')" />
      <template #footer>
        <el-button @click="renameDialogVisible = false">{{ t('common.actions.cancel') }}</el-button>
        <el-button data-testid="drive-rename-submit" type="primary" :loading="mutating" @click="submitRename">{{ t('common.actions.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="moveDialogVisible" :title="t('drive.dialogs.move.title')" width="420px">
      <el-select v-model="moveForm.parentId" data-testid="drive-move-select" :placeholder="t('drive.dialogs.move.placeholder')" class="full-width">
        <el-option
          v-for="option in moveTargetOptions"
          :key="option.value || 'root'"
          :label="option.label"
          :value="option.value"
        />
      </el-select>
      <template #footer>
        <el-button @click="moveDialogVisible = false">{{ t('common.actions.cancel') }}</el-button>
        <el-button data-testid="drive-move-submit" type="primary" :loading="mutating" @click="submitMove">{{ t('common.actions.confirm') }}</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="previewDrawerVisible"
      :title="previewTitle"
      size="760px"
      @closed="onClosePreviewDrawer"
    >
      <div class="preview-panel" v-loading="previewLoading">
        <div class="preview-meta" v-if="previewTarget">
          <span>{{ previewMimeType || previewTarget.mimeType || 'application/octet-stream' }}</span>
          <el-tag v-if="previewTruncated" type="warning" size="small">{{ t('drive.preview.truncated') }}</el-tag>
        </div>

        <pre v-if="previewKind === 'TEXT'" class="preview-text">{{ previewText }}</pre>
        <div v-else-if="previewKind === 'IMAGE'" class="preview-image-wrap">
          <img :src="previewBlobUrl" alt="preview" class="preview-image">
        </div>
        <iframe
          v-else-if="previewKind === 'PDF'"
          :src="previewBlobUrl"
          class="preview-pdf"
          title="pdf-preview"
        />
        <el-empty
          v-else
          :description="t('drive.preview.unsupported')"
        />
      </div>
    </el-drawer>

    <DriveShareLinksDrawer
      v-model="shareDrawerVisible"
      :item="activeShareItem"
      @changed="onShareChanged"
      @update:model-value="onShareDrawerVisibilityChange"
    />

    <DriveBatchShareDialog
      v-model="batchShareDialogVisible"
      :items="selectedItems"
      @created="onBatchShareChanged"
    />

    <el-drawer
      v-model="versionDrawerVisible"
      :title="t('drive.versions.title')"
      size="760px"
      @closed="onCloseVersionDrawer"
    >
      <div class="version-panel">
        <p v-if="activeVersionItem" class="muted">
          {{ t('drive.versions.item', { name: activeVersionItem.name }) }}
        </p>
        <input
          ref="versionUploadInputRef"
          data-testid="drive-version-upload-input"
          class="hidden-file-input"
          type="file"
          @change="onVersionUploadFileSelected"
        >
        <div class="version-upload-row">
          <el-input :model-value="versionUploadFileName" readonly :placeholder="t('drive.versions.chooseNextVersion')" />
          <el-button @click="triggerVersionUploadPicker">{{ t('drive.upload.chooseFile') }}</el-button>
          <el-button data-testid="drive-version-upload-submit" type="success" :loading="versionUploadLoading" @click="onUploadNewVersion">{{ t('drive.versions.uploadNewVersion') }}</el-button>
          <el-button :disabled="!versionUploadFileName" @click="clearVersionUploadSelection">{{ t('common.actions.clear') }}</el-button>
        </div>
        <div class="version-cleanup-row">
          <el-button data-testid="drive-version-cleanup" type="warning" plain :loading="versionCleanupLoading" @click="onCleanupVersions">
            {{ t('drive.versions.cleanupOldVersions') }}
          </el-button>
          <p v-if="lastVersionCleanupResult" class="muted version-cleanup-summary">
            {{ t('drive.versions.cleanupSummary', {
              deleted: lastVersionCleanupResult.deletedVersions,
              remaining: lastVersionCleanupResult.remainingVersions,
              count: lastVersionCleanupResult.appliedRetentionCount,
              days: lastVersionCleanupResult.appliedRetentionDays
            }) }}
          </p>
        </div>
        <el-table :data="versionItems" v-loading="versionLoading" row-key="id" style="width: 100%">
          <el-table-column prop="versionNo" :label="t('drive.versions.columns.version')" width="110">
            <template #default="scope">
              <el-tag type="info">#{{ scope.row.versionNo }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('drive.table.columns.size')" width="130">
            <template #default="scope">
              <span>{{ formatBytes(scope.row.sizeBytes) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('drive.versions.columns.checksum')" min-width="220">
            <template #default="scope">
              <span class="token-url">{{ scope.row.checksum || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('drive.versions.columns.createdAt')" min-width="180">
            <template #default="scope">
              <span>{{ formatTime(scope.row.createdAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('drive.table.columns.actions')" width="160">
            <template #default="scope">
              <el-button
                :data-testid="`drive-version-restore-${scope.row.id}`"
                type="warning"
                text
                :loading="versionMutatingId === scope.row.id"
                @click="onRestoreVersion(scope.row)"
              >
                {{ t('drive.versions.restore') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-drawer
      v-model="trashDrawerVisible"
      :title="t('drive.trash.title')"
      size="760px"
      @closed="onCloseTrashDrawer"
    >
      <div class="trash-panel">
        <p class="muted">{{ t('drive.trash.description') }}</p>
        <div class="batch-row">
          <div class="batch-info">
            <el-tag type="warning">{{ t('drive.trash.selected', { count: selectedTrashIds.length }) }}</el-tag>
          </div>
          <div class="batch-actions">
            <el-button
              type="primary"
              plain
              :loading="batchMutating"
              :disabled="!hasSelectedTrashItems"
              @click="onBatchRestoreTrash"
            >
              {{ t('drive.trash.restoreSelected') }}
            </el-button>
            <el-button
              type="danger"
              plain
              :loading="batchMutating"
              :disabled="!hasSelectedTrashItems"
              @click="onBatchPurgeTrash"
            >
              {{ t('drive.trash.purgeSelected') }}
            </el-button>
            <el-button :disabled="!hasSelectedTrashItems" @click="clearTrashSelection">{{ t('drive.batch.clearSelection') }}</el-button>
          </div>
        </div>
        <el-table
          ref="trashTableRef"
          :data="trashItems"
          v-loading="trashLoading"
          row-key="id"
          style="width: 100%"
          @selection-change="onTrashSelectionChange"
        >
          <el-table-column type="selection" width="46" />
          <el-table-column prop="name" :label="t('drive.table.columns.name')" min-width="220" />
          <el-table-column :label="t('drive.table.columns.type')" width="100">
            <template #default="scope">
              <span>{{ getItemTypeLabel(scope.row.itemType) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('drive.table.columns.size')" width="120">
            <template #default="scope">
              <span>{{ scope.row.itemType === 'FILE' ? formatBytes(scope.row.sizeBytes) : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('drive.trash.columns.trashedAt')" min-width="170">
            <template #default="scope">
              <span>{{ formatTime(scope.row.trashedAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('drive.trash.columns.purgeAfter')" min-width="170">
            <template #default="scope">
              <span>{{ formatTime(scope.row.purgeAfterAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('drive.table.columns.actions')" width="220">
            <template #default="scope">
              <el-button
                type="primary"
                text
                :loading="trashMutatingId === scope.row.id"
                @click="onRestoreTrash(scope.row)"
              >
                {{ t('drive.trash.restore') }}
              </el-button>
              <el-button
                type="danger"
                text
                :loading="trashMutatingId === scope.row.id"
                @click="onPurgeTrash(scope.row)"
              >
                {{ t('drive.trash.purge') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <div
      v-if="contextMenuVisible && contextMenuItem"
      class="drive-context-menu"
      :style="{ left: `${contextMenuX}px`, top: `${contextMenuY}px` }"
      @click.stop
    >
      <button
        v-if="contextMenuItem.itemType === 'FOLDER'"
        type="button"
        class="context-menu-item"
        @click="onContextMenuAction('open')"
      >
        {{ t('drive.table.actions.open') }}
      </button>
      <template v-else>
        <button type="button" class="context-menu-item" @click="onContextMenuAction('download')">{{ t('drive.table.actions.download') }}</button>
        <button type="button" class="context-menu-item" @click="onContextMenuAction('preview')">{{ t('drive.table.actions.preview') }}</button>
        <button type="button" class="context-menu-item" @click="onContextMenuAction('versions')">{{ t('drive.table.actions.versions') }}</button>
      </template>
      <button type="button" class="context-menu-item" @click="onContextMenuAction('shares')">{{ t('drive.table.actions.shares') }}</button>
      <button type="button" class="context-menu-item" @click="onContextMenuAction('rename')">{{ t('common.actions.rename') }}</button>
      <button type="button" class="context-menu-item" @click="onContextMenuAction('move')">{{ t('drive.table.actions.move') }}</button>
      <button type="button" class="context-menu-item context-menu-item--danger" @click="onContextMenuAction('delete')">
        {{ t('common.actions.delete') }}
      </button>
    </div>

    <el-drawer
      v-model="accessLogDrawerVisible"
      :title="t('drive.accessLog.title')"
      size="760px"
      @closed="onCloseAccessLogDrawer"
    >
      <div class="access-log-panel">
        <div class="access-log-filter-row">
          <el-select v-model="accessLogFilters.action" :placeholder="t('drive.accessLog.filters.action')" clearable>
            <el-option
              v-for="option in accessLogActionOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-select v-model="accessLogFilters.accessStatus" :placeholder="t('drive.accessLog.filters.status')" clearable>
            <el-option
              v-for="option in accessLogStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-input-number v-model="accessLogFilters.limit" :min="1" :max="200" />
          <el-button type="primary" :loading="accessLogLoading" @click="onApplyAccessLogFilters">{{ t('drive.accessLog.apply') }}</el-button>
        </div>
        <el-table :data="accessLogs" v-loading="accessLogLoading" row-key="id" style="width: 100%">
          <el-table-column :label="t('drive.accessLog.columns.action')" width="140">
            <template #default="scope">
              <span>{{ getAccessLogActionLabel(scope.row.action) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('drive.accessLog.columns.status')" min-width="180">
            <template #default="scope">
              <el-tag :type="getAccessLogStatusTagType(scope.row.accessStatus)" effect="plain">
                {{ getAccessLogStatusLabel(scope.row.accessStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ipAddress" :label="t('drive.accessLog.columns.ip')" min-width="140" />
          <el-table-column :label="t('drive.accessLog.columns.userAgent')" min-width="220">
            <template #default="scope">
              <span class="token-url">{{ scope.row.userAgent || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('drive.accessLog.columns.createdAt')" min-width="180">
            <template #default="scope">
              <span>{{ formatTime(scope.row.createdAt) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.panel {
  padding: 20px;
  margin-bottom: 16px;
}

.head-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.head-actions {
  display: inline-flex;
  gap: 8px;
}

.view-switch-row {
  margin: 6px 0 14px;
}

:deep(.el-segmented) {
  --el-segmented-item-selected-bg-color: rgba(87, 140, 255, 0.16);
  --el-segmented-item-selected-color: #153a76;
  --el-border-radius-base: 999px;
}

.muted {
  color: var(--mm-muted);
  margin: 0;
}

.breadcrumb {
  margin: 12px 0 14px;
}

.crumb-link {
  color: var(--mm-primary-dark);
  font-weight: 500;
}

.toolbar {
  display: grid;
  grid-template-columns: 1fr 180px auto;
  gap: 12px;
  margin-bottom: 12px;
}

.create-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  margin-bottom: 12px;
}

.hidden-file-input {
  display: none;
}

.upload-zone {
  margin-bottom: 12px;
  border: 1px dashed #97c8c8;
  border-radius: 10px;
  padding: 12px;
  background: #f7fbfb;
  transition: border-color 0.2s ease, background-color 0.2s ease;
}

.upload-zone--active {
  border-color: #0f6e6e;
  background: #e9f6f6;
}

.upload-tip {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--mm-muted);
}

.upload-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto;
  gap: 10px;
}

.batch-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.batch-info {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.batch-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.name-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.usage-panel {
  padding-top: 16px;
}

.usage-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 12px 0;
}

.usage-card {
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  padding: 12px;
  background: #fff;
}

.usage-title {
  color: var(--mm-muted);
  font-size: 13px;
}

.usage-value {
  font-size: 18px;
  font-weight: 600;
  margin: 6px 0 8px;
}

.preview-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--mm-muted);
  font-size: 13px;
}

.preview-text {
  white-space: pre-wrap;
  max-height: 68vh;
  overflow: auto;
  border: 1px solid var(--mm-border);
  border-radius: 8px;
  padding: 12px;
  background: #fff;
  margin: 0;
  line-height: 1.6;
}

.preview-image-wrap {
  display: grid;
  place-items: center;
  border: 1px solid var(--mm-border);
  border-radius: 8px;
  background: #fff;
  min-height: 320px;
}

.preview-image {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

.preview-pdf {
  width: 100%;
  min-height: 70vh;
  border: 1px solid var(--mm-border);
  border-radius: 8px;
  background: #fff;
}

.full-width {
  width: 100%;
}

.share-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.collaborator-workspace {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 16px 0;
  border: 1px solid rgba(61, 182, 157, 0.16);
  background:
    radial-gradient(circle at top right, rgba(61, 182, 157, 0.1), transparent 22%),
    linear-gradient(145deg, rgba(246, 252, 251, 0.96), rgba(239, 247, 246, 0.94));
}

.collaborator-workspace__head {
  align-items: center;
}

.collaborator-upload-row {
  margin-bottom: 6px;
}

.version-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.version-upload-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto;
  gap: 10px;
}

.version-cleanup-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.version-cleanup-summary {
  font-size: 13px;
}

.trash-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.access-log-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.access-log-filter-row {
  display: grid;
  grid-template-columns: 180px 240px 140px auto;
  gap: 10px;
  align-items: center;
}

.share-create-row {
  display: grid;
  grid-template-columns: 120px 1fr auto;
  gap: 10px;
  align-items: center;
}

.share-permission {
  width: 100%;
}

.token-url {
  display: inline-block;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--mm-muted);
  font-size: 12px;
}

.drive-context-menu {
  position: fixed;
  width: 220px;
  padding: 6px;
  border-radius: 10px;
  border: 1px solid var(--mm-border);
  background: #ffffff;
  box-shadow: 0 18px 40px rgba(15, 52, 68, 0.24);
  z-index: 3000;
}

.context-menu-item {
  display: block;
  width: 100%;
  border: 0;
  background: transparent;
  text-align: left;
  border-radius: 8px;
  padding: 8px 10px;
  color: var(--mm-text-primary);
  cursor: pointer;
}

.context-menu-item:hover {
  background: #edf6f7;
}

.context-menu-item--danger {
  color: #cf3e3e;
}

.context-menu-item--danger:hover {
  background: #ffecec;
}

@media (max-width: 1024px) {
  .toolbar,
  .create-row,
  .upload-row,
  .version-upload-row {
    grid-template-columns: 1fr;
  }

  .usage-grid {
    grid-template-columns: 1fr;
  }

  .share-create-row {
    grid-template-columns: 1fr;
  }

  .head-actions,
  .access-log-filter-row {
    grid-template-columns: 1fr;
    display: grid;
  }

  .batch-row,
  .batch-info,
  .batch-actions {
    width: 100%;
  }
}
</style>
