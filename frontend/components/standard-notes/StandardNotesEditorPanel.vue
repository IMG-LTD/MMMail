<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type {
  StandardNotesEditorState,
  StandardNoteDetail,
  StandardNoteFolder,
  StandardNoteType
} from '~/types/standard-notes'
import {
  buildChecklistProgressLabel,
  formatStandardNoteType
} from '~/utils/standard-notes'

defineProps<{
  activeNote: StandardNoteDetail | null
  folders: StandardNoteFolder[]
  saveLoading?: boolean
  deleteLoading?: boolean
  createLoading?: boolean
  noteTypeOptions: Array<{ label: string, value: StandardNoteType }>
}>()

const editor = defineModel<StandardNotesEditorState>('editor', { required: true })

const emit = defineEmits<{
  create: []
  save: []
  remove: []
}>()

const { t } = useI18n()
</script>

<template>
  <section class="notes-shell editor-shell">
    <header class="card-head">
      <div>
        <p class="eyebrow">{{ t('standardNotes.editor.eyebrow') }}</p>
        <h3>{{ activeNote ? t('standardNotes.editor.activeTitle') : t('standardNotes.editor.emptyTitle') }}</h3>
      </div>
      <div class="head-actions">
        <el-button :disabled="!activeNote" :loading="saveLoading" type="primary" @click="emit('save')">{{ t('standardNotes.editor.actions.save') }}</el-button>
        <el-button :disabled="!activeNote" :loading="deleteLoading" type="danger" plain @click="emit('remove')">{{ t('standardNotes.editor.actions.delete') }}</el-button>
      </div>
    </header>

    <template v-if="activeNote">
      <div class="editor-grid">
        <el-input v-model="editor.title" maxlength="128" :placeholder="t('standardNotes.editor.fields.title')" />
        <el-input
          v-model="editor.content"
          type="textarea"
          :rows="16"
          resize="vertical"
          :placeholder="t('standardNotes.editor.fields.content')"
        />
      </div>
      <div class="meta-grid">
        <el-select v-model="editor.noteType">
          <el-option v-for="item in noteTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="editor.folderId" clearable :placeholder="t('standardNotes.editor.fields.folder')">
          <el-option :label="t('standardNotes.editor.unfiled')" value="" />
          <el-option v-for="folder in folders" :key="folder.id" :label="folder.name" :value="folder.id" />
        </el-select>
        <el-input v-model="editor.tagInput" :placeholder="t('standardNotes.editor.fields.tags')" />
      </div>
      <div class="switch-row switch-grid">
        <div class="switch-block">
          <span>{{ t('standardNotes.editor.switches.pin') }}</span>
          <el-switch v-model="editor.pinned" />
        </div>
        <div class="switch-block">
          <span>{{ t('standardNotes.editor.switches.archive') }}</span>
          <el-switch v-model="editor.archived" />
        </div>
      </div>
      <div class="editor-foot">
        <span class="foot-pill">{{ t('standardNotes.editor.foot.type', { value: formatStandardNoteType(editor.noteType, t) }) }}</span>
        <span class="foot-pill">{{ t('standardNotes.editor.foot.folder', { value: activeNote.folderName || t('standardNotes.editor.unfiled') }) }}</span>
        <span class="foot-pill">{{ t('standardNotes.editor.foot.version', { value: editor.currentVersion }) }}</span>
        <span class="foot-pill">{{ buildChecklistProgressLabel(activeNote.checklistTaskCount, activeNote.completedChecklistTaskCount, t) }}</span>
      </div>
    </template>
    <div v-else class="empty-editor">
      <p class="empty-title">{{ t('standardNotes.editor.emptyTitle') }}</p>
      <p class="empty-copy">{{ t('standardNotes.editor.emptyCopy') }}</p>
      <el-button :loading="createLoading" type="primary" @click="emit('create')">{{ t('standardNotes.list.actions.newNote') }}</el-button>
    </div>
  </section>
</template>

<style scoped>
.card-head,
.switch-row,
.editor-foot,
.head-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.card-head {
  justify-content: space-between;
}

.card-head h3,
.card-head p,
.empty-copy,
.empty-title {
  margin: 0;
}

.eyebrow {
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 11px;
  color: #7c5d3d;
}

.editor-grid,
.meta-grid {
  display: grid;
  gap: 12px;
}

.switch-grid {
  justify-content: flex-start;
}

.switch-block {
  min-width: 180px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(112, 85, 57, 0.08);
}

.foot-pill {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(245, 236, 224, 0.88);
  border: 1px solid rgba(110, 83, 56, 0.1);
  font-size: 12px;
}

.empty-editor {
  min-height: 360px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  gap: 12px;
}

.empty-title {
  font-size: 20px;
  font-weight: 700;
}
</style>
