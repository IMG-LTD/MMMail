<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { SuitePlan, SuitePlanCode, SuitePlanPriceMode } from '~/types/suite-lumo'
import type { SuitePlanCatalogSection, SuitePlanJourneyQuotaRow } from '~/utils/suite-plans'

interface Props {
  sections: SuitePlanCatalogSection[]
  changingPlan: boolean
  isCurrentPlan: (planCode: SuitePlanCode) => boolean
  planQuotaRows: (plan: SuitePlan) => SuitePlanJourneyQuotaRow[]
  changePlan: (planCode: SuitePlanCode) => Promise<boolean>
}

const props = defineProps<Props>()
const { t } = useI18n()

function resolvePriceLabel(priceMode: SuitePlanPriceMode, priceValue: string | null): string {
  if (priceMode === 'FREE') {
    return t('suite.plans.price.free')
  }
  if (priceMode === 'FROM' && priceValue) {
    return t('suite.plans.price.from', { value: priceValue })
  }
  if (priceMode === 'PER_USER') {
    return t('suite.plans.price.perUser')
  }
  if (priceMode === 'CONTACT_SALES') {
    return t('suite.plans.price.contactSales')
  }
  return t('suite.plans.price.addOn')
}

function resolvePlanDescription(plan: SuitePlan): string {
  const key = `suite.plans.definition.${plan.code}.description`
  const translated = t(key)
  return translated === key ? plan.description : translated
}

function resolvePlanHighlight(plan: SuitePlan, index: number, fallback: string): string {
  const key = `suite.plans.definition.${plan.code}.highlights.${index + 1}`
  const translated = t(key)
  return translated === key ? fallback : translated
}
</script>

<template>
  <section class="mm-card suite-panel">
    <h2 class="mm-section-title">{{ t('suite.plans.catalogTitle') }}</h2>

    <section
      v-for="section in props.sections"
      :key="section.segment"
      class="plan-section"
    >
      <header class="plan-section-head">
        <div>
          <h3 class="mm-section-subtitle">{{ t(`suite.plans.segment.${section.segment}.title`) }}</h3>
          <p class="mm-muted">{{ t(`suite.plans.segment.${section.segment}.subtitle`) }}</p>
        </div>
      </header>

      <div class="plan-grid">
        <article v-for="plan in section.plans" :key="plan.code" class="plan-card">
          <div class="plan-top">
            <div class="plan-headline">
              <div class="plan-headline-row">
                <h4 class="plan-name">{{ plan.name }}</h4>
                <el-tag v-if="plan.recommended" type="warning">{{ t('suite.plans.recommended') }}</el-tag>
                <el-tag v-if="props.isCurrentPlan(plan.code)" type="success">
                  {{ t('suite.plans.currentPlan') }}
                </el-tag>
              </div>
              <div class="plan-code-row">
                <el-tag effect="plain">{{ plan.code }}</el-tag>
                <span class="plan-price">{{ resolvePriceLabel(plan.priceMode, plan.priceValue) }}</span>
              </div>
            </div>
          </div>

          <p class="plan-description">{{ resolvePlanDescription(plan) }}</p>

          <ul class="highlight-list">
            <li v-for="(highlight, index) in plan.highlights" :key="`${plan.code}-${index}`">
              {{ resolvePlanHighlight(plan, index, highlight) }}
            </li>
          </ul>

          <ul v-if="props.planQuotaRows(plan).length > 0" class="quota-list">
            <li v-for="row in props.planQuotaRows(plan)" :key="row.key">
              {{ t(`suite.plans.quota.${row.key}`) }}: {{ row.value }}
            </li>
          </ul>

          <p v-else class="mm-muted">{{ t('suite.plans.catalogEmpty') }}</p>

          <el-button
            type="primary"
            :disabled="props.isCurrentPlan(plan.code) || props.changingPlan"
            :loading="props.changingPlan && !props.isCurrentPlan(plan.code)"
            @click="void props.changePlan(plan.code)"
          >
            {{ props.isCurrentPlan(plan.code) ? t('suite.plans.currentPlan') : t('suite.plans.switchTo', { plan: plan.name }) }}
          </el-button>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.suite-panel {
  padding: 20px;
}

.plan-section + .plan-section {
  margin-top: 22px;
}

.plan-section-head {
  margin-bottom: 12px;
}

.plan-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.plan-card {
  border-radius: 20px;
  padding: 18px;
  border: 1px solid rgba(89, 105, 162, 0.12);
  background:
    radial-gradient(circle at top right, rgba(97, 111, 255, 0.08), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.99), rgba(246, 248, 255, 0.95));
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.plan-headline {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.plan-headline-row,
.plan-code-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.plan-name {
  margin: 0;
  font-size: 18px;
  color: #1d2946;
}

.plan-price {
  font-weight: 700;
  color: #4b5bdd;
}

.plan-description {
  margin: 0;
  color: #5e6a84;
  line-height: 1.62;
}

.highlight-list,
.quota-list {
  margin: 0;
  padding-left: 18px;
  display: grid;
  gap: 6px;
  color: #4e5b78;
}

@media (max-width: 1120px) {
  .plan-grid {
    grid-template-columns: 1fr;
  }
}
</style>
