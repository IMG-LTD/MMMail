<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StandardNotesCollectionsPanel from '~/components/standard-notes/StandardNotesCollectionsPanel.vue'
import StandardNotesEditorPanel from '~/components/standard-notes/StandardNotesEditorPanel.vue'
import StandardNotesExportPanel from '~/components/standard-notes/StandardNotesExportPanel.vue'
import StandardNotesKnowledgeRail from '~/components/standard-notes/StandardNotesKnowledgeRail.vue'
import StandardNotesListPanel from '~/components/standard-notes/StandardNotesListPanel.vue'
import StandardNotesPulsePanel from '~/components/standard-notes/StandardNotesPulsePanel.vue'
import StandardNotesTaskPanel from '~/components/standard-notes/StandardNotesTaskPanel.vue'
import StandardNotesWorkspaceHero from '~/components/standard-notes/StandardNotesWorkspaceHero.vue'
import { useI18n } from '~/composables/useI18n'
import { useStandardNotesApi } from '~/composables/useStandardNotesApi'
import type {
  CreateStandardNoteFolderRequest,
  StandardNotesEditorState,
  StandardNotesExport,
  StandardNotesFilterState,
  StandardNotesOverview,
  StandardNoteDetail,
  StandardNoteFolder,
  StandardNoteSummary,
  StandardNoteType,
  UpdateStandardNoteFolderRequest
} from '~/types/standard-notes'
import {
  buildStandardNoteTypeOptions,
  buildStandardNotesHealthChips,
  normalizeStandardNoteTags,
  resolvePreferredStandardNoteFolderId,
  resolvePreferredStandardNoteId
} from '~/utils/standard-notes'

definePageMeta({
  layout: 'default'
})

const ALL_FOLDER_ID = 'ALL'
const ALL_NOTE_TYPE = 'ALL'
const UNFILED_FOLDER_ID = 'UNFILED'
const DEFAULT_EXPORT_FILENAME = 'standard-notes-workspace.json'

const route = useRoute()
const router = useRouter()
const api = useStandardNotesApi()
const { t } = useI18n()

const overview = ref<StandardNotesOverview | null>(null)
const folders = ref<StandardNoteFolder[]>([])
const notes = ref<StandardNoteSummary[]>([])
const activeNote = ref<StandardNoteDetail | null>(null)
const selectedFolderId = ref(ALL_FOLDER_ID)
const exportSnapshot = ref<StandardNotesExport | null>(null)
const loading = reactive({
  page: false,
  folders: false,
  list: false,
  detail: false,
  refresh: false,
  create: false,
  save: false,
  delete: false,
  folderSave: false,
  folderDeleteId: '',
  taskIndex: null as number | null,
  export: false
})
const filters = reactive<StandardNotesFilterState>({
  keyword: '',
  includeArchived: false,
  noteType: ALL_NOTE_TYPE as StandardNoteType | 'ALL',
  tag: ''
})
const editor = reactive<StandardNotesEditorState>({
  title: '',
  content: '',
  noteType: 'PLAIN_TEXT',
  tagInput: '',
  folderId: '',
  pinned: false,
  archived: false,
  currentVersion: 1,
  createdAt: '',
  updatedAt: ''
})

const routeNoteId = computed(() => typeof route.query.noteId === 'string' ? route.query.noteId : null)
const routeFolderId = computed(() => typeof route.query.folderId === 'string' ? route.query.folderId : null)
const availableTags = computed(() => Array.from(new Set(notes.value.flatMap((note) => note.tags))).sort())
const activeNoteId = computed(() => activeNote.value?.id || '')
const healthChips = computed(() => buildStandardNotesHealthChips(overview.value, t))
const noteTypeOptions = computed(() => buildStandardNoteTypeOptions(t))

useHead(() => ({
  title: t('page.standardNotes.title')
}))

onMounted(() => {
  void bootstrapPage()
})

