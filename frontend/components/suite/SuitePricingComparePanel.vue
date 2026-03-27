<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { SuiteOfferCode } from '~/types/suite-lumo'
import type { SuitePricingSection } from '~/utils/suite-billing'

interface Props {
  sections: SuitePricingSection[]
  activePlanCode: string | null
  selectedOfferCode: SuiteOfferCode | null
  selectOffer: (offerCode: SuiteOfferCode) => Promise<void>
}

const props = defineProps<Props>()
const { t } = useI18n()
</script>

<template>
  <section class="mm-card pricing-compare-panel">
    <div class="panel-head">
      <div>
        <p class="eyebrow">{{ t('suite.billing.compare.badge') }}</p>
        <h2 class="mm-section-title">{{ t('suite.billing.compare.title') }}</h2>
        <p class="mm-muted">{{ t('suite.billing.compare.subtitle') }}</p>
      </div>
    </div>

    <section
      v-for="section in props.sections"
      :key="section.segment"
      class="compare-section"
    >
      <header class="compare-section-head">
        <h3 class="mm-section-subtitle">{{ t(`suite.plans.segment.${section.segment}.title`) }}</h3>
        <span class="mm-muted">{{ t(`suite.plans.segment.${section.segment}.subtitle`) }}</span>
      </header>

      <div class="compare-grid">
        <article
          v-for="offer in section.offers"
          :key="offer.code"
          class="offer-card"
          :class="{ 'offer-card--selected': props.selectedOfferCode === offer.code }"
        >
          <div class="offer-card-top">
            <div>
              <div class="offer-card-tags">
                <el-tag effect="dark" type="primary">{{ offer.code }}</el-tag>
                <el-tag v-if="offer.marketingBadge" effect="plain" type="warning">{{ offer.marketingBadge }}</el-tag>
                <el-tag
                  v-if="props.activePlanCode === offer.linkedPlanCode"
                  effect="plain"
                  type="success"
                >
                  {{ t('suite.billing.compare.currentEntitlement') }}
                </el-tag>
              </div>
              <h4 class="offer-name">{{ offer.name }}</h4>
              <p class="offer-description">{{ offer.description }}</p>
            </div>
            <div class="offer-price-block">
              <span class="offer-price">{{ offer.priceValue || t('suite.plans.price.contactSales') }}</span>
              <span v-if="offer.originalPriceValue" class="offer-original">{{ offer.originalPriceValue }}</span>
              <span class="offer-mode">{{ t(`suite.billing.checkoutMode.${offer.checkoutMode}`) }}</span>
            </div>
          </div>

          <p v-if="offer.priceNote" class="offer-note">{{ offer.priceNote }}</p>

          <div class="offer-meta">
            <span>
              {{ t('suite.billing.compare.billingCycles') }}:
              {{ offer.billingCycles.map(cycle => t(`suite.billing.billingCycle.${cycle}`)).join(' / ') }}
            </span>
            <span>{{ t('suite.billing.compare.seatCount', { count: offer.defaultSeatCount }) }}</span>
          </div>

          <ul class="offer-highlights">
            <li v-for="(highlight, index) in offer.highlights" :key="`${offer.code}-${index}`">
              {{ highlight }}
            </li>
          </ul>

          <div class="offer-products">
            <el-tag
              v-for="productCode in offer.enabledProducts"
              :key="`${offer.code}-${productCode}`"
              effect="plain"
            >
              {{ productCode }}
            </el-tag>
          </div>

          <el-button
            class="offer-action"
            :type="props.selectedOfferCode === offer.code ? 'success' : 'primary'"
            @click="void props.selectOffer(offer.code)"
          >
            {{ props.selectedOfferCode === offer.code
              ? t('suite.billing.compare.selected')
              : t('suite.billing.compare.select') }}
          </el-button>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.pricing-compare-panel {
  --suite-billing-accent: #6d78ff;
  --suite-billing-ink: #142445;
  --suite-billing-muted: #62708d;
  --suite-billing-border: rgba(94, 110, 190, 0.14);
  padding: 24px;
  border: 1px solid var(--suite-billing-border);
  background:
    linear-gradient(135deg, rgba(11, 20, 42, 0.02), transparent 44%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 248, 255, 0.95));
}

.panel-head {
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 6px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 12px;
  color: var(--suite-billing-accent);
}

.compare-section + .compare-section {
  margin-top: 22px;
}

.compare-section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: baseline;
  margin-bottom: 12px;
}

.compare-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.offer-card {
  border-radius: 22px;
  padding: 18px;
  border: 1px solid var(--suite-billing-border);
  background:
    radial-gradient(circle at top right, rgba(109, 120, 255, 0.08), transparent 26%),
    rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.offer-card--selected {
  border-color: rgba(109, 120, 255, 0.48);
  box-shadow: 0 24px 48px rgba(109, 120, 255, 0.14);
  transform: translateY(-2px);
}

.offer-card-top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.offer-card-tags,
.offer-products {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.offer-name {
  margin: 12px 0 8px;
  color: var(--suite-billing-ink);
  font-size: 22px;
}

.offer-description,
.offer-note,
.offer-meta,
.offer-highlights {
  margin: 0;
  color: var(--suite-billing-muted);
}

.offer-price-block {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
  min-width: 130px;
}

.offer-price {
  font-size: 28px;
  font-weight: 700;
  color: var(--suite-billing-ink);
}

.offer-original {
  text-decoration: line-through;
  color: var(--suite-billing-muted);
}

.offer-mode {
  color: var(--suite-billing-accent);
  font-size: 13px;
  font-weight: 600;
}

.offer-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
}

.offer-highlights {
  padding-left: 18px;
  display: grid;
  gap: 6px;
}

.offer-action {
  margin-top: auto;
}

@media (max-width: 1120px) {
  .compare-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .offer-card-top,
  .compare-section-head,
  .offer-meta {
    flex-direction: column;
  }

  .offer-price-block {
    align-items: flex-start;
  }
}
</style>
