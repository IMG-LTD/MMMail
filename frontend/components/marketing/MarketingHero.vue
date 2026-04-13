<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

const props = defineProps<{
  title: string
  subtitle: string
  primaryLabel: string
  secondaryLabel: string
}>()

const { t } = useI18n()
</script>

<template>
  <section class="marketing-hero mm-card" data-testid="marketing-hero">
    <p class="marketing-hero__eyebrow">{{ t('marketing.hero.eyebrow') }}</p>
    <h1>{{ props.title }}</h1>
    <p class="marketing-hero__subtitle">{{ props.subtitle }}</p>
    <div class="marketing-hero__actions">
      <NuxtLink to="/login" class="marketing-hero__primary">{{ props.primaryLabel }}</NuxtLink>
      <NuxtLink to="/boundary" class="marketing-hero__secondary">{{ props.secondaryLabel }}</NuxtLink>
    </div>
  </section>
</template>
