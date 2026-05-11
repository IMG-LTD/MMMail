<script setup lang="ts">
import StatusBadge from './StatusBadge.vue'

withDefaults(
  defineProps<{
    compact?: boolean
    label?: string
  }>(),
  {
    compact: false,
    label: 'Premium'
  }
)
</script>

<template>
  <StatusBadge class="premium-badge" :compact="compact" :label="label" tone="premium" />
</template>
