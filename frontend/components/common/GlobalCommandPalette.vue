<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { SuiteCommandCenter, SuiteCommandFeed, SuiteRemediationAction } from '~/types/api'
import { useCommandPalette } from '~/composables/useCommandPalette'
import { useSuiteApi } from '~/composables/useSuiteApi'
import { useOrgAccessStore } from '~/stores/org-access'
import {
  filterSuiteCommandCenterByAccess,
  filterSuiteCommandFeedByAccess
} from '~/utils/org-product-surface-filter'

type PaletteCommandKind = 'ROUTE' | 'SEARCH' | 'ACTION' | 'HISTORY'

interface PaletteCommand {
  key: string
  kind: PaletteCommandKind
  badge: string
  label: string
  description: string
  routePath: string
  actionCode: string | null
}

const COMMAND_MAX_ITEMS = 24
const HISTORY_ITEMS_IN_PALETTE = 8

const keyword = ref('')
const loading = ref(false)
const executing = ref(false)
const selectedIndex = ref(0)
const commandCenter = ref<SuiteCommandCenter | null>(null)
const commandFeed = ref<SuiteCommandFeed | null>(null)
const searchInputRef = ref<{ focus: () => void } | null>(null)

const { isOpen, openPalette, closePalette } = useCommandPalette()
const { getCommandCenter, getCommandFeed, executeRemediationAction } = useSuiteApi()
const orgAccessStore = useOrgAccessStore()

const visibleCommandCenter = computed(() => filterSuiteCommandCenterByAccess(
  commandCenter.value,
  orgAccessStore.isProductEnabled
))
const visibleCommandFeed = computed(() => filterSuiteCommandFeedByAccess(
  commandFeed.value,
  orgAccessStore.isProductEnabled
))

const dialogModel = computed({
  get: () => isOpen.value,
  set: (value: boolean) => {
    if (value) {
      openPalette()
      return
    }
    closePalette()
  }
})

const filteredCommands = computed(() => {
  const allCommands = buildPaletteCommands(visibleCommandCenter.value, visibleCommandFeed.value)
  if (!keyword.value.trim()) {
    return allCommands.slice(0, COMMAND_MAX_ITEMS)
  }
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return allCommands
    .filter((item) => {
      const haystack = `${item.label} ${item.description} ${item.badge}`.toLowerCase()
      return haystack.includes(normalizedKeyword)
    })
    .slice(0, COMMAND_MAX_ITEMS)
})

const selectedCommand = computed(() => filteredCommands.value[selectedIndex.value] ?? null)

watch(isOpen, async (open) => {
  if (!open) {
    keyword.value = ''
    selectedIndex.value = 0
    return
  }
  await loadPaletteData()
  selectedIndex.value = 0
  await nextTick()
  searchInputRef.value?.focus()
})

watch(filteredCommands, (list) => {
  if (list.length === 0) {
    selectedIndex.value = 0
    return
  }
  if (selectedIndex.value >= list.length) {
    selectedIndex.value = list.length - 1
  }
})

watch(() => orgAccessStore.activeOrgId, () => {
  if (!isOpen.value) {
    return
  }
  void loadPaletteData()
})

async function loadPaletteData(): Promise<void> {
  loading.value = true
  try {
    const [centerData, feedData] = await Promise.all([
      getCommandCenter(),
      getCommandFeed(20)
    ])
    commandCenter.value = centerData
    commandFeed.value = feedData
  } finally {
    loading.value = false
  }
}

function onPanelKeydown(event: KeyboardEvent): void {
  if (!isOpen.value) {
    return
  }
  if (event.key === 'Escape') {
    event.preventDefault()
    closePalette()
    return
  }
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    moveSelection(1)
    return
  }
  if (event.key === 'ArrowUp') {
    event.preventDefault()
    moveSelection(-1)
    return
  }
  if (event.key === 'Enter' && selectedCommand.value) {
    event.preventDefault()
    void runCommand(selectedCommand.value)
  }
}

