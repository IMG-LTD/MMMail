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
import type { DocsRouteState, DocsScopeFilter } from '~/utils/docs-route'
import { useDocsApi } from '~/composables/useDocsApi'
import { useI18n } from '~/composables/useI18n'
import { useDocsSyncStream } from '~/composables/useDocsSyncStream'
import { useAuthStore } from '~/stores/auth'
import { resolveSessionIdFromAccessToken } from '~/utils/auth-session'
import {
  buildDocsRouteQuery,
  extractDocsRouteState,
  filterDocsNoteSummaries,
  resolveDocsVisibleNoteId,
  upsertDocsNoteSummary
} from '~/utils/docs-route'
import {
  clearDocsDraftSnapshot,
  hasDocsDraftDiff,
  readDocsDraftSnapshot,
  writeDocsDraftSnapshot
} from '~/utils/docs-draft'
import type { DocsDraftSnapshot } from '~/utils/docs-draft'
import {
  resolveDocsPermissionLabelKey,
  resolveDocsScopeLabelKey,
  resolveDocsScopeTagType,
  shouldConfirmDocsUnsavedChange
} from '~/utils/docs-presentation'
import { downloadDocsExport, parseDocsImportFile } from '~/utils/docs-transfer'
import { extractSelectedExcerpt } from '~/utils/docs-selection'
import { needsDocsDetailRefresh } from '~/utils/docs-suggestions'

interface TextareaInputRef {
  textarea?: HTMLTextAreaElement
}

interface DocsSelectResult {
  selected: boolean
  cancelled: boolean
}

interface DocsLoadNotesResult {
  cancelled: boolean
}

const PRESENCE_HEARTBEAT_MS = 20000

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const initialRouteState = extractDocsRouteState(route.query)
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

const keyword = ref(initialRouteState.keyword)
const scopeFilter = ref<DocsScopeFilter>(initialRouteState.scope)
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
const importInputRef = ref<HTMLInputElement | null>(null)
const presenceTicker = ref<ReturnType<typeof setInterval> | null>(null)
const draftRecoverySnapshot = ref<DocsDraftSnapshot | null>(null)

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
const persistedEditorState = reactive({
  title: '',
  content: '',
  currentVersion: 1
})

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
const filteredNotes = computed(() => filterDocsNoteSummaries(notes.value, scopeFilter.value))
const activeCommentCount = computed(() => comments.value.filter((item) => !item.resolved).length)
const hasUnsavedChanges = computed(() => hasActiveNote.value && canEdit.value && hasDocsDraftDiff(editor, persistedEditorState))
const notesEmptyDescription = computed(() => {
  if (keyword.value.trim()) {
    return t('docs.empty.search', { keyword: keyword.value.trim() })
  }
  if (scopeFilter.value !== 'ALL') {
    return t('docs.empty.scope', { scope: t(`docs.search.scopes.${scopeFilter.value.toLowerCase()}`) })
  }
  return t('docs.empty.notes')
})
const sharedBadge = computed(() => {
  if (editor.permission === 'OWNER') return t('docs.badges.owner')
  if (editor.permission === 'EDIT') return t('docs.badges.sharedEdit')
  return t('docs.badges.sharedView')
})
const editorTitle = computed(() => editor.title || t('docs.common.untitled'))
const dirtyStateLabel = computed(() => hasUnsavedChanges.value ? t('docs.dirty.unsaved') : t('docs.dirty.saved'))
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

function markPersistedEditorState(detail: { title: string; content: string; currentVersion: number }): void {
  persistedEditorState.title = detail.title
  persistedEditorState.content = detail.content
  persistedEditorState.currentVersion = detail.currentVersion
}

function syncDraftRecoverySnapshot(): void {
  if (!activeNoteId.value) {
    draftRecoverySnapshot.value = null
    return
  }
  const snapshot = readDocsDraftSnapshot(activeNoteId.value)
  if (!snapshot || !hasDocsDraftDiff(snapshot, editor)) {
    draftRecoverySnapshot.value = null
    return
  }
  draftRecoverySnapshot.value = snapshot
}

