<script setup lang="ts">
withDefaults(
  defineProps<{
    actionLabel?: string
    description: string
    title: string
    variant?: 'empty' | 'error' | 'permission' | 'premium'
  }>(),
  {
    actionLabel: undefined,
    variant: 'empty'
  }
)

const emit = defineEmits<{
  action: []
}>()
</script>

<template>
  <section class="empty-state" :class="`empty-state--${variant}`">
    <div class="empty-state__icon" aria-hidden="true">
      <span v-if="variant === 'error'">!</span>
      <span v-else-if="variant === 'permission'">×</span>
      <span v-else-if="variant === 'premium'">◆</span>
      <span v-else>∅</span>
    </div>
    <div class="empty-state__copy">
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
    </div>
    <button v-if="actionLabel" class="empty-state__action" type="button" @click="emit('action')">
      {{ actionLabel }}
    </button>
  </section>
</template>

<style scoped>
.empty-state {
  display: grid;
  justify-items: center;
  gap: 14px;
  padding: 36px 24px;
  border: 1px dashed var(--mm-border-strong);
  border-radius: var(--mm-radius-lg);
  background: var(--mm-surface-soft);
  text-align: center;
}

.empty-state__icon {
  display: inline-grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 999px;
  background: var(--mm-brand-soft);
  color: var(--mm-brand-primary);
  font-weight: 800;
}

.empty-state__copy h2 {
  margin: 0;
  color: var(--mm-text-primary);
  font-size: 18px;
  letter-spacing: -0.03em;
}

.empty-state__copy p {
  max-width: 460px;
  margin: 8px 0 0;
  color: var(--mm-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.empty-state__action {
  min-height: 36px;
  padding: 0 14px;
  border: 0;
  border-radius: var(--mm-radius-sm);
  background: var(--mm-brand-primary);
  color: var(--mm-brand-contrast);
  font-weight: 700;
}

.empty-state--error .empty-state__icon {
  background: color-mix(in srgb, var(--mm-danger) 12%, white);
  color: var(--mm-danger);
}

.empty-state--permission .empty-state__icon {
  background: color-mix(in srgb, var(--mm-warning) 12%, white);
  color: var(--mm-warning);
}

.empty-state--premium .empty-state__icon {
  background: color-mix(in srgb, var(--mm-premium) 12%, white);
  color: var(--mm-premium);
}
</style>
