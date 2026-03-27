import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import { useNotificationSyncStream } from '~/composables/useNotificationSyncStream'
import { useSuiteApi } from '~/composables/useSuiteApi'
import { useAuthStore } from '~/stores/auth'
import { useOrgAccessStore } from '~/stores/org-access'
import type {
  SuiteNotificationCenter,
  SuiteNotificationItem,
  SuiteNotificationMarkReadResult,
  SuiteNotificationOperationHistory,
  SuiteNotificationOperationHistoryItem,
  SuiteNotificationSync,
  SuiteNotificationSyncEvent,
  SuiteNotificationWorkflowResult
} from '~/types/api'
import { resolveSessionIdFromAccessToken } from '~/utils/auth-session'
import {
  buildNotificationSummary,
  translateNotificationOperation,
  type NotificationWorkflowFilter
} from '~/utils/notification-center'
import { filterSuiteNotificationCenterByAccess } from '~/utils/org-product-surface-filter'

const NOTIFICATION_LIMIT = 60
const OPERATION_HISTORY_LIMIT = 20
const ONE_HOUR_MS = 60 * 60 * 1000

interface LoadDataOptions {
  preserveSelection?: boolean
}

export function useNotificationsWorkspace() {
  const authStore = useAuthStore()
  const orgAccessStore = useOrgAccessStore()
  const { t } = useI18n()
  const {
    getNotificationCenter,
    getNotificationOperationHistory,
    executeRemediationAction,
    batchExecuteRemediationActions,
    markNotificationsRead,
    markAllNotificationsRead,
    archiveNotifications,
    ignoreNotifications,
    restoreNotifications,
    snoozeNotifications,
    assignNotifications,
    undoNotificationWorkflow
  } = useSuiteApi()

  const loading = ref(false)
  const unreadOnly = ref(false)
  const includeSnoozed = ref(false)
  const executingActionId = ref('')
  const workflowOperating = ref(false)
  const severityFilter = ref<'ALL' | 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'>('ALL')
  const channelFilter = ref<'ALL' | 'SECURITY' | 'GOVERNANCE' | 'READINESS' | 'ACTION'>('ALL')
  const workflowFilter = ref<NotificationWorkflowFilter>('ACTIVE')
  const assigneeUserIdInput = ref<number | null>(null)
  const assigneeDisplayNameInput = ref('')
  const currentSessionId = ref('')
  const lastWorkflowOperationId = ref('')
  const notificationCenter = ref<SuiteNotificationCenter | null>(null)
  const operationHistory = ref<SuiteNotificationOperationHistory | null>(null)
  const historyUndoOperatingId = ref('')
  const selectedItems = ref<SuiteNotificationItem[]>([])
  const tableRef = ref<{
    clearSelection: () => void
    toggleRowSelection: (row: SuiteNotificationItem, selected?: boolean) => void
  } | null>(null)
  const syncCursor = ref(0)
  const syncVersion = ref('NTF-0')
  const syncConflictMessage = ref('')
  const lastSyncedAt = ref('')
  const latestSyncEvent = ref<SuiteNotificationSyncEvent | null>(null)
  const syncUpdateCount = ref(0)

  const {
    status: syncStatus,
    errorMessage: syncErrorMessage,
    connect: connectSync,
    reconnect: reconnectSync
  } = useNotificationSyncStream({
    onPayload: handleSyncPayload,
    messages: {
      missingAccessToken: () => t('notifications.messages.syncMissingAccessToken'),
      httpError: status => t('notifications.messages.syncHttpError', { status }),
      emptyBody: () => t('notifications.messages.syncEmptyBody'),
      streamClosed: () => t('notifications.messages.syncClosed'),
      defaultFailure: () => t('notifications.messages.syncFailed')
    }
  })

  const visibleNotificationCenter = computed(() => filterSuiteNotificationCenterByAccess(
    notificationCenter.value,
    orgAccessStore.isProductEnabled
  ))

  const selectedNotificationIds = computed(() => selectedItems.value.map(item => item.notificationId))
  const selectedActionCodes = computed(() => {
    const codes = new Set<string>()
    for (const item of selectedItems.value) {
      if (item.actionCode) {
        codes.add(item.actionCode)
      }
    }
    return Array.from(codes)
  })

  const filteredItems = computed(() => {
    const items = visibleNotificationCenter.value?.items || []
    return items.filter((item) => {
      const matchSeverity = severityFilter.value === 'ALL' || item.severity === severityFilter.value
      const matchChannel = channelFilter.value === 'ALL' || item.channel === channelFilter.value
      return matchSeverity && matchChannel
    })
  })

  const summaryText = computed(() => buildNotificationSummary(
    visibleNotificationCenter.value,
    workflowFilter.value,
    selectedItems.value.length,
    t
  ))

  const syncStatusTagType = computed(() => {
    if (syncStatus.value === 'CONNECTED') {
      return 'success'
    }
    if (syncStatus.value === 'RECONNECTING' || syncStatus.value === 'CONNECTING') {
      return 'warning'
    }
    return 'info'
  })

  watch([unreadOnly, includeSnoozed, workflowFilter], () => {
    void loadData()
  })

  watch(
    () => authStore.accessToken,
    (token) => {
      currentSessionId.value = resolveSessionIdFromAccessToken(token)
    },
    { immediate: true }
  )

  watch(
    () => orgAccessStore.activeOrgId,
    (nextOrgId, previousOrgId) => {
      if (nextOrgId === previousOrgId) {
        return
      }
      clearSelection()
      latestSyncEvent.value = null
      syncUpdateCount.value = 0
      resetSyncConflict()
      void loadData()
      reconnectSync()
    }
  )

  onMounted(async () => {
    await loadData()
    await connectSync(syncCursor.value || undefined)
  })

  function onSelectionChange(items: SuiteNotificationItem[]): void {
    selectedItems.value = items
  }

  function ensureHasSelection(): boolean {
    if (selectedNotificationIds.value.length > 0) {
      return true
    }
    ElMessage.warning(t('notifications.messages.selectFirst'))
    return false
  }

  function buildSnoozeUntil(hours: number): string {
    const date = new Date(Date.now() + hours * ONE_HOUR_MS)
    return date.toISOString().slice(0, 19)
  }

  function applySyncMetadata(cursor: number, version: string, generatedAt: string): void {
    syncCursor.value = cursor
    syncVersion.value = version
    lastSyncedAt.value = generatedAt
  }

  function rememberMutationResult(result: SuiteNotificationWorkflowResult | SuiteNotificationMarkReadResult): void {
    syncCursor.value = result.syncCursor
    syncVersion.value = result.syncVersion
    lastSyncedAt.value = result.executedAt
    if ('operationId' in result && result.operationId) {
      lastWorkflowOperationId.value = result.operationId
    }
  }

  function clearSelection(): void {
    selectedItems.value = []
    tableRef.value?.clearSelection()
  }

  function resetSyncConflict(): void {
    syncConflictMessage.value = ''
  }

  async function loadData(options: LoadDataOptions = {}): Promise<void> {
    const selectedIdSet = options.preserveSelection ? new Set(selectedNotificationIds.value) : new Set<string>()
    loading.value = true
    try {
      const [center, history] = await Promise.all([
        getNotificationCenter(NOTIFICATION_LIMIT, unreadOnly.value, workflowFilter.value, includeSnoozed.value),
        getNotificationOperationHistory(OPERATION_HISTORY_LIMIT)
      ])
      notificationCenter.value = center
      operationHistory.value = history
      applySyncMetadata(center.syncCursor, center.syncVersion, center.generatedAt)
      const scopedCenter = filterSuiteNotificationCenterByAccess(center, orgAccessStore.isProductEnabled)
      await restoreSelection(selectedIdSet, scopedCenter?.items ?? [])
    } finally {
      loading.value = false
    }
  }

  async function restoreSelection(selectedIdSet: Set<string>, items: SuiteNotificationItem[]): Promise<void> {
    clearSelection()
    if (selectedIdSet.size === 0) {
      return
    }
    const nextSelectedItems = items.filter(item => selectedIdSet.has(item.notificationId))
    selectedItems.value = nextSelectedItems
    await nextTick()
    for (const item of nextSelectedItems) {
      tableRef.value?.toggleRowSelection(item, true)
    }
  }

  async function handleSyncPayload(payload: SuiteNotificationSync): Promise<void> {
    applySyncMetadata(payload.syncCursor, payload.syncVersion, payload.generatedAt)
    if (payload.items.length === 0) {
      return
    }
    latestSyncEvent.value = payload.items[payload.items.length - 1]
    syncUpdateCount.value += payload.items.length
    if (payload.items.some(item => isExternalSession(item))) {
      syncConflictMessage.value = t('notifications.sync.conflict')
    }
    await loadData({ preserveSelection: true })
  }

  function isExternalSession(event: SuiteNotificationSyncEvent): boolean {
    if (!event.sessionId || !currentSessionId.value) {
      return false
    }
    return event.sessionId !== currentSessionId.value
  }

  async function refreshNotifications(): Promise<void> {
    resetSyncConflict()
    await loadData()
  }

  async function openRoute(item: SuiteNotificationItem): Promise<void> {
    if (!item.routePath) {
      return
    }
    await navigateTo(item.routePath)
  }

  async function runAction(item: SuiteNotificationItem): Promise<void> {
    if (!item.actionCode) {
      ElMessage.info(t('notifications.messages.noDirectAction'))
      return
    }
    executingActionId.value = item.notificationId
    try {
      const result = await executeRemediationAction(item.actionCode)
      ElMessage.success(result.message)
      await loadData({ preserveSelection: true })
    } finally {
      executingActionId.value = ''
    }
  }

  async function markSelectedRead(): Promise<void> {
    if (!ensureHasSelection()) {
      return
    }
    const result = await markNotificationsRead({ notificationIds: selectedNotificationIds.value })
    rememberMutationResult(result)
    ElMessage.success(t('notifications.messages.markSelectedReadSuccess', {
      affected: result.affectedCount,
      requested: result.requestedCount
    }))
    await loadData()
  }

  async function markEverythingRead(): Promise<void> {
    const result = await markAllNotificationsRead()
    rememberMutationResult(result)
    ElMessage.success(t('notifications.messages.markAllReadSuccess', {
      affected: result.affectedCount
    }))
    await loadData()
  }

  async function confirmDangerousWorkflowAction(operation: string): Promise<boolean> {
    try {
      await ElMessageBox.confirm(
        t('notifications.messages.confirmWorkflowMessage', {
          action: translateNotificationOperation(operation, t),
          count: selectedNotificationIds.value.length
        }),
        t('notifications.messages.confirmWorkflowTitle'),
        {
          type: 'warning',
          confirmButtonText: t('common.actions.confirm'),
          cancelButtonText: t('common.actions.cancel')
        }
      )
      return true
    } catch {
      return false
    }
  }

  async function archiveSelected(): Promise<void> {
    if (!ensureHasSelection() || !(await confirmDangerousWorkflowAction('ARCHIVE'))) {
      return
    }
    workflowOperating.value = true
    try {
      const result = await archiveNotifications({ notificationIds: selectedNotificationIds.value })
      rememberMutationResult(result)
      ElMessage.success(t('notifications.messages.archiveSuccess', {
        affected: result.affectedCount,
        requested: result.requestedCount
      }))
      await loadData()
    } finally {
      workflowOperating.value = false
    }
  }

  async function ignoreSelected(): Promise<void> {
    if (!ensureHasSelection() || !(await confirmDangerousWorkflowAction('IGNORE'))) {
      return
    }
    workflowOperating.value = true
    try {
      const result = await ignoreNotifications({ notificationIds: selectedNotificationIds.value })
      rememberMutationResult(result)
      ElMessage.success(t('notifications.messages.ignoreSuccess', {
        affected: result.affectedCount,
        requested: result.requestedCount
      }))
      await loadData()
    } finally {
      workflowOperating.value = false
    }
  }

  async function restoreSelected(): Promise<void> {
    if (!ensureHasSelection()) {
      return
    }
    workflowOperating.value = true
    try {
      const result = await restoreNotifications({ notificationIds: selectedNotificationIds.value })
      rememberMutationResult(result)
      ElMessage.success(t('notifications.messages.restoreSuccess', {
        affected: result.affectedCount,
        requested: result.requestedCount
      }))
      await loadData()
    } finally {
      workflowOperating.value = false
    }
  }

  async function snoozeSelected(hours: number): Promise<void> {
    if (!ensureHasSelection()) {
      return
    }
    workflowOperating.value = true
    try {
      const result = await snoozeNotifications({
        notificationIds: selectedNotificationIds.value,
        snoozedUntil: buildSnoozeUntil(hours)
      })
      rememberMutationResult(result)
      ElMessage.success(t('notifications.messages.snoozeSuccess', {
        affected: result.affectedCount,
        requested: result.requestedCount,
        hours
      }))
      await loadData()
    } finally {
      workflowOperating.value = false
    }
  }

  async function assignSelected(): Promise<void> {
    if (!ensureHasSelection()) {
      return
    }
    if (!assigneeUserIdInput.value || !assigneeDisplayNameInput.value.trim()) {
      ElMessage.warning(t('notifications.messages.assigneeRequired'))
      return
    }
    workflowOperating.value = true
    try {
      const result = await assignNotifications({
        notificationIds: selectedNotificationIds.value,
        assigneeUserId: assigneeUserIdInput.value,
        assigneeDisplayName: assigneeDisplayNameInput.value.trim()
      })
      rememberMutationResult(result)
      ElMessage.success(t('notifications.messages.assignSuccess', {
        affected: result.affectedCount,
        requested: result.requestedCount
      }))
      await loadData()
    } finally {
      workflowOperating.value = false
    }
  }

  async function undoOperationByOperationId(operationId: string): Promise<void> {
    workflowOperating.value = true
    historyUndoOperatingId.value = operationId
    try {
      const result = await undoNotificationWorkflow({ operationId })
      rememberMutationResult(result)
      ElMessage.success(t('notifications.messages.undoSuccess', {
        affected: result.affectedCount,
        requested: result.requestedCount
      }))
      if (lastWorkflowOperationId.value === operationId) {
        lastWorkflowOperationId.value = ''
      }
      await loadData()
    } finally {
      historyUndoOperatingId.value = ''
      workflowOperating.value = false
    }
  }

  async function undoLastWorkflowOperation(): Promise<void> {
    if (!lastWorkflowOperationId.value) {
      ElMessage.warning(t('notifications.messages.noUndoAvailable'))
      return
    }
    await undoOperationByOperationId(lastWorkflowOperationId.value)
  }

  async function undoHistoryOperation(item: SuiteNotificationOperationHistoryItem): Promise<void> {
    if (!item.undoAvailable) {
      ElMessage.warning(t('notifications.messages.alreadyUndone'))
      return
    }
    await undoOperationByOperationId(item.operationId)
  }

  async function runSelectedActions(): Promise<void> {
    if (selectedActionCodes.value.length === 0) {
      ElMessage.warning(t('notifications.messages.noRunnableActions'))
      return
    }
    const result = await batchExecuteRemediationActions({ actionCodes: selectedActionCodes.value })
    ElMessage.success(t('notifications.messages.batchActionDone', {
      success: result.successCount,
      failed: result.failedCount
    }))
    await loadData({ preserveSelection: true })
  }

  return {
    assigneeDisplayNameInput, assigneeUserIdInput, archiveSelected, assignSelected,
    channelFilter, currentSessionId, executingActionId, filteredItems,
    historyUndoOperatingId, ignoreSelected, includeSnoozed, lastSyncedAt,
    lastWorkflowOperationId, latestSyncEvent, loading, markEverythingRead,
    markSelectedRead, onSelectionChange, openRoute, operationHistory,
    refreshNotifications, reconnectSync, restoreSelected, runAction,
    runSelectedActions, selectedActionCodes, selectedItems, severityFilter,
    snoozeSelected, summaryText, syncConflictMessage, syncCursor,
    syncErrorMessage, syncStatus, syncStatusTagType, syncUpdateCount,
    syncVersion, tableRef, unreadOnly, undoHistoryOperation,
    undoLastWorkflowOperation, workflowFilter, workflowOperating
  }
}
