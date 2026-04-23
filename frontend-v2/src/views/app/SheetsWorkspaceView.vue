<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, useLocaleText } from '@/locales'
import { listSheetsWorkbooks, type SheetsWorkbookSummary } from '@/service/api/sheets'
import { useCopilotPanel } from '@/shared/composables/useCopilotPanel'
import { useAuthStore } from '@/store/modules/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const { tr } = useLocaleText()
const copilotPanel = useCopilotPanel()
const copilotOpen = copilotPanel.open

const workbooks = ref<SheetsWorkbookSummary[]>([])
const workspaceLoading = ref(false)
const loadError = ref('')

let latestSheetsWorkspaceRequest = 0

const visibleWorkbooks = computed(() => {
  return workbooks.value
    .slice()
    .sort((left, right) => compareDateDesc(left.updatedAt, right.updatedAt) || left.title.localeCompare(right.title))
})

const statusCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可读取表格工作区。', '登入後即可讀取試算表工作區。', 'Sign in to load your sheets workspace.'))
  }

  if (loadError.value) {
    return loadError.value
  }

  if (workspaceLoading.value && !workbooks.value.length) {
    return tr(lt('正在加载工作簿列表。', '正在載入活頁簿清單。', 'Loading workbook list.'))
  }

  return `${visibleWorkbooks.value.length} ${tr(lt('个工作簿已载入', '個活頁簿已載入', 'workbooks loaded'))}`
})

const emptyCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可查看已认证工作簿。', '登入後即可查看已驗證活頁簿。', 'Sign in to view authenticated workbooks.'))
  }

  if (workspaceLoading.value) {
    return tr(lt('正在同步表格运行时数据。', '正在同步試算表執行期資料。', 'Syncing sheets runtime data.'))
  }

  return tr(lt('当前没有可显示的工作簿。', '目前沒有可顯示的活頁簿。', 'No workbooks are available right now.'))
})

onMounted(() => {
  void copilotPanel.loadCapabilities().catch(() => {})
})

function toggleCopilotPanel() {
  copilotPanel.toggle()
}

function openWorkbook(workbookId: string) {
  void router.push(`/sheets/${workbookId}`)
}

