<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { StandardNotesExport } from '~/types/standard-notes'

const props = defineProps<{
  snapshot: StandardNotesExport | null
  loading?: boolean
}>()

const emit = defineEmits<{
  export: []
  download: []
}>()
const { t } = useI18n()

const exportText = computed(() => props.snapshot ? JSON.stringify(props.snapshot, null, 2) : '')
</script>

<template>
  <section class="export-panel notes-shell">
    <header class="panel-head compact">
      <div>
        <p class="eyebrow">{{ t('standardNotes.export.eyebrow') }}</p>
        <h3>{{ t('standardNotes.export.title') }}</h3>
      </div>
      <div class="head-actions">
        <el-button :loading="loading" type="primary" @click="emit('export')">{{ t('standardNotes.export.generate') }}</el-button>
        <el-button :disabled="!snapshot" @click="emit('download')">{{ t('standardNotes.export.download') }}</el-button>
      </div>
    </header>

    <template v-if="snapshot">
      <div class="meta-grid">
        <span class="pill">{{ snapshot.format }}</span>
        <span class="pill">{{ snapshot.fileName }}</span>
        <span class="pill">{{ snapshot.exportedAt }}</span>
      </div>
      <el-input :model-value="exportText" type="textarea" :rows="12" readonly />
    </template>

    <div v-else class="export-empty">
      <p class="empty-title">{{ t('standardNotes.export.emptyTitle') }}</p>
      <p class="empty-copy">{{ t('standardNotes.export.emptyCopy') }}</p>
    </div>
  </section>
</template>

<style scoped>
.export-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.panel-head,
.head-actions,
.meta-grid {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.panel-head {
  justify-content: space-between;
  align-items: flex-start;
}
.panel-head h3,
.panel-head p,
.export-empty p {
  margin: 0;
}
.eyebrow {
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 11px;
  color: #7c5d3d;
}
.pill {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(245, 236, 224, 0.88);
  border: 1px solid rgba(110, 83, 56, 0.1);
  font-size: 12px;
}
.export-empty {
  min-height: 160px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}
.empty-title {
  font-size: 18px;
  font-weight: 700;
}
.empty-copy {
  color: rgba(40, 33, 25, 0.68);
  line-height: 1.6;
}
</style>
