<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ApiClientError } from '~/utils/request'
import type {
  DocsNoteComment,
  DocsNotePresence,
  DocsNoteShare,
  DocsNoteSummary,
  DocsNoteSyncEvent,
  DocsPermission
} from '~/types/api'
import type { DocsNoteSuggestion, DocsReviewMode } from '~/types/docs'
import { useDocsApi } from '~/composables/useDocsApi'
import { useI18n } from '~/composables/useI18n'
import { useDocsSyncStream } from '~/composables/useDocsSyncStream'
import { useAuthStore } from '~/stores/auth'
import { resolveSessionIdFromAccessToken } from '~/utils/auth-session'
import { extractSelectedExcerpt } from '~/utils/docs-selection'
import { needsDocsDetailRefresh } from '~/utils/docs-suggestions'

interface TextareaInputRef {
  textarea?: HTMLTextAreaElement
}

const PRESENCE_HEARTBEAT_MS = 20000

const authStore = useAuthStore()
const { t } = useI18n()
const {
  listNotes,
  createNote,
  getNote,
  updateNote,
  deleteNote,
  getCollaborationOverview,
  createShare,
  updateSharePermission,
  revokeShare,
  createComment,
  resolveComment,
  listSuggestions,
  createSuggestion,
  acceptSuggestion,
  rejectSuggestion,
  heartbeatPresence,
  listPresence
} = useDocsApi()

const keyword = ref('')
const scopeFilter = ref<'ALL' | 'OWNED' | 'SHARED'>('ALL')
const loadingList = ref(false)
const loadingDetail = ref(false)
const creating = ref(false)
const saving = ref(false)
const deleting = ref(false)
const sharing = ref(false)
const revokingShareId = ref('')
const updatingShareId = ref('')
const commenting = ref(false)
const suggestionSubmitting = ref(false)
const busySuggestionId = ref('')
const refreshing = ref(false)
const activeNoteId = ref('')
const currentSessionId = ref('')
const notes = ref<DocsNoteSummary[]>([])
const collaborators = ref<DocsNoteShare[]>([])
const comments = ref<DocsNoteComment[]>([])
const suggestions = ref<DocsNoteSuggestion[]>([])
const activeSessions = ref<DocsNotePresence[]>([])
const latestSyncEvent = ref<DocsNoteSyncEvent | null>(null)
const syncConflictMessage = ref('')
const selectedExcerpt = ref('')
const selectedRangeStart = ref(0)
const selectedRangeEnd = ref(0)
const noteDeleted = ref(false)
const editorInputRef = ref<TextareaInputRef | null>(null)
const presenceTicker = ref<ReturnType<typeof setInterval> | null>(null)

const editor = reactive({
  title: '',
  content: '',
  updatedAt: '',
  currentVersion: 1,
  permission: 'OWNER' as DocsPermission,
  ownerDisplayName: '',
  ownerEmail: '',
  syncCursor: 0,
  syncVersion: 'DOC-0'
})

const commentDraft = reactive({
  content: ''
})

const reviewMode = ref<DocsReviewMode>('EDIT')

const {
  status: syncStatus,
  errorMessage: syncErrorMessage,
  connect: connectSync,
  disconnect: disconnectSync,
  reconnect: reconnectSync,
  lastCursor: syncCursor
} = useDocsSyncStream({
  noteId: activeNoteId,
  onPayload: handleSyncPayload
})

