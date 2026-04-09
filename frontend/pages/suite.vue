<script setup lang="ts">
import { computed } from 'vue'
import SuiteBillingSection from '~/components/suite/SuiteBillingSection.vue'
import SuiteBoundarySection from '~/components/suite/SuiteBoundarySection.vue'
import SuiteOperationsSection from '~/components/suite/SuiteOperationsSection.vue'
import SuiteOverviewSection from '~/components/suite/SuiteOverviewSection.vue'
import SuitePlansSection from '~/components/suite/SuitePlansSection.vue'
import SuitePlansHero from '~/components/suite/SuitePlansHero.vue'
import SuiteSectionNav from '~/components/suite/SuiteSectionNav.vue'
import { useI18n } from '~/composables/useI18n'
import { useSuiteBillingCenterWorkspace } from '~/composables/useSuiteBillingCenterWorkspace'
import { useSuiteBillingWorkspace } from '~/composables/useSuiteBillingWorkspace'
import { useSuiteOperationsWorkspace } from '~/composables/useSuiteOperationsWorkspace'
import { useSuiteOverviewWorkspace } from '~/composables/useSuiteOverviewWorkspace'
import { useSuitePlansWorkspace } from '~/composables/useSuitePlansWorkspace'
import type { SuitePlanCode } from '~/types/suite-lumo'
import {
  buildSuiteSectionQuery,
  resolveSuiteSection,
  SUITE_SECTIONS,
  type SuiteSectionCode
} from '~/utils/suite-sections'

definePageMeta({
  layout: 'default'
})

const { t } = useI18n()
const route = useRoute()
const billingWorkspace = useSuiteBillingWorkspace()
const billingCenterWorkspace = useSuiteBillingCenterWorkspace()
const plansWorkspace = useSuitePlansWorkspace()
const operationsWorkspace = useSuiteOperationsWorkspace()
const overviewWorkspace = useSuiteOverviewWorkspace()
const activeSection = computed(() => resolveSuiteSection(route.query.section))

const pageLoading = computed(() => {
  return billingWorkspace.loading.value
    || billingCenterWorkspace.loading.value
    || plansWorkspace.loading.value
    || operationsWorkspace.loading.value
    || overviewWorkspace.loading.value
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

async function onSelectSection(section: SuiteSectionCode): Promise<void> {
  if (section === activeSection.value) {
    return
  }
  await navigateTo({
    path: '/suite',
    query: buildSuiteSectionQuery(route.query, section)
  }, {
    replace: true
  })
}

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

      <SuiteSectionNav
        :active-section="activeSection"
        :sections="SUITE_SECTIONS"
        @select="void onSelectSection($event)"
      />

      <div
        class="suite-section-shell"
        :data-section="activeSection"
        data-testid="suite-section-shell"
      >
        <SuiteOverviewSection
          v-if="activeSection === 'overview'"
          :products="plansWorkspace.visibleProducts.value"
          :sections="SUITE_SECTIONS"
          :collaboration-items="overviewWorkspace.collaborationItems.value"
          :collaboration-loading="overviewWorkspace.loading.value"
          @select="void onSelectSection($event)"
        />

        <SuitePlansSection
          v-else-if="activeSection === 'plans'"
          :plans-workspace="plansWorkspace"
          :change-plan="onChangePlan"
        />

        <SuiteBillingSection
          v-else-if="activeSection === 'billing'"
          :billing-workspace="billingWorkspace"
          :billing-center-workspace="billingCenterWorkspace"
          :save-draft="onSaveDraft"
        />

        <SuiteOperationsSection
          v-else-if="activeSection === 'operations'"
          :plans-workspace="plansWorkspace"
          :operations-workspace="operationsWorkspace"
        />

        <SuiteBoundarySection v-else />
      </div>
    </section>
  </div>
</template>

<style scoped>
.suite-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.suite-section-shell {
  display: grid;
  gap: 16px;
}
</style>
