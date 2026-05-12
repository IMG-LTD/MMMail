<script setup lang="ts">
import type { RecipientTrustState } from './mail-types'

defineProps<{
  copy: string
  loading?: boolean
  state: RecipientTrustState | null
}>()
</script>

<template>
  <section
    class="mail-trust-panel"
    :class="{
      'mail-trust-panel--blocked': state?.status === 'blocked',
      'mail-trust-panel--ready': state?.status === 'ready',
      'mail-trust-panel--warning': state?.status === 'warning'
    }"
  >
    <span class="section-label">Trust state</span>
    <strong>{{ state?.status || 'Pending review' }}</strong>
    <p>{{ loading ? 'Checking recipient security state.' : copy }}</p>
  </section>
</template>
