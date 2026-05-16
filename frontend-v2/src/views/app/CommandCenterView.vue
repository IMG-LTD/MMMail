<script setup lang="ts">
import { NButton } from "naive-ui";
import { computed, ref, watch } from "vue";
import ChartCard from "@/design-system/components/ChartCard.vue";
import DataTable, { type DataTableColumn } from "@/design-system/components/DataTable.vue";
import ErrorState from "@/design-system/components/ErrorState.vue";
import TerminalLog, { type TerminalLogLine } from "@/design-system/components/TerminalLog.vue";
import CompactPageHeader from "@/shared/components/CompactPageHeader.vue";
import { lt, useLocaleText } from "@/locales";
import {
  createCommandCenterRun,
  listCommandCenterAudit,
  listCommandCenterCommands,
  listCommandCenterWorkflows,
  type CommandCenterAuditEntry,
  type CommandCenterCommand,
  type CommandCenterRun,
  type CommandCenterWorkflow,
} from "@/service/api/command-center";
import { useScopeGuard } from "@/shared/composables/useScopeGuard";
import { useAutomationRunbook } from "@/shared/composables/useAutomationRunbook";
import { useAuthStore } from "@/store/modules/auth";
import { resolveOptionalRuntimeNotice } from "@/shared/utils/premium-runtime";

const { tr } = useLocaleText();
const authStore = useAuthStore();
const { requestHeaders } = useScopeGuard();
const { currentView, setView } = useAutomationRunbook();
const commands = ref<CommandCenterCommand[]>([]);
const workflows = ref<CommandCenterWorkflow[]>([]);
const auditEntries = ref<CommandCenterAuditEntry[]>([]);
const activeRun = ref<CommandCenterRun | null>(null);
const commandCenterLoading = ref(false);
const runError = ref("");
const loadError = ref("");
const premiumRuntimeNotice = ref("");
let latestCommandCenterRequest = 0;

const enabledCommands = computed(() => commands.value.filter((command) => command.enabled));
const latestAudit = computed(() => auditEntries.value.slice(0, 6));
const auditColumns = computed<DataTableColumn[]>(() => [
  { key: "action", label: tr(lt("操作", "操作", "Action")), sortable: true },
  { key: "actor", label: tr(lt("执行人", "執行人", "Actor")) },
  { key: "status", label: tr(lt("状态", "狀態", "Status")) },
  { key: "createdAt", label: tr(lt("时间", "時間", "Time")) },
]);
const auditRows = computed(() => {
  return latestAudit.value.map((item) => ({
    id: item.id,
    action: item.action,
    actor: item.actorEmail,
    createdAt: item.createdAt,
    status: item.status,
  }));
});
const statusCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(
      lt("登录后即可读取指挥中心。", "登入後即可讀取指揮中心。", "Sign in to load Command Center."),
    );
  }

  if (loadError.value) {
    return loadError.value;
  }

  return commandCenterLoading.value
    ? tr(lt("正在读取命令运行时。", "正在讀取命令執行期。", "Loading command runtime."))
    : `${commands.value.length} ${tr(lt("个命令模板已载入", "個命令範本已載入", "command templates loaded"))}`;
});
const terminalLogLevel = computed<TerminalLogLine["level"]>(() => {
  if (runError.value || loadError.value) {
    return "error";
  }

  return activeRun.value?.status === "SUCCEEDED" ? "success" : "info";
});
const terminalLogLines = computed<TerminalLogLine[]>(() => {
  const activeLogLines = activeRun.value?.logTail || [];
  const lines = activeLogLines.length ? activeLogLines : [statusCopy.value];

  return lines.map<TerminalLogLine>((text, index) => ({
    id: `${activeRun.value?.id || "command-center"}-${index}`,
    level: terminalLogLevel.value,
    stream: activeRun.value ? "stdout" : "system",
    text,
    timestamp: activeRun.value?.startedAt || "-",
  }));
});
const commandSummary = computed(() => {
  return `${enabledCommands.value.length}/${commands.value.length} ${tr(lt("可用命令", "可用命令", "enabled commands"))}`;
});
const workflowSummary = computed(() => {
  return `${workflows.value.length} ${tr(lt("条工作流", "條工作流", "workflows"))}`;
});

