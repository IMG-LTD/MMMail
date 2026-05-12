<script setup lang="ts">
import type { PassMonitorItem } from '@/service/api/pass'
import { resolveMonitorFlags } from './pass-view-helpers'

defineProps<{
  actionError: string
  activeRisk: PassMonitorItem | null
  items: PassMonitorItem[]
}>()

defineEmits<{
  openRisk: [id: string]
  retry: []
}>()
</script>

<template>
  <aside class="pass-risk-monitor-panel">
    <header>
      <span class="section-label">Risk monitor</span>
      <strong>{{ items.length }} signals</strong>
    </header>
    <button v-for="item in items" :key="item.id" class="pass-risk-trigger" type="button" @click="$emit('openRisk', item.id)">
      <strong>{{ item.title || 'Untitled item' }}</strong>
      <span>{{ resolveMonitorFlags(item) || 'Tracked' }}</span>
    </button>
    <button v-if="!items.length" class="pass-risk-trigger" type="button" @click="$emit('openRisk', '')">
      <strong>Risk review</strong>
      <span>No runtime signals loaded</span>
    </button>
    <section class="pass-risk-detail">
      <strong>{{ activeRisk?.title || 'Risk detail' }}</strong>
      <p>{{ activeRisk ? resolveMonitorFlags(activeRisk) : 'Select a risk alert to inspect remediation.' }}</p>
      <p v-if="actionError" class="pass-action-error">{{ actionError }}</p>
      <button class="pass-action-retry" type="button" @click="$emit('retry')">Retry remediation</button>
    </section>
  </aside>
</template>