const hasActiveNote = computed(() => Boolean(activeNoteId.value) && !noteDeleted.value)
const canEdit = computed(() => editor.permission === 'OWNER' || editor.permission === 'EDIT')
const canShare = computed(() => editor.permission === 'OWNER')
const canResolveSuggestions = computed(() => editor.permission === 'OWNER')
const editorWritable = computed(() => canEdit.value && reviewMode.value === 'EDIT')
const filteredNotes = computed(() => notes.value.filter((note) => scopeFilter.value === 'ALL' || note.scope === scopeFilter.value))
const activeCommentCount = computed(() => comments.value.filter((item) => !item.resolved).length)
const sharedBadge = computed(() => {
  if (editor.permission === 'OWNER') return t('docs.badges.owner')
  if (editor.permission === 'EDIT') return t('docs.badges.sharedEdit')
  return t('docs.badges.sharedView')
})
const reviewModeLabel = computed(() => {
  return reviewMode.value === 'EDIT' ? t('docs.review.edit') : t('docs.review.suggest')
})
const syncStatusLabel = computed(() => {
  if (syncStatus.value === 'CONNECTED') return t('docs.sync.connected')
  if (syncStatus.value === 'RECONNECTING') return t('docs.sync.reconnecting')
  if (syncStatus.value === 'CONNECTING') return t('docs.sync.connecting')
  if (syncStatus.value === 'ERROR') return t('docs.sync.error')
  return t('docs.sync.idle')
})
const syncStatusType = computed(() => {
  if (syncStatus.value === 'CONNECTED') return 'success'
  if (syncStatus.value === 'RECONNECTING' || syncStatus.value === 'CONNECTING') return 'warning'
  if (syncStatus.value === 'ERROR') return 'danger'
  return 'info'
})
const latestSyncSummary = computed(() => {
  if (!latestSyncEvent.value) {
    return t('docs.sync.noEvent')
  }
  const source = isExternalEvent(latestSyncEvent.value) ? t('docs.sync.otherSession') : t('docs.sync.thisSession')
  return t('docs.sync.latestEvent', {
    eventType: latestSyncEvent.value.eventType,
    actor: latestSyncEvent.value.actorEmail || t('docs.sync.systemActor'),
    source
  })
})

function formatTime(value: string | null): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function isExternalEvent(event: DocsNoteSyncEvent): boolean {
  return Boolean(currentSessionId.value && event.sessionId && event.sessionId !== currentSessionId.value)
}

async function loadNotes(keepSelection = true): Promise<void> {
  loadingList.value = true
  try {
    const next = await listNotes(keyword.value.trim(), 200)
    notes.value = next
    if (!next.length) {
      resetEditor()
      return
    }
    if (!keepSelection || !activeNoteId.value || !next.some((note) => note.id === activeNoteId.value)) {
      await selectNote(next[0].id)
    }
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.loadNotesFailed'))
  } finally {
    loadingList.value = false
  }
}

async function selectNote(noteId: string): Promise<void> {
  stopPresenceTicker()
  disconnectSync()
  noteDeleted.value = false
  activeNoteId.value = noteId
  syncConflictMessage.value = ''
  selectedExcerpt.value = ''
  selectedRangeStart.value = 0
  selectedRangeEnd.value = 0
  await Promise.all([loadActiveNote(), loadCollaboration(), loadSuggestions()])
  await sendPresence(true)
  startPresenceTicker()
  await nextTick()
  void connectSync(editor.syncCursor || undefined)
}

async function loadActiveNote(): Promise<void> {
  if (!activeNoteId.value) {
    return
  }
  loadingDetail.value = true
  try {
    const detail = await getNote(activeNoteId.value)
    noteDeleted.value = false
    editor.title = detail.title
    editor.content = detail.content || ''
    editor.updatedAt = detail.updatedAt
    editor.currentVersion = detail.currentVersion
    editor.permission = detail.permission
    editor.ownerDisplayName = detail.ownerDisplayName
    editor.ownerEmail = detail.ownerEmail
    editor.syncCursor = detail.syncCursor
    editor.syncVersion = detail.syncVersion
    syncCursor.value = detail.syncCursor
  } catch (error) {
    const normalized = error as ApiClientError
    if (normalized.status === 404) {
      noteDeleted.value = true
      syncConflictMessage.value = 'This note is no longer available. It may have been deleted by the owner.'
      return
    }
    ElMessage.error(normalized.message || t('docs.messages.loadNoteFailed'))
  } finally {
    loadingDetail.value = false
  }
}

async function loadCollaboration(): Promise<void> {
  if (!activeNoteId.value) {
    return
  }
  try {
    const overview = await getCollaborationOverview(activeNoteId.value)
    collaborators.value = overview.collaborators
    comments.value = overview.comments
    activeSessions.value = overview.activeSessions
    editor.syncCursor = overview.syncCursor
    editor.syncVersion = overview.syncVersion
    syncCursor.value = overview.syncCursor
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.loadCollaborationFailed'))
  }
}