function clearCommandCenterState() {
  commands.value = [];
  workflows.value = [];
  auditEntries.value = [];
  activeRun.value = null;
  runError.value = "";
  loadError.value = "";
  premiumRuntimeNotice.value = "";
  commandCenterLoading.value = false;
}

function resolveErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : tr(lt("读取指挥中心失败。", "讀取指揮中心失敗。", "Failed to load Command Center."));
}

async function loadCommandCenter() {
  const requestId = ++latestCommandCenterRequest;
  const requestToken = authStore.accessToken;
  const scopeHeaders = requestHeaders.value;
  if (!requestToken) {
    clearCommandCenterState();
    return;
  }

  commandCenterLoading.value = true;
  loadError.value = "";
  premiumRuntimeNotice.value = "";

  try {
    const options = { scopeHeaders, token: requestToken };
    const commandsPromise = listCommandCenterCommands(options);
    const premiumRuntimePromise = loadOptionalCommandCenterRuntime(options);
    const nextCommands = await commandsPromise;
    const premiumRuntime = await premiumRuntimePromise;
    if (requestId !== latestCommandCenterRequest || requestToken !== authStore.accessToken) {
      return;
    }
    commands.value = Array.isArray(nextCommands) ? nextCommands : [];
    workflows.value = premiumRuntime.workflows;
    auditEntries.value = premiumRuntime.auditEntries;
    premiumRuntimeNotice.value = premiumRuntime.notice;
  } catch (error) {
    if (requestId !== latestCommandCenterRequest || requestToken !== authStore.accessToken) {
      return;
    }
    clearCommandCenterState();
    loadError.value = resolveErrorMessage(error);
  } finally {
    if (requestId === latestCommandCenterRequest && requestToken === authStore.accessToken) {
      commandCenterLoading.value = false;
    }
  }
}

async function loadOptionalCommandCenterRuntime(
  options: Parameters<typeof listCommandCenterWorkflows>[0],
) {
  const [workflowResult, auditResult] = await Promise.allSettled([
    listCommandCenterWorkflows(options),
    listCommandCenterAudit(options),
  ]);

  return {
    auditEntries: optionalList(auditResult),
    notice: resolveOptionalRuntimeNotice(
      [workflowResult, auditResult],
      "Command Center automation requires premium access.",
    ),
    workflows: optionalList(workflowResult),
  };
}

function optionalList<T>(result: PromiseSettledResult<T[]>) {
  return result.status === "fulfilled" && Array.isArray(result.value) ? result.value : [];
}

async function runCommand(command: CommandCenterCommand) {
  const requestToken = authStore.accessToken;
  if (!requestToken) {
    return;
  }

  runError.value = "";
  try {
    activeRun.value = await createCommandCenterRun(
      { commandId: command.id, parameters: {} },
      { scopeHeaders: requestHeaders.value, token: requestToken },
    );
    setView("runs");
  } catch (error) {
    runError.value = resolveErrorMessage(error);
  }
}

watch(
  () => [authStore.accessToken, JSON.stringify(requestHeaders.value)],
  () => {
    void loadCommandCenter();
  },
  { immediate: true },
);
</script>

