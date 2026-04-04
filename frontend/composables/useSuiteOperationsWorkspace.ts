import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useSuiteApi } from '~/composables/useSuiteApi'
import { useOrganizationApi } from '~/composables/useOrganizationApi'
import { useI18n } from '~/composables/useI18n'
import { useAuthStore } from '~/stores/auth'
import { useOrgAccessStore } from '~/stores/org-access'
import type {
  OrgMember,
  OrgWorkspace,
  SuiteGovernanceChangeRequest,
  SuiteGovernanceOverview,
  SuiteGovernancePolicyTemplate,
  SuiteReadinessItem,
  SuiteReadinessReport,
  SuiteRemediationAction,
  SuiteRemediationExecutionResult,
  SuiteRiskLevel,
  SuiteSecurityPosture,
  SuiteUnifiedSearchItem,
  SuiteUnifiedSearchResult
} from '~/types/api'
import { resolveSessionIdFromAccessToken } from '~/utils/auth-session'
import {
  buildGovernanceOverviewCards,
  canReviewRequest,
  priorityOrder
} from '~/utils/suite-operations'
import { organizationRoleLabel } from '~/utils/organization-admin'
import {
  filterSuiteReadinessByAccess,
  filterSuiteSecurityPostureByAccess,
  filterSuiteUnifiedSearchByAccess
} from '~/utils/org-product-surface-filter'

function messageFromError(error: unknown, fallbackMessage: string): string {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message
  }
  return fallbackMessage
}

function formatGovernanceReviewerLabel(member: OrgMember, t: ReturnType<typeof useI18n>['t']): string {
  return `${member.userEmail} (${organizationRoleLabel(member.role, t)})`
}

