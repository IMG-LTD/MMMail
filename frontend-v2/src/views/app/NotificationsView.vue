<script setup lang="ts">
import { NButton } from "naive-ui";
import { computed, ref, watch } from "vue";
import ChartCard from "@/design-system/components/ChartCard.vue";
import DataTable, { type DataTableColumn } from "@/design-system/components/DataTable.vue";
import ErrorState from "@/design-system/components/ErrorState.vue";
import CompactPageHeader from "@/shared/components/CompactPageHeader.vue";
import { lt, useLocaleText } from "@/locales";
import { useScopeGuard } from "@/shared/composables/useScopeGuard";
import {
  listNotifications,
  listNotificationRules,
  listNotificationSubscriptions,
  listNotificationTemplates,
  patchNotification,
  readNotificationAnalytics,
  type NotificationAnalytics,
  type NotificationItem,
  type NotificationRule,
  type NotificationSubscription,
  type NotificationTemplate,
} from "@/service/api/notifications";
import { useAuthStore } from "@/store/modules/auth";
import { resolveOptionalRuntimeNotice } from "@/shared/utils/premium-runtime";

const { tr } = useLocaleText();
const authStore = useAuthStore();
const { requestHeaders } = useScopeGuard();
const notifications = ref<NotificationItem[]>([]);
const rules = ref<NotificationRule[]>([]);
const subscriptions = ref<NotificationSubscription[]>([]);
const templates = ref<NotificationTemplate[]>([]);
const analytics = ref<NotificationAnalytics | null>(null);
const notificationsLoading = ref(false);
const loadError = ref("");
const premiumRuntimeNotice = ref("");
let latestNotificationsRequest = 0;

const unreadCount = computed(
  () => notifications.value.filter((item) => item.status === "UNREAD").length,
);
const criticalCount = computed(
  () => notifications.value.filter((item) => item.severity === "CRITICAL").length,
);
const enabledRules = computed(() => rules.value.filter((rule) => rule.enabled).length);
const enabledSubscriptions = computed(
  () => subscriptions.value.filter((item) => item.enabled).length,
);
const notificationColumns = computed<DataTableColumn[]>(() => [
  {
    key: "severity",
    label: tr(lt("严重程度", "嚴重程度", "Severity")),
    sortable: true,
    width: "120px",
  },
  { key: "title", label: tr(lt("标题", "標題", "Title")) },
  { key: "product", label: tr(lt("来源", "來源", "Source")), width: "120px" },
  { key: "status", label: tr(lt("状态", "狀態", "Status")), width: "110px" },
  { key: "createdAt", label: tr(lt("时间", "時間", "Time")), width: "180px" },
]);
const notificationRows = computed(() => {
  return notifications.value.map((item) => ({
    id: item.id,
    createdAt: item.createdAt,
    product: item.product,
    severity: item.severity,
    status: item.status,
    title: item.title,
  }));
});
const statusCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt("登录后即可读取通知。", "登入後即可讀取通知。", "Sign in to load notifications."));
  }

  if (loadError.value) {
    return loadError.value;
  }

  return notificationsLoading.value
    ? tr(lt("正在读取通知运行时。", "正在讀取通知執行期。", "Loading notification runtime."))
    : `${notifications.value.length} ${tr(lt("条通知已载入", "則通知已載入", "notifications loaded"))}`;
});

const analyticsCards = computed(() => [
  [lt("未读", "未讀", "Unread"), `${analytics.value?.unreadCount ?? unreadCount.value}`],
  [lt("严重", "嚴重", "Critical"), `${analytics.value?.criticalCount ?? criticalCount.value}`],
  [lt("送达率", "送達率", "Delivery"), `${analytics.value?.deliveryRate ?? 0}%`],
]);
const rulesSummary = computed(() => {
  return `${rules.value.length} ${tr(lt("条规则", "條規則", "rules"))} · ${templates.value.length} ${tr(lt("个模板", "個範本", "templates"))}`;
});

