import type { SupportedLocale } from '../constants/i18n'
import type {
  SuiteGovernanceChangeRequest,
  SuiteGovernanceOverview,
  SuiteGovernanceRequestStatus,
  SuiteGovernanceReviewStage,
  SuiteRemediationAction,
  SuiteRemediationExecutionResult,
  SuiteRiskLevel
} from '../types/api'

export interface GovernanceOverviewCard {
  key: string
  label: string
  value: number
  tone: 'neutral' | 'warning' | 'danger' | 'success'
}

export type SuiteTranslate = (key: string, params?: Record<string, string | number>) => string

export function priorityOrder(priority: SuiteRemediationAction['priority']): number {
  if (priority === 'P0') {
    return 0
  }
  if (priority === 'P1') {
    return 1
  }
  return 2
}

export function buildGovernanceOverviewCards(
  overview: SuiteGovernanceOverview | null,
  t: SuiteTranslate
): GovernanceOverviewCard[] {
  if (!overview) {
    return []
  }
  return [
    { key: 'total', label: t('suite.operations.governance.overview.totalRequests'), value: overview.totalRequests, tone: 'neutral' },
    { key: 'pending-review', label: t('suite.operations.governance.overview.pendingReview'), value: overview.pendingReviewCount, tone: 'warning' },
    { key: 'pending-second', label: t('suite.operations.governance.overview.pendingSecondReview'), value: overview.pendingSecondReviewCount, tone: 'warning' },
    { key: 'pending-exec', label: t('suite.operations.governance.overview.pendingExecution'), value: overview.approvedPendingExecutionCount, tone: 'warning' },
    {
      key: 'executed',
      label: t('suite.operations.governance.overview.executed'),
      value: overview.executedCount + overview.executedWithFailureCount,
      tone: 'success'
    },
    { key: 'rejected', label: t('suite.operations.governance.overview.rejected'), value: overview.rejectedCount, tone: 'neutral' },
    {
      key: 'rolled-back',
      label: t('suite.operations.governance.overview.rolledBack'),
      value: overview.rolledBackCount + overview.rollbackWithFailureCount,
      tone: 'neutral'
    },
    { key: 'sla-breached', label: t('suite.operations.governance.overview.slaBreached'), value: overview.slaBreachedCount, tone: 'danger' }
  ]
}

export function riskTagType(riskLevel: SuiteRiskLevel): 'success' | 'warning' | 'danger' | 'info' {
  if (riskLevel === 'LOW') {
    return 'success'
  }
  if (riskLevel === 'MEDIUM') {
    return 'warning'
  }
  if (riskLevel === 'HIGH' || riskLevel === 'CRITICAL') {
    return 'danger'
  }
  return 'info'
}

export function riskLevelLabel(riskLevel: SuiteRiskLevel, t: SuiteTranslate): string {
  return t(`suite.operations.riskLevel.${riskLevel}`)
}

export function governanceOverviewCardClass(tone: GovernanceOverviewCard['tone']): string {
  if (tone === 'danger') {
    return 'danger'
  }
  if (tone === 'warning') {
    return 'warning'
  }
  if (tone === 'success') {
    return 'success'
  }
  return ''
}

export function governanceStatusTagType(
  status: SuiteGovernanceRequestStatus
): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'EXECUTED' || status === 'ROLLED_BACK') {
    return 'success'
  }
  if (status === 'PENDING_REVIEW' || status === 'PENDING_SECOND_REVIEW' || status === 'APPROVED_PENDING_EXECUTION') {
    return 'warning'
  }
  if (status === 'EXECUTED_WITH_FAILURE' || status === 'ROLLBACK_WITH_FAILURE') {
    return 'danger'
  }
  return 'info'
}

export function governanceStatusLabel(
  status: SuiteGovernanceRequestStatus,
  t: SuiteTranslate
): string {
  return t(`suite.operations.governance.status.${status}`)
}

export function canExecuteAction(action: SuiteRemediationAction): boolean {
  return Boolean(action.actionCode)
}

export function formatDateTime(value: string | null, locale: SupportedLocale = 'en'): string {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString(locale)
}