watch(routeFolderId, (nextFolderId) => {
  const resolved = resolvePreferredStandardNoteFolderId(folders.value, nextFolderId)
  if (resolved !== selectedFolderId.value) {
    selectedFolderId.value = resolved
    void loadNotes(routeNoteId.value)
  }
})

watch(routeNoteId, (nextId) => {
  if (!nextId || nextId === activeNote.value?.id || notes.value.length === 0) {
    return
  }
  const resolved = resolvePreferredStandardNoteId(notes.value, nextId, activeNote.value?.id || '')
  if (resolved && resolved !== activeNote.value?.id) {
    void selectNote(resolved)
  }
})

async function bootstrapPage(): Promise<void> {
  loading.page = true
  try {
    await Promise.all([loadOverview(), loadFolders(), loadNotes(routeNoteId.value)])
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('standardNotes.messages.loadWorkspaceFailed')))
  } finally {
    loading.page = false
  }
}

async function loadOverview(): Promise<void> {
  overview.value = await api.getOverview()
}

async function loadFolders(): Promise<void> {
  loading.folders = true
  try {
    folders.value = await api.listFolders()
    selectedFolderId.value = resolvePreferredStandardNoteFolderId(folders.value, routeFolderId.value)
  } finally {
    loading.folders = false
  }
}

async function loadNotes(preferredNoteId: string | null = null): Promise<void> {
  loading.list = true
  try {
    notes.value = await api.listNotes({
      keyword: filters.keyword.trim(),
      includeArchived: filters.includeArchived,
      noteType: filters.noteType,
      tag: filters.tag || undefined,
      folderId: selectedFolderId.value,
      limit: 120
    })
    const resolved = resolvePreferredStandardNoteId(notes.value, preferredNoteId, activeNote.value?.id || '')
    if (!resolved) {
      resetEditor()
      await syncRoute('', selectedFolderId.value)
      return
    }
    await selectNote(resolved)
  } finally {
    loading.list = false
  }
}

async function selectNote(noteId: string): Promise<void> {
  if (!noteId) {
    resetEditor()
    return
  }
  loading.detail = true
  try {
    const detail = await api.getNote(noteId)
    applyDetail(detail)
    await syncRoute(detail.id, selectedFolderId.value)
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('standardNotes.messages.loadNoteFailed')))
  } finally {
    loading.detail = false
  }
}

async function onRefresh(): Promise<void> {
  loading.refresh = true
  try {
    await Promise.all([loadOverview(), loadFolders(), loadNotes(activeNote.value?.id || routeNoteId.value)])
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('standardNotes.messages.refreshWorkspaceFailed')))
  } finally {
    loading.refresh = false
  }
}

async function onCreate(): Promise<void> {
  loading.create = true
  try {
    const suffix = new Date().toISOString().slice(0, 16).replace('T', ' ')
    const created = await api.createNote({
      title: t('standardNotes.seed.title', { time: suffix }),
      content: selectedFolderId.value === UNFILED_FOLDER_ID ? '' : t('standardNotes.seed.checklistLine'),
      noteType: selectedFolderId.value === UNFILED_FOLDER_ID ? 'PLAIN_TEXT' : 'CHECKLIST',
      tags: [],
      folderId: selectedFolderId.value !== ALL_FOLDER_ID && selectedFolderId.value !== UNFILED_FOLDER_ID ? selectedFolderId.value : undefined,
      pinned: false
    })
    ElMessage.success(t('standardNotes.messages.noteCreated'))
    await Promise.all([loadOverview(), loadFolders(), loadNotes(created.id)])
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('standardNotes.messages.noteCreateFailed')))
  } finally {
    loading.create = false
  }
}

