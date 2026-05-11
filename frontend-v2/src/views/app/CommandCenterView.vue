<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, useLocaleText } from '@/locales'
import {
  createCommandCenterRun,
  listCommandCenterAudit,
  listCommandCenterCommands,
  listCommandCenterWorkflows,
  type CommandCenterAuditEntry,
  type CommandCenterCommand,
  type CommandCenterRun,
  type CommandCenterWorkflow
} from '@/service/api/command-center'
import { useScopeGuard } from '@/shared/composables/useScopeGuard'
import { useAutomationRunbook } from '@/shared/composables/useAutomationRunbook'
import { useAuthStore } from '@/store/modules/auth'

const { tr } = useLocaleText()
const authStore = useAuthStore()
const { requestHeaders } = useScopeGuard()
const { currentView, setView } = useAutomationRunbook()
const commands = ref<CommandCenterCommand[]>([])
const workflows = ref<CommandCenterWorkflow[]>([])
const auditEntries = ref<CommandCenterAuditEntry[]>([])
const activeRun = ref<CommandCenterRun | null>(null)
const commandCenterLoading = ref(false)
const runError = ref('')
const loadError = ref('')
let latestCommandCenterRequest = 0

const enabledCommands = computed(() => commands.value.filter(command => command.enabled))
const latestAudit = computed(() => auditEntries.value.slice(0, 4))
const statusCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可读取指挥中心。', '登入後即可讀取指揮中心。', 'Sign in to load Command Center.'))
  }

  if (loadError.value) {
    return loadError.value
  }

  return commandCenterLoading.value
    ? tr(lt('正在读取命令运行时。', '正在讀取命令執行期。', 'Loading command runtime.'))
    : `${commands.value.length} ${tr(lt('个命令模板已载入', '個命令範本已載入', 'command templates loaded'))}`
})
const logLines = computed(() => activeRun.value?.logTail || [statusCopy.value])

function clearCommandCenterState() {
  commands.value = []
  workflows.value = []
  auditEntries.value = []
  activeRun.value = null
  runError.value = ''
  loadError.value = ''
  commandCenterLoading.value = false
}

function resolveErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : tr(lt('读取指挥中心失败。', '讀取指揮中心失敗。', 'Failed to load Command Center.'))
}

async function loadCommandCenter() {
  const requestId = ++latestCommandCenterRequest
  const requestToken = authStore.accessToken
  const scopeHeaders = requestHeaders.value
  if (!requestToken) {
    clearCommandCenterState()
    return
  }

  commandCenterLoading.value = true
  loadError.value = ''

  try {
    const options = { scopeHeaders, token: requestToken }
    const [nextCommands, nextWorkflows, nextAudit] = await Promise.all([
      listCommandCenterCommands(options),
      listCommandCenterWorkflows(options),
      listCommandCenterAudit(options)
    ])
    if (requestId !== latestCommandCenterRequest || requestToken !== authStore.accessToken) {
      return
    }
    commands.value = Array.isArray(nextCommands) ? nextCommands : []
    workflows.value = Array.isArray(nextWorkflows) ? nextWorkflows : []
    auditEntries.value = Array.isArray(nextAudit) ? nextAudit : []
  } catch (error) {
    if (requestId !== latestCommandCenterRequest || requestToken !== authStore.accessToken) {
      return
    }
    clearCommandCenterState()
    loadError.value = resolveErrorMessage(error)
  } finally {
    if (requestId === latestCommandCenterRequest && requestToken === authStore.accessToken) {
      commandCenterLoading.value = false
    }
  }
}

async function runCommand(command: CommandCenterCommand) {
  const requestToken = authStore.accessToken
  if (!requestToken) {
    return
  }

  runError.value = ''
  try {
    activeRun.value = await createCommandCenterRun(
      { commandId: command.id, parameters: {} },
      { scopeHeaders: requestHeaders.value, token: requestToken }
    )
    setView('runs')
  } catch (error) {
    runError.value = resolveErrorMessage(error)
  }
}

