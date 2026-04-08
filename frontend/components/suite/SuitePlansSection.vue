<script setup lang="ts">
import type { PropType } from 'vue'
import { useSuitePlansWorkspace } from '~/composables/useSuitePlansWorkspace'
import type { SuitePlanCode } from '~/types/suite-lumo'
import SuiteCapabilityMatrixPanel from '~/components/suite/SuiteCapabilityMatrixPanel.vue'
import SuitePlanCatalogPanel from '~/components/suite/SuitePlanCatalogPanel.vue'

type SuitePlansWorkspaceState = ReturnType<typeof useSuitePlansWorkspace>

const props = defineProps({
  plansWorkspace: {
    type: Object as PropType<SuitePlansWorkspaceState>,
    required: true
  },
  changePlan: {
    type: Function as PropType<(planCode: SuitePlanCode) => Promise<boolean>>,
    required: true
  }
})
</script>

<template>
  <section class="suite-section-stack" data-testid="suite-section-plans">
    <SuitePlanCatalogPanel
      :sections="props.plansWorkspace.planSections.value"
      :changing-plan="props.plansWorkspace.changingPlan.value"
      :is-current-plan="props.plansWorkspace.isCurrentPlan"
      :plan-quota-rows="props.plansWorkspace.planQuotaRows"
      :change-plan="props.changePlan"
    />

    <SuiteCapabilityMatrixPanel
      :plans="props.plansWorkspace.plans.value"
      :product-columns="props.plansWorkspace.productColumns.value"
    />
  </section>
</template>

<style scoped>
.suite-section-stack {
  display: grid;
  gap: 16px;
}
</style>
