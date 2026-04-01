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
  countActiveWorkbookCollaborators,
  countPendingIncomingShares,
  filterSheetsWorkbooksByScope
} from '~/utils/sheets-sharing-version'
import { sortSheetsWorkbooks } from '~/utils/sheets'
import {
  buildSheetsWorkspaceRouteQuery,
  extractSheetsWorkspaceRouteState,
  hasSheetsWorkspaceRouteStateChanged,
  type SheetsWorkspaceView
} from '~/utils/sheets-workspace-route'
import type { SelectSheetsWorkbook } from '~/utils/sheets-workspace-runtime'

interface UseSheetsSharingVersionWorkbenchOptions {
  workbooks: Ref<SheetsWorkbookSummary[]>
  activeWorkbook: Ref<SheetsWorkbookDetail | null>
  activeWorkbookId: ComputedRef<string | null>
  selectWorkbook: SelectSheetsWorkbook
  refreshCollaboration: () => Promise<void>
  confirmDiscardChangesIfNeeded: () => Promise<boolean>
}

export function useSheetsSharingVersionWorkbench(options: UseSheetsSharingVersionWorkbenchOptions) {
  const route = useRoute()
  const router = useRouter()
  const api = useSheetsApi()
  const { t } = useI18n()
  const initialRouteState = extractSheetsWorkspaceRouteState(route.query)
  const workspaceView = ref<SheetsWorkspaceView>(initialRouteState.view)
  const scopeFilter = ref<SheetsScopeFilter>(initialRouteState.scope)
  const shares = ref<SheetsWorkbookShare[]>([])
  const incomingShares = ref<SheetsIncomingShare[]>([])
  const versions = ref<SheetsWorkbookVersion[]>([])
  const inviteEmail = ref('')
  const invitePermission = ref<'VIEW' | 'EDIT'>('VIEW')
  const sharesLoading = ref(false)
  const incomingLoading = ref(false)
  const versionsLoading = ref(false)
  const sharesErrorMessage = ref('')
  const incomingErrorMessage = ref('')
  const versionsErrorMessage = ref('')
  const shareSubmitting = ref(false)
  const shareMutationId = ref('')
  const incomingMutationId = ref('')
  const versionMutationId = ref('')
  const versionDrawerVisible = ref(false)
  let shareRequestId = 0
  let incomingRequestId = 0
  let versionRequestId = 0
  let incomingMutationToken = 0
  const filteredWorkbooks = computed(() => filterSheetsWorkbooksByScope(options.workbooks.value, scopeFilter.value))
  const pendingIncomingCount = computed(() => countPendingIncomingShares(incomingShares.value))
  const canManageShares = computed(() => Boolean(options.activeWorkbook.value?.canManageShares))
  const canRestoreVersions = computed(() => Boolean(options.activeWorkbook.value?.canRestoreVersions))
  watch(options.activeWorkbookId, (nextWorkbookId, previousWorkbookId) => {
    if (nextWorkbookId !== previousWorkbookId) {
      resetShareContext()
      resetVersionContext()
    }
    void refreshShares()
  }, { immediate: true })
  watch(canManageShares, (value) => { if (!value) resetShareContext() }, { immediate: true })
  watch(canRestoreVersions, (value) => { if (!value) resetVersionContext() }, { immediate: true })
  watch(
    () => [route.query.view, route.query.scope] as const,
    () => {
      const nextRouteState = extractSheetsWorkspaceRouteState(route.query)
      if (workspaceView.value !== nextRouteState.view) {
        workspaceView.value = nextRouteState.view
      }
      if (scopeFilter.value !== nextRouteState.scope) {
        scopeFilter.value = nextRouteState.scope
      }
    },
    { immediate: true }
  )

  watch([workspaceView, scopeFilter], async ([view, scope]) => {
    const currentRouteState = extractSheetsWorkspaceRouteState(route.query)
    const nextState = {
      ...currentRouteState,
      workbookId: options.activeWorkbookId.value || currentRouteState.workbookId,
      view,
      scope
    }
    if (!hasSheetsWorkspaceRouteStateChanged(route.query, nextState)) {
      return
    }
    await router.replace({
      path: '/sheets',
      query: buildSheetsWorkspaceRouteQuery(route.query, nextState)
    })
  })
  onMounted(() => { void refreshIncomingShares() })
  async function refreshVisibleWorkbooks(canApply: () => boolean = () => true): Promise<boolean> {
    try {
      const nextWorkbooks = sortSheetsWorkbooks(await api.listWorkbooks(100))
      if (!canApply()) return false
      options.workbooks.value = nextWorkbooks
      return true
    } catch (error) {
      if (!canApply()) return false
      incomingErrorMessage.value = (error as Error).message || t('sheets.messages.shareResponseFailed')
      ElMessage.error(incomingErrorMessage.value)
      return false
    }
  }
  function syncActiveWorkbookCollaboratorCount(collaboratorCount: number): void {
    const activeWorkbook = options.activeWorkbook.value
    if (!activeWorkbook) {
      return
    }
    options.activeWorkbook.value = {
      ...activeWorkbook,
      collaboratorCount
    }
    options.workbooks.value = sortSheetsWorkbooks(options.workbooks.value.map((workbook) => {
      if (workbook.id !== activeWorkbook.id) {
        return workbook
      }
      return {
        ...workbook,
        collaboratorCount
      }
    }))
  }
  async function syncWorkspaceRouteState(
    workbookId: string | null,
    view: SheetsWorkspaceView,
    scope: SheetsScopeFilter,
  ): Promise<void> {
    const nextState = {
      ...extractSheetsWorkspaceRouteState(route.query),
      workbookId,
      view,
      scope,
    }
    if (!hasSheetsWorkspaceRouteStateChanged(route.query, nextState)) {
      return
    }
    await router.replace({
      path: '/sheets',
      query: buildSheetsWorkspaceRouteQuery(route.query, nextState),
    })
  }

  async function openSharedWorkbook(workbookId: string): Promise<boolean> {
    const selected = await options.selectWorkbook(workbookId, false)
    if (!selected) {
      return false
    }
    await syncWorkspaceRouteState(workbookId, 'WORKBOOKS', 'SHARED')
    return true
  }

  function resetVersionContext(): void {
    versionRequestId += 1
    versions.value = []
    versionsLoading.value = false
    versionsErrorMessage.value = ''
    versionMutationId.value = ''
    versionDrawerVisible.value = false
  }

  function resetShareContext(): void {
    shareRequestId += 1
    shares.value = []
    sharesLoading.value = false
    sharesErrorMessage.value = ''
    inviteEmail.value = ''
    invitePermission.value = 'VIEW'
    shareSubmitting.value = false
    shareMutationId.value = ''
  }

  function isActiveShareRequest(requestId: number, workbookId: string): boolean {
    return requestId === shareRequestId && options.activeWorkbookId.value === workbookId && canManageShares.value
  }

  function isActiveVersionRequest(requestId: number, workbookId: string): boolean {
    return requestId === versionRequestId && options.activeWorkbookId.value === workbookId && canRestoreVersions.value
  }

  function isCurrentShareContext(workbookId: string): boolean {
    return options.activeWorkbookId.value === workbookId && canManageShares.value
  }

  function isCurrentVersionContext(workbookId: string): boolean {
    return options.activeWorkbookId.value === workbookId && canRestoreVersions.value
  }
  function isActiveIncomingMutation(mutationToken: number, shareId: string): boolean {
    return mutationToken === incomingMutationToken && incomingMutationId.value === shareId
  }
  function isActiveIncomingRequest(requestId: number): boolean {
    return requestId === incomingRequestId
  }
  async function refreshShares(): Promise<boolean> {
    sharesErrorMessage.value = ''
    const workbookId = options.activeWorkbookId.value
    if (!workbookId || !canManageShares.value) {
      shares.value = []
      sharesLoading.value = false
      return true
    }
    shareRequestId += 1
    const requestId = shareRequestId
    sharesLoading.value = true
    try {
      const nextShares = await api.listShares(workbookId)
      if (!isActiveShareRequest(requestId, workbookId)) {
        return false
      }
      shares.value = nextShares
      return true
    } catch (error) {
      if (!isActiveShareRequest(requestId, workbookId)) {
        return false
      }
      sharesErrorMessage.value = (error as Error).message || t('sheets.messages.shareLoadFailed')
      ElMessage.error(sharesErrorMessage.value)
      return false
    } finally {
      if (requestId === shareRequestId) {
        sharesLoading.value = false
      }
    }
  }
  async function refreshIncomingShares(): Promise<boolean> {
    incomingRequestId += 1
    const requestId = incomingRequestId
    incomingLoading.value = true
    incomingErrorMessage.value = ''
    try {
      const nextIncomingShares = await api.listIncomingShares()
      if (!isActiveIncomingRequest(requestId)) return false
      incomingShares.value = nextIncomingShares
      return true
    } catch (error) {
      if (!isActiveIncomingRequest(requestId)) return false
      incomingErrorMessage.value = (error as Error).message || t('sheets.messages.loadIncomingSharesFailed')
      ElMessage.error(incomingErrorMessage.value)
      return false
    } finally {
      if (requestId === incomingRequestId) incomingLoading.value = false
    }
  }

  async function submitShare(): Promise<void> {
    const workbookId = options.activeWorkbookId.value
    const targetEmail = inviteEmail.value.trim()
    if (!workbookId || !targetEmail) {
      return
    }
    const permission = invitePermission.value
    shareSubmitting.value = true
    sharesErrorMessage.value = ''
    try {
      await api.createShare(workbookId, {
        targetEmail,
        permission,
      })
      if (!isCurrentShareContext(workbookId)) {
        return
      }
      if (!await refreshShares()) {
        return
      }
      if (!isCurrentShareContext(workbookId)) {
        return
      }
      inviteEmail.value = ''
      invitePermission.value = 'VIEW'
      syncActiveWorkbookCollaboratorCount(countActiveWorkbookCollaborators(shares.value))
      await options.refreshCollaboration()
      ElMessage.success(t('sheets.messages.shareCreated'))
    } catch (error) {
      sharesErrorMessage.value = (error as Error).message || t('sheets.messages.shareCreateFailed')
      ElMessage.error(sharesErrorMessage.value)
    } finally {
      if (options.activeWorkbookId.value === workbookId) {
        shareSubmitting.value = false
      }
    }
  }

  async function updateSharePermission(shareId: string, permission: 'VIEW' | 'EDIT'): Promise<void> {
    const workbookId = options.activeWorkbookId.value
    if (!workbookId) {
      return
    }
    shareMutationId.value = shareId
    sharesErrorMessage.value = ''
    try {
      await api.updateShare(workbookId, shareId, { permission })
      if (!isCurrentShareContext(workbookId)) {
        return
      }
      if (!await refreshShares()) {
        return
      }
      if (!isCurrentShareContext(workbookId)) {
        return
      }
      await options.refreshCollaboration()
      ElMessage.success(t('sheets.messages.shareUpdated'))
    } catch (error) {
      sharesErrorMessage.value = (error as Error).message || t('sheets.messages.shareUpdateFailed')
      ElMessage.error(sharesErrorMessage.value)
    } finally {
      if (shareMutationId.value === shareId) {
        shareMutationId.value = ''
      }
    }
  }

  async function removeShare(shareId: string): Promise<void> {
    const workbookId = options.activeWorkbookId.value
    if (!workbookId) {
      return
    }
    shareMutationId.value = shareId
    sharesErrorMessage.value = ''
    try {
      await api.removeShare(workbookId, shareId)
      if (!isCurrentShareContext(workbookId)) {
        return
      }
      if (!await refreshShares()) {
        return
      }
      if (!isCurrentShareContext(workbookId)) {
        return
      }
      syncActiveWorkbookCollaboratorCount(countActiveWorkbookCollaborators(shares.value))
      await options.refreshCollaboration()
      ElMessage.success(t('sheets.messages.shareRemoved'))
    } catch (error) {
      sharesErrorMessage.value = (error as Error).message || t('sheets.messages.shareRemoveFailed')
      ElMessage.error(sharesErrorMessage.value)
    } finally {
      if (shareMutationId.value === shareId) {
        shareMutationId.value = ''
      }
    }
  }

  async function respondIncomingShare(shareId: string, response: 'ACCEPT' | 'DECLINE'): Promise<void> {
    incomingMutationToken += 1
    const mutationToken = incomingMutationToken
    incomingMutationId.value = shareId
    incomingErrorMessage.value = ''
    try {
      const updated = await api.respondIncomingShare(shareId, { response })
      if (!isActiveIncomingMutation(mutationToken, shareId)) return
      if (!await refreshIncomingShares()) {
        return
      }
      if (!isActiveIncomingMutation(mutationToken, shareId)) return
      if (!await refreshVisibleWorkbooks(() => isActiveIncomingMutation(mutationToken, shareId))) {
        return
      }
      if (!isActiveIncomingMutation(mutationToken, shareId)) return
      await options.refreshCollaboration()
      if (response === 'ACCEPT') {
        await openSharedWorkbook(updated.workbookId)
      }
      if (!isActiveIncomingMutation(mutationToken, shareId)) return
      ElMessage.success(t('sheets.messages.shareResponseUpdated'))
    } catch (error) {
      if (!isActiveIncomingMutation(mutationToken, shareId)) return
      incomingErrorMessage.value = (error as Error).message || t('sheets.messages.shareResponseFailed')
      ElMessage.error(incomingErrorMessage.value)
    } finally {
      if (isActiveIncomingMutation(mutationToken, shareId)) {
        incomingMutationId.value = ''
      }
    }
  }
  async function openIncomingWorkbook(item: SheetsIncomingShare): Promise<void> {
    await openSharedWorkbook(item.workbookId)
  }
  async function openVersionHistory(): Promise<void> {
    if (!options.activeWorkbookId.value || !canRestoreVersions.value) {
      return
    }
    versionDrawerVisible.value = true
    await refreshVersions(options.activeWorkbookId.value)
  }

  async function refreshVersions(workbookId: string): Promise<boolean> {
    versionRequestId += 1
    const requestId = versionRequestId
    versionsLoading.value = true
    versionsErrorMessage.value = ''
    try {
      const nextVersions = await api.listVersions(workbookId)
      if (!isActiveVersionRequest(requestId, workbookId)) {
        return false
      }
      versions.value = nextVersions
      return true
    } catch (error) {
      if (!isActiveVersionRequest(requestId, workbookId)) {
        return false
      }
      versionsErrorMessage.value = (error as Error).message || t('sheets.messages.loadVersionHistoryFailed')
      ElMessage.error(versionsErrorMessage.value)
      return false
    } finally {
      if (requestId === versionRequestId) {
        versionsLoading.value = false
      }
    }
  }

  async function restoreVersion(versionId: string): Promise<void> {
    const workbookId = options.activeWorkbookId.value
    if (!workbookId) {
      return
    }
    if (!await options.confirmDiscardChangesIfNeeded()) {
      return
    }
    versionMutationId.value = versionId
    versionsErrorMessage.value = ''
    try {
      await api.restoreVersion(workbookId, versionId)
      if (!isCurrentVersionContext(workbookId)) {
        return
      }
      if (!await options.selectWorkbook(workbookId, false, { skipDiscardConfirm: true })) {
        return
      }
      if (!isCurrentVersionContext(workbookId)) {
        return
      }
      if (!await refreshVersions(workbookId)) {
        return
      }
      if (!isCurrentVersionContext(workbookId)) {
        return
      }
      await options.refreshCollaboration()
      ElMessage.success(t('sheets.messages.versionRestored'))
    } catch (error) {
      versionsErrorMessage.value = (error as Error).message || t('sheets.messages.versionRestoreFailed')
      ElMessage.error(versionsErrorMessage.value)
    } finally {
      if (versionMutationId.value === versionId) {
        versionMutationId.value = ''
      }
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
    sharesErrorMessage,
    incomingErrorMessage,
    versionsErrorMessage,
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
