<script setup lang="ts">
const props = defineProps<{
  title: string
  subtitle: string
  primaryLabel: string
  secondaryLabel: string
}>()
</script>

<template>
  <section class="marketing-hero mm-card" data-testid="marketing-hero">
    <p class="marketing-hero__eyebrow">MMMail Community Edition</p>
    <h1>{{ props.title }}</h1>
    <p class="marketing-hero__subtitle">{{ props.subtitle }}</p>
    <div class="marketing-hero__actions">
      <NuxtLink to="/login" class="marketing-hero__primary">{{ props.primaryLabel }}</NuxtLink>
      <NuxtLink to="/suite?section=boundary" class="marketing-hero__secondary">{{ props.secondaryLabel }}</NuxtLink>
    </div>
  </section>
</template>
