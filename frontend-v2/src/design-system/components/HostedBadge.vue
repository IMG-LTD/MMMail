<script setup lang="ts">
import StatusBadge from './StatusBadge.vue'

withDefaults(
  defineProps<{
    compact?: boolean
    label?: string
  }>(),
  {
    compact: false,
    label: 'Hosted'
  }
)
</script>

<template>
  <StatusBadge class="hosted-badge" :compact="compact" :label="label" tone="hosted" />
</template>
