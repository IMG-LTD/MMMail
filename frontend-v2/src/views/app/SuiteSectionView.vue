<script setup lang="ts">
import { NButton } from "naive-ui";
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import HostedBadge from "@/design-system/components/HostedBadge.vue";
import PremiumBadge from "@/design-system/components/PremiumBadge.vue";
import { lt, useLocaleText } from "@/locales";
import {
  listWorkspaceActivity,
  listWorkspaceTasks,
  patchWorkspaceTask,
  readWorkspaceSummary,
  type WorkspaceActivityItem,
  type WorkspaceSummary,
  type WorkspaceSummaryProduct,
  type WorkspaceTask,
} from "@/service/api/workspace";
import { findSurface, suiteSections } from "@/shared/content/route-surfaces";
import { useAuthStore } from "@/store/modules/auth";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { tr } = useLocaleText();

const current = computed(() =>
  findSurface(suiteSections, String(route.meta.surfaceKey ?? "overview"), "overview"),
);
const workspaceSummary = ref<WorkspaceSummary | null>(null);
const workspaceActivity = ref<WorkspaceActivityItem[]>([]);
const workspaceTasks = ref<WorkspaceTask[]>([]);
const workspaceLoading = ref(false);
const taskUpdatingId = ref("");
const loadError = ref("");
let latestWorkspaceRequest = 0;

const productCards = computed<WorkspaceSummaryProduct[]>(() => {
  return workspaceSummary.value?.productCards ?? [];
});

const workspaceStateCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(
      lt(
        "登录后读取 Workspace 汇总。",
        "登入後讀取 Workspace 摘要。",
        "Sign in to load the Workspace summary.",
      ),
    );
  }

  if (workspaceLoading.value) {
    return tr(
      lt(
        "正在读取 Workspace 实时摘要。",
        "正在讀取 Workspace 即時摘要。",
        "Loading the live Workspace summary.",
      ),
    );
  }

  if (loadError.value) {
    return loadError.value;
  }

  return (
    workspaceSummary.value?.systemStatus ||
    tr(
      lt(
        "Workspace 已连接 v2.1 运行时。",
        "Workspace 已連接 v2.1 執行期。",
        "Workspace is connected to the v2.1 runtime.",
      ),
    )
  );
});

function openSection(key: string) {
  const pathMap: Record<string, string> = {
    overview: "/suite",
    plans: "/suite/plans",
    billing: "/suite/billing",
    operations: "/suite/operations",
    boundary: "/suite/boundary",
  };
  router.push(pathMap[key] ?? "/suite");
}

function clearWorkspaceState() {
  workspaceSummary.value = null;
  workspaceActivity.value = [];
  workspaceTasks.value = [];
  loadError.value = "";
  workspaceLoading.value = false;
}

function resolveErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : tr(
        lt(
          "读取 Workspace 数据失败。",
          "讀取 Workspace 資料失敗。",
          "Failed to load Workspace data.",
        ),
      );
}

async function loadWorkspace() {
  const requestId = ++latestWorkspaceRequest;
  const requestToken = authStore.accessToken;
  const requestPath = route.fullPath;

  if (!requestToken) {
    if (requestId === latestWorkspaceRequest) {
      clearWorkspaceState();
    }
    return;
  }

  workspaceLoading.value = true;
  loadError.value = "";

  try {
    const [summary, activity, tasks] = await Promise.all([
      readWorkspaceSummary(requestToken),
      listWorkspaceActivity(requestToken),
      listWorkspaceTasks(requestToken),
    ]);

    if (
      requestId !== latestWorkspaceRequest ||
      requestToken !== authStore.accessToken ||
      requestPath !== route.fullPath
    ) {
      return;
    }

    workspaceSummary.value = summary;
    workspaceActivity.value = activity;
    workspaceTasks.value = tasks;
  } catch (error) {
    if (
      requestId !== latestWorkspaceRequest ||
      requestToken !== authStore.accessToken ||
      requestPath !== route.fullPath
    ) {
      return;
    }

    workspaceSummary.value = null;
    workspaceActivity.value = [];
    workspaceTasks.value = [];
    loadError.value = resolveErrorMessage(error);
  } finally {
    if (
      requestId === latestWorkspaceRequest &&
      requestToken === authStore.accessToken &&
      requestPath === route.fullPath
    ) {
      workspaceLoading.value = false;
    }
  }
}

async function toggleTask(task: WorkspaceTask) {
  const requestToken = authStore.accessToken;
  if (!requestToken || taskUpdatingId.value) {
    return;
  }

  taskUpdatingId.value = task.id;
  try {
    const updatedTask = await patchWorkspaceTask(
      task.id,
      { completed: !task.completed },
      requestToken,
    );
    workspaceTasks.value = workspaceTasks.value.map((item) =>
      item.id === updatedTask.id ? updatedTask : item,
    );
  } finally {
    taskUpdatingId.value = "";
  }
}