function moveSelection(step: number): void {
  if (filteredCommands.value.length === 0) {
    selectedIndex.value = 0
    return
  }
  const nextIndex = selectedIndex.value + step
  if (nextIndex < 0) {
    selectedIndex.value = filteredCommands.value.length - 1
    return
  }
  if (nextIndex >= filteredCommands.value.length) {
    selectedIndex.value = 0
    return
  }
  selectedIndex.value = nextIndex
}

function selectCommand(index: number): void {
  selectedIndex.value = index
}

async function runCommand(command: PaletteCommand): Promise<void> {
  if (command.kind === 'ACTION' && command.actionCode) {
    executing.value = true
    try {
      const result = await executeRemediationAction(command.actionCode)
      ElMessage.success(result.message)
      await loadPaletteData()
    } finally {
      executing.value = false
    }
    return
  }
  if (!command.routePath) {
    return
  }
  closePalette()
  await navigateTo(command.routePath)
}

onMounted(() => {
  window.addEventListener('keydown', onPanelKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onPanelKeydown)
})

function buildPaletteCommands(center: SuiteCommandCenter | null, feed: SuiteCommandFeed | null): PaletteCommand[] {
  const commandList: PaletteCommand[] = []
  const keySet = new Set<string>()
  appendStaticCommands(commandList, keySet)
  if (center) {
    appendQuickRouteCommands(commandList, keySet, center)
    appendSearchCommands(commandList, keySet, center)
    appendActionCommands(commandList, keySet, center.recommendedActions)
  }
  if (feed) {
    appendHistoryCommands(commandList, keySet, feed)
  }
  return commandList
}

function appendStaticCommands(commandList: PaletteCommand[], keySet: Set<string>): void {
  appendCommand(commandList, keySet, {
    key: 'route-collaboration',
    kind: 'ROUTE',
    badge: 'ROUTE',
    label: 'Collaboration',
    description: 'Open cross-product collaboration center',
    routePath: '/collaboration',
    actionCode: null
  })
  appendCommand(commandList, keySet, {
    key: 'route-business',
    kind: 'ROUTE',
    badge: 'ROUTE',
    label: 'Business',
    description: 'Open Business overview and team spaces',
    routePath: '/business',
    actionCode: null
  })
  appendCommand(commandList, keySet, {
    key: 'route-notifications',
    kind: 'ROUTE',
    badge: 'ROUTE',
    label: 'Notifications',
    description: 'Open cross-product notification center',
    routePath: '/notifications',
    actionCode: null
  })
}

function appendQuickRouteCommands(
  commandList: PaletteCommand[],
  keySet: Set<string>,
  center: SuiteCommandCenter
): void {
  for (const item of center.quickRoutes) {
    appendCommand(commandList, keySet, {
      key: `route-${item.label}-${item.routePath}`,
      kind: 'ROUTE',
      badge: 'ROUTE',
      label: item.label,
      description: item.description || item.routePath,
      routePath: item.routePath || '/suite',
      actionCode: null
    })
  }
}

function appendSearchCommands(
  commandList: PaletteCommand[],
  keySet: Set<string>,
  center: SuiteCommandCenter
): void {
  for (const item of center.pinnedSearches) {
    appendCommand(commandList, keySet, {
      key: `preset-${item.label}-${item.routePath}`,
      kind: 'SEARCH',
      badge: 'SEARCH',
      label: item.label,
      description: item.description || 'Pinned search',
      routePath: item.routePath || '/search',
      actionCode: null
    })
  }
  for (const keywordItem of center.recentKeywords) {
    appendCommand(commandList, keySet, {
      key: `keyword-${keywordItem}`,
      kind: 'SEARCH',
      badge: 'SEARCH',
      label: `Search "${keywordItem}"`,
      description: 'Recent keyword',
      routePath: `/search?keyword=${encodeURIComponent(keywordItem)}`,
      actionCode: null
    })
  }
}

function appendActionCommands(
  commandList: PaletteCommand[],
  keySet: Set<string>,
  actions: SuiteRemediationAction[]
): void {
  for (const action of actions) {
    appendCommand(commandList, keySet, {
      key: `action-${action.actionCode || action.action}`,
      kind: 'ACTION',
      badge: 'ACTION',
      label: action.action,
      description: `${action.productCode} · ${action.priority}`,
      routePath: '/command-center',
      actionCode: action.actionCode || null
    })
  }
}

