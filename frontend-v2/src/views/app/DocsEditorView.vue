<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { lt, useLocaleText } from '@/locales'
import { readDocsNote, updateDocsNote, type DocsNoteDetail } from '@/service/api/docs'
import { useCopilotPanel } from '@/shared/composables/useCopilotPanel'
import { useAuthStore } from '@/store/modules/auth'
import {
  canSubmitRouteEntitySave,
  createRouteEntityNavigationReset,
  hasRouteEntityChanged,
  isCurrentRouteEntity,
  isRouteEntityEditingLocked
} from './route-bound-editor-state'

interface OutlineItem {
  key: string
  label: string
}

interface DocsFact {
  label: string
  value: string
}

const route = useRoute()
const authStore = useAuthStore()
const { tr } = useLocaleText()
const copilotPanel = useCopilotPanel()
const copilotOpen = copilotPanel.open

const note = ref<DocsNoteDetail | null>(null)
const draftTitle = ref('')
const draftContent = ref('')
const noteLoading = ref(false)
const saveLoading = ref(false)
const loadError = ref('')
const saveError = ref('')

let latestDocsNoteRequest = 0
let latestDocsSaveRequest = 0

const noteId = computed(() => String(route.params.id || ''))
const loadedNoteMatchesRoute = computed(() => {
  return isCurrentRouteEntity(noteId.value, note.value?.id)
})
const editingLocked = computed(() => {
  return isRouteEntityEditingLocked(noteId.value, note.value?.id, noteLoading.value)
})

const title = computed(() => {
  return draftTitle.value.trim()
    || note.value?.title
    || noteId.value.replace(/-/g, ' ')
    || tr(lt('未命名文档', '未命名文件', 'Untitled doc'))
})

const canEdit = computed(() => {
  return loadedNoteMatchesRoute.value && note.value ? note.value.permission !== 'VIEW' : false
})

const hasChanges = computed(() => {
  if (!note.value) {
    return false
  }

  return draftTitle.value !== note.value.title || draftContent.value !== note.value.content
})

const saveDisabled = computed(() => {
  return !authStore.accessToken || !note.value || !canSubmitRouteEntitySave(
    noteId.value,
    note.value.id,
    noteLoading.value,
    saveLoading.value,
    canEdit.value,
    hasChanges.value
  )
})

const editorStatus = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可读取文档详情。', '登入後即可讀取文件詳情。', 'Sign in to load document detail.'))
  }

  if (loadError.value) {
    return loadError.value
  }

  if (saveError.value) {
    return saveError.value
  }

  if (noteLoading.value) {
    return tr(lt('正在加载文档内容。', '正在載入文件內容。', 'Loading document content.'))
  }

  if (!note.value) {
    return tr(lt('当前文档不可用。', '目前文件無法使用。', 'This document is unavailable.'))
  }

  if (!canEdit.value) {
    return tr(lt('该文档当前为只读模式。', '該文件目前為唯讀模式。', 'This document is currently read only.'))
  }

  if (hasChanges.value) {
    return tr(lt('存在未保存的更改。', '有尚未儲存的變更。', 'You have unsaved changes.'))
  }

  return tr(lt('文档已从运行时接口载入。', '文件已從執行期介面載入。', 'Document loaded from runtime APIs.'))
})

const outlineItems = computed<OutlineItem[]>(() => {
  return deriveOutlineItems(draftContent.value)
})

const detailFacts = computed<DocsFact[]>(() => {
  if (!note.value) {
    return []
  }

  return [
    {
      label: tr(lt('权限', '權限', 'Permission')),
      value: resolvePermission(note.value.permission)
    },
    {
      label: tr(lt('协作者', '協作者', 'Collaborators')),
      value: `${note.value.collaboratorCount}`
    },
    {
      label: tr(lt('版本', '版本', 'Version')),
      value: `v${note.value.currentVersion}`
    },
    {
      label: tr(lt('同步光标', '同步游標', 'Sync cursor')),
      value: `${note.value.syncCursor}`
    },
    {
      label: tr(lt('同步版本', '同步版本', 'Sync version')),
      value: note.value.syncVersion
    },
    {
      label: tr(lt('更新于', '更新於', 'Updated')),
      value: formatDateTime(note.value.updatedAt)
    }
  ]
})