watch(
  () => [route.fullPath, authStore.accessToken],
  () => {
    void loadWorkspace();
  },
  { immediate: true },
);
</script>

<template>
  <section class="page-shell surface-grid suite-section">
    <header class="surface-card suite-section__hero">
      <div>
        <span class="section-label">{{ tr(lt("套件", "套件", "Suite")) }}</span>
        <h1>{{ tr(current.label) }}</h1>
        <p class="page-subtitle">{{ tr(current.description) }}</p>
      </div>
      <nav class="suite-section__nav">
        <NButton
          v-for="item in suiteSections"
          :key="item.key"
          native-type="button"
          :class="{ 'suite-section__nav--active': item.key === current.key }"
          @click="openSection(item.key)"
        >
          {{ tr(item.label) }}
        </NButton>
      </nav>
    </header>

    <article class="surface-card suite-section__main">
      <span class="section-label">{{ tr(lt("当前焦点", "目前焦點", "Current focus")) }}</span>
      <strong>{{ `${tr(current.label)} ${tr(lt("界面", "介面", "surface"))}` }}</strong>
      <p class="page-subtitle">{{ workspaceStateCopy }}</p>
      <div class="suite-section__meta">
        <span>Community</span>
        <PremiumBadge compact />
        <HostedBadge compact />
      </div>
    </article>

    <div class="suite-section__cards">
      <article
        v-for="card in productCards"
        :key="card.key"
        class="surface-card suite-section__card"
      >
        <span class="section-label">{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <div class="suite-section__meta">
          <span v-if="card.state === 'community'" class="suite-section__community">Community</span>
          <PremiumBadge v-else-if="card.state === 'premium'" compact />
          <HostedBadge v-else-if="card.state === 'hosted'" compact />
          <small v-if="card.updatedAt">{{ card.updatedAt }}</small>
        </div>
      </article>
      <article
        v-if="!productCards.length"
        class="surface-card suite-section__card suite-section__empty"
      >
        <span class="section-label">{{ tr(lt("Workspace", "Workspace", "Workspace")) }}</span>
        <strong>{{ workspaceStateCopy }}</strong>
      </article>
    </div>

    <div class="suite-section__panels">
      <article class="surface-card suite-section__panel">
        <span class="section-label">{{ tr(lt("最近活动", "最近活動", "Recent activity")) }}</span>
        <ul>
          <li v-for="item in workspaceActivity" :key="item.id">
            <strong>{{ item.title }}</strong>
            <span>{{ item.product }} · {{ item.occurredAt }}</span>
          </li>
        </ul>
        <p v-if="!workspaceActivity.length" class="page-subtitle">{{ workspaceStateCopy }}</p>
      </article>

      <article class="surface-card suite-section__panel">
        <span class="section-label">{{ tr(lt("待办", "待辦", "Tasks")) }}</span>
        <ul>
          <li v-for="task in workspaceTasks" :key="task.id">
            <NButton
              native-type="button"
              :disabled="Boolean(taskUpdatingId)"
              @click="toggleTask(task)"
            >
              {{
                task.completed
                  ? tr(lt("已完成", "已完成", "Done"))
                  : tr(lt("完成", "完成", "Complete"))
              }}
            </NButton>
            <span>{{ task.title }}</span>
          </li>
        </ul>
        <p v-if="!workspaceTasks.length" class="page-subtitle">{{ workspaceStateCopy }}</p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.suite-section__hero,
.suite-section__cards {
  display: grid;
  gap: 16px;
}

.suite-section__hero {
  grid-template-columns: minmax(0, 1fr) auto;
  padding: 18px;
}

.suite-section__hero h1 {
  margin: 8px 0 0;
  font-size: 28px;
  letter-spacing: -0.04em;
}

.suite-section__nav {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.suite-section__nav button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 999px;
  background: var(--mm-card);
}

.suite-section__nav--active {
  border-color: var(--mm-accent-border) !important;
  background: var(--mm-accent-soft) !important;
  color: var(--mm-primary);
}

.suite-section__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.suite-section__community {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border: 1px solid var(--mm-border);
  border-radius: 6px;
  background: var(--mm-surface-muted);
  font-size: 12px;
  font-weight: 700;
}

.suite-section__main,
.suite-section__card,
.suite-section__panel {
  display: grid;
  gap: 10px;
  padding: 18px;
}

.suite-section__cards {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.suite-section__panels {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.suite-section__panel ul {
  display: grid;
  gap: 10px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.suite-section__panel li {
  display: grid;
  gap: 4px;
}

.suite-section__panel button {
  justify-self: start;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid var(--mm-border);
  border-radius: 6px;
  background: var(--mm-card);
}

@media (max-width: 980px) {
  .suite-section__hero,
  .suite-section__cards,
  .suite-section__panels {
    grid-template-columns: 1fr;
  }
}
</style>