async function onSave(): Promise<void> {
  if (!activeNote.value?.id) {
    ElMessage.warning(t('standardNotes.messages.noteSelectionRequired'))
    return
  }
  loading.save = true
  try {
    const updated = await api.updateNote(activeNote.value.id, {
      title: editor.title.trim(),
      content: editor.content,
      currentVersion: editor.currentVersion,
      noteType: editor.noteType,
      tags: normalizeStandardNoteTags(editor.tagInput),
      folderId: editor.folderId || undefined,
      pinned: editor.pinned,
      archived: editor.archived
    })
    applyDetail(updated)
    ElMessage.success(t('standardNotes.messages.noteSaved'))
    await Promise.all([loadOverview(), loadFolders(), loadNotes(updated.id)])
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('standardNotes.messages.noteSaveFailed')))
  } finally {
    loading.save = false
  }
}

async function onDelete(): Promise<void> {
  if (!activeNote.value?.id) {
    return
  }
  try {
    await ElMessageBox.confirm(
      t('standardNotes.messages.deleteNoteMessage'),
      t('standardNotes.messages.deleteNoteTitle'),
      { type: 'warning' }
    )
  } catch {
    return
  }
  loading.delete = true
  try {
    await api.deleteNote(activeNote.value.id)
    ElMessage.success(t('standardNotes.messages.noteDeleted'))
    resetEditor()
    await Promise.all([loadOverview(), loadFolders(), loadNotes(routeNoteId.value)])
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('standardNotes.messages.noteDeleteFailed')))
  } finally {
    loading.delete = false
  }
}

async function onCreateFolder(payload: CreateStandardNoteFolderRequest): Promise<void> {
  loading.folderSave = true
  try {
    const created = await api.createFolder(payload)
    ElMessage.success(t('standardNotes.messages.folderCreated'))
    await Promise.all([loadOverview(), loadFolders()])
    selectedFolderId.value = created.id
    await loadNotes(activeNote.value?.id || routeNoteId.value)
    await syncRoute(activeNote.value?.id || '', created.id)
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('standardNotes.messages.folderCreateFailed')))
  } finally {
    loading.folderSave = false
  }
}

async function onUpdateFolder(folderId: string, payload: UpdateStandardNoteFolderRequest): Promise<void> {
  loading.folderSave = true
  try {
    await api.updateFolder(folderId, payload)
    ElMessage.success(t('standardNotes.messages.folderUpdated'))
    await Promise.all([loadOverview(), loadFolders(), loadNotes(activeNote.value?.id || routeNoteId.value)])
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('standardNotes.messages.folderUpdateFailed')))
  } finally {
    loading.folderSave = false
  }
}

async function onDeleteFolder(folderId: string): Promise<void> {
  loading.folderDeleteId = folderId
  try {
    await api.deleteFolder(folderId)
    ElMessage.success(t('standardNotes.messages.folderDeleted'))
    if (selectedFolderId.value === folderId) {
      selectedFolderId.value = ALL_FOLDER_ID
    }
    await Promise.all([loadOverview(), loadFolders(), loadNotes(activeNote.value?.id || routeNoteId.value)])
    await syncRoute(activeNote.value?.id || '', selectedFolderId.value)
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('standardNotes.messages.folderDeleteFailed')))
  } finally {
    loading.folderDeleteId = ''
  }
}

async function onSelectFolder(folderId: string): Promise<void> {
  selectedFolderId.value = folderId
  await syncRoute(activeNote.value?.id || routeNoteId.value || '', folderId)
  await loadNotes(activeNote.value?.id || routeNoteId.value)
}

async function onToggleChecklistItem(itemIndex: number, completed: boolean): Promise<void> {
  if (!activeNote.value?.id) {
    return
  }
  loading.taskIndex = itemIndex
  try {
    const updated = await api.toggleChecklistItem(activeNote.value.id, itemIndex, {
      currentVersion: editor.currentVersion,
      completed
    })
    applyDetail(updated)
    ElMessage.success(t('standardNotes.messages.checklistUpdated'))
    await Promise.all([loadOverview(), loadFolders(), loadNotes(updated.id)])
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('standardNotes.messages.checklistUpdateFailed')))
  } finally {
    loading.taskIndex = null
  }
}