async function loadSuggestions(): Promise<void> {
  if (!activeNoteId.value) {
    return
  }
  try {
    suggestions.value = await listSuggestions(activeNoteId.value, true)
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.loadSuggestionsFailed'))
  }
}

async function onCreate(): Promise<void> {
  creating.value = true
  try {
    const suffix = new Date().toISOString().slice(0, 16).replace('T', ' ')
    const created = await createNote({ title: `${t('docs.defaultNoteTitle')} ${suffix}`, content: '' })
    ElMessage.success(t('docs.messages.noteCreated'))
    await loadNotes(false)
    activeNoteId.value = created.id
    await selectNote(created.id)
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.createFailed'))
  } finally {
    creating.value = false
  }
}

async function onSave(): Promise<void> {
  if (!activeNoteId.value || !editorWritable.value) {
    ElMessage.warning(t('docs.messages.readOnly'))
    return
  }
  if (!editor.title.trim()) {
    ElMessage.warning(t('docs.messages.titleRequired'))
    return
  }
  saving.value = true
  try {
    const updated = await updateNote(activeNoteId.value, {
      title: editor.title.trim(),
      content: editor.content,
      currentVersion: editor.currentVersion
    })
    applyDetail(updated)
    syncConflictMessage.value = ''
    ElMessage.success(t('docs.messages.noteSaved'))
    await loadNotes(true)
    await loadCollaboration()
  } catch (error) {
    const normalized = error as ApiClientError
    if (normalized.code === 30018 || normalized.status === 409) {
      syncConflictMessage.value = t('docs.messages.syncConflict')
    }
    ElMessage.error(normalized.message || t('docs.messages.saveFailed'))
  } finally {
    saving.value = false
  }
}

async function onDelete(): Promise<void> {
  if (!activeNoteId.value) {
    ElMessage.warning(t('docs.messages.selectNote'))
    return
  }
  try {
    await ElMessageBox.confirm(t('docs.messages.deleteConfirm'), t('docs.messages.deleteTitle'), {
      type: 'warning',
      confirmButtonText: t('common.actions.delete'),
      cancelButtonText: t('common.actions.cancel')
    })
  } catch {
    return
  }
  deleting.value = true
  try {
    await deleteNote(activeNoteId.value)
    ElMessage.success(t('docs.messages.noteDeleted'))
    await loadNotes(false)
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.deleteFailed'))
  } finally {
    deleting.value = false
  }
}

