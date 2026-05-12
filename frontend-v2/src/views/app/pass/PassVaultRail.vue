<script setup lang="ts">
import { useLocaleText } from '@/locales'
import type { SurfaceOption } from '@/shared/content/route-surfaces'

defineProps<{
  activeKey: string
  sections: SurfaceOption[]
}>()

defineEmits<{
  open: [key: string]
}>()

const { tr } = useLocaleText()
</script>

<template>
  <aside class="pass-vault-rail">
    <span class="section-label">Pass</span>
    <button v-for="item in sections" :key="item.key" :class="{ 'pass-vault-rail__item--active': item.key === activeKey }" type="button" @click="$emit('open', item.key)">
      {{ tr(item.label) }}
    </button>
    <article class="pass-vault-rail__status">
      <strong>Local encryption, privacy first</strong>
      <p>Secrets remain masked until explicitly revealed.</p>
    </article>
  </aside>
</template>