async function loadSheetsWorkspace() {
  const requestId = ++latestSheetsWorkspaceRequest
  const requestToken = authStore.accessToken
  const requestPath = route.fullPath

  if (!requestToken) {
    if (requestId !== latestSheetsWorkspaceRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
      return
    }

    workbooks.value = []
    loadError.value = ''
    workspaceLoading.value = false
    return
  }

  workspaceLoading.value = true
  loadError.value = ''

  try {
    const response = await listSheetsWorkbooks(requestToken)

    if (requestId !== latestSheetsWorkspaceRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
      return
    }

    workbooks.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    if (requestId !== latestSheetsWorkspaceRequest || requestToken !== authStore.accessToken || requestPath !== route.fullPath) {
      return
    }

    workbooks.value = []
    loadError.value = resolveErrorMessage(
      error,
      tr(lt('读取工作簿列表失败，请稍后重试。', '讀取活頁簿清單失敗，請稍後重試。', 'Failed to load workbooks. Please try again later.'))
    )
  } finally {
    if (requestId === latestSheetsWorkspaceRequest && requestToken === authStore.accessToken && requestPath === route.fullPath) {
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

function formatDateTime(value: string | null) {
  if (!value) {
    return tr(lt('未设置', '未設定', 'Not set'))
  }

  const parsed = new Date(value)

  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return parsed.toLocaleString()
}

function resolveWorkbookMeta(workbook: SheetsWorkbookSummary) {
  return `${workbook.rowCount} × ${workbook.colCount} · ${workbook.sheetCount} ${tr(lt('个工作表', '個工作表', 'sheets'))} · ${formatDateTime(workbook.updatedAt)}`
}

function resolveCollaboratorCopy(workbook: SheetsWorkbookSummary) {
  return `${workbook.collaboratorCount} ${tr(lt('位协作者', '位協作者', 'collaborators'))}`
}

function resolveVisibilityCopy(workbook: SheetsWorkbookSummary) {
  const scope = workbook.scope === 'SHARED'
    ? tr(lt('共享', '共享', 'Shared'))
    : tr(lt('个人', '個人', 'Personal'))
  const permission = workbook.permission === 'OWNER'
    ? tr(lt('所有者', '擁有者', 'Owner'))
    : workbook.permission === 'EDIT'
      ? tr(lt('可编辑', '可編輯', 'Editable'))
      : tr(lt('只读', '唯讀', 'Read only'))

  return `${scope} · ${permission}`
}

function resolveErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}

watch(() => [route.fullPath, authStore.accessToken], () => {
  void loadSheetsWorkspace()
}, { immediate: true })
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      :eyebrow="lt('表格', '試算表', 'Sheets')"
      :title="lt('面向轻量分析的表格界面', '面向輕量分析的試算表介面', 'Sheet surfaces for lightweight analysis')"
      :description="lt('以网格为中心的规划与报表能力，采用克制的 Beta 呈现。', '以網格為中心的規劃與報表能力，採用克制的 Beta 呈現。', 'Grid-oriented planning and reporting in a restrained beta presentation.')"
      :badge="lt('Beta', 'Beta', 'Beta')"
      badge-tone="beta"
    >
      <div class="sheets-actions">
        <button class="page-action" type="button">{{ tr(lt('新建表格', '新增試算表', 'New sheet')) }}</button>
        <button class="page-action page-action--secondary" type="button" @click="toggleCopilotPanel()">
          {{ copilotOpen ? tr(lt('Copilot 已打开', 'Copilot 已開啟', 'Copilot open')) : tr(lt('切换 Copilot', '切換 Copilot', 'Toggle Copilot')) }}
        </button>
      </div>
    </compact-page-header>

    <p class="page-subtitle sheets-status">{{ statusCopy }}</p>

    <article class="surface-card sheets-table">
      <header class="sheets-table__head">
        <span>{{ tr(lt('名称', '名稱', 'Name')) }}</span>
        <span>{{ tr(lt('协作者', '協作者', 'Collaborators')) }}</span>
        <span>{{ tr(lt('可见性', '可見性', 'Visibility')) }}</span>
      </header>
      <button
        v-for="sheet in visibleWorkbooks"
        :key="sheet.id"
        class="sheets-table__row"
        type="button"
        @click="openWorkbook(sheet.id)"
      >
        <div>
          <strong>{{ sheet.title || tr(lt('未命名工作簿', '未命名活頁簿', 'Untitled workbook')) }}</strong>
          <p>{{ resolveWorkbookMeta(sheet) }}</p>
        </div>
        <span>{{ resolveCollaboratorCopy(sheet) }}</span>
        <span>{{ resolveVisibilityCopy(sheet) }}</span>
      </button>
      <p v-if="!visibleWorkbooks.length" class="sheets-table__empty">{{ emptyCopy }}</p>
    </article>
  </section>
</template>

<style scoped>
.sheets-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.page-action {
  min-height: 34px;
  padding: 0 14px;
  border: 0;
  border-radius: 10px;
  background: var(--mm-sheets);
  color: #fff;
}

.page-action--secondary {
  border: 1px solid var(--mm-border);
  background: var(--mm-card);
  color: var(--mm-text);
}

.sheets-status {
  margin: 0;
}

.sheets-table {
  overflow: hidden;
}

.sheets-table__head,
.sheets-table__row {
  display: grid;
  grid-template-columns: 1.4fr 0.9fr 0.7fr;
  gap: 16px;
  width: 100%;
  padding: 16px 18px;
}

.sheets-table__head {
  border-bottom: 1px solid var(--mm-border);
  color: var(--mm-text-secondary);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.sheets-table__row {
  border: 0;
  border-bottom: 1px solid var(--mm-border);
  background: transparent;
  font: inherit;
  font-size: 13px;
  text-align: left;
}

.sheets-table__row strong {
  display: block;
}

.sheets-table__row p,
.sheets-table__row span,
.sheets-table__empty {
  color: var(--mm-text-secondary);
}

.sheets-table__row p {
  margin: 4px 0 0;
  font-size: 12px;
}

.sheets-table__empty {
  margin: 0;
  padding: 18px;
  font-size: 13px;
  line-height: 1.6;
}

@media (max-width: 720px) {
  .sheets-table__head,
  .sheets-table__row {
    grid-template-columns: 1fr;
  }
}
</style>
