<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  description: string
}>()
</script>

<template>
  <header class="hero">
    <div>
      <p class="section-label">{{ eyebrow }}</p>
      <h1 class="page-title">{{ title }}</h1>
    </div>
    <p class="page-subtitle">{{ description }}</p>
  </header>
</template>

<style scoped>
.hero {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
}

.hero .page-title {
  margin-top: 8px;
}

.hero .page-subtitle {
  max-width: 420px;
}

@media (max-width: 900px) {
  .hero {
    flex-direction: column;
    align-items: start;
  }
}
</style>
