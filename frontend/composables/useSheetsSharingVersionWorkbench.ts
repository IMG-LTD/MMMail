import { computed, onMounted, ref, watch, type ComputedRef, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { useSheetsApi } from '~/composables/useSheetsApi'
import type {
  SheetsIncomingShare,
  SheetsScopeFilter,
  SheetsWorkbookDetail,
  SheetsWorkbookShare,
  SheetsWorkbookSummary,
  SheetsWorkbookVersion
} from '~/types/sheets'
import {
  countPendingIncomingShares,
  filterSheetsWorkbooksByScope
} from '~/utils/sheets-sharing-version'
import { sortSheetsWorkbooks } from '~/utils/sheets'

interface UseSheetsSharingVersionWorkbenchOptions {
  workbooks: Ref<SheetsWorkbookSummary[]>
  activeWorkbook: Ref<SheetsWorkbookDetail | null>
  activeWorkbookId: ComputedRef<string | null>
  selectWorkbook: (workbookId: string, syncRouteAfterLoad: boolean) => Promise<boolean>
  refreshCollaboration: () => Promise<void>
}

export function useSheetsSharingVersionWorkbench(options: UseSheetsSharingVersionWorkbenchOptions) {
  const api = useSheetsApi()
  const { t } = useI18n()

  const workspaceView = ref<'WORKBOOKS' | 'INCOMING_SHARES'>('WORKBOOKS')
  const scopeFilter = ref<SheetsScopeFilter>('ALL')
  const shares = ref<SheetsWorkbookShare[]>([])
  const incomingShares = ref<SheetsIncomingShare[]>([])
  const versions = ref<SheetsWorkbookVersion[]>([])
  const inviteEmail = ref('')
  const invitePermission = ref<'VIEW' | 'EDIT'>('VIEW')
  const sharesLoading = ref(false)
  const incomingLoading = ref(false)
  const versionsLoading = ref(false)
  const shareSubmitting = ref(false)
  const shareMutationId = ref('')
  const incomingMutationId = ref('')
  const versionMutationId = ref('')
  const versionDrawerVisible = ref(false)

  const filteredWorkbooks = computed(() => filterSheetsWorkbooksByScope(options.workbooks.value, scopeFilter.value))
  const pendingIncomingCount = computed(() => countPendingIncomingShares(incomingShares.value))
  const canManageShares = computed(() => Boolean(options.activeWorkbook.value?.canManageShares))
  const canRestoreVersions = computed(() => Boolean(options.activeWorkbook.value?.canRestoreVersions))

  watch(options.activeWorkbookId, () => {
    void refreshShares()
  }, { immediate: true })

  onMounted(() => {
    void refreshIncomingShares()
  })

  async function refreshVisibleWorkbooks(): Promise<void> {
    options.workbooks.value = sortSheetsWorkbooks(await api.listWorkbooks(100))
  }

  async function refreshShares(): Promise<void> {
    shares.value = []
    if (!options.activeWorkbookId.value || !canManageShares.value) {
      return
    }
    sharesLoading.value = true
    try {
      shares.value = await api.listShares(options.activeWorkbookId.value)
    } finally {
      sharesLoading.value = false
    }
  }

  async function refreshIncomingShares(): Promise<void> {
    incomingLoading.value = true
    try {
      incomingShares.value = await api.listIncomingShares()
    } catch (error) {
      ElMessage.error((error as Error).message || t('sheets.messages.loadIncomingSharesFailed'))
    } finally {
      incomingLoading.value = false
    }
  }

  async function submitShare(): Promise<void> {
    if (!options.activeWorkbookId.value || !inviteEmail.value.trim()) {
      return
    }
    shareSubmitting.value = true
    try {
      await api.createShare(options.activeWorkbookId.value, {
        targetEmail: inviteEmail.value.trim(),
        permission: invitePermission.value
      })
      inviteEmail.value = ''
      invitePermission.value = 'VIEW'
      await refreshShares()
      await options.selectWorkbook(options.activeWorkbookId.value, false)
      await options.refreshCollaboration()
      ElMessage.success(t('sheets.messages.shareCreated'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('sheets.messages.shareCreateFailed'))
    } finally {
      shareSubmitting.value = false
    }
  }

  async function updateSharePermission(shareId: string, permission: 'VIEW' | 'EDIT'): Promise<void> {
    if (!options.activeWorkbookId.value) {
      return
    }
    shareMutationId.value = shareId
    try {
      await api.updateShare(options.activeWorkbookId.value, shareId, { permission })
      await refreshShares()
      await options.refreshCollaboration()
      ElMessage.success(t('sheets.messages.shareUpdated'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('sheets.messages.shareUpdateFailed'))
    } finally {
      shareMutationId.value = ''
    }
  }

  async function removeShare(shareId: string): Promise<void> {
    if (!options.activeWorkbookId.value) {
      return
    }
    shareMutationId.value = shareId
    try {
      await api.removeShare(options.activeWorkbookId.value, shareId)
      await refreshShares()
      await options.selectWorkbook(options.activeWorkbookId.value, false)
      await options.refreshCollaboration()
      ElMessage.success(t('sheets.messages.shareRemoved'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('sheets.messages.shareRemoveFailed'))
    } finally {
      shareMutationId.value = ''
    }
  }

  async function respondIncomingShare(shareId: string, response: 'ACCEPT' | 'DECLINE'): Promise<void> {
    incomingMutationId.value = shareId
    try {
      const updated = await api.respondIncomingShare(shareId, { response })
      await refreshIncomingShares()
      await refreshVisibleWorkbooks()
      await options.refreshCollaboration()
      if (response === 'ACCEPT') {
        workspaceView.value = 'WORKBOOKS'
        scopeFilter.value = 'SHARED'
        await options.selectWorkbook(updated.workbookId, true)
      }
      ElMessage.success(t('sheets.messages.shareResponseUpdated'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('sheets.messages.shareResponseFailed'))
    } finally {
      incomingMutationId.value = ''
    }
  }

  async function openIncomingWorkbook(item: SheetsIncomingShare): Promise<void> {
    workspaceView.value = 'WORKBOOKS'
    scopeFilter.value = 'SHARED'
    await options.selectWorkbook(item.workbookId, true)
  }

  async function openVersionHistory(): Promise<void> {
    if (!options.activeWorkbookId.value || !canRestoreVersions.value) {
      return
    }
    versionDrawerVisible.value = true
    versionsLoading.value = true
    try {
      versions.value = await api.listVersions(options.activeWorkbookId.value)
    } catch (error) {
      ElMessage.error((error as Error).message || t('sheets.messages.loadVersionHistoryFailed'))
    } finally {
      versionsLoading.value = false
    }
  }

  async function restoreVersion(versionId: string): Promise<void> {
    if (!options.activeWorkbookId.value) {
      return
    }
    versionMutationId.value = versionId
    try {
      await api.restoreVersion(options.activeWorkbookId.value, versionId)
      await options.selectWorkbook(options.activeWorkbookId.value, false)
      versions.value = await api.listVersions(options.activeWorkbookId.value)
      await options.refreshCollaboration()
      ElMessage.success(t('sheets.messages.versionRestored'))
    } catch (error) {
      ElMessage.error((error as Error).message || t('sheets.messages.versionRestoreFailed'))
    } finally {
      versionMutationId.value = ''
    }
  }

  return {
    workspaceView,
    scopeFilter,
    filteredWorkbooks,
    shares,
    incomingShares,
    versions,
    inviteEmail,
    invitePermission,
    sharesLoading,
    incomingLoading,
    versionsLoading,
    shareSubmitting,
    shareMutationId,
    incomingMutationId,
    versionMutationId,
    versionDrawerVisible,
    pendingIncomingCount,
    canManageShares,
    canRestoreVersions,
    refreshShares,
    refreshIncomingShares,
    submitShare,
    updateSharePermission,
    removeShare,
    respondIncomingShare,
    openIncomingWorkbook,
    openVersionHistory,
    restoreVersion
  }
}