function persistLocalDraftSnapshot(): void {
  if (!activeNoteId.value || !hasActiveNote.value || !canEdit.value) {
    return
  }
  if (!hasDocsDraftDiff(editor, persistedEditorState)) {
    if (!draftRecoverySnapshot.value) {
      clearDocsDraftSnapshot(activeNoteId.value)
    }
    return
  }
  writeDocsDraftSnapshot({
    noteId: activeNoteId.value,
    title: editor.title,
    content: editor.content,
    baseVersion: persistedEditorState.currentVersion,
    savedAt: new Date().toISOString()
  })
}

function captureAppliedDocsViewState(): DocsRouteState {
  const routeState = resolveRouteState()
  return {
    noteId: activeNoteId.value || routeState.noteId,
    keyword: routeState.keyword,
    scope: routeState.scope
  }
}

async function restoreDocsViewState(state: DocsRouteState): Promise<void> {
  keyword.value = state.keyword
  scopeFilter.value = state.scope
  await syncRoute(state.noteId)
}

async function clearDocsSelection(nextNotes: DocsNoteSummary[]): Promise<DocsLoadNotesResult> {
  if (!(await confirmDiscardUnsavedChanges(null))) {
    return { cancelled: true }
  }
  notes.value = nextNotes
  resetEditor()
  await syncRoute(null)
  return { cancelled: false }
}

async function loadNotes(keepSelection = true): Promise<DocsLoadNotesResult> {
  loadingList.value = true
  try {
    const next = await listNotes(keyword.value.trim(), 200)
    const visibleNotes = filterDocsNoteSummaries(next, scopeFilter.value)
    if (!visibleNotes.length) {
      return await clearDocsSelection(next)
    }
    let unavailableNoteId: string | null = null
    const routeNoteId = resolveRouteNoteId()
    if (routeNoteId && routeNoteId !== activeNoteId.value && visibleNotes.some((note) => note.id === routeNoteId)) {
      const result = await selectNote(routeNoteId, { syncRouteAfterLoad: false })
      if (result.selected || result.cancelled) {
        if (result.selected) {
          notes.value = next
        }
        return { cancelled: result.cancelled }
      }
      if (noteDeleted.value) {
        unavailableNoteId = routeNoteId
        ElMessage.warning(t('docs.messages.noteUnavailable'))
      }
    }
    const fallbackVisibleNotes = unavailableNoteId
      ? visibleNotes.filter((note) => note.id !== unavailableNoteId)
      : visibleNotes
    if (!fallbackVisibleNotes.length) {
      return await clearDocsSelection(next)
    }
    if (keepSelection && activeNoteId.value && fallbackVisibleNotes.some((note) => note.id === activeNoteId.value)) {
      notes.value = next
      await syncRoute(activeNoteId.value)
      return { cancelled: false }
    }
    const nextVisibleNoteId = resolveDocsVisibleNoteId(fallbackVisibleNotes, activeNoteId.value)
    if (!nextVisibleNoteId) {
      return await clearDocsSelection(next)
    }
    const result = await selectNote(nextVisibleNoteId)
    if (result.selected) {
      notes.value = next
    }
    return { cancelled: result.cancelled }
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.loadNotesFailed'))
    return { cancelled: false }
  } finally {
    loadingList.value = false
  }
}

async function confirmDiscardUnsavedChanges(targetNoteId?: string | null): Promise<boolean> {
  if (!shouldConfirmDocsUnsavedChange(hasUnsavedChanges.value, activeNoteId.value, targetNoteId)) {
    return true
  }
  try {
    await ElMessageBox.confirm(
      t('docs.unsaved.description'),
      t('docs.unsaved.title'),
      {
        type: 'warning',
        confirmButtonText: t('docs.unsaved.leave'),
        cancelButtonText: t('common.actions.cancel')
      }
    )
    return true
  } catch {
    return false
  }
}

