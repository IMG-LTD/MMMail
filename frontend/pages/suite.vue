<script setup lang="ts">
import { computed } from 'vue'
import SuiteCapabilityMatrixPanel from '~/components/suite/SuiteCapabilityMatrixPanel.vue'
import SuiteBillingCenterPanel from '~/components/suite/SuiteBillingCenterPanel.vue'
import SuiteBillingOverviewPanel from '~/components/suite/SuiteBillingOverviewPanel.vue'
import SuiteCommandSearchPanel from '~/components/suite/SuiteCommandSearchPanel.vue'
import SuiteCheckoutPanel from '~/components/suite/SuiteCheckoutPanel.vue'
import SuiteGovernancePanel from '~/components/suite/SuiteGovernancePanel.vue'
import SuitePlanCatalogPanel from '~/components/suite/SuitePlanCatalogPanel.vue'
import SuitePlansHero from '~/components/suite/SuitePlansHero.vue'
import SuitePricingComparePanel from '~/components/suite/SuitePricingComparePanel.vue'
import SuiteProductHubPanel from '~/components/suite/SuiteProductHubPanel.vue'
import SuiteReadinessSecurityPanel from '~/components/suite/SuiteReadinessSecurityPanel.vue'
import SuiteRemediationPanel from '~/components/suite/SuiteRemediationPanel.vue'
import { useI18n } from '~/composables/useI18n'
import { useSuiteBillingCenterWorkspace } from '~/composables/useSuiteBillingCenterWorkspace'
import { useSuiteBillingWorkspace } from '~/composables/useSuiteBillingWorkspace'
import { useSuiteOperationsWorkspace } from '~/composables/useSuiteOperationsWorkspace'
import { useSuitePlansWorkspace } from '~/composables/useSuitePlansWorkspace'
import type { SuitePlanCode } from '~/types/suite-lumo'

definePageMeta({
  layout: 'default'
})

const { t } = useI18n()
const billingWorkspace = useSuiteBillingWorkspace()
const billingCenterWorkspace = useSuiteBillingCenterWorkspace()
const plansWorkspace = useSuitePlansWorkspace()
const operationsWorkspace = useSuiteOperationsWorkspace()

const pageLoading = computed(() => {
  return billingWorkspace.loading.value
    || billingCenterWorkspace.loading.value
    || plansWorkspace.loading.value
    || operationsWorkspace.loading.value
})

useHead(() => ({
  title: t('page.suite.title'),
  meta: [
    {
      name: 'description',
      content: t('suite.hero.subtitle')
    }
  ]
}))

async function onChangePlan(planCode: SuitePlanCode): Promise<boolean> {
  const changed = await plansWorkspace.onChangePlan(planCode)
  if (!changed) {
    return false
  }
  await operationsWorkspace.reloadAfterPlanChange()
  await billingWorkspace.loadBillingData()
  await billingCenterWorkspace.loadBillingCenter()
  return true
}

async function onSaveDraft(): Promise<void> {
  await billingWorkspace.saveDraft()
  await billingCenterWorkspace.loadBillingCenter()
}

function onReadinessRiskFilterChange(value: 'ALL' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'): void {
  operationsWorkspace.readinessRiskFilter.value = value
}

function onCommandKeywordChange(value: string): void {
  operationsWorkspace.commandKeyword.value = value
}

function onSelectedTemplateCodeChange(value: string): void {
  operationsWorkspace.selectedTemplateCode.value = value
}

function onGovernanceReasonChange(value: string): void {
  operationsWorkspace.governanceReason.value = value
}

function onGovernanceScopeTypeChange(value: 'PERSONAL' | 'ORG'): void {
  operationsWorkspace.governanceScopeType.value = value
}

function onGovernanceScopeOrgIdChange(value: string): void {
  operationsWorkspace.governanceScopeOrgId.value = value
}

