<script setup lang="ts">
type StatusTone = 'neutral' | 'success' | 'info' | 'warning' | 'danger' | 'premium' | 'hosted' | 'preview'

withDefaults(
  defineProps<{
    compact?: boolean
    label: string
    tone?: StatusTone
  }>(),
  {
    compact: false,
    tone: 'neutral'
  }
)
</script>

<template>
  <span class="status-badge" :class="[`status-badge--${tone}`, { 'status-badge--compact': compact }]">
    {{ label }}
  </span>
</template>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border: 1px solid var(--mm-border);
  border-radius: 999px;
  background: var(--mm-surface-soft);
  color: var(--mm-text-secondary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.status-badge--compact {
  min-height: 18px;
  padding: 0 7px;
  font-size: 9px;
}

.status-badge--success {
  border-color: color-mix(in srgb, var(--mm-success) 24%, white);
  background: color-mix(in srgb, var(--mm-success) 11%, white);
  color: var(--mm-success);
}

.status-badge--info {
  border-color: color-mix(in srgb, var(--mm-info) 24%, white);
  background: color-mix(in srgb, var(--mm-info) 11%, white);
  color: var(--mm-info);
}

.status-badge--warning {
  border-color: color-mix(in srgb, var(--mm-warning) 26%, white);
  background: color-mix(in srgb, var(--mm-warning) 12%, white);
  color: var(--mm-warning);
}

.status-badge--danger {
  border-color: color-mix(in srgb, var(--mm-danger) 24%, white);
  background: color-mix(in srgb, var(--mm-danger) 11%, white);
  color: var(--mm-danger);
}

.status-badge--premium {
  border-color: color-mix(in srgb, var(--mm-premium) 26%, white);
  background: color-mix(in srgb, var(--mm-premium) 12%, white);
  color: var(--mm-premium);
}

.status-badge--hosted {
  border-color: color-mix(in srgb, var(--mm-hosted) 26%, white);
  background: color-mix(in srgb, var(--mm-hosted) 12%, white);
  color: var(--mm-hosted);
}

.status-badge--preview {
  border-color: color-mix(in srgb, var(--mm-preview) 28%, white);
  background: color-mix(in srgb, var(--mm-preview) 14%, white);
  color: var(--mm-preview);
}
</style>
