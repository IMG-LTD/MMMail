<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { StandardNoteDetail } from '~/types/standard-notes'
import { buildChecklistProgressLabel } from '~/utils/standard-notes'

const props = defineProps<{
  note: StandardNoteDetail | null
  loading?: boolean
  togglingIndex?: number | null
}>()

const emit = defineEmits<{
  toggle: [itemIndex: number, completed: boolean]
}>()
const { t } = useI18n()

const progress = computed(() => {
  if (!props.note) {
    return 0
  }
  if (props.note.checklistTaskCount === 0) {
    return 0
  }
  return Math.round((props.note.completedChecklistTaskCount / props.note.checklistTaskCount) * 100)
})
</script>

<template>
  <section class="task-panel notes-shell">
    <header class="panel-head compact">
      <div>
        <p class="eyebrow">{{ t('standardNotes.task.eyebrow') }}</p>
        <h3>{{ t('standardNotes.task.title') }}</h3>
      </div>
      <span class="task-pill">{{ note ? buildChecklistProgressLabel(note.checklistTaskCount, note.completedChecklistTaskCount, t) : t('standardNotes.task.noNote') }}</span>
    </header>

    <template v-if="note && note.noteType === 'CHECKLIST'">
      <el-progress :percentage="progress" :stroke-width="10" :show-text="false" />
      <div class="task-list">
        <label v-for="item in note.checklistItems" :key="item.itemIndex" class="task-row">
          <el-checkbox
            :model-value="item.completed"
            :disabled="loading || togglingIndex === item.itemIndex"
            @change="emit('toggle', item.itemIndex, Boolean($event))"
          />
          <span :class="['task-copy', { done: item.completed }]">{{ item.text }}</span>
          <span v-if="togglingIndex === item.itemIndex" class="task-status">{{ t('standardNotes.task.saving') }}</span>
        </label>
      </div>
    </template>

    <div v-else class="task-empty">
      <p class="empty-title">{{ t('standardNotes.task.emptyTitle') }}</p>
      <p class="empty-copy">{{ t('standardNotes.task.emptyCopy') }}</p>
    </div>
  </section>
</template>

<style scoped>
.task-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}
.panel-head h3,
.panel-head p,
.task-empty p {
  margin: 0;
}
.eyebrow {
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 11px;
  color: #7c5d3d;
}
.task-pill {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(245, 236, 224, 0.88);
  border: 1px solid rgba(110, 83, 56, 0.1);
  font-size: 12px;
}
.task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.task-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(112, 85, 57, 0.08);
}
.task-copy.done {
  text-decoration: line-through;
  color: rgba(44, 90, 60, 0.72);
}
.task-status {
  font-size: 12px;
  color: rgba(40, 33, 25, 0.58);
}
.task-empty {
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
code {
  padding: 2px 6px;
  border-radius: 8px;
  background: rgba(245, 236, 224, 0.88);
}
</style>