watch(
  () => [authStore.accessToken, JSON.stringify(requestHeaders.value)],
  () => {
    void loadCommandCenter()
  },
  { immediate: true }
)
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      :eyebrow="lt('聚合', '聚合', 'Aggregation')"
      :title="lt('指挥中心', '指揮中心', 'Command Center')"
      :description="lt('快速入口、固定搜索、最近关键词与聚合动态都集中在一个键盘优先的界面中。', '快速入口、固定搜尋、最近關鍵字與聚合動態都集中在一個鍵盤優先的介面中。', 'Quick routes, pinned search, recent keywords, and aggregated activity in one keyboard-first surface.')"
      :badge="lt('预览', '預覽', 'Preview')"
      badge-tone="preview"
    />

    <div class="command-grid">
      <article class="surface-card command-card" :class="{ 'command-card--active': currentView === 'overview' }">
        <button
          type="button"
          class="section-label command-card__label"
          :class="{ 'command-card__label--active': currentView === 'overview' }"
          :aria-pressed="currentView === 'overview'"
          @click="setView('overview')"
        >
          {{ tr(lt('命令模板', '命令範本', 'Command templates')) }}
        </button>
        <p v-if="!enabledCommands.length" class="page-subtitle">{{ statusCopy }}</p>
        <div class="command-card__chips">
          <button v-for="command in enabledCommands" :key="command.id" type="button" class="metric-chip command-chip" @click="runCommand(command)">
            {{ command.name }}
          </button>
        </div>
      </article>

      <article class="surface-card command-card" :class="{ 'command-card--active': currentView === 'automation' }">
        <button
          type="button"
          class="section-label command-card__label"
          :class="{ 'command-card__label--active': currentView === 'automation' }"
          :aria-pressed="currentView === 'automation'"
          @click="setView('automation')"
        >
          {{ tr(lt('工作流', '工作流', 'Workflows')) }}
        </button>
        <div class="command-card__stack">
          <strong v-for="workflow in workflows" :key="workflow.id">{{ workflow.name }} · {{ workflow.status }}</strong>
          <p v-if="!workflows.length" class="page-subtitle">{{ statusCopy }}</p>
        </div>
      </article>

      <article class="surface-card command-card command-card--feed" :class="{ 'command-card--active': currentView === 'runs' }">
        <button
          type="button"
          class="section-label command-card__label"
          :class="{ 'command-card__label--active': currentView === 'runs' }"
          :aria-pressed="currentView === 'runs'"
          @click="setView('runs')"
        >
          {{ tr(lt('运行与审计', '執行與稽核', 'Runs and audit')) }}
        </button>
        <p v-if="runError" class="page-subtitle">{{ runError }}</p>
        <div v-for="item in latestAudit" :key="item.id" class="command-feed__row">
          <div>
            <strong>{{ item.action }}</strong>
            <p>{{ item.actorEmail }} · {{ item.status }}</p>
          </div>
          <span>{{ item.createdAt }}</span>
        </div>
      </article>
    </div>

    <article class="surface-card command-log">
      <span class="section-label">{{ tr(lt('运行日志', '執行日誌', 'Run log')) }}</span>
      <code v-for="(line, index) in logLines" :key="index">{{ line }}</code>
    </article>
  </section>
</template>

<style scoped>
.command-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.command-card {
  padding: 20px;
}

.command-card--active {
  border-color: var(--mm-accent-border);
  box-shadow: 0 0 0 1px var(--mm-accent-border), var(--mm-shadow);
}

.command-card__label {
  display: inline-flex;
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.command-card__label--active {
  color: var(--mm-primary);
}

.command-card__chips,
.command-card__stack {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.command-chip {
  border: 1px solid var(--mm-border);
}

.command-card__stack strong {
  display: inline-flex;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 8px;
  align-items: center;
}

.command-card--feed {
  grid-column: span 1;
}

.command-feed__row {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--mm-border);
}

.command-feed__row p,
.command-feed__row span {
  margin: 4px 0 0;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.command-log {
  display: grid;
  gap: 8px;
  padding: 18px;
  background: #111827;
  color: #f8fafc;
}

.command-log code {
  color: inherit;
  white-space: pre-wrap;
}

@media (max-width: 920px) {
  .command-grid {
    grid-template-columns: 1fr;
  }
}
</style>
