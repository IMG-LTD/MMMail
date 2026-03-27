<script setup lang="ts">
import type { OrganizationSummaryCard } from '~/types/organization-admin'

defineProps<{
  cards: OrganizationSummaryCard[]
}>()
</script>

<template>
  <section class="summary-grid">
    <article v-for="card in cards" :key="card.label" class="mm-card summary-card">
      <div class="summary-label">{{ card.label }}</div>
      <div class="summary-value">{{ card.value }}</div>
      <div class="summary-hint">{{ card.hint }}</div>
    </article>
  </section>
</template>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.summary-card {
  padding: 20px;
}

.summary-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--mm-muted);
}

.summary-value {
  margin-top: 10px;
  font-size: 26px;
  font-weight: 700;
  color: var(--mm-primary-dark);
}

.summary-hint {
  color: var(--mm-muted);
}
</style>
