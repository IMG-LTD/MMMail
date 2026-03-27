<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { StandardNotesOverview } from '~/types/standard-notes'
import { buildStandardNotesHealthChips } from '~/utils/standard-notes'

const props = defineProps<{
  overview: StandardNotesOverview | null
  availableTags: string[]
}>()
const { t } = useI18n()
const readinessNotes = computed(() => buildStandardNotesHealthChips(props.overview, t))
</script>

<template>
  <aside class="knowledge-rail">
    <article class="rail-card">
      <p class="rail-eyebrow">{{ t('standardNotes.rail.scopeEyebrow') }}</p>
      <h3>{{ t('standardNotes.rail.scopeTitle') }}</h3>
      <ul>
        <li>{{ t('standardNotes.rail.scope.folders') }}</li>
        <li>{{ t('standardNotes.rail.scope.checklist') }}</li>
        <li>{{ t('standardNotes.rail.scope.export') }}</li>
        <li>{{ t('standardNotes.rail.scope.search') }}</li>
      </ul>
    </article>

    <article class="rail-card accent">
      <p class="rail-eyebrow">{{ t('standardNotes.rail.healthEyebrow') }}</p>
      <div class="chips">
        <span v-for="note in readinessNotes" :key="note" class="chip">{{ note }}</span>
      </div>
    </article>

    <article class="rail-card">
      <p class="rail-eyebrow">{{ t('standardNotes.rail.tagsEyebrow') }}</p>
      <div v-if="availableTags.length" class="chips tags-light">
        <span v-for="tag in availableTags.slice(0, 10)" :key="tag" class="tag-chip">#{{ tag }}</span>
      </div>
      <p v-else class="empty-copy">{{ t('standardNotes.rail.noTags') }}</p>
    </article>

    <article class="rail-card">
      <p class="rail-eyebrow">{{ t('standardNotes.rail.limitsEyebrow') }}</p>
      <ul>
        <li>{{ t('standardNotes.rail.limits.encryption') }}</li>
        <li>{{ t('standardNotes.rail.limits.offline') }}</li>
        <li>{{ t('standardNotes.rail.limits.extensions') }}</li>
      </ul>
    </article>
  </aside>
</template>

<style scoped>
.knowledge-rail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.rail-card {
  padding: 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(111, 82, 53, 0.1);
  box-shadow: 0 18px 44px rgba(78, 60, 38, 0.08);
}
.rail-card.accent {
  background: linear-gradient(180deg, rgba(47, 36, 24, 0.96), rgba(79, 60, 41, 0.96));
  color: #f8f2e7;
}
.rail-card h3,
.rail-card p,
.rail-card ul {
  margin: 0;
}
.rail-card ul {
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.rail-eyebrow {
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 11px;
  color: #7c5d3d;
}
.accent .rail-eyebrow {
  color: rgba(247, 233, 213, 0.82);
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chip,
.tag-chip {
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
}
.chip {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.12);
}
.tags-light .tag-chip {
  background: rgba(245, 236, 224, 0.9);
  border: 1px solid rgba(111, 82, 53, 0.12);
}
.empty-copy {
  color: rgba(40, 33, 25, 0.62);
}
</style>
