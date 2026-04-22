<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, useLocaleText } from '@/locales'
import { listDocsNotes, type DocsNoteSummary } from '@/service/api/docs'
import { useCopilotPanel } from '@/shared/composables/useCopilotPanel'
import { useAuthStore } from '@/store/modules/auth'

type DocsWorkspaceFilter = 'recent' | 'owned' | 'shared'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { tr } = useLocaleText()
const copilotPanel = useCopilotPanel()
const copilotOpen = copilotPanel.open

const notes = ref<DocsNoteSummary[]>([])
const workspaceLoading = ref(false)
const loadError = ref('')
const activeFilter = ref<DocsWorkspaceFilter>('recent')

let latestDocsWorkspaceRequest = 0

const sortedNotes = computed(() => {
  return notes.value
    .slice()
    .sort((left, right) => compareDateDesc(left.updatedAt, right.updatedAt) || left.title.localeCompare(right.title))
})

const visibleNotes = computed(() => {
  if (activeFilter.value === 'owned') {
    return sortedNotes.value.filter(note => note.scope !== 'SHARED')
  }

  if (activeFilter.value === 'shared') {
    return sortedNotes.value.filter(note => note.scope === 'SHARED')
  }

  return sortedNotes.value
})

const statusCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可读取文档工作区。', '登入後即可讀取文件工作區。', 'Sign in to load your docs workspace.'))
  }

  if (loadError.value) {
    return loadError.value
  }

  if (workspaceLoading.value && !notes.value.length) {
    return tr(lt('正在加载文档列表。', '正在載入文件清單。', 'Loading document list.'))
  }

  return `${visibleNotes.value.length} ${tr(lt('份文档已载入', '份文件已載入', 'docs loaded'))}`
})

const listEmptyCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可查看最近、个人和共享文档。', '登入後即可查看最近、個人與共享文件。', 'Sign in to view recent, personal, and shared docs.'))
  }

  if (workspaceLoading.value) {
    return tr(lt('正在同步文档工作区。', '正在同步文件工作區。', 'Syncing docs workspace.'))
  }

  if (activeFilter.value === 'owned') {
    return tr(lt('当前没有个人文档。', '目前沒有個人文件。', 'No personal docs are available.'))
  }

  if (activeFilter.value === 'shared') {
    return tr(lt('当前没有共享文档。', '目前沒有共享文件。', 'No shared docs are available.'))
  }

  return tr(lt('当前没有可显示的文档。', '目前沒有可顯示的文件。', 'No docs are available right now.'))
})

onMounted(() => {
  void copilotPanel.loadCapabilities().catch(() => {})
})

function toggleCopilotPanel() {
  copilotPanel.toggle()
}

function setActiveFilter(filter: DocsWorkspaceFilter) {
  activeFilter.value = filter
}

function openDocsNote(noteId: string) {
  void router.push(`/docs/${noteId}`)
}

async function loadDocsWorkspace() {
  const requestId = ++latestDocsWorkspaceRequest
  const requestToken = authStore.accessToken
  const requestPath = route.fullPath

  if (!requestToken) {
    if (requestId !== latestDocsWorkspaceRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
      return
    }

    notes.value = []
    loadError.value = ''
    workspaceLoading.value = false
    return
  }

  workspaceLoading.value = true
  loadError.value = ''

  try {
    const response = await listDocsNotes(requestToken)

    if (requestId !== latestDocsWorkspaceRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
      return
    }

    notes.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    if (requestId !== latestDocsWorkspaceRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
      return
    }

    notes.value = []
    loadError.value = resolveErrorMessage(
      error,
      tr(lt('读取文档列表失败，请稍后重试。', '讀取文件清單失敗，請稍後重試。', 'Failed to load docs. Please try again later.'))
    )
  } finally {
    if (requestId === latestDocsWorkspaceRequest && requestToken === authStore.accessToken && requestPath === route.fullPath) {
      workspaceLoading.value = false
    }
  }
}

function compareDateDesc(left: string, right: string) {
  return parseDateValue(right) - parseDateValue(left)
}

function parseDateValue(value: string) {
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? 0 : parsed.getTime()
}

function formatDateTime(value: string) {
  const parsed = new Date(value)

  if (Number.isNaN(parsed.getTime())) {
    return value || tr(lt('未知时间', '未知時間', 'Unknown time'))
  }

  return parsed.toLocaleString()
}

function resolveNoteMeta(note: DocsNoteSummary) {
  return `${formatDateTime(note.updatedAt)} · ${note.ownerDisplayName || note.ownerEmail}`
}

