import type { SupportedLocale } from '~/constants/i18n'
import type {
  SuiteNotificationCenter,
  SuiteNotificationOperationHistory,
  SuiteNotificationOperationHistoryItem,
  SuiteNotificationSyncEvent
} from '~/types/api'
import type { NotificationSyncStatus } from '~/composables/useNotificationSyncStream'

export type NotificationSeverityFilter = 'ALL' | 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'
export type NotificationChannelFilter = 'ALL' | 'SECURITY' | 'GOVERNANCE' | 'READINESS' | 'ACTION'
export type NotificationWorkflowFilter = 'ACTIVE' | 'ARCHIVED' | 'IGNORED' | 'SNOOZED' | 'ALL'
export type NotificationTranslator = (key: string, params?: Record<string, string | number>) => string

export interface NotificationOption<T extends string> {
  label: string
  value: T
}

const NOTIFICATION_DATE_TIME_OPTIONS: Intl.DateTimeFormatOptions = {
  dateStyle: 'medium',
  timeStyle: 'short'
}

const NOTIFICATION_SEVERITY_KEYS: Record<NotificationSeverityFilter, string> = {
  ALL: 'notifications.filters.severity.ALL',
  CRITICAL: 'notifications.filters.severity.CRITICAL',
  HIGH: 'notifications.filters.severity.HIGH',
  MEDIUM: 'notifications.filters.severity.MEDIUM',
  LOW: 'notifications.filters.severity.LOW'
}

const NOTIFICATION_CHANNEL_KEYS: Record<NotificationChannelFilter, string> = {
  ALL: 'notifications.filters.channel.ALL',
  SECURITY: 'notifications.filters.channel.SECURITY',
  GOVERNANCE: 'notifications.filters.channel.GOVERNANCE',
  READINESS: 'notifications.filters.channel.READINESS',
  ACTION: 'notifications.filters.channel.ACTION'
}

const NOTIFICATION_WORKFLOW_KEYS: Record<NotificationWorkflowFilter, string> = {
  ACTIVE: 'notifications.filters.workflow.ACTIVE',
  ARCHIVED: 'notifications.filters.workflow.ARCHIVED',
  IGNORED: 'notifications.filters.workflow.IGNORED',
  SNOOZED: 'notifications.filters.workflow.SNOOZED',
  ALL: 'notifications.filters.workflow.ALL'
}

export function buildNotificationSeverityOptions(
  t?: NotificationTranslator
): NotificationOption<NotificationSeverityFilter>[] {
  return Object.entries(NOTIFICATION_SEVERITY_KEYS).map(([value, key]) => ({
    value: value as NotificationSeverityFilter,
    label: t ? t(key) : key
  }))
}

export function buildNotificationChannelOptions(
  t?: NotificationTranslator
): NotificationOption<NotificationChannelFilter>[] {
  return Object.entries(NOTIFICATION_CHANNEL_KEYS).map(([value, key]) => ({
    value: value as NotificationChannelFilter,
    label: t ? t(key) : key
  }))
}

export function buildNotificationWorkflowOptions(
  t?: NotificationTranslator
): NotificationOption<NotificationWorkflowFilter>[] {
  return Object.entries(NOTIFICATION_WORKFLOW_KEYS).map(([value, key]) => ({
    value: value as NotificationWorkflowFilter,
    label: t ? t(key) : key
  }))
}

export function formatNotificationDateTime(
  value: string | null | undefined,
  locale: SupportedLocale = 'en'
): string {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat(locale, NOTIFICATION_DATE_TIME_OPTIONS).format(date)
}

export function buildNotificationSummary(
  notificationCenter: SuiteNotificationCenter | null,
  workflowFilter: NotificationWorkflowFilter,
  selectedCount: number,
  t?: NotificationTranslator
): string {
  if (!notificationCenter) {
    return t ? t('notifications.summary.loading') : 'Loading notifications...'
  }
  const workflow = t
    ? t(NOTIFICATION_WORKFLOW_KEYS[workflowFilter])
    : workflowFilter
  return t
    ? t('notifications.summary.loaded', {
      workflow,
      unread: notificationCenter.unreadCount,
      critical: notificationCenter.criticalCount,
      selected: selectedCount
    })
    : `View ${workflow} · Unread ${notificationCenter.unreadCount} · Critical ${notificationCenter.criticalCount} · Selected ${selectedCount}`
}

