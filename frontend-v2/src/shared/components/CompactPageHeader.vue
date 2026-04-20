<script setup lang="ts">
import MaturityBadge from '@/shared/components/MaturityBadge.vue'
import { useLocaleText, type TextLike } from '@/locales'

const { tr } = useLocaleText()

withDefaults(
  defineProps<{
    eyebrow: TextLike
    title: TextLike
    description?: TextLike
    badge?: TextLike
    badgeTone?: 'neutral' | 'beta' | 'preview' | 'ga'
  }>(),
  {
    badge: undefined,
    badgeTone: 'neutral',
    description: undefined
  }
)
</script>

<template>
  <header class="compact-page-header">
    <div class="compact-page-header__copy">
      <span class="section-label">{{ tr(eyebrow) }}</span>
      <div class="compact-page-header__title-row">
        <h1>{{ tr(title) }}</h1>
        <maturity-badge v-if="badge && badgeTone !== 'neutral'" :level="badgeTone" :text="badge" />
        <span v-else-if="badge" class="compact-page-header__badge">{{ tr(badge) }}</span>
      </div>
      <p v-if="description" class="page-subtitle">{{ tr(description) }}</p>
    </div>
    <slot />
  </header>
</template>

<style scoped>
.compact-page-header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
}

.compact-page-header__title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.compact-page-header__copy {
  min-width: 0;
}

h1 {
  margin: 4px 0 0;
  font-size: 22px;
  line-height: 1.05;
  letter-spacing: -0.04em;
}

.compact-page-header__badge {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid var(--mm-border);
  background: var(--mm-card-muted);
  color: var(--mm-text-secondary);
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

@media (max-width: 860px) {
  .compact-page-header {
    align-items: start;
    flex-direction: column;
  }
}
</style>
