<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { lt, useLocaleText } from '@/locales'
import { isRouteMatch, mobilePrimaryTabs } from './shell-nav'

const route = useRoute()
const router = useRouter()
const { tr } = useLocaleText()
</script>

<template>
  <nav class="mobile-tab-bar" :aria-label="tr(lt('移动主导航', '行動主導覽', 'Primary mobile navigation'))">
    <button
      v-for="tab in mobilePrimaryTabs"
      :key="tab.key"
      class="mobile-tab-bar__item"
      :class="{ 'mobile-tab-bar__item--active': isRouteMatch(route.path, tab) }"
      type="button"
      @click="router.push(tab.path)"
    >
      <span class="mobile-tab-bar__label">{{ tr(tab.label) }}</span>
      <span class="mobile-tab-bar__hint">{{ tr(tab.hint) }}</span>
    </button>
  </nav>
</template>

<style scoped>
.mobile-tab-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 30;
  display: none;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
  padding: 10px 12px calc(10px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--mm-border);
  background: color-mix(in srgb, var(--mm-card) 94%, transparent);
  backdrop-filter: blur(18px);
}

.mobile-tab-bar__item {
  display: grid;
  gap: 4px;
  min-height: 52px;
  padding: 8px 6px;
  border: 1px solid transparent;
  border-radius: 14px;
  background: transparent;
  text-align: center;
}

.mobile-tab-bar__item--active {
  border-color: var(--mm-accent-border);
  background: var(--mm-accent-soft);
}

.mobile-tab-bar__label {
  color: var(--mm-ink);
  font-size: 12px;
  font-weight: 600;
}

.mobile-tab-bar__hint {
  color: var(--mm-text-secondary);
  font-size: 10px;
}

@media (max-width: 820px) {
  .mobile-tab-bar {
    display: grid;
  }
}
</style>
