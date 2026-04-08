<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { SuiteSectionCode, SuiteSectionDefinition } from '~/utils/suite-sections'

const props = defineProps<{
  activeSection: SuiteSectionCode
  sections: readonly SuiteSectionDefinition[]
}>()

const emit = defineEmits<{
  select: [section: SuiteSectionCode]
}>()

const { t } = useI18n()

const sectionCards = computed(() => props.sections.map(section => ({
  ...section,
  active: section.code === props.activeSection
})))

function onSelect(section: SuiteSectionCode): void {
  emit('select', section)
}
</script>

<template>
  <nav class="mm-card suite-section-nav" :aria-label="t('suite.sectionNav.title')" data-testid="suite-section-nav">
    <div class="suite-section-nav__head">
      <div>
        <p class="eyebrow">{{ t('suite.sectionNav.badge') }}</p>
        <h2 class="mm-section-title">{{ t('suite.sectionNav.title') }}</h2>
        <p class="mm-muted">{{ t('suite.sectionNav.description') }}</p>
      </div>
    </div>

    <div class="suite-section-nav__grid">
      <button
        v-for="section in sectionCards"
        :key="section.code"
        type="button"
        class="suite-section-nav__item"
        :class="{ 'suite-section-nav__item--active': section.active }"
        :aria-pressed="section.active ? 'true' : 'false'"
        :data-testid="`suite-section-tab-${section.code}`"
        @click="onSelect(section.code)"
      >
        <span class="suite-section-nav__state">
          {{ t(section.active ? 'suite.sectionNav.current' : 'suite.sectionNav.available') }}
        </span>
        <strong class="suite-section-nav__label">{{ t(section.labelKey) }}</strong>
        <span class="suite-section-nav__description">{{ t(section.descriptionKey) }}</span>
      </button>
    </div>
  </nav>
</template>

<style scoped>
.suite-section-nav {
  padding: 20px;
  display: grid;
  gap: 16px;
  border: 1px solid rgba(24, 63, 61, 0.08);
  background:
    linear-gradient(180deg, rgba(246, 251, 250, 0.98), rgba(255, 255, 255, 0.94)),
    #fff;
}

.suite-section-nav__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.suite-section-nav__item {
  display: grid;
  gap: 8px;
  padding: 16px;
  border: 1px solid rgba(16, 77, 73, 0.12);
  border-radius: 16px;
  text-align: left;
  background: rgba(255, 255, 255, 0.9);
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.suite-section-nav__item:hover,
.suite-section-nav__item:focus-visible {
  transform: translateY(-1px);
  border-color: rgba(16, 109, 102, 0.3);
  box-shadow: 0 12px 28px rgba(15, 64, 62, 0.08);
  outline: none;
}

.suite-section-nav__item--active {
  border-color: rgba(16, 109, 102, 0.42);
  background: linear-gradient(135deg, rgba(232, 248, 244, 0.95), rgba(255, 255, 255, 0.98));
  box-shadow: 0 14px 32px rgba(15, 79, 75, 0.12);
}

.suite-section-nav__state {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #0d6c68;
}

.suite-section-nav__label {
  font-size: 16px;
  color: #123735;
}

.suite-section-nav__description {
  color: var(--mm-muted);
  line-height: 1.5;
}

.eyebrow {
  margin: 0 0 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #0f6e6e;
}
</style>
