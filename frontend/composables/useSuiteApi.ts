import type {
  AssignSuiteNotificationsRequest,
  ApproveSuiteGovernanceChangeRequestRequest,
  ApiResponse,
  BatchExecuteSuiteRemediationActionsRequest,
  BatchReviewSuiteGovernanceChangeRequestsRequest,
  CreateSuiteGovernanceChangeRequestRequest,
  MarkSuiteNotificationsReadRequest,
  SnoozeSuiteNotificationsRequest,
  UndoSuiteNotificationWorkflowRequest,
  ReviewSuiteGovernanceChangeRequestRequest,
  RollbackSuiteGovernanceChangeRequestRequest,
  SuiteBatchGovernanceReviewResult,
  SuiteBatchRemediationExecutionResult,
  SuiteCollaborationCenter,
  SuiteCollaborationSync,
  SuiteCommandCenter,
  SuiteCommandFeed,
  SuiteNotificationCenter,
  SuiteNotificationOperationHistory,
  SuiteNotificationMarkReadResult,
  SuiteNotificationSync,
  SuiteNotificationWorkflowResult,
  SuiteWebPushStatus,
  SuiteWebPushSubscribeRequest,
  SuiteWebPushUnsubscribeRequest,
  SuiteGovernanceChangeRequest,
  SuiteGovernanceOverview,
  SuiteGovernancePolicyTemplate,
  SuiteProductItem,
  SuiteReadinessReport,
  SuiteRemediationExecutionResult,
  SuiteSecurityPosture,
  SuiteUnifiedSearchResult
} from '~/types/api'
import type { ChangeSuitePlanRequest, SuitePlan, SuiteSubscription } from '~/types/suite-lumo'