export function buildOperationHistorySummary(
  operationHistory: SuiteNotificationOperationHistory | null,
  t?: NotificationTranslator
): string {
  if (!operationHistory) {
    return t ? t('notifications.history.loadingSummary') : 'Loading operation history...'
  }
  return t
    ? t('notifications.history.summary', { count: operationHistory.total })
    : `Latest ${operationHistory.total} operations`
}

export function buildSyncStatusLabel(
  status: NotificationSyncStatus,
  t?: NotificationTranslator
): string {
  const key = `notifications.sync.status.${status}`
  return t ? t(key) : status
}

export function buildSyncStatusText(
  status: NotificationSyncStatus,
  errorMessage: string,
  t?: NotificationTranslator
): string {
  if (errorMessage) {
    return errorMessage
  }
  const key = `notifications.sync.text.${status}`
  return t ? t(key) : status
}

export function buildLatestSyncSummary(
  latestSyncEvent: SuiteNotificationSyncEvent | null,
  currentSessionId: string,
  t?: NotificationTranslator
): string {
  if (!latestSyncEvent) {
    return t ? t('notifications.sync.eventNone') : 'No sync event yet'
  }
  const sourceKey = latestSyncEvent.sessionId && latestSyncEvent.sessionId !== currentSessionId
    ? 'notifications.sync.source.otherSession'
    : 'notifications.sync.source.thisSession'
  const source = t ? t(sourceKey) : sourceKey
  return t
    ? t('notifications.sync.latestSummary', {
      operation: translateNotificationOperation(latestSyncEvent.operation, t),
      requested: latestSyncEvent.requestedCount,
      affected: latestSyncEvent.affectedCount,
      source
    })
    : `${latestSyncEvent.operation} · Requested ${latestSyncEvent.requestedCount} · Affected ${latestSyncEvent.affectedCount} · ${source}`
}

export function severityTagType(
  severity: string
): 'danger' | 'warning' | 'success' | 'info' {
  if (severity === 'CRITICAL' || severity === 'HIGH') {
    return 'danger'
  }
  if (severity === 'MEDIUM') {
    return 'warning'
  }
  if (severity === 'LOW') {
    return 'success'
  }
  return 'info'
}

export function workflowTagType(
  status: string
): 'success' | 'info' | 'warning' | 'danger' {
  if (status === 'ACTIVE') {
    return 'success'
  }
  if (status === 'SNOOZED') {
    return 'warning'
  }
  if (status === 'ARCHIVED') {
    return 'info'
  }
  return 'danger'
}

export function operationTagType(
  operation: string
): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  if (operation === 'RESTORE') {
    return 'success'
  }
  if (operation === 'ARCHIVE' || operation === 'UNDO') {
    return 'primary'
  }
  if (operation === 'SNOOZE' || operation === 'ASSIGN') {
    return 'warning'
  }
  if (operation === 'IGNORE') {
    return 'danger'
  }
  return 'info'
}

export function translateNotificationSeverity(
  severity: string,
  t?: NotificationTranslator
): string {
  const key = `notifications.severity.${severity}`
  return t ? t(key) : severity
}

export function translateNotificationChannel(
  channel: string,
  t?: NotificationTranslator
): string {
  const key = `notifications.channel.${channel}`
  return t ? t(key) : channel
}

export function translateNotificationWorkflow(
  workflow: string,
  t?: NotificationTranslator
): string {
  const key = `notifications.workflow.${workflow}`
  return t ? t(key) : workflow
}

export function translateNotificationOperation(
  operation: string,
  t?: NotificationTranslator
): string {
  const key = `notifications.operation.${operation}`
  return t ? t(key) : operation
}

export function translateNotificationState(
  read: boolean,
  t?: NotificationTranslator
): string {
  return t
    ? t(read ? 'notifications.table.state.read' : 'notifications.table.state.unread')
    : read ? 'Read' : 'Unread'
}

export function buildOperationHistoryLine(
  item: SuiteNotificationOperationHistoryItem,
  t?: NotificationTranslator
): string {
  return t
    ? t('notifications.history.requestedAffected', {
      requested: item.requestedCount,
      affected: item.affectedCount
    })
    : `Requested ${item.requestedCount} · Affected ${item.affectedCount}`
}