async function onRefreshWorkspace(): Promise<void> {
  if (!activeNoteId.value) {
    return
  }
  refreshing.value = true
  try {
    await Promise.all([loadActiveNote(), loadCollaboration(), loadSuggestions(), loadNotes(true)])
    syncConflictMessage.value = ''
    ElMessage.success(t('docs.messages.workspaceRefreshed'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.refreshFailed'))
  } finally {
    refreshing.value = false
  }
}

async function onCreateShare(payload: { collaboratorEmail: string; permission: 'VIEW' | 'EDIT' }): Promise<void> {
  if (!activeNoteId.value || !canShare.value) {
    ElMessage.warning(t('docs.messages.shareOwnerOnly'))
    return
  }
  sharing.value = true
  try {
    const created = await createShare(activeNoteId.value, payload)
    collaborators.value = [created, ...collaborators.value]
    await loadNotes(true)
    await loadCollaboration()
    ElMessage.success(t('docs.messages.collaboratorAdded'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.shareFailed'))
  } finally {
    sharing.value = false
  }
}

async function onUpdateSharePermission(payload: { shareId: string; permission: 'VIEW' | 'EDIT' }): Promise<void> {
  if (!activeNoteId.value || !canShare.value) {
    ElMessage.warning(t('docs.messages.shareOwnerOnly'))
    return
  }
  updatingShareId.value = payload.shareId
  try {
    const updated = await updateSharePermission(activeNoteId.value, payload.shareId, { permission: payload.permission })
    collaborators.value = collaborators.value.map((item) => item.shareId === payload.shareId ? updated : item)
    await Promise.all([loadActiveNote(), loadNotes(true)])
    ElMessage.success(t('docs.messages.sharePermissionUpdated'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.sharePermissionUpdateFailed'))
  } finally {
    updatingShareId.value = ''
  }
}

async function onRevokeShare(share: DocsNoteShare): Promise<void> {
  if (!activeNoteId.value || !canShare.value) {
    ElMessage.warning(t('docs.messages.shareOwnerOnly'))
    return
  }
  try {
    await ElMessageBox.confirm(
      t('docs.messages.revokeConfirm', { email: share.collaboratorEmail }),
      t('docs.messages.revokeTitle'),
      {
        type: 'warning',
        confirmButtonText: t('docs.share.revoke'),
        cancelButtonText: t('common.actions.cancel')
      }
    )
  } catch {
    return
  }

  revokingShareId.value = share.shareId
  try {
    await revokeShare(activeNoteId.value, share.shareId)
    await Promise.all([loadActiveNote(), loadCollaboration(), loadNotes(true)])
    ElMessage.success(t('docs.messages.collaboratorRevoked'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.revokeFailed'))
  } finally {
    revokingShareId.value = ''
  }
}

async function onCreateSuggestion(payload: { replacementText: string }): Promise<void> {
  if (!activeNoteId.value || !canEdit.value) {
    ElMessage.warning(t('docs.messages.suggestionEditorOnly'))
    return
  }
  if (!selectedExcerpt.value) {
    ElMessage.warning(t('docs.messages.suggestionSelectionRequired'))
    return
  }
  suggestionSubmitting.value = true
  try {
    const created = await createSuggestion(activeNoteId.value, {
      selectionStart: selectedRangeStart.value,
      selectionEnd: selectedRangeEnd.value,
      originalText: selectedExcerpt.value,
      replacementText: payload.replacementText,
      baseVersion: editor.currentVersion
    })
    suggestions.value = [created, ...suggestions.value]
    selectedExcerpt.value = ''
    selectedRangeStart.value = 0
    selectedRangeEnd.value = 0
    ElMessage.success(t('docs.messages.suggestionCreated'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.suggestionCreateFailed'))
  } finally {
    suggestionSubmitting.value = false
  }
}

async function onAcceptSuggestion(suggestionId: string): Promise<void> {
  if (!activeNoteId.value || !canResolveSuggestions.value) {
    ElMessage.warning(t('docs.messages.suggestionOwnerOnly'))
    return
  }
  busySuggestionId.value = suggestionId
  try {
    const updated = await acceptSuggestion(activeNoteId.value, suggestionId, {
      currentVersion: editor.currentVersion
    })
    suggestions.value = suggestions.value.map((item) => item.suggestionId === suggestionId ? updated : item)
    await Promise.all([loadActiveNote(), loadCollaboration(), loadNotes(true)])
    ElMessage.success(t('docs.messages.suggestionAccepted'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.suggestionAcceptFailed'))
  } finally {
    busySuggestionId.value = ''
  }
}

async function onRejectSuggestion(suggestionId: string): Promise<void> {
  if (!activeNoteId.value || !canResolveSuggestions.value) {
    ElMessage.warning(t('docs.messages.suggestionOwnerOnly'))
    return
  }
  busySuggestionId.value = suggestionId
  try {
    const updated = await rejectSuggestion(activeNoteId.value, suggestionId)
    suggestions.value = suggestions.value.map((item) => item.suggestionId === suggestionId ? updated : item)
    ElMessage.success(t('docs.messages.suggestionRejected'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.suggestionRejectFailed'))
  } finally {
    busySuggestionId.value = ''
  }
}

async function onCreateComment(): Promise<void> {
  if (!activeNoteId.value || !commentDraft.content.trim()) {
    ElMessage.warning(t('docs.messages.commentRequired'))
    return
  }
  commenting.value = true
  try {
    const created = await createComment(activeNoteId.value, {
      excerpt: selectedExcerpt.value || undefined,
      content: commentDraft.content.trim()
    })
    comments.value = [created, ...comments.value]
    commentDraft.content = ''
    selectedExcerpt.value = ''
    ElMessage.success(t('docs.messages.commentAdded'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.commentFailed'))
  } finally {
    commenting.value = false
  }
}

async function onResolveComment(commentId: string): Promise<void> {
  if (!activeNoteId.value || !canEdit.value) {
    ElMessage.warning(t('docs.messages.resolveEditorOnly'))
    return
  }
  try {
    const updated = await resolveComment(activeNoteId.value, commentId)
    comments.value = comments.value.map((item) => item.commentId === commentId ? updated : item)
    ElMessage.success(t('docs.messages.commentResolved'))
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.resolveFailed'))
  }
}

function captureSelection(): void {
  const textarea = editorInputRef.value?.textarea
  if (!textarea) {
    selectedExcerpt.value = ''
    selectedRangeStart.value = 0
    selectedRangeEnd.value = 0
    return
  }
  selectedRangeStart.value = textarea.selectionStart
  selectedRangeEnd.value = textarea.selectionEnd
  selectedExcerpt.value = extractSelectedExcerpt(editor.content, textarea.selectionStart, textarea.selectionEnd)
}

async function sendPresence(silent = false): Promise<void> {
  if (!activeNoteId.value) {
    return
  }
  try {
    await heartbeatPresence(activeNoteId.value, {
      activeMode: canEdit.value ? 'EDIT' : 'VIEW'
    })
    activeSessions.value = await listPresence(activeNoteId.value)
    if (!silent) {
      ElMessage.success(t('docs.messages.presenceUpdated'))
    }
  } catch (error) {
    if (!silent) {
      ElMessage.error((error as Error).message || t('docs.messages.presenceFailed'))
    }
  }
}

function startPresenceTicker(): void {
  stopPresenceTicker()
  presenceTicker.value = setInterval(() => {
    void sendPresence(true)
  }, PRESENCE_HEARTBEAT_MS)
}

function stopPresenceTicker(): void {
  if (!presenceTicker.value) {
    return
  }
  clearInterval(presenceTicker.value)
  presenceTicker.value = null
}

async function handleSyncPayload(payload: { syncCursor: number; syncVersion: string; items: DocsNoteSyncEvent[] }): Promise<void> {
  editor.syncCursor = payload.syncCursor
  editor.syncVersion = payload.syncVersion
  syncCursor.value = payload.syncCursor
  latestSyncEvent.value = payload.items.at(-1) || null
  if (!latestSyncEvent.value) {
    return
  }
  const external = isExternalEvent(latestSyncEvent.value)
  await loadNotes(true)
  await loadCollaboration()
  await loadSuggestions()
  if (needsDocsDetailRefresh(latestSyncEvent.value.eventType) && external) {
    syncConflictMessage.value = 'Another session updated this note. Refresh to load the latest content.'
    await loadActiveNote()
    return
  }
  if (needsDocsDetailRefresh(latestSyncEvent.value.eventType)) {
    await loadActiveNote()
    return
  }
  if (external) {
    syncConflictMessage.value = `Collaboration activity detected from ${latestSyncEvent.value.actorEmail}.`
  }
}

function applyDetail(detail: {
  title: string
  content: string
  updatedAt: string
  currentVersion: number
  permission: DocsPermission
  ownerDisplayName: string
  ownerEmail: string
  syncCursor: number
  syncVersion: string
}): void {
  editor.title = detail.title
  editor.content = detail.content || ''
  editor.updatedAt = detail.updatedAt
  editor.currentVersion = detail.currentVersion
  editor.permission = detail.permission
  editor.ownerDisplayName = detail.ownerDisplayName
  editor.ownerEmail = detail.ownerEmail
  editor.syncCursor = detail.syncCursor
  editor.syncVersion = detail.syncVersion
  syncCursor.value = detail.syncCursor
}

function resetEditor(): void {
  activeNoteId.value = ''
  noteDeleted.value = false
  editor.title = ''
  editor.content = ''
  editor.updatedAt = ''
  editor.currentVersion = 1
  editor.permission = 'OWNER'
  editor.ownerDisplayName = ''
  editor.ownerEmail = ''
  editor.syncCursor = 0
  editor.syncVersion = 'DOC-0'
  collaborators.value = []
  comments.value = []
  suggestions.value = []
  activeSessions.value = []
  latestSyncEvent.value = null
  syncConflictMessage.value = ''
  selectedExcerpt.value = ''
  selectedRangeStart.value = 0
  selectedRangeEnd.value = 0
  reviewMode.value = 'EDIT'
}

watch(() => authStore.accessToken, (token) => {
  currentSessionId.value = resolveSessionIdFromAccessToken(token || '')
}, { immediate: true })

onMounted(() => {
  void loadNotes(false)
})

onBeforeUnmount(() => {
  stopPresenceTicker()
  disconnectSync()
})
</script>

<template>
  <section class="docs-workbench">
    <header class="docs-topbar mm-card">
      <div class="docs-search-row">
        <el-input v-model="keyword" :placeholder="t('docs.search.placeholder')" @keyup.enter="loadNotes(false)" />
        <el-select v-model="scopeFilter" class="scope-select">
          <el-option :label="t('docs.search.scopes.all')" value="ALL" />
          <el-option :label="t('docs.search.scopes.owned')" value="OWNED" />
          <el-option :label="t('docs.search.scopes.shared')" value="SHARED" />
        </el-select>
        <el-button :loading="loadingList" @click="loadNotes(false)">{{ t('docs.search.action') }}</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">{{ t('docs.actions.newNote') }}</el-button>
        <el-button :loading="refreshing" @click="onRefreshWorkspace">{{ t('docs.actions.refreshWorkspace') }}</el-button>
      </div>
    </header>

    <div class="docs-grid">
      <aside class="docs-panel navigator-panel mm-card">
        <div class="panel-heading">
          <div>
            <div class="panel-title">Note Navigator</div>
            <div class="panel-subtitle">{{ filteredNotes.length }} visible notes</div>
          </div>
        </div>
        <button
          v-for="note in filteredNotes"
          :key="note.id"
          type="button"
          class="note-tile"
          :class="{ active: note.id === activeNoteId }"
          @click="selectNote(note.id)"
        >
          <div class="note-tile-head">
            <span class="note-title">{{ note.title }}</span>
            <el-tag size="small" :type="note.scope === 'SHARED' ? 'warning' : 'info'">{{ note.scope }}</el-tag>
          </div>
          <div class="note-meta-row">
            <span>{{ note.permission }}</span>
            <span>v{{ note.currentVersion }}</span>
          </div>
          <div class="note-meta-row muted">
            <span>{{ note.ownerDisplayName || note.ownerEmail }}</span>
            <span>{{ formatTime(note.updatedAt) }}</span>
          </div>
        </button>
        <el-empty v-if="!filteredNotes.length" :description="t('docs.empty.notes')" />
      </aside>

      <section class="docs-panel editor-panel mm-card">
        <template v-if="hasActiveNote">
          <div class="panel-heading editor-header">
            <div>
              <div class="panel-title">{{ editor.title || 'Untitled note' }}</div>
              <div class="panel-subtitle">
                {{ editor.ownerDisplayName || editor.ownerEmail }} · {{ sharedBadge }} · Updated {{ formatTime(editor.updatedAt) }}
              </div>
            </div>
            <div class="editor-badges">
              <el-tag>{{ editor.permission }}</el-tag>
              <el-tag type="info">v{{ editor.currentVersion }}</el-tag>
              <el-tag type="warning">{{ editor.syncVersion }}</el-tag>
            </div>
          </div>

          <el-alert
            v-if="syncConflictMessage"
            class="alert-block"
            type="warning"
            :closable="false"
            :title="syncConflictMessage"
          />

          <div class="editor-fields">
            <el-input v-model="editor.title" maxlength="128" :placeholder="t('docs.editor.titlePlaceholder')" :disabled="!canEdit" />
            <el-input
              ref="editorInputRef"
              v-model="editor.content"
              type="textarea"
              :rows="20"
              maxlength="100000"
              show-word-limit
              :placeholder="t('docs.editor.contentPlaceholder')"
              :readonly="!canEdit || reviewMode === 'SUGGEST'"
              @select="captureSelection"
              @mouseup="captureSelection"
              @keyup="captureSelection"
            />
          </div>

          <div class="editor-toolbar">
            <div class="selection-pill">{{ t('docs.editor.selection', { value: selectedExcerpt || t('docs.common.none') }) }}</div>
            <div class="editor-actions">
              <el-button @click="captureSelection">{{ t('docs.actions.quoteSelection') }}</el-button>
              <el-button type="primary" :disabled="!editorWritable" :loading="saving" @click="onSave">{{ t('common.actions.save') }}</el-button>
              <el-button :loading="refreshing" @click="onRefreshWorkspace">{{ t('common.actions.refresh') }}</el-button>
              <el-button type="danger" :disabled="editor.permission !== 'OWNER'" :loading="deleting" @click="onDelete">{{ t('common.actions.delete') }}</el-button>
            </div>
          </div>
        </template>

        <el-empty v-else-if="noteDeleted" :description="t('docs.empty.deleted')" />
        <el-empty v-else :description="t('docs.empty.start')" />
      </section>

      <aside class="docs-panel rail-panel mm-card">
        <div class="panel-heading">
          <div>
            <div class="panel-title">{{ t('docs.rail.title') }}</div>
            <div class="panel-subtitle">{{ t('docs.rail.subtitle', { comments: activeCommentCount, sessions: activeSessions.length }) }}</div>
          </div>
        </div>

        <section class="rail-card">
          <div class="rail-title">{{ t('docs.review.title') }}</div>
          <div class="sync-row">
            <el-select v-model="reviewMode" size="small">
              <el-option :label="t('docs.review.edit')" value="EDIT" />
              <el-option :label="t('docs.review.suggest')" value="SUGGEST" />
            </el-select>
            <el-tag type="info">v{{ editor.currentVersion }}</el-tag>
          </div>
          <div class="rail-copy">{{ t('docs.review.description', { mode: reviewModeLabel }) }}</div>
          <div class="rail-meta">{{ t('docs.review.lastSaved', { value: formatTime(editor.updatedAt) }) }}</div>
        </section>

        <section class="rail-card">
          <div class="rail-title">{{ t('docs.sync.title') }}</div>
          <div class="sync-row">
            <el-tag :type="syncStatusType">{{ syncStatusLabel }}</el-tag>
            <el-button text @click="reconnectSync">{{ t('docs.sync.reconnect') }}</el-button>
          </div>
          <div class="rail-copy">{{ syncErrorMessage || latestSyncSummary }}</div>
          <div class="rail-meta">{{ t('docs.sync.cursorMeta', { cursor: editor.syncCursor, version: editor.syncVersion }) }}</div>
        </section>

        <section class="rail-card">
          <div class="rail-title">{{ t('docs.collaborators.title') }}</div>
          <div v-if="activeSessions.length" class="session-list">
            <div v-for="session in activeSessions" :key="session.presenceId" class="session-item">
              <div>
                <div class="session-name">{{ session.displayName || session.email }}</div>
                <div class="session-meta">{{ session.permission }} · {{ session.activeMode }}</div>
              </div>
              <div class="session-time">{{ formatTime(session.lastHeartbeatAt) }}</div>
            </div>
          </div>
          <el-empty v-else :description="t('docs.collaborators.empty')" :image-size="70" />
        </section>

        <section class="rail-card">
          <DocsShareManager
            :shares="collaborators"
            :can-manage="canShare"
            :sharing="sharing"
            :revoking-share-id="revokingShareId"
            :updating-share-id="updatingShareId"
            @create="onCreateShare"
            @update-permission="onUpdateSharePermission"
            @revoke="onRevokeShare"
          />
        </section>

        <section class="rail-card">
          <DocsSuggestionInbox
            :suggestions="suggestions"
            :selected-excerpt="selectedExcerpt"
            :review-mode="reviewMode"
            :can-create="canEdit"
            :can-resolve="canResolveSuggestions"
            :submitting="suggestionSubmitting"
            :busy-suggestion-id="busySuggestionId"
            @create="onCreateSuggestion"
            @accept="onAcceptSuggestion"
            @reject="onRejectSuggestion"
          />
        </section>

        <section class="rail-card comments-card">
          <div class="rail-title">{{ t('docs.comments.title') }}</div>
          <div class="comment-compose">
            <div class="selection-pill">{{ t('docs.comments.excerpt', { value: selectedExcerpt || t('docs.common.none') }) }}</div>
            <el-input
              v-model="commentDraft.content"
              type="textarea"
              :rows="3"
              maxlength="2000"
              show-word-limit
              :placeholder="t('docs.comments.placeholder')"
            />
            <el-button type="primary" :loading="commenting" @click="onCreateComment">{{ t('docs.comments.add') }}</el-button>
          </div>
          <div v-if="comments.length" class="comment-list">
            <article v-for="comment in comments" :key="comment.commentId" class="comment-item" :class="{ resolved: comment.resolved }">
              <div class="comment-head">
                <div>
                  <div class="comment-author">{{ comment.authorDisplayName || comment.authorEmail }}</div>
                  <div class="comment-time">{{ formatTime(comment.createdAt) }}</div>
                </div>
                <el-button v-if="!comment.resolved && canEdit" text @click="onResolveComment(comment.commentId)">{{ t('docs.comments.resolve') }}</el-button>
              </div>
              <blockquote v-if="comment.excerpt" class="comment-excerpt">{{ comment.excerpt }}</blockquote>
              <p class="comment-body">{{ comment.content }}</p>
              <div v-if="comment.resolved" class="comment-resolved">{{ t('docs.comments.resolvedAt', { value: formatTime(comment.resolvedAt) }) }}</div>
            </article>
          </div>
          <el-empty v-else :description="t('docs.comments.empty')" :image-size="70" />
        </section>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.docs-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.docs-topbar,
.docs-panel {
  padding: 16px;
}

.docs-search-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 140px auto auto auto;
  gap: 12px;
}

.docs-grid {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 360px;
  gap: 16px;
  min-height: 760px;
}

.panel-heading,
.note-tile-head,
.note-meta-row,
.editor-toolbar,
.sync-row,
.share-item,
.session-item,
.comment-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
}

.panel-subtitle,
.muted,
.rail-copy,
.rail-meta,
.session-meta,
.session-time,
.comment-time,
.comment-resolved,
.share-meta,
.selection-pill {
  color: var(--mm-muted);
  font-size: 12px;
}

.navigator-panel,
.rail-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.note-tile,
.rail-card,
.comment-item {
  border: 1px solid rgba(125, 125, 145, 0.18);
  border-radius: 16px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 249, 252, 0.98));
}

.note-tile {
  width: 100%;
  padding: 14px;
  text-align: left;
}

.note-tile.active {
  border-color: rgba(103, 80, 255, 0.4);
  box-shadow: 0 18px 40px rgba(84, 70, 190, 0.12);
}

.note-title,
.session-name,
.share-name,
.comment-author {
  font-weight: 600;
}

.editor-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.editor-header,
.editor-badges,
.editor-fields,
.editor-actions,
.share-form,
.comment-compose,
.comment-list,
.session-list,
.share-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.editor-badges {
  flex-direction: row;
}

.share-item-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.alert-block,
.rail-card {
  margin: 0;
}

.comments-card {
  flex: 1;
}

.comment-excerpt {
  margin: 8px 0;
  padding-left: 12px;
  border-left: 3px solid rgba(103, 80, 255, 0.35);
  color: #4b5563;
}

.comment-body {
  margin: 0;
  line-height: 1.6;
}

.comment-item.resolved {
  opacity: 0.72;
}

@media (max-width: 1280px) {
  .docs-grid {
    grid-template-columns: 240px minmax(0, 1fr);
  }

  .rail-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 900px) {
  .docs-search-row,
  .docs-grid {
    grid-template-columns: 1fr;
  }
}
</style>