export function useSuiteApi() {
  const { $apiClient } = useNuxtApp()

  async function listPlans(): Promise<SuitePlan[]> {
    const response = await $apiClient.get<ApiResponse<SuitePlan[]>>('/api/v1/suite/plans')
    return response.data.data
  }

  async function getSubscription(): Promise<SuiteSubscription> {
    const response = await $apiClient.get<ApiResponse<SuiteSubscription>>('/api/v1/suite/subscription')
    return response.data.data
  }

  async function listProducts(): Promise<SuiteProductItem[]> {
    const response = await $apiClient.get<ApiResponse<SuiteProductItem[]>>('/api/v1/suite/products')
    return response.data.data
  }

  async function getReadiness(): Promise<SuiteReadinessReport> {
    const response = await $apiClient.get<ApiResponse<SuiteReadinessReport>>('/api/v1/suite/readiness')
    return response.data.data
  }

  async function getSecurityPosture(): Promise<SuiteSecurityPosture> {
    const response = await $apiClient.get<ApiResponse<SuiteSecurityPosture>>('/api/v1/suite/security-posture')
    return response.data.data
  }

  async function getCommandCenter(): Promise<SuiteCommandCenter> {
    const response = await $apiClient.get<ApiResponse<SuiteCommandCenter>>('/api/v1/suite/command-center')
    return response.data.data
  }

  async function getCollaborationCenter(limit?: number): Promise<SuiteCollaborationCenter> {
    const response = await $apiClient.get<ApiResponse<SuiteCollaborationCenter>>('/api/v1/suite/collaboration-center', {
      params: {
        ...(typeof limit === 'number' ? { limit } : {})
      }
    })
    return response.data.data
  }

  async function getCollaborationSync(afterEventId?: number, limit?: number): Promise<SuiteCollaborationSync> {
    const response = await $apiClient.get<ApiResponse<SuiteCollaborationSync>>('/api/v1/suite/collaboration-center/sync', {
      params: {
        ...(typeof afterEventId === 'number' ? { afterEventId } : {}),
        ...(typeof limit === 'number' ? { limit } : {})
      }
    })
    return response.data.data
  }

  async function getCommandFeed(limit?: number): Promise<SuiteCommandFeed> {
    const response = await $apiClient.get<ApiResponse<SuiteCommandFeed>>('/api/v1/suite/command-feed', {
      params: {
        ...(typeof limit === 'number' ? { limit } : {})
      }
    })
    return response.data.data
  }

  async function getNotificationCenter(
    limit?: number,
    unreadOnly?: boolean,
    status?: 'ALL' | 'ACTIVE' | 'ARCHIVED' | 'IGNORED' | 'SNOOZED',
    includeSnoozed?: boolean
  ): Promise<SuiteNotificationCenter> {
    const response = await $apiClient.get<ApiResponse<SuiteNotificationCenter>>('/api/v1/suite/notification-center', {
      params: {
        ...(typeof limit === 'number' ? { limit } : {}),
        ...(typeof unreadOnly === 'boolean' ? { unreadOnly } : {}),
        ...(typeof status === 'string' ? { status } : {}),
        ...(typeof includeSnoozed === 'boolean' ? { includeSnoozed } : {})
      }
    })
    return response.data.data
  }

  async function getNotificationOperationHistory(limit?: number): Promise<SuiteNotificationOperationHistory> {
    const response = await $apiClient.get<ApiResponse<SuiteNotificationOperationHistory>>(
      '/api/v1/suite/notification-center/operations',
      {
        params: {
          ...(typeof limit === 'number' ? { limit } : {})
        }
      }
    )
    return response.data.data
  }

  async function getNotificationSync(afterEventId?: number, limit?: number): Promise<SuiteNotificationSync> {
    const response = await $apiClient.get<ApiResponse<SuiteNotificationSync>>('/api/v1/suite/notification-center/sync', {
      params: {
        ...(typeof afterEventId === 'number' ? { afterEventId } : {}),
        ...(typeof limit === 'number' ? { limit } : {})
      }
    })
    return response.data.data
  }

  async function getWebPushStatus(): Promise<SuiteWebPushStatus> {
    const response = await $apiClient.get<ApiResponse<SuiteWebPushStatus>>('/api/v1/suite/web-push')
    return response.data.data
  }

  async function subscribeWebPush(payload: SuiteWebPushSubscribeRequest): Promise<void> {
    await $apiClient.post<ApiResponse<null>>('/api/v1/suite/web-push/subscriptions', payload)
  }

  async function unsubscribeWebPush(payload: SuiteWebPushUnsubscribeRequest): Promise<void> {
    await $apiClient.delete<ApiResponse<null>>('/api/v1/suite/web-push/subscriptions', { data: payload })
  }

  async function markNotificationsRead(
    payload: MarkSuiteNotificationsReadRequest
  ): Promise<SuiteNotificationMarkReadResult> {
    const response = await $apiClient.post<ApiResponse<SuiteNotificationMarkReadResult>>(
      '/api/v1/suite/notification-center/mark-read',
      payload
    )
    return response.data.data
  }

  async function markAllNotificationsRead(): Promise<SuiteNotificationMarkReadResult> {
    const response = await $apiClient.post<ApiResponse<SuiteNotificationMarkReadResult>>(
      '/api/v1/suite/notification-center/mark-all-read'
    )
    return response.data.data
  }

  async function archiveNotifications(
    payload: MarkSuiteNotificationsReadRequest
  ): Promise<SuiteNotificationWorkflowResult> {
    const response = await $apiClient.post<ApiResponse<SuiteNotificationWorkflowResult>>(
      '/api/v1/suite/notification-center/archive',
      payload
    )
    return response.data.data
  }

  async function ignoreNotifications(
    payload: MarkSuiteNotificationsReadRequest
  ): Promise<SuiteNotificationWorkflowResult> {
    const response = await $apiClient.post<ApiResponse<SuiteNotificationWorkflowResult>>(
      '/api/v1/suite/notification-center/ignore',
      payload
    )
    return response.data.data
  }

  async function restoreNotifications(
    payload: MarkSuiteNotificationsReadRequest
  ): Promise<SuiteNotificationWorkflowResult> {
    const response = await $apiClient.post<ApiResponse<SuiteNotificationWorkflowResult>>(
      '/api/v1/suite/notification-center/restore',
      payload
    )
    return response.data.data
  }

  async function snoozeNotifications(
    payload: SnoozeSuiteNotificationsRequest
  ): Promise<SuiteNotificationWorkflowResult> {
    const response = await $apiClient.post<ApiResponse<SuiteNotificationWorkflowResult>>(
      '/api/v1/suite/notification-center/snooze',
      payload
    )
    return response.data.data
  }

  async function assignNotifications(
    payload: AssignSuiteNotificationsRequest
  ): Promise<SuiteNotificationWorkflowResult> {
    const response = await $apiClient.post<ApiResponse<SuiteNotificationWorkflowResult>>(
      '/api/v1/suite/notification-center/assign',
      payload
    )
    return response.data.data
  }

  async function undoNotificationWorkflow(
    payload: UndoSuiteNotificationWorkflowRequest
  ): Promise<SuiteNotificationWorkflowResult> {
    const response = await $apiClient.post<ApiResponse<SuiteNotificationWorkflowResult>>(
      '/api/v1/suite/notification-center/undo',
      payload
    )
    return response.data.data
  }

  async function getUnifiedSearch(keyword: string, limit?: number): Promise<SuiteUnifiedSearchResult> {
    const response = await $apiClient.get<ApiResponse<SuiteUnifiedSearchResult>>('/api/v1/suite/unified-search', {
      params: {
        keyword,
        ...(typeof limit === 'number' ? { limit } : {})
      }
    })
    return response.data.data
  }

  async function getGovernanceOverview(): Promise<SuiteGovernanceOverview> {
    const response = await $apiClient.get<ApiResponse<SuiteGovernanceOverview>>('/api/v1/suite/governance/overview')
    return response.data.data
  }

  async function listGovernanceTemplates(): Promise<SuiteGovernancePolicyTemplate[]> {
    const response = await $apiClient.get<ApiResponse<SuiteGovernancePolicyTemplate[]>>('/api/v1/suite/governance/templates')
    return response.data.data
  }

  async function listGovernanceChangeRequests(): Promise<SuiteGovernanceChangeRequest[]> {
    const response = await $apiClient.get<ApiResponse<SuiteGovernanceChangeRequest[]>>('/api/v1/suite/governance/change-requests')
    return response.data.data
  }

  async function createGovernanceChangeRequest(
    payload: CreateSuiteGovernanceChangeRequestRequest
  ): Promise<SuiteGovernanceChangeRequest> {
    const response = await $apiClient.post<ApiResponse<SuiteGovernanceChangeRequest>>(
      '/api/v1/suite/governance/change-requests',
      payload
    )
    return response.data.data
  }

  async function approveGovernanceChangeRequest(
    payload: ApproveSuiteGovernanceChangeRequestRequest
  ): Promise<SuiteGovernanceChangeRequest> {
    const response = await $apiClient.post<ApiResponse<SuiteGovernanceChangeRequest>>(
      '/api/v1/suite/governance/change-requests/approve',
      payload
    )
    return response.data.data
  }

  async function reviewGovernanceChangeRequest(
    payload: ReviewSuiteGovernanceChangeRequestRequest
  ): Promise<SuiteGovernanceChangeRequest> {
    const response = await $apiClient.post<ApiResponse<SuiteGovernanceChangeRequest>>(
      '/api/v1/suite/governance/change-requests/review',
      payload
    )
    return response.data.data
  }

  async function rollbackGovernanceChangeRequest(
    payload: RollbackSuiteGovernanceChangeRequestRequest
  ): Promise<SuiteGovernanceChangeRequest> {
    const response = await $apiClient.post<ApiResponse<SuiteGovernanceChangeRequest>>(
      '/api/v1/suite/governance/change-requests/rollback',
      payload
    )
    return response.data.data
  }

  async function changePlan(payload: ChangeSuitePlanRequest): Promise<SuiteSubscription> {
    const response = await $apiClient.post<ApiResponse<SuiteSubscription>>('/api/v1/suite/subscription/change', payload)
    return response.data.data
  }

  async function executeRemediationAction(actionCode: string): Promise<SuiteRemediationExecutionResult> {
    const response = await $apiClient.post<ApiResponse<SuiteRemediationExecutionResult>>(
      '/api/v1/suite/remediation-actions/execute',
      { actionCode }
    )
    return response.data.data
  }

  async function batchExecuteRemediationActions(
    payload: BatchExecuteSuiteRemediationActionsRequest
  ): Promise<SuiteBatchRemediationExecutionResult> {
    const response = await $apiClient.post<ApiResponse<SuiteBatchRemediationExecutionResult>>(
      '/api/v1/suite/remediation-actions/batch-execute',
      payload
    )
    return response.data.data
  }

  async function batchReviewGovernanceChangeRequests(
    payload: BatchReviewSuiteGovernanceChangeRequestsRequest
  ): Promise<SuiteBatchGovernanceReviewResult> {
    const response = await $apiClient.post<ApiResponse<SuiteBatchGovernanceReviewResult>>(
      '/api/v1/suite/governance/change-requests/batch-review',
      payload
    )
    return response.data.data
  }

  return {
    listPlans,
    getSubscription,
    listProducts,
    getReadiness,
    getSecurityPosture,
    getCommandCenter,
    getCollaborationCenter,
    getCollaborationSync,
    getCommandFeed,
    getNotificationCenter,
    getNotificationOperationHistory,
    getNotificationSync,
    getWebPushStatus,
    subscribeWebPush,
    unsubscribeWebPush,
    markNotificationsRead,
    markAllNotificationsRead,
    archiveNotifications,
    ignoreNotifications,
    restoreNotifications,
    snoozeNotifications,
    assignNotifications,
    undoNotificationWorkflow,
    getUnifiedSearch,
    getGovernanceOverview,
    listGovernanceTemplates,
    listGovernanceChangeRequests,
    createGovernanceChangeRequest,
    reviewGovernanceChangeRequest,
    approveGovernanceChangeRequest,
    rollbackGovernanceChangeRequest,
    changePlan,
    executeRemediationAction,
    batchExecuteRemediationActions,
    batchReviewGovernanceChangeRequests
  }
}