const collaborationHeading = computed(() => {
  if (!note.value) {
    return tr(lt('暂无运行时详情', '暫無執行期詳情', 'No runtime detail yet'))
  }

  return `${note.value.ownerDisplayName || note.value.ownerEmail} · ${note.value.shared ? tr(lt('共享', '共享', 'Shared')) : tr(lt('私有', '私人', 'Private'))}`
})

const shareLabel = computed(() => {
  return note.value?.shared
    ? tr(lt('已共享', '已共享', 'Shared'))
    : tr(lt('私有', '私人', 'Private'))
})

onMounted(() => {
  void copilotPanel.loadCapabilities().catch(() => {})
})

function toggleCopilotPanel() {
  copilotPanel.toggle()
}

function syncDrafts(detail: DocsNoteDetail | null) {
  draftTitle.value = detail?.title || ''
  draftContent.value = detail?.content || ''
}

function clearEditorState(nextRouteNoteId = noteId.value, nextToken = authStore.accessToken) {
  note.value = null
  syncDrafts(null)
  loadError.value = ''
  saveError.value = ''

  const resetState = createRouteEntityNavigationReset(nextRouteNoteId, nextToken)
  noteLoading.value = resetState.entityLoading
  saveLoading.value = resetState.saveLoading
}

async function loadNote() {
  const requestId = ++latestDocsNoteRequest
  const requestToken = authStore.accessToken
  const requestPath = route.fullPath
  const requestNoteId = String(route.params.id || '')

  if (!requestToken || !requestNoteId) {
    if (requestId !== latestDocsNoteRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
      return
    }

    clearEditorState(requestNoteId, requestToken)
    return
  }

  noteLoading.value = true
  loadError.value = ''
  saveError.value = ''

  try {
    const response = await readDocsNote(String(route.params.id || ''), requestToken)

    if (requestId !== latestDocsNoteRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath || requestNoteId !== String(route.params.id || '')) {
      return
    }

    note.value = response.data || null
    syncDrafts(note.value)
  } catch (error) {
    if (requestId !== latestDocsNoteRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath || requestNoteId !== String(route.params.id || '')) {
      return
    }

    note.value = null
    syncDrafts(null)
    loadError.value = resolveErrorMessage(
      error,
      tr(lt('读取文档详情失败，请稍后重试。', '讀取文件詳情失敗，請稍後重試。', 'Failed to load the document. Please try again later.'))
    )
  } finally {
    if (requestId === latestDocsNoteRequest && requestToken === authStore.accessToken && requestPath === route.fullPath && requestNoteId === String(route.params.id || '')) {
      noteLoading.value = false
    }
  }
}

async function saveNote() {
  const requestToken = authStore.accessToken
  const requestPath = route.fullPath
  const requestNoteId = noteId.value
  const currentNote = note.value

  if (!requestToken || !requestNoteId || !currentNote || !canSubmitRouteEntitySave(
    requestNoteId,
    currentNote.id,
    noteLoading.value,
    saveLoading.value,
    canEdit.value,
    hasChanges.value
  )) {
    return
  }

  const requestId = ++latestDocsSaveRequest

  saveLoading.value = true
  saveError.value = ''

  try {
    const response = await updateDocsNote(requestNoteId, {
      title: draftTitle.value.trim() || currentNote.title,
      content: draftContent.value,
      currentVersion: currentNote.currentVersion
    }, requestToken)

    if (requestId !== latestDocsSaveRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath || requestNoteId !== String(route.params.id || '')) {
      return
    }

    note.value = response.data || null
    syncDrafts(note.value)
  } catch (error) {
    if (requestId !== latestDocsSaveRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath || requestNoteId !== String(route.params.id || '')) {
      return
    }

    saveError.value = resolveErrorMessage(
      error,
      tr(lt('保存文档失败，请稍后重试。', '儲存文件失敗，請稍後重試。', 'Failed to save the document. Please try again later.'))
    )
  } finally {
    if (requestId === latestDocsSaveRequest && requestToken === authStore.accessToken && requestPath === route.fullPath && requestNoteId === String(route.params.id || '')) {
      saveLoading.value = false
    }
  }
}

