<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import type {
  CreateStandardNoteFolderRequest,
  StandardNoteFolder,
  UpdateStandardNoteFolderRequest
} from '~/types/standard-notes'
import { buildChecklistProgressLabel } from '~/utils/standard-notes'

const props = defineProps<{
  folders: StandardNoteFolder[]
  selectedFolderId: string
  loading?: boolean
  saving?: boolean
  deletingId?: string
}>()

const emit = defineEmits<{
  select: [folderId: string]
  create: [payload: CreateStandardNoteFolderRequest]
  update: [folderId: string, payload: UpdateStandardNoteFolderRequest]
  remove: [folderId: string]
}>()
const { t } = useI18n()

const dialogVisible = ref(false)
const editingFolderId = ref('')
const form = reactive({
  name: '',
  color: '#C7A57A',
  description: ''
})

const folderCards = computed(() => [
  {
    id: 'ALL',
    name: t('standardNotes.collections.allNotes'),
    color: '#5B4A37',
    description: t('standardNotes.collections.allNotesDescription'),
    noteCount: props.folders.reduce((sum, item) => sum + item.noteCount, 0),
    checklistTaskCount: props.folders.reduce((sum, item) => sum + item.checklistTaskCount, 0),
    completedChecklistTaskCount: props.folders.reduce((sum, item) => sum + item.completedChecklistTaskCount, 0),
    updatedAt: ''
  },
  {
    id: 'UNFILED',
    name: t('standardNotes.collections.unfiled'),
    color: '#A8906E',
    description: t('standardNotes.collections.unfiledDescription'),
    noteCount: 0,
    checklistTaskCount: 0,
    completedChecklistTaskCount: 0,
    updatedAt: ''
  },
  ...props.folders
])

watch(dialogVisible, (visible) => {
  if (!visible) {
    resetForm()
  }
})

function openCreate(): void {
  editingFolderId.value = ''
  dialogVisible.value = true
}

function openEdit(folder: StandardNoteFolder): void {
  editingFolderId.value = folder.id
  form.name = folder.name
  form.color = folder.color
  form.description = folder.description || ''
  dialogVisible.value = true
}

function resetForm(): void {
  editingFolderId.value = ''
  form.name = ''
  form.color = '#C7A57A'
  form.description = ''
}

function submit(): void {
  const payload = {
    name: form.name.trim(),
    color: form.color.trim() || '#C7A57A',
    description: form.description.trim() || undefined
  }
  if (editingFolderId.value) {
    emit('update', editingFolderId.value, payload)
  } else {
    emit('create', payload)
  }
  dialogVisible.value = false
}

async function remove(folderId: string): Promise<void> {
  try {
    await ElMessageBox.confirm(
      t('standardNotes.collections.confirm.deleteMessage'),
      t('standardNotes.collections.confirm.deleteTitle'),
      { type: 'warning' }
    )
  } catch {
    return
  }
  emit('remove', folderId)
}
</script>

<template>
  <section class="collections-panel notes-shell">
    <header class="panel-head">
      <div>
        <p class="eyebrow">{{ t('standardNotes.collections.eyebrow') }}</p>
        <h3>{{ t('standardNotes.collections.title') }}</h3>
      </div>
      <el-button :loading="saving" type="primary" @click="openCreate">{{ t('standardNotes.collections.newFolder') }}</el-button>
    </header>

    <div class="folder-list">
      <button
        v-for="folder in folderCards"
        :key="folder.id"
        class="folder-card"
        :class="{ active: selectedFolderId === folder.id }"
        @click="emit('select', folder.id)"
      >
        <div class="folder-head">
          <span class="folder-swatch" :style="{ background: folder.color }" />
          <div>
            <strong>{{ folder.name }}</strong>
            <p>{{ folder.description || t('standardNotes.collections.noFolderNote') }}</p>
          </div>
        </div>
        <div class="folder-meta">
          <span>{{ t('standardNotes.collections.notesCount', { count: folder.noteCount }) }}</span>
          <span>{{ buildChecklistProgressLabel(folder.checklistTaskCount, folder.completedChecklistTaskCount, t) }}</span>
        </div>
        <div v-if="!['ALL', 'UNFILED'].includes(folder.id)" class="folder-actions">
          <el-button size="small" text @click.stop="openEdit(folder)">{{ t('standardNotes.collections.edit') }}</el-button>
          <el-button size="small" text type="danger" :loading="deletingId === folder.id" @click.stop="remove(folder.id)">{{ t('standardNotes.collections.delete') }}</el-button>
        </div>
      </button>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingFolderId ? t('standardNotes.collections.dialog.editTitle') : t('standardNotes.collections.dialog.createTitle')" width="440px">
      <div class="dialog-grid">
        <el-input v-model="form.name" maxlength="64" :placeholder="t('standardNotes.collections.dialog.namePlaceholder')" />
        <el-input v-model="form.color" maxlength="7" :placeholder="t('standardNotes.collections.dialog.colorPlaceholder')" />
        <el-input v-model="form.description" type="textarea" :rows="3" maxlength="160" show-word-limit :placeholder="t('standardNotes.collections.dialog.descriptionPlaceholder')" />
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.actions.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="submit">{{ editingFolderId ? t('standardNotes.collections.dialog.save') : t('standardNotes.collections.dialog.create') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.collections-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.panel-head,
.folder-head,
.folder-meta,
.folder-actions {
  display: flex;
  gap: 10px;
}
.panel-head,
.folder-meta {
  justify-content: space-between;
  align-items: center;
}
.panel-head h3,
.panel-head p,
.folder-head p {
  margin: 0;
}
.eyebrow {
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 11px;
  color: #7c5d3d;
}
.folder-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.folder-card {
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(112, 85, 57, 0.1);
  background: rgba(255, 255, 255, 0.72);
  display: flex;
  flex-direction: column;
  gap: 10px;
  text-align: left;
}
.folder-card.active,
.folder-card:hover {
  border-color: rgba(112, 85, 57, 0.28);
  background: rgba(255, 255, 255, 0.92);
}
.folder-head {
  align-items: flex-start;
}
.folder-head p {
  color: rgba(40, 33, 25, 0.68);
}
.folder-swatch {
  width: 14px;
  height: 14px;
  margin-top: 4px;
  border-radius: 999px;
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.6);
}
.folder-meta {
  font-size: 12px;
  color: rgba(66, 50, 33, 0.68);
}
.folder-actions {
  justify-content: flex-end;
}
.dialog-grid {
  display: grid;
  gap: 12px;
}
</style>
