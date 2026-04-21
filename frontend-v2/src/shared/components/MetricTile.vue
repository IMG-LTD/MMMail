<script setup lang="ts">
defineProps<{
  label: string
  value: string
  meta: string
}>()
</script>

<template>
  <article class="metric-tile surface-card">
    <span class="section-label">{{ label }}</span>
    <strong>{{ value }}</strong>
    <span>{{ meta }}</span>
  </article>
</template>

<style scoped>
.metric-tile {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
}

strong {
  font-size: 28px;
  line-height: 1;
  letter-spacing: -0.04em;
  color: var(--mm-ink);
}

span:last-child {
  color: var(--mm-text-secondary);
  font-size: 12px;
}
</style>
