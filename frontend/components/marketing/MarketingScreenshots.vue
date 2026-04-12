<script setup lang="ts">
defineProps<{ cards: Array<{ title: string; caption: string }> }>()
</script>

<template>
  <section class="marketing-shots" data-testid="marketing-screenshots">
    <article v-for="card in cards" :key="card.title" class="marketing-shots__card mm-card">
      <div class="marketing-shots__placeholder">{{ card.title }}</div>
      <p>{{ card.caption }}</p>
    </article>
  </section>
</template>