function deriveOutlineItems(content: string) {
  return content
    .split(/\n+/)
    .map((line, index) => {
      const trimmed = line.trim()
      const headingMatch = trimmed.match(/^#{1,6}\s+(.*)$/)
      const bulletMatch = trimmed.match(/^[-*]\s+(.*)$/)
      const label = headingMatch?.[1] || bulletMatch?.[1] || trimmed

      return {
        key: `${index}-${label}`,
        label
      }
    })
    .filter(item => Boolean(item.label))
    .slice(0, 6)
}

function resolvePermission(permission: DocsNoteDetail['permission']) {
  if (permission === 'OWNER') {
    return tr(lt('所有者', '擁有者', 'Owner'))
  }

  if (permission === 'EDIT') {
    return tr(lt('可编辑', '可編輯', 'Editable'))
  }

  return tr(lt('只读', '唯讀', 'Read only'))
}

function formatDateTime(value: string) {
  const parsed = new Date(value)

  if (Number.isNaN(parsed.getTime())) {
    return value || tr(lt('未知时间', '未知時間', 'Unknown time'))
  }

  return parsed.toLocaleString()
}

function resolveErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}

watch(() => [noteId.value, route.fullPath, authStore.accessToken], (nextValue, previousValue) => {
  if (!previousValue || hasRouteEntityChanged(previousValue[0], nextValue[0])) {
    clearEditorState(nextValue[0], nextValue[2])
  }

  void loadNote()
}, { immediate: true })
</script>

