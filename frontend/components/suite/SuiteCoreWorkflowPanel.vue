<script setup lang="ts">
import { computed } from 'vue'
import {
  COMMUNITY_V1_CORE_WORKFLOW_MODULES,
  filterCommunityMainlineProductItems
} from '~/constants/module-maturity'
import { useI18n } from '~/composables/useI18n'
import type { SuiteProductItem } from '~/types/api'

const props = defineProps<{
  products: SuiteProductItem[]
}>()

const { t } = useI18n()

const workflowItems = computed(() => {
  const visibleCodes = new Set(filterCommunityMainlineProductItems(props.products).map(item => item.code))
  return COMMUNITY_V1_CORE_WORKFLOW_MODULES.filter(item => visibleCodes.has(item.productCode))
})
</script>

<template>
  <section class="mm-card workflow-panel" data-testid="suite-core-workflow-panel">
    <div class="workflow-panel__copy">
      <span class="workflow-panel__eyebrow">{{ t('suite.sectionOverview.workflows.eyebrow') }}</span>
      <h2 class="mm-section-title">{{ t('suite.sectionOverview.workflows.title') }}</h2>
      <p class="mm-muted">{{ t('suite.sectionOverview.workflows.description') }}</p>
    </div>

    <div class="workflow-panel__grid">
      <article
        v-for="item in workflowItems"
        :key="item.productCode"
        class="workflow-panel__card"
        :data-testid="`suite-core-workflow-${item.productCode.toLowerCase()}`"
      >
        <div class="workflow-panel__card-head">
          <div>
            <span class="workflow-panel__product">{{ t(`organizations.products.${item.productCode}`) }}</span>
            <h3 class="workflow-panel__title">{{ t(item.titleKey) }}</h3>
          </div>
          <span class="workflow-panel__status">{{ t(item.statusKey) }}</span>
        </div>
        <p class="workflow-panel__description">{{ t(item.descriptionKey) }}</p>
        <p class="workflow-panel__boundary">{{ t(item.boundaryKey) }}</p>
        <NuxtLink class="workflow-panel__action" :to="item.route">
          {{ t(item.actionKey) }}
        </NuxtLink>
      </article>
    </div>
  </section>
</template>

<style scoped>
.workflow-panel {
  display: grid;
  gap: 16px;
  padding: 20px;
  border: 1px solid rgba(14, 98, 92, 0.12);
  background:
    radial-gradient(circle at top right, rgba(18, 126, 120, 0.14), transparent 40%),
    linear-gradient(180deg, rgba(247, 252, 251, 0.98), rgba(255, 255, 255, 0.95));
}

.workflow-panel__copy {
  display: grid;
  gap: 8px;
}

.workflow-panel__eyebrow,
.workflow-panel__product {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #0f6e6e;
}

.workflow-panel__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}

.workflow-panel__card {
  display: grid;
  gap: 10px;
  min-height: 220px;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(16, 109, 102, 0.14);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 14px 32px rgba(15, 79, 75, 0.06);
}

.workflow-panel__card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.workflow-panel__title {
  margin: 6px 0 0;
  font-size: 18px;
  color: #133736;
}

.workflow-panel__status {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(15, 110, 110, 0.1);
  color: #0f6e6e;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.workflow-panel__description,
.workflow-panel__boundary {
  margin: 0;
  color: var(--mm-muted);
  line-height: 1.6;
}

.workflow-panel__boundary {
  font-size: 13px;
}

.workflow-panel__action {
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
  .workflow-panel__grid {
    grid-template-columns: 1fr;
  }
}
</style>