async function onExportWorkspace(): Promise<void> {
  loading.export = true
  try {
    exportSnapshot.value = await api.exportWorkspace()
    ElMessage.success(t('standardNotes.messages.exportGenerated'))
  } catch (error) {
    ElMessage.error(resolveMessage(error, t('standardNotes.messages.exportFailed')))
  } finally {
    loading.export = false
  }
}

function downloadExportSnapshot(): void {
  if (!exportSnapshot.value) {
    return
  }
  const blob = new Blob([JSON.stringify(exportSnapshot.value, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = exportSnapshot.value.fileName || DEFAULT_EXPORT_FILENAME
  anchor.click()
  URL.revokeObjectURL(url)
}

function applyDetail(detail: StandardNoteDetail): void {
  activeNote.value = detail
  editor.title = detail.title
  editor.content = detail.content
  editor.noteType = detail.noteType
  editor.tagInput = detail.tags.join(', ')
  editor.folderId = detail.folderId || ''
  editor.pinned = detail.pinned
  editor.archived = detail.archived
  editor.currentVersion = detail.currentVersion
  editor.createdAt = detail.createdAt
  editor.updatedAt = detail.updatedAt
}

function resetEditor(): void {
  activeNote.value = null
  editor.title = ''
  editor.content = ''
  editor.noteType = 'PLAIN_TEXT'
  editor.tagInput = ''
  editor.folderId = ''
  editor.pinned = false
  editor.archived = false
  editor.currentVersion = 1
  editor.createdAt = ''
  editor.updatedAt = ''
}

async function syncRoute(noteId: string, folderId: string): Promise<void> {
  const query = { ...route.query }
  if (noteId) {
    query.noteId = noteId
  } else {
    delete query.noteId
  }
  if (folderId && folderId !== ALL_FOLDER_ID) {
    query.folderId = folderId
  } else {
    delete query.folderId
  }
  await router.replace({ query })
}

function resolveMessage(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback
}
</script>

<template>
  <div class="standard-notes-page">
    <StandardNotesWorkspaceHero :overview="overview" :loading="loading.refresh" @refresh="onRefresh" />

    <div class="workspace-grid">
      <StandardNotesCollectionsPanel
        :folders="folders"
        :selected-folder-id="selectedFolderId"
        :loading="loading.folders"
        :saving="loading.folderSave"
        :deleting-id="loading.folderDeleteId"
        @select="onSelectFolder"
        @create="onCreateFolder"
        @update="onUpdateFolder"
        @remove="onDeleteFolder"
      />

      <div class="center-column">
        <StandardNotesListPanel
          v-model:filters="filters"
          :notes="notes"
          :active-note-id="activeNoteId"
          :loading="loading.list"
          :creating="loading.create"
          :note-type-options="noteTypeOptions"
          @create="onCreate"
          @refresh="onRefresh"
          @search="loadNotes(activeNote?.id || routeNoteId)"
          @select="selectNote"
        />

        <StandardNotesEditorPanel
          v-model:editor="editor"
          :active-note="activeNote"
          :folders="folders"
          :save-loading="loading.save"
          :delete-loading="loading.delete"
          :create-loading="loading.create"
          :note-type-options="noteTypeOptions"
          @create="onCreate"
          @save="onSave"
          @remove="onDelete"
        />
      </div>

      <div class="aside-column">
        <StandardNotesTaskPanel
          :note="activeNote"
          :loading="loading.taskIndex !== null"
          :toggling-index="loading.taskIndex"
          @toggle="onToggleChecklistItem"
        />
        <StandardNotesExportPanel
          :snapshot="exportSnapshot"
          :loading="loading.export"
          @export="onExportWorkspace"
          @download="downloadExportSnapshot"
        />
        <StandardNotesPulsePanel :health-chips="healthChips" />
        <StandardNotesKnowledgeRail :overview="overview" :available-tags="availableTags" />
      </div>
    </div>
  </div>
</template>

<style scoped src="../assets/styles/standard-notes-page.css"></style>
