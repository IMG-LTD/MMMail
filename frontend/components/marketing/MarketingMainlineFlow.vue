<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

defineProps<{ items: Array<{ name: string; description: string }> }>()

const { t } = useI18n()
</script>

<template>
  <section class="marketing-flow mm-card" data-testid="marketing-mainline-flow">
    <h2>{{ t('marketing.mainline.title') }}</h2>
    <div class="marketing-flow__rail">
      <article v-for="item in items" :key="item.name" class="marketing-flow__step">
        <strong>{{ item.name }}</strong>
        <p>{{ item.description }}</p>
      </article>
    </div>
  </section>
</template>
