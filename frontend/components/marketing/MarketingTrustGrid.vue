<script setup lang="ts">
defineProps<{ cards: Array<{ title: string; description: string; href: string }> }>()
</script>

<template>
  <section class="marketing-grid" data-testid="marketing-trust-grid">
    <NuxtLink v-for="card in cards" :key="card.title" :to="card.href" class="marketing-grid__card mm-card">
      <strong>{{ card.title }}</strong>
      <p>{{ card.description }}</p>
    </NuxtLink>
  </section>
</template>