function appendHistoryCommands(
  commandList: PaletteCommand[],
  keySet: Set<string>,
  feed: SuiteCommandFeed
): void {
  for (const item of feed.items.slice(0, HISTORY_ITEMS_IN_PALETTE)) {
    appendCommand(commandList, keySet, {
      key: `history-${item.eventId}`,
      kind: 'HISTORY',
      badge: item.category,
      label: item.title,
      description: item.detail || item.eventType,
      routePath: item.routePath || '/command-center',
      actionCode: null
    })
  }
}

function appendCommand(
  commandList: PaletteCommand[],
  keySet: Set<string>,
  command: PaletteCommand
): void {
  if (keySet.has(command.key)) {
    return
  }
  keySet.add(command.key)
  commandList.push(command)
}
</script>

<template>
  <el-dialog
    v-model="dialogModel"
    class="palette-dialog"
    width="760px"
    :show-close="false"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    align-center
    destroy-on-close
  >
    <div class="palette-root">
      <header class="palette-header">
        <h2>Suite Command Palette</h2>
        <p>Type to search routes, actions, and history. Enter to run.</p>
      </header>

      <el-input
        ref="searchInputRef"
        v-model="keyword"
        placeholder="Search commands..."
        clearable
        class="palette-input"
      />

      <el-skeleton v-if="loading" :rows="4" animated />

      <template v-else>
        <el-scrollbar max-height="380px" class="palette-scroll">
          <button
            v-for="(item, index) in filteredCommands"
            :key="item.key"
            type="button"
            class="command-row"
            :class="{ active: selectedIndex === index }"
            @mouseenter="selectCommand(index)"
            @click="runCommand(item)"
          >
            <span class="command-label-wrap">
              <span class="command-label">{{ item.label }}</span>
              <span class="command-desc">{{ item.description }}</span>
            </span>
            <span class="command-meta">
              <el-tag size="small" effect="plain">{{ item.badge }}</el-tag>
            </span>
          </button>
          <el-empty v-if="filteredCommands.length === 0" description="No command found" />
        </el-scrollbar>
      </template>

      <footer class="palette-footer">
        <span>↑/↓ select</span>
        <span>Enter run</span>
        <span>Esc close</span>
        <el-badge v-if="executing" value="RUNNING" />
      </footer>
    </div>
  </el-dialog>
</template>

<style scoped>
.palette-dialog :deep(.el-dialog) {
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid #d4e1e7;
  box-shadow: 0 26px 50px rgba(16, 44, 60, 0.24);
}

.palette-dialog :deep(.el-dialog__header) {
  display: none;
}

.palette-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.palette-root {
  padding: 18px 18px 14px;
  background:
    radial-gradient(circle at top right, rgba(27, 125, 123, 0.16), transparent 38%),
    radial-gradient(circle at 0% 100%, rgba(217, 142, 4, 0.12), transparent 44%),
    #f9fcfd;
}

.palette-header h2 {
  margin: 0;
  font-size: 22px;
  color: #103747;
  letter-spacing: 0.3px;
}

.palette-header p {
  margin: 4px 0 12px;
  color: #5b6f7d;
  font-size: 13px;
}

.palette-input {
  margin-bottom: 12px;
}

.palette-scroll {
  border: 1px solid #d7e5eb;
  border-radius: 12px;
  background: #ffffff;
}

.command-row {
  width: 100%;
  border: 0;
  text-align: left;
  background: transparent;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #edf3f6;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.command-row:last-child {
  border-bottom: 0;
}

.command-row:hover,
.command-row.active {
  background: linear-gradient(90deg, #edf8f8 0%, #f7f2e8 100%);
}

.command-label-wrap {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.command-label {
  font-weight: 600;
  color: #1a2b37;
}

.command-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #6a7d89;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.palette-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 10px;
  color: #6d7d89;
  font-size: 12px;
}

@media (max-width: 768px) {
  .palette-dialog :deep(.el-dialog) {
    width: calc(100vw - 16px) !important;
    margin: 8px auto;
  }

  .palette-root {
    padding: 14px 12px;
  }

  .command-row {
    padding: 10px;
  }
}
</style>
