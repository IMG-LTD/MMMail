<script setup lang="ts">
import { computed } from 'vue'
import {
  COMMUNITY_V1_MAINLINE_JOURNEY_STAGES,
  filterCommunityMainlineProductItems
} from '~/constants/module-maturity'
import { useI18n } from '~/composables/useI18n'
import type { SuiteProductItem } from '~/types/api'

const props = defineProps<{
  products: SuiteProductItem[]
}>()

const { t } = useI18n()

const stages = computed(() => {
  const visibleCodes = new Set(filterCommunityMainlineProductItems(props.products).map(item => item.code))
  return COMMUNITY_V1_MAINLINE_JOURNEY_STAGES.filter(stage => visibleCodes.has(stage.productCode))
})
</script>

<template>
  <section class="mm-card journey-panel" data-testid="suite-mainline-journey-panel">
    <div class="journey-panel__copy">
      <span class="journey-panel__eyebrow">{{ t('suite.sectionOverview.mainline.eyebrow') }}</span>
      <h2 class="mm-section-title">{{ t('suite.sectionOverview.mainline.title') }}</h2>
      <p class="mm-muted">{{ t('suite.sectionOverview.mainline.description') }}</p>
    </div>

    <div class="journey-panel__grid">
      <article
        v-for="(stage, index) in stages"
        :key="stage.productCode"
        class="journey-panel__card"
        :data-testid="`suite-mainline-stage-${stage.productCode.toLowerCase()}`"
      >
        <div class="journey-panel__card-head">
          <div class="journey-panel__step-badge">{{ index + 1 }}</div>
          <span class="journey-panel__product">{{ t(`organizations.products.${stage.productCode}`) }}</span>
        </div>
        <h3 class="journey-panel__title">{{ t(stage.titleKey) }}</h3>
        <p class="journey-panel__description">{{ t(stage.descriptionKey) }}</p>
        <dl class="journey-panel__details">
          <div>
            <dt>{{ t('suite.sectionOverview.mainline.proofLabel') }}</dt>
            <dd>{{ t(stage.proofKey) }}</dd>
          </div>
          <div>
            <dt>{{ t('suite.sectionOverview.mainline.handoffLabel') }}</dt>
            <dd>{{ t(stage.handoffKey) }}</dd>
          </div>
        </dl>
        <NuxtLink class="journey-panel__action" :to="stage.route">
          {{ t(stage.actionKey) }}
        </NuxtLink>
      </article>
    </div>
  </section>
</template>

<style scoped>
.journey-panel {
  display: grid;
  gap: 16px;
  padding: 20px;
  border: 1px solid rgba(19, 88, 96, 0.14);
  background:
    radial-gradient(circle at top left, rgba(15, 122, 116, 0.14), transparent 42%),
    linear-gradient(180deg, rgba(246, 251, 252, 0.98), rgba(255, 255, 255, 0.97));
}

.journey-panel__copy {
  display: grid;
  gap: 8px;
}

.journey-panel__eyebrow,
.journey-panel__product {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #0f6e6e;
}

.journey-panel__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 12px;
}

.journey-panel__card {
  display: grid;
  gap: 12px;
  padding: 18px;
  border-radius: 18px;
  border: 1px solid rgba(15, 110, 110, 0.14);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 32px rgba(15, 79, 75, 0.06);
}

.journey-panel__card-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.journey-panel__step-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: linear-gradient(135deg, #0c5a5a, #0f7a74);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.journey-panel__title {
  margin: 0;
  font-size: 18px;
  color: #12363a;
}

.journey-panel__description {
  margin: 0;
  color: var(--mm-muted);
  line-height: 1.6;
}

.journey-panel__details {
  display: grid;
  gap: 10px;
  margin: 0;
}

.journey-panel__details div {
  display: grid;
  gap: 4px;
}

.journey-panel__details dt {
  font-size: 12px;
  font-weight: 700;
  color: #0f6e6e;
}

.journey-panel__details dd {
  margin: 0;
  color: #4a6670;
  line-height: 1.5;
}

.journey-panel__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  width: fit-content;
  padding: 0 14px;
  border-radius: 999px;
  text-decoration: none;
  color: #fff;
  background: linear-gradient(135deg, #0c5a5a, #0f7a74);
}

@media (max-width: 1120px) {
  .journey-panel__grid {
    grid-template-columns: 1fr;
  }
}
</style>