async function selectNote(
  noteId: string,
  options: { syncRouteAfterLoad?: boolean; confirmUnsaved?: boolean } = {}
): Promise<DocsSelectResult> {
  const syncRouteAfterLoad = options.syncRouteAfterLoad ?? true
  const confirmUnsaved = options.confirmUnsaved ?? true
  if (noteId === activeNoteId.value) {
    return { selected: true, cancelled: false }
  }
  if (confirmUnsaved && !(await confirmDiscardUnsavedChanges(noteId))) {
    return { selected: false, cancelled: true }
  }
  stopPresenceTicker()
  disconnectSync()
  noteDeleted.value = false
  activeNoteId.value = noteId
  syncConflictMessage.value = ''
  selectedExcerpt.value = ''
  selectedRangeStart.value = 0
  selectedRangeEnd.value = 0
  const loaded = await loadActiveNote()
  if (!loaded) {
    return { selected: false, cancelled: false }
  }
  await Promise.all([loadCollaboration(), loadSuggestions()])
  await sendPresence(true)
  startPresenceTicker()
  await nextTick()
  if (syncRouteAfterLoad) {
    await syncRoute(noteId)
  }
  void connectSync(editor.syncCursor || undefined)
  return { selected: true, cancelled: false }
}

async function loadActiveNote(): Promise<boolean> {
  if (!activeNoteId.value) {
    return false
  }
  loadingDetail.value = true
  try {
    const detail = await getNote(activeNoteId.value)
    noteDeleted.value = false
    applyDetail(detail)
    markPersistedEditorState({
      title: detail.title,
      content: detail.content || '',
      currentVersion: detail.currentVersion
    })
    syncDraftRecoverySnapshot()
    notes.value = upsertDocsNoteSummary(notes.value, detail)
    return true
  } catch (error) {
    const normalized = error as ApiClientError
    if (normalized.status === 404) {
      markUnavailableNoteState()
      return false
    }
    ElMessage.error(normalized.message || t('docs.messages.loadNoteFailed'))
    return false
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
  if (!(await confirmDiscardUnsavedChanges())) {
    return
  }
  creating.value = true
  try {
    const suffix = new Date().toISOString().slice(0, 16).replace('T', ' ')
    const created = await createNote({ title: `${t('docs.defaultNoteTitle')} ${suffix}`, content: '' })
    if (scopeFilter.value === 'SHARED') {
      scopeFilter.value = 'OWNED'
    }
    ElMessage.success(t('docs.messages.noteCreated'))
    await loadNotes(false)
    await selectNote(created.id, { confirmUnsaved: false })
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
    markPersistedEditorState({
      title: updated.title,
      content: updated.content || '',
      currentVersion: updated.currentVersion
    })
    clearDocsDraftSnapshot(activeNoteId.value)
    draftRecoverySnapshot.value = null
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
    const deletedNoteId = activeNoteId.value
    await deleteNote(activeNoteId.value)
    clearDocsDraftSnapshot(deletedNoteId)
    draftRecoverySnapshot.value = null
    ElMessage.success(t('docs.messages.noteDeleted'))
    await loadNotes(false)
  } catch (error) {
    ElMessage.error((error as Error).message || t('docs.messages.deleteFailed'))
  } finally {
    deleting.value = false
  }
}

function onExport(format: 'MARKDOWN' | 'TEXT'): void {
  if (!activeNoteId.value) {
    ElMessage.warning(t('docs.messages.selectNote'))
    return
  }
  downloadDocsExport(editor.title || t('docs.defaultNoteTitle'), editor.content, format)
  ElMessage.success(t('docs.messages.exported', { format: format === 'MARKDOWN' ? 'Markdown' : 'TXT' }))
}

function onImportTrigger(): void {
  if (!canEdit.value) {
    ElMessage.warning(t('docs.messages.readOnly'))
    return
  }
  importInputRef.value?.click()
}

async function onImportFileChange(event: Event): Promise<void> {
  const target = event.target as HTMLInputElement | null
  const file = target?.files?.[0]
  if (!file) {
    return
  }
  if (!activeNoteId.value) {
    ElMessage.warning(t('docs.messages.selectNote'))
    if (target) {
      target.value = ''
    }
    return
  }
  if (!canEdit.value) {
    ElMessage.warning(t('docs.messages.readOnly'))
    if (target) {
      target.value = ''
    }
    return
  }
  if (!(await confirmDiscardUnsavedChanges())) {
    if (target) {
      target.value = ''
    }
    return
  }
  try {
    const parsed = await parseDocsImportFile(file)
    editor.title = parsed.title
    editor.content = parsed.content
    syncConflictMessage.value = t('docs.messages.importApplied')
    ElMessage.success(t('docs.messages.importReady', { format: parsed.format }))
  } catch (error) {
    const message = error instanceof Error ? error.message : ''
    if (message === 'UNSUPPORTED_DOCS_IMPORT_FORMAT') {
      ElMessage.error(t('docs.messages.importUnsupported'))
    } else if (message === 'EMPTY_DOCS_IMPORT_FILE') {
      ElMessage.error(t('docs.messages.importEmpty'))
    } else {
      ElMessage.error(t('docs.messages.importFailed'))
    }
  } finally {
    if (target) {
      target.value = ''
    }
  }
}

async function onRefreshWorkspace(): Promise<void> {
  if (!activeNoteId.value) {
    return
  }
  if (!(await confirmDiscardUnsavedChanges())) {
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
    syncConflictMessage.value = t('docs.messages.syncRefreshRequired')
    await loadActiveNote()
    return
  }
  if (needsDocsDetailRefresh(latestSyncEvent.value.eventType)) {
    await loadActiveNote()
    return
  }
  if (external) {
    syncConflictMessage.value = t('docs.messages.externalActivity', {
      actor: latestSyncEvent.value.actorEmail || t('docs.sync.systemActor')
    })
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

function markUnavailableNoteState(): void {
  activeNoteId.value = ''
  noteDeleted.value = true
  draftRecoverySnapshot.value = null
  editor.title = ''
  editor.content = ''
  editor.updatedAt = ''
  editor.currentVersion = 1
  editor.permission = 'OWNER'
  editor.ownerDisplayName = ''
  editor.ownerEmail = ''
  editor.syncCursor = 0
  editor.syncVersion = 'DOC-0'
  persistedEditorState.title = ''
  persistedEditorState.content = ''
  persistedEditorState.currentVersion = 1
  collaborators.value = []
  comments.value = []
  suggestions.value = []
  activeSessions.value = []
  latestSyncEvent.value = null
  syncConflictMessage.value = t('docs.messages.noteUnavailableInline')
  selectedExcerpt.value = ''
  selectedRangeStart.value = 0
  selectedRangeEnd.value = 0
  reviewMode.value = 'EDIT'
}

function resetEditor(): void {
  activeNoteId.value = ''
  noteDeleted.value = false
  draftRecoverySnapshot.value = null
  editor.title = ''
  editor.content = ''
  editor.updatedAt = ''
  editor.currentVersion = 1
  editor.permission = 'OWNER'
  editor.ownerDisplayName = ''
  editor.ownerEmail = ''
  editor.syncCursor = 0
  editor.syncVersion = 'DOC-0'
  persistedEditorState.title = ''
  persistedEditorState.content = ''
  persistedEditorState.currentVersion = 1
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

function onRestoreLocalDraft(): void {
  if (!draftRecoverySnapshot.value) {
    return
  }
  editor.title = draftRecoverySnapshot.value.title
  editor.content = draftRecoverySnapshot.value.content
  syncConflictMessage.value = t('docs.messages.localDraftRestored')
  draftRecoverySnapshot.value = null
}

function onDiscardLocalDraft(): void {
  if (!activeNoteId.value) {
    return
  }
  clearDocsDraftSnapshot(activeNoteId.value)
  draftRecoverySnapshot.value = null
  ElMessage.success(t('docs.messages.localDraftDiscarded'))
}

async function recoverUnavailableNote(unavailableNoteId: string): Promise<void> {
  ElMessage.warning(t('docs.messages.noteUnavailable'))
  const fallbackVisibleNotes = filterDocsNoteSummaries(notes.value, scopeFilter.value)
    .filter((note) => note.id !== unavailableNoteId)
  const nextVisibleNoteId = resolveDocsVisibleNoteId(fallbackVisibleNotes, null)
  if (!nextVisibleNoteId) {
    await syncRoute(null)
    return
  }
  await selectNote(nextVisibleNoteId, { confirmUnsaved: false })
}

async function onSelectNote(noteId: string): Promise<void> {
  const result = await selectNote(noteId)
  if (result.selected || result.cancelled || !noteDeleted.value) {
    return
  }
  await recoverUnavailableNote(noteId)
}

function onWindowBeforeUnload(event: BeforeUnloadEvent): void {
  if (!hasUnsavedChanges.value) {
    return
  }
  event.preventDefault()
  event.returnValue = ''
}

function resolveRouteState(): DocsRouteState {
  return extractDocsRouteState(route.query)
}

function resolveRouteNoteId(): string | null {
  return resolveRouteState().noteId
}

async function syncRoute(noteId: string | null): Promise<void> {
  const currentRouteState = resolveRouteState()
  const nextRouteState: DocsRouteState = {
    noteId,
    keyword: keyword.value.trim(),
    scope: scopeFilter.value
  }
  if (
    currentRouteState.noteId === nextRouteState.noteId
    && currentRouteState.keyword === nextRouteState.keyword
    && currentRouteState.scope === nextRouteState.scope
  ) {
    return
  }
  await router.replace({
    path: '/docs',
    query: buildDocsRouteQuery(route.query, nextRouteState)
  })
}

async function onScopeFilterChange(): Promise<void> {
  const previousState = captureAppliedDocsViewState()
  const nextVisibleNoteId = resolveDocsVisibleNoteId(filteredNotes.value, activeNoteId.value)
  if (!nextVisibleNoteId) {
    const result = await clearDocsSelection(notes.value)
    if (result.cancelled) {
      await restoreDocsViewState(previousState)
    }
    return
  }
  if (nextVisibleNoteId === activeNoteId.value) {
    await syncRoute(activeNoteId.value)
    return
  }
  const result = await selectNote(nextVisibleNoteId)
  if (result.cancelled) {
    await restoreDocsViewState(previousState)
  }
}

async function onApplyFilters(): Promise<void> {
  const previousState = captureAppliedDocsViewState()
  const result = await loadNotes(false)
  if (result.cancelled) {
    await restoreDocsViewState(previousState)
  }
}

watch(() => authStore.accessToken, (token) => {
  currentSessionId.value = resolveSessionIdFromAccessToken(token || '')
}, { immediate: true })

watch(() => [activeNoteId.value, editor.title, editor.content], () => {
  if (loadingDetail.value) {
    return
  }
  persistLocalDraftSnapshot()
}, { flush: 'post' })

watch(() => [route.query.noteId, route.query.keyword, route.query.scope], async () => {
  const previousState: DocsRouteState = {
    noteId: activeNoteId.value || null,
    keyword: keyword.value.trim(),
    scope: scopeFilter.value
  }
  const routeState = resolveRouteState()
  if (routeState.keyword !== keyword.value || routeState.scope !== scopeFilter.value) {
    keyword.value = routeState.keyword
    scopeFilter.value = routeState.scope
    const result = await loadNotes(false)
    if (result.cancelled) {
      await restoreDocsViewState(previousState)
    }
    return
  }
  if (!routeState.noteId || routeState.noteId === activeNoteId.value) {
    return
  }
  const result = await selectNote(routeState.noteId, { syncRouteAfterLoad: false })
  if (result.cancelled) {
    await syncRoute(activeNoteId.value || null)
    return
  }
  if (!result.selected) {
    ElMessage.warning(t('docs.messages.noteUnavailable'))
    await loadNotes(false)
  }
})

onMounted(() => {
  if (typeof window !== 'undefined') {
    window.addEventListener('beforeunload', onWindowBeforeUnload)
  }
  void loadNotes(false)
})

onBeforeRouteLeave(async () => {
  if (await confirmDiscardUnsavedChanges()) {
    return true
  }
  return false
})

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('beforeunload', onWindowBeforeUnload)
  }
  stopPresenceTicker()
  disconnectSync()
})
</script>

<template>
  <section class="docs-workbench">
    <input
      ref="importInputRef"
      type="file"
      class="hidden-file-input"
      accept=".md,.markdown,.txt,text/plain,text/markdown"
      @change="onImportFileChange"
    >
    <header class="docs-topbar mm-card">
      <div class="docs-search-row">
        <el-input v-model="keyword" :placeholder="t('docs.search.placeholder')" @keyup.enter="onApplyFilters" />
        <el-select v-model="scopeFilter" class="scope-select" @change="onScopeFilterChange">
          <el-option :label="t('docs.search.scopes.all')" value="ALL" />
          <el-option :label="t('docs.search.scopes.owned')" value="OWNED" />
          <el-option :label="t('docs.search.scopes.shared')" value="SHARED" />
        </el-select>
        <el-button :loading="loadingList" @click="onApplyFilters">{{ t('docs.search.action') }}</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">{{ t('docs.actions.newNote') }}</el-button>
        <el-button :loading="refreshing" @click="onRefreshWorkspace">{{ t('docs.actions.refreshWorkspace') }}</el-button>
      </div>
    </header>

    <div class="docs-grid">
      <aside class="docs-panel navigator-panel mm-card">
        <div class="panel-heading">
          <div>
            <div class="panel-title">{{ t('docs.navigator.title') }}</div>
            <div class="panel-subtitle">{{ t('docs.navigator.subtitle', { visible: filteredNotes.length, total: notes.length }) }}</div>
          </div>
        </div>
        <button
          v-for="note in filteredNotes"
          :key="note.id"
          type="button"
          class="note-tile"
          :class="{ active: note.id === activeNoteId }"
          @click="onSelectNote(note.id)"
        >
          <div class="note-tile-head">
            <span class="note-title">{{ note.title }}</span>
            <el-tag size="small" :type="resolveDocsScopeTagType(note.scope)">{{ t(resolveDocsScopeLabelKey(note.scope)) }}</el-tag>
          </div>
          <div class="note-meta-row">
            <span>{{ t(resolveDocsPermissionLabelKey(note.permission)) }}</span>
            <span>v{{ note.currentVersion }}</span>
          </div>
          <div class="note-meta-row muted">
            <span>{{ note.ownerDisplayName || note.ownerEmail }}</span>
            <span>{{ formatTime(note.updatedAt) }}</span>
          </div>
        </button>
        <el-empty v-if="!filteredNotes.length" :description="notesEmptyDescription" />
      </aside>

      <section class="docs-panel editor-panel mm-card">
        <template v-if="hasActiveNote">
          <div class="panel-heading editor-header">
            <div>
              <div class="panel-title">{{ editorTitle }}</div>
              <div class="panel-subtitle">
                {{ editor.ownerDisplayName || editor.ownerEmail }} · {{ sharedBadge }} · Updated {{ formatTime(editor.updatedAt) }}
              </div>
            </div>
            <div class="editor-badges">
              <el-tag :type="hasUnsavedChanges ? 'warning' : 'success'">{{ dirtyStateLabel }}</el-tag>
              <el-tag>{{ t(resolveDocsPermissionLabelKey(editor.permission)) }}</el-tag>
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
          <el-alert
            v-if="draftRecoverySnapshot"
            class="alert-block"
            type="info"
            :closable="false"
            :title="t('docs.draftRecovery.title', { value: formatTime(draftRecoverySnapshot.savedAt) })"
          >
            <template #default>
              <div class="draft-recovery-alert">
                <span>{{ t('docs.draftRecovery.description') }}</span>
                <div class="draft-recovery-actions">
                  <el-button size="small" type="primary" @click="onRestoreLocalDraft">{{ t('docs.draftRecovery.restore') }}</el-button>
                  <el-button size="small" @click="onDiscardLocalDraft">{{ t('docs.draftRecovery.discard') }}</el-button>
                </div>
              </div>
            </template>
          </el-alert>

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
              <el-button :disabled="!canEdit" @click="onImportTrigger">{{ t('docs.actions.import') }}</el-button>
              <el-button @click="onExport('MARKDOWN')">{{ t('docs.actions.exportMarkdown') }}</el-button>
              <el-button @click="onExport('TEXT')">{{ t('docs.actions.exportText') }}</el-button>
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

.hidden-file-input {
  display: none;
}

.draft-recovery-alert {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.draft-recovery-actions {
  display: flex;
  gap: 8px;
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