function resolveNoteState(note: DocsNoteSummary) {
  const scope = note.scope === 'SHARED'
    ? tr(lt('共享文档', '共享文件', 'Shared doc'))
    : tr(lt('我的文档', '我的文件', 'My doc'))
  const permission = note.permission === 'OWNER'
    ? tr(lt('所有者', '擁有者', 'Owner'))
    : note.permission === 'EDIT'
      ? tr(lt('可编辑', '可編輯', 'Editable'))
      : tr(lt('只读', '唯讀', 'Read only'))

  return `${scope} · ${permission} · ${note.collaboratorCount} ${tr(lt('位协作者', '位協作者', 'collaborators'))}`
}

function resolveErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}

watch(() => [route.fullPath, authStore.accessToken], () => {
  void loadDocsWorkspace()
}, { immediate: true })
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      :eyebrow="lt('文档', '文件', 'Docs')"
      :title="lt('轻量文档工作区', '輕量文件工作區', 'Lightweight document workspace')"
      :description="lt('面向最近、共享和个人写作的 Beta 界面，但不承诺完整套件对等能力。', '面向最近、共享與個人寫作的 Beta 介面，但不承諾完整套件對等能力。', 'Beta surfaces for recent, shared, and personal writing without promising full-suite parity.')"
      :badge="lt('Beta', 'Beta', 'Beta')"
      badge-tone="beta"
    >
      <div class="docs-actions">
        <button class="page-action" type="button">{{ tr(lt('新建文档', '新增文件', 'New document')) }}</button>
        <button class="page-action page-action--secondary" type="button" @click="toggleCopilotPanel()">
          {{ copilotOpen ? tr(lt('Copilot 已打开', 'Copilot 已開啟', 'Copilot open')) : tr(lt('切换 Copilot', '切換 Copilot', 'Toggle Copilot')) }}
        </button>
      </div>
    </compact-page-header>

    <p class="page-subtitle docs-shell__status">{{ statusCopy }}</p>

    <article class="surface-card docs-shell">
      <aside class="docs-shell__nav">
        <button
          type="button"
          :class="{ 'docs-shell__nav--active': activeFilter === 'recent' }"
          @click="setActiveFilter('recent')"
        >
          {{ tr(lt('最近', '最近', 'Recent')) }}
        </button>
        <button
          type="button"
          :class="{ 'docs-shell__nav--active': activeFilter === 'owned' }"
          @click="setActiveFilter('owned')"
        >
          {{ tr(lt('我的文档', '我的文件', 'My docs')) }}
        </button>
        <button
          type="button"
          :class="{ 'docs-shell__nav--active': activeFilter === 'shared' }"
          @click="setActiveFilter('shared')"
        >
          {{ tr(lt('共享', '共享', 'Shared')) }}
        </button>
      </aside>
      <div class="docs-shell__list">
        <button
          v-for="doc in visibleNotes"
          :key="doc.id"
          class="docs-row"
          type="button"
          @click="openDocsNote(doc.id)"
        >
          <div>
            <strong>{{ doc.title || tr(lt('未命名文档', '未命名文件', 'Untitled doc')) }}</strong>
            <p>{{ resolveNoteMeta(doc) }}</p>
          </div>
          <span>{{ resolveNoteState(doc) }}</span>
        </button>
        <p v-if="!visibleNotes.length" class="docs-shell__empty">{{ listEmptyCopy }}</p>
      </div>
    </article>
  </section>
</template>

<style scoped>
.docs-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.page-action {
  min-height: 34px;
  padding: 0 14px;
  border: 0;
  border-radius: 10px;
  background: var(--mm-docs);
  color: #fff;
}

.page-action--secondary {
  border: 1px solid var(--mm-border);
  background: var(--mm-card);
  color: var(--mm-text);
}

.docs-shell__status {
  margin: 0;
}

.docs-shell {
  display: grid;
  grid-template-columns: 220px 1fr;
  overflow: hidden;
}

.docs-shell__nav {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-right: 1px solid var(--mm-border);
  background: color-mix(in srgb, var(--mm-docs) 6%, white);
}

.docs-shell__nav button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: var(--mm-text-secondary);
  text-align: left;
}

.docs-shell__nav--active {
  border-color: color-mix(in srgb, var(--mm-docs) 24%, white) !important;
  background: #fff !important;
  color: var(--mm-ink) !important;
}

.docs-shell__list {
  display: grid;
  gap: 0;
  padding: 10px 18px;
}

.docs-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  padding: 16px 0;
  border: 0;
  border-bottom: 1px solid var(--mm-border);
  background: transparent;
  font: inherit;
  text-align: left;
}

.docs-row strong {
  display: block;
}

.docs-row p,
.docs-row span,
.docs-shell__empty {
  margin: 4px 0 0;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.docs-shell__empty {
  padding: 16px 0 8px;
  line-height: 1.6;
}

@media (max-width: 900px) {
  .docs-shell {
    grid-template-columns: 1fr;
  }

  .docs-shell__nav {
    border-right: 0;
    border-bottom: 1px solid var(--mm-border);
  }
}
</style>
