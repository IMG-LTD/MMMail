<script setup lang="ts">
interface MarketingTrustCard {
  title: string
  description: string
  href: string
  mode?: 'document' | 'route'
}

const props = defineProps<{ cards: MarketingTrustCard[] }>()

function isDocumentLink(card: MarketingTrustCard) {
  return card.mode === 'document'
}
</script>

<template>
  <section class="marketing-grid" data-testid="marketing-trust-grid">
    <template v-for="card in props.cards" :key="card.title">
      <a v-if="isDocumentLink(card)" :href="card.href" class="marketing-grid__card mm-card">
        <strong>{{ card.title }}</strong>
        <p>{{ card.description }}</p>
      </a>
      <NuxtLink v-else :to="card.href" class="marketing-grid__card mm-card">
        <strong>{{ card.title }}</strong>
        <p>{{ card.description }}</p>
      </NuxtLink>
    </template>
  </section>
</template>
