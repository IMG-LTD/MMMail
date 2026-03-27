import { describe, expect, it } from 'vitest'
import {
  buildLatestSyncSummary,
  buildNotificationChannelOptions,
  buildNotificationSeverityOptions,
  buildNotificationSummary,
  buildNotificationWorkflowOptions,
  buildOperationHistorySummary,
  buildSyncStatusLabel,
  buildSyncStatusText,
  operationTagType,
  severityTagType,
  translateNotificationChannel,
  translateNotificationOperation,
  translateNotificationSeverity,
  translateNotificationState,
  translateNotificationWorkflow,
  workflowTagType
} from '../utils/notification-center'

describe('notification center utils', () => {
  const tMock = (key: string, params?: Record<string, string | number>) => {
    if (key === 'notifications.summary.loading') {
      return 'loading'
    }
    if (key === 'notifications.summary.loaded') {
      return `view:${params?.workflow}|unread:${params?.unread}|critical:${params?.critical}|selected:${params?.selected}`
    }
    if (key === 'notifications.history.loadingSummary') {
      return 'history-loading'
    }
    if (key === 'notifications.history.summary') {
      return `history:${params?.count}`
    }
    if (key === 'notifications.sync.eventNone') {
      return 'none'
    }
    if (key === 'notifications.sync.latestSummary') {
      return `latest:${params?.operation}|${params?.requested}|${params?.affected}|${params?.source}`
    }
    return `t:${key}`
  }

  it('builds filter options and summaries', () => {
    expect(buildNotificationSeverityOptions(tMock)[0]).toEqual({ value: 'ALL', label: 't:notifications.filters.severity.ALL' })
    expect(buildNotificationChannelOptions(tMock)[1]).toEqual({ value: 'SECURITY', label: 't:notifications.filters.channel.SECURITY' })
    expect(buildNotificationWorkflowOptions(tMock)[0]).toEqual({ value: 'ACTIVE', label: 't:notifications.filters.workflow.ACTIVE' })
    expect(buildNotificationSummary(null, 'ACTIVE', 0, tMock)).toBe('loading')
    expect(buildNotificationSummary({
      generatedAt: '',
      limit: 60,
      total: 5,
      criticalCount: 2,
      unreadCount: 3,
      syncCursor: 1,
      syncVersion: 'v1',
      items: []
    }, 'ACTIVE', 4, tMock)).toBe('view:t:notifications.filters.workflow.ACTIVE|unread:3|critical:2|selected:4')
    expect(buildOperationHistorySummary(null, tMock)).toBe('history-loading')
    expect(buildOperationHistorySummary({
      generatedAt: '',
      limit: 20,
      total: 7,
      syncCursor: 1,
      syncVersion: 'v1',
      items: []
    }, tMock)).toBe('history:7')
  })

  it('translates states and tags', () => {
    expect(buildSyncStatusLabel('CONNECTED', tMock)).toBe('t:notifications.sync.status.CONNECTED')
    expect(buildSyncStatusText('CONNECTED', '', tMock)).toBe('t:notifications.sync.text.CONNECTED')
    expect(buildSyncStatusText('ERROR', 'stream failed', tMock)).toBe('stream failed')
    expect(translateNotificationSeverity('CRITICAL', tMock)).toBe('t:notifications.severity.CRITICAL')
    expect(translateNotificationChannel('SECURITY', tMock)).toBe('t:notifications.channel.SECURITY')
    expect(translateNotificationWorkflow('ACTIVE', tMock)).toBe('t:notifications.workflow.ACTIVE')
    expect(translateNotificationOperation('ARCHIVE', tMock)).toBe('t:notifications.operation.ARCHIVE')
    expect(translateNotificationState(true, tMock)).toBe('t:notifications.table.state.read')
    expect(severityTagType('HIGH')).toBe('danger')
    expect(workflowTagType('SNOOZED')).toBe('warning')
    expect(operationTagType('IGNORE')).toBe('danger')
  })

  it('builds sync summary with session awareness', () => {
    expect(buildLatestSyncSummary(null, 'session-a', tMock)).toBe('none')
    expect(buildLatestSyncSummary({
      eventId: 1,
      eventType: 'UPDATED',
      operation: 'ARCHIVE',
      operationId: 'op-1',
      requestedCount: 4,
      affectedCount: 3,
      sessionId: 'session-b',
      createdAt: '2026-03-12T10:00:00'
    }, 'session-a', tMock)).toBe('latest:t:notifications.operation.ARCHIVE|4|3|t:notifications.sync.source.otherSession')
  })
})
