<script setup lang="ts">
import type { PropType } from 'vue'
import { useSuiteOperationsWorkspace } from '~/composables/useSuiteOperationsWorkspace'
import { useSuitePlansWorkspace } from '~/composables/useSuitePlansWorkspace'
import SuiteCommandSearchPanel from '~/components/suite/SuiteCommandSearchPanel.vue'
import SuiteGovernancePanel from '~/components/suite/SuiteGovernancePanel.vue'
import SuiteReadinessSecurityPanel from '~/components/suite/SuiteReadinessSecurityPanel.vue'
import SuiteRemediationPanel from '~/components/suite/SuiteRemediationPanel.vue'

type SuitePlansWorkspaceState = ReturnType<typeof useSuitePlansWorkspace>
type SuiteOperationsWorkspaceState = ReturnType<typeof useSuiteOperationsWorkspace>

const props = defineProps({
  plansWorkspace: {
    type: Object as PropType<SuitePlansWorkspaceState>,
    required: true
  },
  operationsWorkspace: {
    type: Object as PropType<SuiteOperationsWorkspaceState>,
    required: true
  }
})
</script>

<template>
  <section class="suite-section-stack" data-testid="suite-section-operations">
    <SuiteCommandSearchPanel
      :command-keyword="props.operationsWorkspace.commandKeyword.value"
      :command-search-loading="props.operationsWorkspace.commandSearchLoading.value"
      :command-search-summary="props.plansWorkspace.commandSearchSummary.value"
      :command-search-result="props.operationsWorkspace.visibleCommandSearchResult.value"
      :search-command="props.operationsWorkspace.onCommandSearch"
      :clear-command="props.operationsWorkspace.onClearCommandSearch"
      :open-command-result="props.operationsWorkspace.onOpenCommandResult"
      @update-command-keyword="props.operationsWorkspace.commandKeyword.value = $event"
    />

    <SuiteReadinessSecurityPanel
      :loading="props.operationsWorkspace.loading.value"
      :readiness="props.operationsWorkspace.visibleReadiness.value"
      :wallet-readiness="props.operationsWorkspace.walletReadiness.value"
      :readiness-risk-filter="props.operationsWorkspace.readinessRiskFilter.value"
      :filtered-readiness-items="props.operationsWorkspace.filteredReadinessItems.value"
      :security-posture="props.operationsWorkspace.visibleSecurityPosture.value"
      :refresh-operations="props.operationsWorkspace.loadOperationsData"
      @update-readiness-risk-filter="props.operationsWorkspace.readinessRiskFilter.value = $event"
    />

    <SuiteGovernancePanel
      :loading="props.operationsWorkspace.loading.value"
      :governance-overview-cards="props.operationsWorkspace.governanceOverviewCards.value"
      :governance-templates="props.operationsWorkspace.governanceTemplates.value"
      :governance-requests="props.operationsWorkspace.governanceRequests.value"
      :managed-organizations="props.operationsWorkspace.managedOrganizations.value"
      :selected-template-code="props.operationsWorkspace.selectedTemplateCode.value"
      :governance-reason="props.operationsWorkspace.governanceReason.value"
      :governance-scope-type="props.operationsWorkspace.governanceScopeType.value"
      :governance-scope-org-id="props.operationsWorkspace.governanceScopeOrgId.value"
      :governance-second-reviewer-user-id="props.operationsWorkspace.governanceSecondReviewerUserId.value"
      :governance-reviewer-options="props.operationsWorkspace.governanceReviewerOptions.value"
      :review-note="props.operationsWorkspace.reviewNote.value"
      :approval-note="props.operationsWorkspace.approvalNote.value"
      :rollback-reason="props.operationsWorkspace.rollbackReason.value"
      :creating-governance-request="props.operationsWorkspace.creatingGovernanceRequest.value"
      :can-create-governance-request="props.operationsWorkspace.canCreateGovernanceRequest.value"
      :selected-governance-template="props.operationsWorkspace.selectedGovernanceTemplate.value"
      :current-session-id="props.operationsWorkspace.currentSessionId.value"
      :governance-action-loading-request-id="props.operationsWorkspace.governanceActionLoadingRequestId.value"
      :governance-action-loading-type="props.operationsWorkspace.governanceActionLoadingType.value"
      :refresh-operations="props.operationsWorkspace.loadOperationsData"
      :create-governance-request="props.operationsWorkspace.onCreateGovernanceRequest"
      :review-governance-request="props.operationsWorkspace.onReviewGovernanceRequest"
      :execute-governance-request="props.operationsWorkspace.onExecuteGovernanceRequest"
      :rollback-governance-request="props.operationsWorkspace.onRollbackGovernanceRequest"
      @update-selected-template-code="props.operationsWorkspace.selectedTemplateCode.value = $event"
      @update-governance-reason="props.operationsWorkspace.governanceReason.value = $event"
      @update-governance-scope-type="props.operationsWorkspace.governanceScopeType.value = $event"
      @update-governance-scope-org-id="props.operationsWorkspace.governanceScopeOrgId.value = $event"
      @update-governance-second-reviewer-user-id="props.operationsWorkspace.governanceSecondReviewerUserId.value = $event"
      @update-review-note="props.operationsWorkspace.reviewNote.value = $event"
      @update-approval-note="props.operationsWorkspace.approvalNote.value = $event"
      @update-rollback-reason="props.operationsWorkspace.rollbackReason.value = $event"
    />

    <SuiteRemediationPanel
      :priority-actions="props.operationsWorkspace.priorityActions.value"
      :last-execution-result="props.operationsWorkspace.lastExecutionResult.value"
      :running-action-code="props.operationsWorkspace.runningActionCode.value"
      :execute-action="props.operationsWorkspace.onExecuteAction"
    />
  </section>
</template>

<style scoped>
.suite-section-stack {
  display: grid;
  gap: 16px;
}
</style>
