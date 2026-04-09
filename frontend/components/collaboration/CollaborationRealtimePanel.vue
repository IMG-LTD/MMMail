<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

const props = defineProps<{
  latestSyncSummary: string
  currentFilterLabel: string
  latestEventTitle: string
  latestEventDescription: string
  syncStatusLabel: string
}>()

const { t } = useI18n()
</script>

<template>
  <aside class="context-panel mm-card">
    <div class="panel-header compact">
      <div>
        <h2>{{ t('collaboration.realtime.title') }}</h2>
        <p>{{ props.latestSyncSummary }}</p>
      </div>
    </div>

    <div class="context-block">
      <span class="context-label">{{ t('collaboration.realtime.currentFilter') }}</span>
      <strong>{{ props.currentFilterLabel }}</strong>
    </div>
    <div class="context-block">
      <span class="context-label">{{ t('collaboration.realtime.latestEvent') }}</span>
      <strong>{{ props.latestEventTitle }}</strong>
      <p>{{ props.latestEventDescription }}</p>
    </div>
    <div class="context-block">
      <span class="context-label">{{ t('collaboration.realtime.mode') }}</span>
      <strong>{{ props.syncStatusLabel }}</strong>
      <p>{{ t('collaboration.realtime.description') }}</p>
    </div>
  </aside>
</template>

<style scoped>
.context-panel {
  display: grid;
  gap: 14px;
  padding: 18px;
  background: rgba(6, 18, 31, 0.78);
  border: 1px solid rgba(120, 227, 214, 0.12);
  box-shadow: 0 16px 36px rgba(2, 8, 15, 0.35);
}

.panel-header.compact {
  margin-bottom: 0;
}

.panel-header h2 {
  margin: 0;
  font-size: 20px;
}

.panel-header p,
.context-block p {
  margin: 8px 0 0;
  color: rgba(236, 246, 255, 0.7);
  line-height: 1.5;
}

.context-block {
  padding: 14px;
  border-radius: 16px;
  background: rgba(13, 33, 47, 0.82);
  border: 1px solid rgba(120, 227, 214, 0.1);
}

.context-label {
  display: block;
  color: rgba(236, 246, 255, 0.62);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.context-block strong {
  display: block;
  margin-top: 8px;
  font-size: 18px;
}
</style>
