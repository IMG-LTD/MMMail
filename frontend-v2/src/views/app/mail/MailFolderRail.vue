<script setup lang="ts">
import { useLocaleText } from '@/locales'
import type { SurfaceOption } from '@/shared/content/route-surfaces'

defineProps<{
  activeKey: string
  items: SurfaceOption[]
}>()

defineEmits<{
  open: [item: SurfaceOption]
}>()

const { tr } = useLocaleText()
</script>

<template>
  <aside class="mail-folder-rail">
    <button
      v-for="item in items"
      :key="item.key"
      type="button"
      :class="{ 'mail-folder-rail__item--active': item.key === activeKey }"
      @click="$emit('open', item)"
    >
      <span>{{ tr(item.label) }}</span>
      <small>{{ tr(item.description) }}</small>
    </button>
    <article class="mail-folder-rail__security">
      <span class="section-label">Mail security</span>
      <strong>Encrypted delivery enabled</strong>
      <p>Sender identities and recipient trust checks remain visible while composing.</p>
    </article>
  </aside>
</template>