<template>
  <section class="page-shell surface-grid docs-editor">
    <article class="surface-card docs-editor__top">
      <div>
        <span class="section-label">{{ tr(lt('文档编辑器', '文件編輯器', 'Docs editor')) }}</span>
        <h1>{{ title }}</h1>
        <p class="page-subtitle">{{ tr(lt('块级编辑、提纲导航、协作者上下文和 Beta 指引都保持可见，但不夸大能力对等。', '區塊編輯、大綱導覽、協作者內容脈絡與 Beta 指引都保持可見，但不誇大能力對等。', 'Block editing, outline navigation, collaborator context, and beta guidance remain visible without overstating parity.')) }}</p>
        <p class="page-subtitle docs-editor__status">{{ editorStatus }}</p>
      </div>
      <div class="docs-editor__actions">
        <button type="button">{{ shareLabel }}</button>
        <button type="button">{{ note ? `v${note.currentVersion}` : 'v0' }}</button>
        <button type="button" :disabled="saveDisabled" @click="saveNote()">
          {{ saveLoading ? tr(lt('保存中', '儲存中', 'Saving')) : tr(lt('保存', '儲存', 'Save')) }}
        </button>
        <button type="button" @click="toggleCopilotPanel()">
          {{ copilotOpen ? tr(lt('Copilot 已打开', 'Copilot 已開啟', 'Copilot open')) : tr(lt('切换 Copilot', '切換 Copilot', 'Toggle Copilot')) }}
        </button>
      </div>
    </article>

    <section class="docs-editor__layout">
      <aside class="surface-card docs-editor__panel">
        <span class="section-label">{{ tr(lt('提纲', '大綱', 'Outline')) }}</span>
        <button v-for="item in outlineItems" :key="item.key" type="button">{{ item.label }}</button>
        <p v-if="!outlineItems.length" class="page-subtitle docs-editor__empty">{{ tr(lt('输入内容后，提纲会根据运行时文档生成。', '輸入內容後，大綱會依據執行期文件產生。', 'The outline is generated from the runtime document content.')) }}</p>
      </aside>
      <article class="surface-card docs-editor__canvas">
        <label class="docs-editor__field">
          <span class="section-label">{{ tr(lt('标题', '標題', 'Title')) }}</span>
          <input
            v-model="draftTitle"
            class="docs-editor__title-input"
            type="text"
            :readonly="editingLocked || !canEdit"
            :placeholder="tr(lt('输入文档标题', '輸入文件標題', 'Enter document title'))"
          >
        </label>
        <label class="docs-editor__field docs-editor__field--content">
          <span class="section-label">{{ tr(lt('正文', '內容', 'Content')) }}</span>
          <textarea
            v-model="draftContent"
            class="docs-editor__textarea"
            :readonly="editingLocked || !canEdit"
            :placeholder="tr(lt('输入文档内容', '輸入文件內容', 'Enter document content'))"
          />
        </label>
      </article>
      <aside class="surface-card docs-editor__panel">
        <span class="section-label">{{ tr(lt('运行时详情', '執行期詳情', 'Runtime detail')) }}</span>
        <strong>{{ collaborationHeading }}</strong>
        <p class="page-subtitle">{{ note ? `${note.ownerEmail} · ${formatDateTime(note.updatedAt)}` : tr(lt('认证后会在此显示文档状态。', '驗證後會在此顯示文件狀態。', 'Document state appears here after authentication.')) }}</p>
        <div v-if="detailFacts.length" class="docs-editor__facts">
          <div v-for="fact in detailFacts" :key="fact.label" class="docs-editor__fact">
            <span class="section-label">{{ fact.label }}</span>
            <strong>{{ fact.value }}</strong>
          </div>
        </div>
      </aside>
    </section>
  </section>
</template>

<style scoped>
.docs-editor__top,
.docs-editor__layout {
  display: grid;
  gap: 16px;
}

.docs-editor__top {
  grid-template-columns: minmax(0, 1fr) auto;
  padding: 18px;
}

.docs-editor__top h1 {
  margin: 8px 0 0;
  font-size: 24px;
  letter-spacing: -0.04em;
  text-transform: capitalize;
}

.docs-editor__status {
  margin-top: 10px;
}

.docs-editor__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.docs-editor__actions button,
.docs-editor__panel button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  background: var(--mm-card);
}

.docs-editor__actions button:disabled {
  opacity: 0.6;
}

.docs-editor__layout {
  grid-template-columns: 220px minmax(0, 1fr) 280px;
}

.docs-editor__panel,
.docs-editor__canvas {
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 18px;
}

.docs-editor__empty {
  margin: 0;
  line-height: 1.6;
}

.docs-editor__field {
  display: grid;
  gap: 10px;
}

.docs-editor__field--content {
  min-height: 0;
}

.docs-editor__title-input,
.docs-editor__textarea {
  width: 100%;
  border: 1px solid var(--mm-border);
  border-radius: 14px;
  background: var(--mm-card-muted);
  color: var(--mm-text);
}

.docs-editor__title-input {
  min-height: 48px;
  padding: 0 14px;
  font-size: 16px;
  font-weight: 600;
}

.docs-editor__textarea {
  min-height: 460px;
  padding: 14px;
  resize: vertical;
  line-height: 1.7;
}

.docs-editor__facts {
  display: grid;
  gap: 12px;
  margin-top: 8px;
}

.docs-editor__fact {
  display: grid;
  gap: 6px;
  padding-top: 12px;
  border-top: 1px solid var(--mm-border);
}

@media (max-width: 980px) {
  .docs-editor__layout,
  .docs-editor__top {
    grid-template-columns: 1fr;
  }
}
</style>
