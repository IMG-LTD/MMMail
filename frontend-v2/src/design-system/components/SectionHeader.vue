<script setup lang="ts">
defineProps<{
  description?: string;
  eyebrow?: string;
  title: string;
}>();
</script>

<template>
  <header class="section-header">
    <div class="section-header__copy">
      <span v-if="eyebrow" class="section-label">{{ eyebrow }}</span>
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="section-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-header__copy {
  display: grid;
  gap: 6px;
}

.section-header__copy h2 {
  margin: 0;
  color: var(--mm-text-primary);
  font-size: 20px;
  letter-spacing: -0.035em;
}

.section-header__copy p {
  margin: 0;
  color: var(--mm-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.section-header__actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

@media (max-width: 767px) {
  .section-header {
    display: grid;
  }

  .section-header__actions {
    justify-content: flex-start;
  }
}
</style>