function onGovernanceSecondReviewerChange(value: string): void {
  operationsWorkspace.governanceSecondReviewerUserId.value = value
}

function onReviewNoteChange(value: string): void {
  operationsWorkspace.reviewNote.value = value
}

function onApprovalNoteChange(value: string): void {
  operationsWorkspace.approvalNote.value = value
}

function onRollbackReasonChange(value: string): void {
  operationsWorkspace.rollbackReason.value = value
}
</script>

<template>
  <div class="mm-page" v-loading="pageLoading">
    <section class="suite-shell">
      <SuitePlansHero
        :subscription="plansWorkspace.subscription.value"
        :usage-rows="plansWorkspace.usageRows.value"
        :show-drive-entity-usage="plansWorkspace.showDriveEntityUsage.value"
        :upgrade-summary="plansWorkspace.upgradeSummary.value"
        :resolve-status-label="plansWorkspace.resolveSubscriptionStatusLabel"
      />

      <SuiteProductHubPanel :products="plansWorkspace.visibleProducts.value" />

      <SuiteBillingOverviewPanel
        :overview="billingWorkspace.overview.value"
        :selected-offer-code="billingWorkspace.selectedOfferCode.value"
        :restore-latest-draft="billingWorkspace.restoreLatestDraft"
      />

      <SuitePricingComparePanel
        :sections="billingWorkspace.pricingSections.value"
        :active-plan-code="billingWorkspace.overview.value?.activePlanCode || null"
        :selected-offer-code="billingWorkspace.selectedOfferCode.value"
        :select-offer="billingWorkspace.onSelectOffer"
      />

      <SuiteCheckoutPanel
        :selected-offer="billingWorkspace.selectedOffer.value"
        :selected-billing-cycle="billingWorkspace.selectedBillingCycle.value"
        :seat-count="billingWorkspace.seatCount.value"
        :organization-name="billingWorkspace.organizationName.value"
        :domain-name="billingWorkspace.domainName.value"
        :quote="billingWorkspace.quote.value"
        :quote-loading="billingWorkspace.quoteLoading.value"
        :draft-loading="billingWorkspace.draftLoading.value"
        :show-organization-fields="billingWorkspace.showOrganizationFields.value"
        :refresh-quote="billingWorkspace.refreshQuote"
        :save-draft="onSaveDraft"
        @update:selected-billing-cycle="billingWorkspace.selectedBillingCycle.value = $event"
        @update:seat-count="billingWorkspace.seatCount.value = $event"
        @update:organization-name="billingWorkspace.organizationName.value = $event"
        @update:domain-name="billingWorkspace.domainName.value = $event"
      />

      <SuiteBillingCenterPanel
        :center="billingCenterWorkspace.center.value"
        :payment-method-saving="billingCenterWorkspace.paymentMethodSaving.value"
        :action-loading-code="billingCenterWorkspace.actionLoadingCode.value"
        :add-payment-method="billingCenterWorkspace.addPaymentMethod"
        :set-default-payment-method="billingCenterWorkspace.setDefaultPaymentMethod"
        :execute-subscription-action="billingCenterWorkspace.executeSubscriptionAction"
      />

      <SuitePlanCatalogPanel
        :sections="plansWorkspace.planSections.value"
        :changing-plan="plansWorkspace.changingPlan.value"
        :is-current-plan="plansWorkspace.isCurrentPlan"
        :plan-quota-rows="plansWorkspace.planQuotaRows"
        :change-plan="onChangePlan"
      />

      <SuiteCapabilityMatrixPanel
        :plans="plansWorkspace.plans.value"
        :product-columns="plansWorkspace.productColumns.value"
      />

      <SuiteCommandSearchPanel
        :command-keyword="operationsWorkspace.commandKeyword.value"
        :command-search-loading="operationsWorkspace.commandSearchLoading.value"
        :command-search-summary="plansWorkspace.commandSearchSummary.value"
        :command-search-result="operationsWorkspace.visibleCommandSearchResult.value"
        :search-command="operationsWorkspace.onCommandSearch"
        :clear-command="operationsWorkspace.onClearCommandSearch"
        :open-command-result="operationsWorkspace.onOpenCommandResult"
        @update-command-keyword="onCommandKeywordChange"
      />

      <SuiteReadinessSecurityPanel
        :loading="operationsWorkspace.loading.value"
        :readiness="operationsWorkspace.visibleReadiness.value"
        :wallet-readiness="operationsWorkspace.walletReadiness.value"
        :readiness-risk-filter="operationsWorkspace.readinessRiskFilter.value"
        :filtered-readiness-items="operationsWorkspace.filteredReadinessItems.value"
        :security-posture="operationsWorkspace.visibleSecurityPosture.value"
        :refresh-operations="operationsWorkspace.loadOperationsData"
        @update-readiness-risk-filter="onReadinessRiskFilterChange"
      />

      <SuiteGovernancePanel
        :loading="operationsWorkspace.loading.value"
        :governance-overview-cards="operationsWorkspace.governanceOverviewCards.value"
        :governance-templates="operationsWorkspace.governanceTemplates.value"
        :governance-requests="operationsWorkspace.governanceRequests.value"
        :managed-organizations="operationsWorkspace.managedOrganizations.value"
        :selected-template-code="operationsWorkspace.selectedTemplateCode.value"
        :governance-reason="operationsWorkspace.governanceReason.value"
        :governance-scope-type="operationsWorkspace.governanceScopeType.value"
        :governance-scope-org-id="operationsWorkspace.governanceScopeOrgId.value"
        :governance-second-reviewer-user-id="operationsWorkspace.governanceSecondReviewerUserId.value"
        :governance-reviewer-options="operationsWorkspace.governanceReviewerOptions.value"
        :review-note="operationsWorkspace.reviewNote.value"
        :approval-note="operationsWorkspace.approvalNote.value"
        :rollback-reason="operationsWorkspace.rollbackReason.value"
        :creating-governance-request="operationsWorkspace.creatingGovernanceRequest.value"
        :can-create-governance-request="operationsWorkspace.canCreateGovernanceRequest.value"
        :selected-governance-template="operationsWorkspace.selectedGovernanceTemplate.value"
        :current-session-id="operationsWorkspace.currentSessionId.value"
        :governance-action-loading-request-id="operationsWorkspace.governanceActionLoadingRequestId.value"
        :governance-action-loading-type="operationsWorkspace.governanceActionLoadingType.value"
        :refresh-operations="operationsWorkspace.loadOperationsData"
        :create-governance-request="operationsWorkspace.onCreateGovernanceRequest"
        :review-governance-request="operationsWorkspace.onReviewGovernanceRequest"
        :execute-governance-request="operationsWorkspace.onExecuteGovernanceRequest"
        :rollback-governance-request="operationsWorkspace.onRollbackGovernanceRequest"
        @update-selected-template-code="onSelectedTemplateCodeChange"
        @update-governance-reason="onGovernanceReasonChange"
        @update-governance-scope-type="onGovernanceScopeTypeChange"
        @update-governance-scope-org-id="onGovernanceScopeOrgIdChange"
        @update-governance-second-reviewer-user-id="onGovernanceSecondReviewerChange"
        @update-review-note="onReviewNoteChange"
        @update-approval-note="onApprovalNoteChange"
        @update-rollback-reason="onRollbackReasonChange"
      />

      <SuiteRemediationPanel
        :priority-actions="operationsWorkspace.priorityActions.value"
        :last-execution-result="operationsWorkspace.lastExecutionResult.value"
        :running-action-code="operationsWorkspace.runningActionCode.value"
        :execute-action="operationsWorkspace.onExecuteAction"
      />
    </section>
  </div>
</template>

<style scoped>
.suite-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
