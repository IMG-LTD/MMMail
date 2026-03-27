<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type {
  StandardNotesFilterState,
  StandardNoteSummary,
  StandardNoteType
} from '~/types/standard-notes'
import {
  buildChecklistProgressLabel,
  formatStandardNoteType
} from '~/utils/standard-notes'

defineProps<{
  notes: StandardNoteSummary[]
  activeNoteId: string
  loading?: boolean
  creating?: boolean
  noteTypeOptions: Array<{ label: string, value: StandardNoteType }>
}>()

const filters = defineModel<StandardNotesFilterState>('filters', { required: true })

const emit = defineEmits<{
  create: []
  refresh: []
  search: []
  select: [noteId: string]
}>()

const { t } = useI18n()

const allTypeOption = computed(() => ({
  label: t('standardNotes.list.filter.allTypes'),
  value: 'ALL'
}))
</script>

<template>
  <section class="notes-shell sidebar-shell">
    <header class="card-head">
      <div>
        <p class="eyebrow">{{ t('standardNotes.list.eyebrow') }}</p>
        <h3>{{ t('standardNotes.list.title') }}</h3>
      </div>
      <div class="head-actions">
        <el-button :loading="creating" type="primary" @click="emit('create')">{{ t('standardNotes.list.actions.newNote') }}</el-button>
        <el-button @click="emit('refresh')">{{ t('standardNotes.list.actions.refresh') }}</el-button>
      </div>
    </header>

    <div class="filter-grid">
      <el-input
        v-model="filters.keyword"
        :placeholder="t('standardNotes.list.filter.searchPlaceholder')"
        @keyup.enter="emit('search')"
      />
      <el-select v-model="filters.noteType" @change="emit('search')">
        <el-option :label="allTypeOption.label" :value="allTypeOption.value" />
        <el-option
          v-for="item in noteTypeOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-input
        v-model="filters.tag"
        :placeholder="t('standardNotes.list.filter.tagPlaceholder')"
        @keyup.enter="emit('search')"
      />
      <div class="switch-row">
        <span>{{ t('standardNotes.list.filter.includeArchived') }}</span>
        <el-switch v-model="filters.includeArchived" @change="emit('search')" />
      </div>
    </div>

    <div class="note-list">
      <button
        v-for="note in notes"
        :key="note.id"
        class="note-row"
        :class="{ active: note.id === activeNoteId }"
        @click="emit('select', note.id)"
      >
        <div class="row-head">
          <div>
            <strong>{{ note.title }}</strong>
            <div class="title-meta">
              <span v-if="note.pinned" class="mini-pill">{{ t('standardNotes.list.badges.pinned') }}</span>
              <span v-if="note.archived" class="mini-pill muted">{{ t('standardNotes.list.badges.archived') }}</span>
              <span v-if="note.folderName" class="mini-pill">{{ note.folderName }}</span>
            </div>
          </div>
          <span v-if="note.noteType === 'CHECKLIST'" class="mini-pill">
            {{ buildChecklistProgressLabel(note.checklistTaskCount, note.completedChecklistTaskCount, t) }}
          </span>
        </div>
        <p>{{ note.preview || t('standardNotes.list.noContent') }}</p>
        <div class="row-meta">
          <span>{{ formatStandardNoteType(note.noteType, t) }}</span>
          <span>{{ note.updatedAt }}</span>
        </div>
        <div class="tag-row">
          <span v-for="tag in note.tags.slice(0, 3)" :key="tag" class="tag-chip">#{{ tag }}</span>
          <span v-if="note.tags.length > 3" class="tag-chip muted">+{{ note.tags.length - 3 }}</span>
        </div>
      </button>
      <el-empty v-if="!loading && notes.length === 0" :description="t('standardNotes.list.empty')" />
    </div>
  </section>
</template>

<style scoped>
.card-head,
.row-head,
.row-meta,
.title-meta,
.tag-row,
.switch-row,
.head-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.card-head,
.row-head,
.row-meta {
  justify-content: space-between;
}

.card-head h3,
.card-head p,
.note-row p {
  margin: 0;
}

.eyebrow {
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 11px;
  color: #7c5d3d;
}

.filter-grid {
  display: grid;
  gap: 12px;
}

.note-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 700px;
  overflow: auto;
  padding-right: 4px;
}

.note-row {
  border: 1px solid rgba(112, 85, 57, 0.1);
  background: rgba(255, 255, 255, 0.66);
  border-radius: 18px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  text-align: left;
}

.note-row.active,
.note-row:hover {
  border-color: rgba(112, 85, 57, 0.28);
  background: rgba(255, 255, 255, 0.9);
}

.note-row p {
  color: rgba(40, 33, 25, 0.72);
  line-height: 1.5;
}

.mini-pill,
.tag-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(245, 236, 224, 0.88);
  border: 1px solid rgba(110, 83, 56, 0.1);
  font-size: 12px;
}

.mini-pill.muted,
.tag-chip.muted {
  opacity: 0.7;
}
</style>