export function useSuiteOperationsWorkspace() {
  const { t } = useI18n()
  const authStore = useAuthStore()
  const orgAccessStore = useOrgAccessStore()
  const {
    approveGovernanceChangeRequest,
    createGovernanceChangeRequest,
    executeRemediationAction,
    getGovernanceOverview,
    getReadiness,
    getSecurityPosture,
    getUnifiedSearch,
    listGovernanceChangeRequests,
    listGovernanceTemplates,
    reviewGovernanceChangeRequest,
    rollbackGovernanceChangeRequest
  } = useSuiteApi()
  const { listMembers, listOrganizations } = useOrganizationApi()

  const loading = ref(false)
  const commandKeyword = ref('')
  const commandSearchLoading = ref(false)
  const commandSearchResult = ref<SuiteUnifiedSearchResult | null>(null)
  const readiness = ref<SuiteReadinessReport | null>(null)
  const securityPosture = ref<SuiteSecurityPosture | null>(null)
  const governanceOverview = ref<SuiteGovernanceOverview | null>(null)
  const governanceTemplates = ref<SuiteGovernancePolicyTemplate[]>([])
  const governanceRequests = ref<SuiteGovernanceChangeRequest[]>([])
  const managedOrganizations = ref<OrgWorkspace[]>([])
  const readinessRiskFilter = ref<'ALL' | SuiteRiskLevel>('ALL')
  const runningActionCode = ref('')
  const selectedTemplateCode = ref('')
  const governanceReason = ref('')
  const governanceScopeType = ref<'PERSONAL' | 'ORG'>('PERSONAL')
  const governanceScopeOrgId = ref('')
  const governanceSecondReviewerUserId = ref('')
  const governanceOrgMembers = ref<OrgMember[]>([])
  const reviewNote = ref('')
  const approvalNote = ref('')
  const rollbackReason = ref('')
  const creatingGovernanceRequest = ref(false)
  const governanceActionLoadingRequestId = ref('')
  const governanceActionLoadingType = ref<'REVIEW_APPROVE' | 'REVIEW_REJECT' | 'EXECUTE' | 'ROLLBACK' | ''>('')
  const lastExecutionResult = ref<SuiteRemediationExecutionResult | null>(null)
  const currentSessionId = ref('')

  const visibleReadiness = computed(() => filterSuiteReadinessByAccess(
    readiness.value,
    orgAccessStore.isProductEnabled
  ))
  const visibleSecurityPosture = computed(() => filterSuiteSecurityPostureByAccess(
    securityPosture.value,
    orgAccessStore.isProductEnabled
  ))
  const visibleCommandSearchResult = computed(() => filterSuiteUnifiedSearchByAccess(
    commandSearchResult.value,
    orgAccessStore.isProductEnabled
  ))
  const readinessItems = computed<SuiteReadinessItem[]>(() => visibleReadiness.value?.items ?? [])
  const walletReadiness = computed(() => {
    return readinessItems.value.find(item => item.productCode === 'WALLET') ?? null
  })
  const filteredReadinessItems = computed(() => {
    if (readinessRiskFilter.value === 'ALL') {
      return readinessItems.value
    }
    return readinessItems.value.filter(item => item.riskLevel === readinessRiskFilter.value)
  })
  const priorityActions = computed(() => {
    const actions = new Map<string, SuiteRemediationAction>()
    for (const item of readinessItems.value) {
      for (const action of item.actions) {
        const key = `${action.priority}|${action.productCode}|${action.action}|${action.actionCode || ''}`
        if (!actions.has(key)) {
          actions.set(key, action)
        }
      }
    }
    for (const action of visibleSecurityPosture.value?.recommendedActions ?? []) {
      const key = `${action.priority}|${action.productCode}|${action.action}|${action.actionCode || ''}`
      if (!actions.has(key)) {
        actions.set(key, action)
      }
    }
    return Array.from(actions.values())
      .sort((a, b) => priorityOrder(a.priority) - priorityOrder(b.priority))
      .slice(0, 12)
  })
  const selectedGovernanceTemplate = computed(() => {
    if (!selectedTemplateCode.value) {
      return null
    }
    return governanceTemplates.value.find(item => item.templateCode === selectedTemplateCode.value) ?? null
  })
  const governanceReviewerOptions = computed(() => {
    return governanceOrgMembers.value
      .filter(member => member.status === 'ACTIVE' && member.userId && (member.role === 'OWNER' || member.role === 'ADMIN'))
      .map(member => ({
        userId: member.userId as string,
        label: formatGovernanceReviewerLabel(member, t)
      }))
  })
  const canCreateGovernanceRequest = computed(() => {
    if (!selectedTemplateCode.value || governanceReason.value.trim().length === 0) {
      return false
    }
    if (governanceScopeType.value === 'ORG') {
      return Boolean(governanceScopeOrgId.value)
    }
    return true
  })
  const governanceOverviewCards = computed(() => buildGovernanceOverviewCards(governanceOverview.value, t))

  async function refreshGovernanceReviewerCandidates(): Promise<void> {
    if (governanceScopeType.value !== 'ORG' || !governanceScopeOrgId.value) {
      governanceOrgMembers.value = []
      governanceSecondReviewerUserId.value = ''
      return
    }
    try {
      governanceOrgMembers.value = await listMembers(governanceScopeOrgId.value)
      const reviewerExists = governanceReviewerOptions.value.some(
        item => item.userId === governanceSecondReviewerUserId.value
      )
      if (!reviewerExists) {
        governanceSecondReviewerUserId.value = ''
      }
    } catch {
      governanceOrgMembers.value = []
      governanceSecondReviewerUserId.value = ''
    }
  }

  async function loadOperationsData(): Promise<void> {
    loading.value = true
    try {
      const [readinessReport, posture, templates, requests, organizations, overview] = await Promise.all([
        getReadiness(),
        getSecurityPosture(),
        listGovernanceTemplates(),
        listGovernanceChangeRequests(),
        listOrganizations(),
        getGovernanceOverview()
      ])
      readiness.value = readinessReport
      securityPosture.value = posture
      governanceTemplates.value = templates
      governanceRequests.value = requests
      governanceOverview.value = overview
      managedOrganizations.value = organizations.filter(org => org.role === 'OWNER' || org.role === 'ADMIN')
      if (governanceScopeType.value === 'ORG' && !managedOrganizations.value.some(item => item.id === governanceScopeOrgId.value)) {
        governanceScopeOrgId.value = managedOrganizations.value[0]?.id || ''
        if (!governanceScopeOrgId.value) {
          governanceScopeType.value = 'PERSONAL'
        }
      }
      await refreshGovernanceReviewerCandidates()
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.operations.messages.loadFailed')))
    } finally {
      loading.value = false
    }
  }

  async function refreshGovernanceAndRisk(): Promise<void> {
    const [requests, readinessReport, posture, overview] = await Promise.all([
      listGovernanceChangeRequests(),
      getReadiness(),
      getSecurityPosture(),
      getGovernanceOverview()
    ])
    governanceRequests.value = requests
    readiness.value = readinessReport
    securityPosture.value = posture
    governanceOverview.value = overview
  }

  async function onCommandSearch(): Promise<void> {
    const keyword = commandKeyword.value.trim()
    if (!keyword) {
      ElMessage.warning(t('suite.operations.messages.searchKeywordRequired'))
      return
    }
    commandSearchLoading.value = true
    try {
      commandSearchResult.value = await getUnifiedSearch(keyword, 50)
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.operations.messages.commandSearchFailed')))
    } finally {
      commandSearchLoading.value = false
    }
  }

  function onClearCommandSearch(): void {
    commandKeyword.value = ''
    commandSearchResult.value = null
  }

  function resetScopeSensitiveState(): void {
    commandKeyword.value = ''
    commandSearchResult.value = null
    lastExecutionResult.value = null
  }

  async function onOpenCommandResult(item: SuiteUnifiedSearchItem): Promise<void> {
    if (!item.routePath) {
      return
    }
    await navigateTo(item.routePath)
  }

  async function onExecuteAction(action: SuiteRemediationAction): Promise<void> {
    if (!action.actionCode) {
      ElMessage.info(t('suite.operations.messages.manualHandlingRequired'))
      return
    }
    runningActionCode.value = action.actionCode
    try {
      const result = await executeRemediationAction(action.actionCode)
      lastExecutionResult.value = result
      if (result.status === 'SUCCESS') {
        ElMessage.success(result.message || t('suite.operations.messages.remediationExecuted'))
      } else if (result.status === 'NO_OP') {
        ElMessage.info(result.message || t('suite.operations.messages.remediationNoChanges'))
      } else {
        ElMessage.warning(result.message || t('suite.operations.messages.remediationWarnings'))
      }
      await loadOperationsData()
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.operations.messages.remediationFailed')))
    } finally {
      runningActionCode.value = ''
    }
  }

  async function onCreateGovernanceRequest(): Promise<void> {
    const templateCode = selectedTemplateCode.value
    const reason = governanceReason.value.trim()
    if (!templateCode || !reason) {
      ElMessage.warning(t('suite.operations.messages.templateAndReasonRequired'))
      return
    }
    if (governanceScopeType.value === 'ORG' && !governanceScopeOrgId.value) {
      ElMessage.warning(t('suite.operations.messages.selectOrganization'))
      return
    }
    creatingGovernanceRequest.value = true
    try {
      const created = await createGovernanceChangeRequest({
        templateCode,
        reason,
        orgId: governanceScopeType.value === 'ORG' ? governanceScopeOrgId.value : undefined,
        secondReviewerUserId: governanceScopeType.value === 'ORG'
          ? governanceSecondReviewerUserId.value || undefined
          : undefined
      })
      ElMessage.success(t('suite.operations.messages.governanceCreated', { requestId: created.requestId }))
      governanceReason.value = ''
      governanceSecondReviewerUserId.value = ''
      await refreshGovernanceAndRisk()
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.operations.messages.governanceCreateFailed')))
    } finally {
      creatingGovernanceRequest.value = false
    }
  }

  async function onReviewGovernanceRequest(
    request: SuiteGovernanceChangeRequest,
    decision: 'APPROVE' | 'REJECT'
  ): Promise<void> {
    if (!canReviewRequest(request)) {
      return
    }
    governanceActionLoadingRequestId.value = request.requestId
    governanceActionLoadingType.value = decision === 'APPROVE' ? 'REVIEW_APPROVE' : 'REVIEW_REJECT'
    try {
      const updated = await reviewGovernanceChangeRequest({
        requestId: request.requestId,
        decision,
        reviewNote: reviewNote.value.trim() || undefined
      })
      if (updated.status === 'PENDING_SECOND_REVIEW') {
        ElMessage.success(t('suite.operations.messages.firstReviewApproved'))
      } else if (updated.status === 'REJECTED') {
        ElMessage.info(t('suite.operations.messages.governanceRejected'))
      } else {
        ElMessage.success(t('suite.operations.messages.governanceApproved'))
      }
      reviewNote.value = ''
      await refreshGovernanceAndRisk()
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.operations.messages.governanceReviewFailed')))
    } finally {
      governanceActionLoadingRequestId.value = ''
      governanceActionLoadingType.value = ''
    }
  }

  async function onExecuteGovernanceRequest(request: SuiteGovernanceChangeRequest): Promise<void> {
    governanceActionLoadingRequestId.value = request.requestId
    governanceActionLoadingType.value = 'EXECUTE'
    try {
      const updated = await approveGovernanceChangeRequest({
        requestId: request.requestId,
        approvalNote: approvalNote.value.trim() || undefined
      })
      if (updated.status === 'EXECUTED_WITH_FAILURE') {
        ElMessage.warning(t('suite.operations.messages.governanceExecutedWithFailures'))
      } else {
        ElMessage.success(t('suite.operations.messages.governanceExecuted'))
      }
      approvalNote.value = ''
      await refreshGovernanceAndRisk()
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.operations.messages.governanceExecuteFailed')))
    } finally {
      governanceActionLoadingRequestId.value = ''
      governanceActionLoadingType.value = ''
    }
  }

  async function onRollbackGovernanceRequest(request: SuiteGovernanceChangeRequest): Promise<void> {
    const reason = rollbackReason.value.trim()
    if (!reason) {
      ElMessage.warning(t('suite.operations.messages.rollbackReasonRequired'))
      return
    }
    governanceActionLoadingRequestId.value = request.requestId
    governanceActionLoadingType.value = 'ROLLBACK'
    try {
      const updated = await rollbackGovernanceChangeRequest({
        requestId: request.requestId,
        rollbackReason: reason
      })
      if (updated.status === 'ROLLBACK_WITH_FAILURE') {
        ElMessage.warning(t('suite.operations.messages.rollbackWithFailures'))
      } else {
        ElMessage.success(t('suite.operations.messages.rollbackCompleted'))
      }
      rollbackReason.value = ''
      await refreshGovernanceAndRisk()
    } catch (error) {
      ElMessage.error(messageFromError(error, t('suite.operations.messages.governanceRollbackFailed')))
    } finally {
      governanceActionLoadingRequestId.value = ''
      governanceActionLoadingType.value = ''
    }
  }

  async function reloadAfterPlanChange(): Promise<void> {
    resetScopeSensitiveState()
    await loadOperationsData()
  }

  onMounted(() => {
    void loadOperationsData()
  })

  watch(
    () => orgAccessStore.activeOrgId,
    (nextOrgId, previousOrgId) => {
      if (nextOrgId === previousOrgId) {
        return
      }
      resetScopeSensitiveState()
      void loadOperationsData()
    }
  )

  watch(
    () => authStore.accessToken,
    (token) => {
      currentSessionId.value = resolveSessionIdFromAccessToken(token)
    },
    { immediate: true }
  )

  watch(governanceScopeType, (scopeType) => {
    if (scopeType === 'ORG' && !governanceScopeOrgId.value) {
      governanceScopeOrgId.value = managedOrganizations.value[0]?.id || ''
    }
    if (scopeType === 'PERSONAL') {
      governanceSecondReviewerUserId.value = ''
    }
    void refreshGovernanceReviewerCandidates()
  })

  watch(governanceScopeOrgId, () => {
    void refreshGovernanceReviewerCandidates()
  })

  return {
    loading,
    commandKeyword,
    commandSearchLoading,
    visibleCommandSearchResult,
    visibleReadiness,
    visibleSecurityPosture,
    governanceOverviewCards,
    governanceTemplates,
    governanceRequests,
    managedOrganizations,
    readinessRiskFilter,
    walletReadiness,
    filteredReadinessItems,
    priorityActions,
    runningActionCode,
    selectedTemplateCode,
    selectedGovernanceTemplate,
    governanceReason,
    governanceScopeType,
    governanceScopeOrgId,
    governanceSecondReviewerUserId,
    governanceReviewerOptions,
    reviewNote,
    approvalNote,
    rollbackReason,
    creatingGovernanceRequest,
    governanceActionLoadingRequestId,
    governanceActionLoadingType,
    canCreateGovernanceRequest,
    lastExecutionResult,
    currentSessionId,
    loadOperationsData,
    reloadAfterPlanChange,
    onCommandSearch,
    onClearCommandSearch,
    onOpenCommandResult,
    onExecuteAction,
    onCreateGovernanceRequest,
    onReviewGovernanceRequest,
    onExecuteGovernanceRequest,
    onRollbackGovernanceRequest
  }
}
