<script setup lang="ts">
import type { PropType } from 'vue'
import { useSuiteBillingCenterWorkspace } from '~/composables/useSuiteBillingCenterWorkspace'
import { useSuiteBillingWorkspace } from '~/composables/useSuiteBillingWorkspace'
import SuiteBillingCenterPanel from '~/components/suite/SuiteBillingCenterPanel.vue'
import SuiteBillingOverviewPanel from '~/components/suite/SuiteBillingOverviewPanel.vue'
import SuiteCheckoutPanel from '~/components/suite/SuiteCheckoutPanel.vue'
import SuitePricingComparePanel from '~/components/suite/SuitePricingComparePanel.vue'

type SuiteBillingWorkspaceState = ReturnType<typeof useSuiteBillingWorkspace>
type SuiteBillingCenterWorkspaceState = ReturnType<typeof useSuiteBillingCenterWorkspace>

const props = defineProps({
  billingWorkspace: {
    type: Object as PropType<SuiteBillingWorkspaceState>,
    required: true
  },
  billingCenterWorkspace: {
    type: Object as PropType<SuiteBillingCenterWorkspaceState>,
    required: true
  },
  saveDraft: {
    type: Function as PropType<() => Promise<void>>,
    required: true
  }
})
</script>

<template>
  <section class="suite-section-stack" data-testid="suite-section-billing">
    <SuiteBillingOverviewPanel
      :overview="props.billingWorkspace.overview.value"
      :selected-offer-code="props.billingWorkspace.selectedOfferCode.value"
      :restore-latest-draft="props.billingWorkspace.restoreLatestDraft"
    />

    <SuitePricingComparePanel
      :sections="props.billingWorkspace.pricingSections.value"
      :active-plan-code="props.billingWorkspace.overview.value?.activePlanCode || null"
      :selected-offer-code="props.billingWorkspace.selectedOfferCode.value"
      :select-offer="props.billingWorkspace.onSelectOffer"
    />

    <SuiteCheckoutPanel
      :selected-offer="props.billingWorkspace.selectedOffer.value"
      :selected-billing-cycle="props.billingWorkspace.selectedBillingCycle.value"
      :seat-count="props.billingWorkspace.seatCount.value"
      :organization-name="props.billingWorkspace.organizationName.value"
      :domain-name="props.billingWorkspace.domainName.value"
      :quote="props.billingWorkspace.quote.value"
      :quote-loading="props.billingWorkspace.quoteLoading.value"
      :draft-loading="props.billingWorkspace.draftLoading.value"
      :show-organization-fields="props.billingWorkspace.showOrganizationFields.value"
      :refresh-quote="props.billingWorkspace.refreshQuote"
      :save-draft="props.saveDraft"
      @update:selected-billing-cycle="props.billingWorkspace.selectedBillingCycle.value = $event"
      @update:seat-count="props.billingWorkspace.seatCount.value = $event"
      @update:organization-name="props.billingWorkspace.organizationName.value = $event"
      @update:domain-name="props.billingWorkspace.domainName.value = $event"
    />

    <SuiteBillingCenterPanel
      :center="props.billingCenterWorkspace.center.value"
      :payment-method-saving="props.billingCenterWorkspace.paymentMethodSaving.value"
      :action-loading-code="props.billingCenterWorkspace.actionLoadingCode.value"
      :add-payment-method="props.billingCenterWorkspace.addPaymentMethod"
      :set-default-payment-method="props.billingCenterWorkspace.setDefaultPaymentMethod"
      :execute-subscription-action="props.billingCenterWorkspace.executeSubscriptionAction"
    />
  </section>
</template>

<style scoped>
.suite-section-stack {
  display: grid;
  gap: 16px;
}
</style>