<template>
  <section class="page-shell surface-grid command-center-page">
    <compact-page-header
      :eyebrow="lt('聚合', '聚合', 'Aggregation')"
      :title="lt('指挥中心', '指揮中心', 'Command Center')"
      :description="
        lt(
          '快速入口、固定搜索、最近关键词与聚合动态都集中在一个键盘优先的界面中。',
          '快速入口、固定搜尋、最近關鍵字與聚合動態都集中在一個鍵盤優先的介面中。',
          'Quick routes, pinned search, recent keywords, and aggregated activity in one keyboard-first surface.',
        )
      "
      :badge="lt('预览', '預覽', 'Preview')"
      badge-tone="preview"
    />

    <ErrorState
      v-if="loadError"
      :description="loadError"
      :title="tr(lt('指挥中心读取失败', '指揮中心讀取失敗', 'Command Center failed to load'))"
      retry-label="Retry"
      variant="inline"
      @retry="loadCommandCenter"
    />

    <div class="command-grid">
      <ChartCard
        :description="
          tr(
            lt(
              '常用跨模块指令与自动化入口集中呈现。',
              '常用跨模組指令與自動化入口集中呈現。',
              'Frequently used cross-module commands and automation entries.',
            ),
          )
        "
        :loading="commandCenterLoading"
        :status="currentView === 'overview' ? 'info' : 'neutral'"
        :summary="commandSummary"
        :title="tr(lt('命令模板', '命令範本', 'Command templates'))"
        :value="`${enabledCommands.length}`"
      >
        <NButton
          native-type="button"
          class="section-label command-card__label"
          :class="{ 'command-card__label--active': currentView === 'overview' }"
          :aria-pressed="currentView === 'overview'"
          @click="setView('overview')"
        >
          {{ tr(lt("命令模板", "命令範本", "Command templates")) }}
        </NButton>
        <p v-if="!enabledCommands.length" class="page-subtitle">{{ statusCopy }}</p>
        <div class="command-card__chips">
          <NButton
            v-for="command in enabledCommands"
            :key="command.id"
            native-type="button"
            class="metric-chip command-chip"
            @click="runCommand(command)"
          >
            {{ command.name }}
          </NButton>
        </div>
      </ChartCard>

      <ChartCard
        :description="
          tr(
            lt(
              '跟踪跨产品自动化链路的最新状态。',
              '追蹤跨產品自動化鏈路的最新狀態。',
              'Latest status across product automation workflows.',
            ),
          )
        "
        :loading="commandCenterLoading"
        :status="currentView === 'automation' ? 'success' : 'neutral'"
        :summary="workflowSummary"
        :title="tr(lt('工作流', '工作流', 'Workflows'))"
        :value="`${workflows.length}`"
      >
        <NButton
          native-type="button"
          class="section-label command-card__label"
          :class="{ 'command-card__label--active': currentView === 'automation' }"
          :aria-pressed="currentView === 'automation'"
          @click="setView('automation')"
        >
          {{ tr(lt("工作流", "工作流", "Workflows")) }}
        </NButton>
        <div class="command-card__stack">
          <strong v-for="workflow in workflows" :key="workflow.id"
            >{{ workflow.name }} · {{ workflow.status }}</strong
          >
          <p v-if="premiumRuntimeNotice" class="page-subtitle">{{ premiumRuntimeNotice }}</p>
          <p v-if="!workflows.length" class="page-subtitle">{{ statusCopy }}</p>
        </div>
      </ChartCard>

      <ChartCard
        :description="
          tr(
            lt(
              '审计最近的命令执行、执行人和状态。',
              '稽核最近的命令執行、執行人和狀態。',
              'Recent command runs, actors, and outcomes.',
            ),
          )
        "
        :loading="commandCenterLoading"
        :status="currentView === 'runs' ? 'warning' : 'neutral'"
        :summary="`${auditRows.length} ${tr(lt('条审计记录', '條稽核記錄', 'audit records'))}`"
        :title="tr(lt('运行与审计', '執行與稽核', 'Runs and audit'))"
        :value="`${auditRows.length}`"
      >
        <NButton
          native-type="button"
          class="section-label command-card__label"
          :class="{ 'command-card__label--active': currentView === 'runs' }"
          :aria-pressed="currentView === 'runs'"
          @click="setView('runs')"
        >
          {{ tr(lt("运行与审计", "執行與稽核", "Runs and audit")) }}
        </NButton>
        <ErrorState
          v-if="runError"
          :description="runError"
          :title="tr(lt('命令运行失败', '命令執行失敗', 'Command run failed'))"
          variant="inline"
        />
        <template v-else>
          <p v-if="premiumRuntimeNotice" class="page-subtitle">{{ premiumRuntimeNotice }}</p>
          <DataTable
            :columns="auditColumns"
            density="compact"
            :empty="!auditRows.length"
            row-key="id"
            :rows="auditRows"
            stacked
            @row-action="setView('runs')"
          />
        </template>
      </ChartCard>
    </div>

    <TerminalLog :lines="terminalLogLines" :running="Boolean(activeRun && !activeRun.finishedAt)" />
  </section>
</template>

<style scoped>
.command-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
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

@media (max-width: 920px) {
  .command-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .command-center-page {
    gap: 20px;
  }
}
</style>
