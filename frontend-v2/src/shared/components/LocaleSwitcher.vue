<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/store/modules/app'
import { useLocaleText } from '@/locales'

const props = withDefaults(
  defineProps<{
    compact?: boolean
  }>(),
  {
    compact: false
  }
)

const appStore = useAppStore()
const { locale, options, tr } = useLocaleText()

const ariaLabel = computed(() => tr({ 'zh-CN': '语言切换', 'zh-TW': '語言切換', en: 'Language switcher' }))
</script>

<template>
  <div class="locale-switcher" :class="{ 'locale-switcher--compact': props.compact }" :aria-label="ariaLabel" role="group">
    <button
      v-for="option in options"
      :key="option.value"
      type="button"
      class="locale-switcher__option"
      :class="{ 'locale-switcher__option--active': option.value === locale }"
      :aria-pressed="option.value === locale"
      @click="appStore.setLocale(option.value)"
    >
      <span class="locale-switcher__badge">{{ option.badge }}</span>
      <span v-if="!props.compact" class="locale-switcher__label">{{ option.label }}</span>
    </button>
  </div>
</template>

<style scoped>
.locale-switcher {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--mm-border);
  border-radius: 12px;
  background: color-mix(in srgb, var(--mm-card) 92%, transparent);
  backdrop-filter: blur(14px);
}

.locale-switcher__option {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
  padding: 0 8px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--mm-text-secondary);
  transition:
    background-color 0.18s ease,
    color 0.18s ease;
}

.locale-switcher__option--active {
  background: var(--mm-accent-soft);
  color: var(--mm-primary);
}

.locale-switcher__badge {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.locale-switcher__label {
  font-size: 11px;
}

.locale-switcher--compact .locale-switcher__option {
  min-width: 34px;
  justify-content: center;
}
</style>
