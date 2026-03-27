<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'

defineProps<{
  healthChips: string[]
}>()

const { t } = useI18n()
</script>

<template>
  <section class="notes-shell metadata-shell">
    <header class="card-head compact">
      <div>
        <p class="eyebrow">{{ t('standardNotes.pulse.eyebrow') }}</p>
        <h3>{{ t('standardNotes.pulse.title') }}</h3>
      </div>
    </header>
    <div class="chips-row">
      <span v-for="chip in healthChips" :key="chip" class="tag-chip">{{ chip }}</span>
    </div>
  </section>
</template>

<style scoped>
.card-head,
.chips-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.card-head h3,
.card-head p {
  margin: 0;
}

.eyebrow {
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 11px;
  color: #7c5d3d;
}

.tag-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(245, 236, 224, 0.88);
  border: 1px solid rgba(110, 83, 56, 0.1);
  font-size: 12px;
}
</style>