function clearNotificationsState() {
  notifications.value = [];
  rules.value = [];
  subscriptions.value = [];
  templates.value = [];
  analytics.value = null;
  loadError.value = "";
  premiumRuntimeNotice.value = "";
  notificationsLoading.value = false;
}

function resolveErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : tr(lt("读取通知失败。", "讀取通知失敗。", "Failed to load notifications."));
}

async function loadNotifications() {
  const requestId = ++latestNotificationsRequest;
  const requestToken = authStore.accessToken;
  const scopeHeaders = requestHeaders.value;
  if (!requestToken) {
    clearNotificationsState();
    return;
  }

  notificationsLoading.value = true;
  loadError.value = "";
  premiumRuntimeNotice.value = "";

  try {
    const options = { scopeHeaders, token: requestToken };
    const coreRuntimePromise = Promise.all([
      listNotifications(options),
      listNotificationSubscriptions(options),
    ]);
    const premiumRuntimePromise = loadOptionalNotificationRuntime(options);
    const [nextNotifications, nextSubscriptions] = await coreRuntimePromise;
    const premiumRuntime = await premiumRuntimePromise;
    if (requestId !== latestNotificationsRequest || requestToken !== authStore.accessToken) {
      return;
    }
    notifications.value = Array.isArray(nextNotifications) ? nextNotifications : [];
    subscriptions.value = Array.isArray(nextSubscriptions) ? nextSubscriptions : [];
    rules.value = premiumRuntime.rules;
    templates.value = premiumRuntime.templates;
    analytics.value = premiumRuntime.analytics;
    premiumRuntimeNotice.value = premiumRuntime.notice;
  } catch (error) {
    if (requestId !== latestNotificationsRequest || requestToken !== authStore.accessToken) {
      return;
    }
    clearNotificationsState();
    loadError.value = resolveErrorMessage(error);
  } finally {
    if (requestId === latestNotificationsRequest && requestToken === authStore.accessToken) {
      notificationsLoading.value = false;
    }
  }
}

async function loadOptionalNotificationRuntime(
  options: Parameters<typeof listNotificationRules>[0],
) {
  const [rulesResult, templatesResult, analyticsResult] = await Promise.allSettled([
    listNotificationRules(options),
    listNotificationTemplates(options),
    readNotificationAnalytics(options),
  ]);

  return {
    analytics: optionalValue(analyticsResult),
    notice: resolveOptionalRuntimeNotice(
      [rulesResult, templatesResult, analyticsResult],
      "Notification automation and analytics require premium access.",
    ),
    rules: optionalList(rulesResult),
    templates: optionalList(templatesResult),
  };
}

function optionalList<T>(result: PromiseSettledResult<T[]>) {
  return result.status === "fulfilled" && Array.isArray(result.value) ? result.value : [];
}

function optionalValue<T>(result: PromiseSettledResult<T>) {
  return result.status === "fulfilled" ? result.value : null;
}

async function markAllRead() {
  const requestToken = authStore.accessToken;
  if (!requestToken) {
    return;
  }

  const options = { scopeHeaders: requestHeaders.value, token: requestToken };
  const unreadNotifications = notifications.value.filter((item) => item.status === "UNREAD");
  await Promise.all(
    unreadNotifications.map((item) => patchNotification(item.id, { status: "READ" }, options)),
  );
  await loadNotifications();
}

async function markNotificationRowRead(row: Record<string, unknown>) {
  const requestToken = authStore.accessToken;
  const notificationId = typeof row.id === "string" ? row.id : "";
  if (!requestToken || !notificationId) {
    return;
  }

  const options = { scopeHeaders: requestHeaders.value, token: requestToken };
  await patchNotification(notificationId, { status: "READ" }, options);
  await loadNotifications();
}