export function summarizeExecution(
  results: SuiteRemediationExecutionResult[],
  t: SuiteTranslate
): string {
  if (!results.length) {
    return '-'
  }
  const successCount = results.filter(item => item.status === 'SUCCESS').length
  const noOpCount = results.filter(item => item.status === 'NO_OP').length
  const failedCount = results.filter(item => item.status === 'FAILED').length
  return [
    `${t('suite.operations.remediation.status.SUCCESS')} ${successCount}`,
    `${t('suite.operations.remediation.status.NO_OP')} ${noOpCount}`,
    `${t('suite.operations.remediation.status.FAILED')} ${failedCount}`
  ].join(' / ')
}

export function remediationExecutionStatusLabel(
  status: SuiteRemediationExecutionResult['status'],
  t: SuiteTranslate
): string {
  return t(`suite.operations.remediation.status.${status}`)
}

export function formatActorUserId(userId: string | null): string {
  return userId ? String(userId) : '-'
}

export function formatActorSessionId(sessionId: string | null): string {
  return sessionId ? String(sessionId) : '-'
}

export function governanceSlaTagType(
  request: SuiteGovernanceChangeRequest
): 'danger' | 'warning' | 'success' | 'info' {
  if (!request.reviewDueAt) {
    return 'info'
  }
  if (request.reviewSlaBreached) {
    return 'danger'
  }
  if (request.status === 'PENDING_REVIEW' || request.status === 'PENDING_SECOND_REVIEW') {
    return 'warning'
  }
  return 'success'
}

export function governanceSlaLabel(
  request: SuiteGovernanceChangeRequest,
  t: SuiteTranslate
): string {
  if (!request.reviewDueAt) {
    return t('suite.operations.governance.sla.NA')
  }
  return request.reviewSlaBreached
    ? t('suite.operations.governance.sla.BREACHED')
    : t('suite.operations.governance.sla.ON_TIME')
}

export function canReviewRequest(request: SuiteGovernanceChangeRequest): boolean {
  return request.status === 'PENDING_REVIEW' || request.status === 'PENDING_SECOND_REVIEW'
}

export function reviewStageTagType(stage: SuiteGovernanceReviewStage): 'info' | 'warning' | 'success' {
  if (stage === 'FIRST_REVIEW_PENDING' || stage === 'SECOND_REVIEW_PENDING') {
    return 'warning'
  }
  if (stage === 'REVIEW_COMPLETED') {
    return 'success'
  }
  return 'info'
}

export function reviewStageLabel(stage: SuiteGovernanceReviewStage, t: SuiteTranslate): string {
  if (stage === 'FIRST_REVIEW_PENDING') {
    return t('suite.operations.governance.reviewStage.FIRST_REVIEW_PENDING')
  }
  if (stage === 'SECOND_REVIEW_PENDING') {
    return t('suite.operations.governance.reviewStage.SECOND_REVIEW_PENDING')
  }
  if (stage === 'REVIEW_COMPLETED') {
    return t('suite.operations.governance.reviewStage.REVIEW_COMPLETED')
  }
  return t('suite.operations.governance.reviewStage.SINGLE_REVIEW')
}

export function commandItemTypeLabel(itemType: string, t: SuiteTranslate): string {
  return t(`suite.operations.command.itemType.${itemType}`)
}

export function formatCommandUpdatedAt(updatedAt: string | null, locale: SupportedLocale = 'en'): string {
  return formatDateTime(updatedAt, locale)
}

export function isReviewedByCurrentSession(
  request: SuiteGovernanceChangeRequest,
  currentSessionId: string
): boolean {
  const reviewedBySessionId = request.reviewedBySessionId ? String(request.reviewedBySessionId) : ''
  return Boolean(currentSessionId && reviewedBySessionId && reviewedBySessionId === currentSessionId)
}

export function canExecuteGovernanceRequest(
  request: SuiteGovernanceChangeRequest,
  currentSessionId: string
): boolean {
  if (request.status !== 'APPROVED_PENDING_EXECUTION') {
    return false
  }
  return !isReviewedByCurrentSession(request, currentSessionId)
}

export function canRollbackRequest(request: SuiteGovernanceChangeRequest): boolean {
  return request.status === 'EXECUTED' || request.status === 'EXECUTED_WITH_FAILURE'
}

export function isGovernanceActionLoading(
  requestId: string,
  actionType: 'REVIEW_APPROVE' | 'REVIEW_REJECT' | 'EXECUTE' | 'ROLLBACK',
  loadingRequestId: string,
  loadingActionType: 'REVIEW_APPROVE' | 'REVIEW_REJECT' | 'EXECUTE' | 'ROLLBACK' | ''
): boolean {
  return loadingRequestId === requestId && loadingActionType === actionType
}
