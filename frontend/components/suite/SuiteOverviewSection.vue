<script setup lang="ts">
import { computed } from 'vue'
import { filterCommunityCoreProductItems } from '~/constants/module-maturity'
import type { SuiteProductItem } from '~/types/api'
import { useI18n } from '~/composables/useI18n'
import type { SuiteSectionCode, SuiteSectionDefinition } from '~/utils/suite-sections'
import SuiteCoreWorkflowPanel from '~/components/suite/SuiteCoreWorkflowPanel.vue'
import SuiteProductHubPanel from '~/components/suite/SuiteProductHubPanel.vue'

const props = defineProps<{
  products: SuiteProductItem[]
  sections: readonly SuiteSectionDefinition[]
}>()

const emit = defineEmits<{
  select: [section: SuiteSectionCode]
}>()

const { t } = useI18n()

const jumpSections = computed(() => props.sections.filter(section => section.code !== 'overview'))
const coreProducts = computed(() => filterCommunityCoreProductItems(props.products))

function onSelect(section: SuiteSectionCode): void {
  emit('select', section)
}
</script>

<template>
  <section class="suite-overview" data-testid="suite-section-overview">
    <article class="mm-card suite-overview__intro">
      <p class="eyebrow">{{ t('suite.sectionOverview.badge') }}</p>
      <h2 class="mm-section-title">{{ t('suite.sectionOverview.title') }}</h2>
      <p class="mm-muted">{{ t('suite.sectionOverview.description') }}</p>
    </article>

    <div class="suite-overview__grid">
      <button
        v-for="section in jumpSections"
        :key="section.code"
        type="button"
        class="suite-overview__card"
        :data-testid="`suite-overview-card-${section.code}`"
        @click="onSelect(section.code)"
      >
        <span class="suite-overview__eyebrow">{{ t('suite.sectionOverview.action') }}</span>
        <strong class="suite-overview__title">{{ t(section.labelKey) }}</strong>
        <span class="suite-overview__description">{{ t(section.descriptionKey) }}</span>
      </button>
    </div>

    <SuiteCoreWorkflowPanel :products="coreProducts" />

    <SuiteProductHubPanel :products="coreProducts" />
  </section>
</template>

<style scoped>
.suite-overview {
  display: grid;
  gap: 16px;
}

.suite-overview__intro {
  padding: 20px;
  border: 1px solid rgba(16, 77, 73, 0.08);
  background:
    radial-gradient(circle at top right, rgba(18, 126, 120, 0.1), transparent 42%),
    linear-gradient(180deg, rgba(247, 252, 251, 0.98), rgba(255, 255, 255, 0.95));
}

.suite-overview__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.suite-overview__card {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid rgba(16, 77, 73, 0.12);
  border-radius: 16px;
  text-align: left;
  background: rgba(255, 255, 255, 0.92);
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.suite-overview__card:hover,
.suite-overview__card:focus-visible {
  transform: translateY(-1px);
  border-color: rgba(16, 109, 102, 0.32);
  box-shadow: 0 14px 32px rgba(15, 79, 75, 0.08);
  outline: none;
}

.suite-overview__eyebrow,
.eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #0f6e6e;
}

.eyebrow {
  margin: 0 0 6px;
}

.suite-overview__title {
  font-size: 16px;
  color: #133736;
}

.suite-overview__description {
  color: var(--mm-muted);
  line-height: 1.5;
}
</style>