watch(
  () => [authStore.accessToken, JSON.stringify(requestHeaders.value)],
  () => {
    void loadNotifications();
  },
  { immediate: true },
);
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      :eyebrow="lt('聚合', '聚合', 'Aggregation')"
      :title="lt('通知', '通知', 'Notifications')"
      :description="
        lt(
          '基于范围过滤模块告警，并支持按未读、严重程度和来源控制。',
          '依據範圍過濾模組告警，並支援依未讀、嚴重程度與來源控制。',
          'Scope-filtered module alerts with unread, severity, and source-based control.',
        )
      "
      :badge="lt('预览', '預覽', 'Preview')"
      badge-tone="preview"
    />

    <div class="notification-filters">
      <span class="metric-chip">{{ tr(lt("未读", "未讀", "Unread")) }} {{ unreadCount }}</span>
      <span class="metric-chip"
        >{{ tr(lt("严重程度", "嚴重程度", "Severity")) }} {{ criticalCount }}</span
      >
      <span class="metric-chip">{{ tr(lt("规则", "規則", "Rules")) }} {{ enabledRules }}</span>
      <span class="metric-chip"
        >{{ tr(lt("订阅", "訂閱", "Subscriptions")) }} {{ enabledSubscriptions }}</span
      >
      <NButton class="notifications-mark-all" native-type="button" @click="markAllRead">{{
        tr(lt("全部标为已读", "全部標示為已讀", "Mark all read"))
      }}</NButton>
    </div>

    <div class="notifications-layout">
      <div class="notifications-main">
        <ErrorState
          v-if="loadError"
          :description="loadError"
          :title="tr(lt('通知读取失败', '通知讀取失敗', 'Notifications failed to load'))"
          retry-label="Retry"
          variant="inline"
          @retry="loadNotifications"
        />
        <DataTable
          :columns="notificationColumns"
          density="compact"
          :empty="!notificationRows.length"
          :loading="notificationsLoading"
          row-key="id"
          :rows="notificationRows"
          stacked
          @row-action="markNotificationRowRead"
        />
        <p v-if="!notificationRows.length" class="page-subtitle">{{ statusCopy }}</p>
      </div>

      <aside class="notifications-side">
        <ChartCard
          :description="
            tr(
              lt(
                '聚合未读、严重告警和送达表现。',
                '聚合未讀、嚴重告警和送達表現。',
                'Unread, critical alert, and delivery indicators.',
              ),
            )
          "
          :loading="notificationsLoading"
          status="info"
          :summary="statusCopy"
          :title="tr(lt('分析', '分析', 'Analytics'))"
          :value="`${analytics?.totalCount ?? notifications.length}`"
        >
          <div
            v-for="([label, value], index) in analyticsCards"
            :key="index"
            class="notifications-stat"
          >
            <span>{{ tr(label) }}</span>
            <strong>{{ value }}</strong>
          </div>
          <p v-if="premiumRuntimeNotice" class="page-subtitle">{{ premiumRuntimeNotice }}</p>
        </ChartCard>

        <ChartCard
          :description="
            tr(
              lt(
                '集中查看当前启用规则、订阅和模板。',
                '集中查看目前啟用規則、訂閱和範本。',
                'Active rules, subscriptions, and templates in one place.',
              ),
            )
          "
          :loading="notificationsLoading"
          status="success"
          :summary="rulesSummary"
          :title="tr(lt('规则与模板', '規則與範本', 'Rules and templates'))"
          :value="`${enabledRules}`"
        >
          <p class="page-subtitle">{{ rulesSummary }}</p>
          <p v-if="premiumRuntimeNotice" class="page-subtitle">{{ premiumRuntimeNotice }}</p>
          <span v-for="item in templates" :key="item.id" class="metric-chip">{{ item.name }}</span>
        </ChartCard>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.notification-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.notifications-mark-all {
  min-height: 34px;
  margin-left: auto;
  padding: 0 14px;
  border: 1px solid var(--mm-border);
  border-radius: 8px;
  background: #fff;
}

.notifications-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.notifications-main {
  display: grid;
  gap: 12px;
}

.notifications-side {
  display: grid;
  gap: 16px;
}

.notifications-stat {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.notifications-stat span {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

@media (max-width: 900px) {
  .notifications-layout {
    grid-template-columns: 1fr;
  }

  .notification-filters {
    gap: 12px;
  }

  .notifications-mark-all {
    min-height: 40px;
  }
}

@media (max-width: 720px) {
  .notifications-mark-all {
    margin-left: 0;
  }
}
</style>
