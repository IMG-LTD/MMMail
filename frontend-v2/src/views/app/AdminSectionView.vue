<script setup lang="ts">
import { NButton } from "naive-ui";
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import CompactPageHeader from "@/shared/components/CompactPageHeader.vue";
import { V211MetricCard } from "@/design-system/v211";
import HostedBadge from "@/design-system/components/HostedBadge.vue";
import PremiumBadge from "@/design-system/components/PremiumBadge.vue";
import { lt, type TextLike, useLocaleText } from "@/locales";
import {
  listAdminAlerts,
  listAdminAudit,
  listAdminDomains,
  listAdminPolicies,
  listAdminRoles,
  listAdminUsers,
  readAdminRisk,
  readAdminSummary,
  readAdminSystem,
  type AdminAlert,
  type AdminAuditEntry,
  type AdminDomain,
  type AdminPolicy,
  type AdminRiskOverview,
  type AdminRole,
  type AdminSummary,
  type AdminSystemStatus,
  type AdminUser,
} from "@/service/api/admin";
import { readBillingSummary, type BillingSummary } from "@/service/api/billing";
import { listEntitlements, type EntitlementState } from "@/service/api/entitlements";
import { useScopeGuard } from "@/shared/composables/useScopeGuard";
import { useAuthStore } from "@/store/modules/auth";

interface AdminSection {
  key: string;
  label: string;
  path: string;
}

const adminSections: AdminSection[] = [
  { key: "overview", label: "Overview", path: "/admin" },
  { key: "users", label: "Users", path: "/admin/users" },
  { key: "roles", label: "Roles", path: "/admin/roles" },
  { key: "organizations", label: "Organizations", path: "/admin/organizations" },
  { key: "domains", label: "Domains", path: "/admin/domains" },
  { key: "policies", label: "Policies", path: "/admin/policies" },
  { key: "audit", label: "Audit", path: "/admin/audit" },
  { key: "alerts", label: "Alerts", path: "/admin/alerts" },
  { key: "integrations", label: "Integrations", path: "/admin/integrations" },
  { key: "billing", label: "Billing", path: "/admin/billing" },
  { key: "system", label: "System", path: "/admin/system" },
  { key: "risk", label: "Risk", path: "/admin/risk" },
];

const { tr } = useLocaleText();
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const { requestHeaders } = useScopeGuard();

const summary = ref<AdminSummary | null>(null);
const users = ref<AdminUser[]>([]);
const roles = ref<AdminRole[]>([]);
const domains = ref<AdminDomain[]>([]);
const policies = ref<AdminPolicy[]>([]);
const audit = ref<AdminAuditEntry[]>([]);
const alerts = ref<AdminAlert[]>([]);
const system = ref<AdminSystemStatus | null>(null);
const risk = ref<AdminRiskOverview | null>(null);
const billing = ref<BillingSummary | null>(null);
const entitlements = ref<EntitlementState[]>([]);
const adminLoading = ref(false);
const loadError = ref("");
let latestAdminRequest = 0;

const currentSection = computed(() => {
  const surfaceKey = String(route.meta.surfaceKey || "overview");
  return adminSections.find((item) => item.key === surfaceKey) || adminSections[0];
});

const statusCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt("登录后即可读取管理后台。", "登入後即可讀取管理後台。", "Sign in to load Admin."));
  }
  if (loadError.value) {
    return loadError.value;
  }
  return adminLoading.value
    ? tr(lt("正在读取治理运行时。", "正在讀取治理執行期。", "Loading governance runtime."))
    : tr(
        lt(
          "管理后台已连接 v2.1 运行时。",
          "管理後台已連接 v2.1 執行期。",
          "Admin is connected to the v2.1 runtime.",
        ),
      );
});

const adminKpiCards = computed<readonly [TextLike, string][]>(() => [
  [
    lt("用户总数", "使用者總數", "Total users"),
    `${summary.value?.userCount || users.value.length}`,
  ],
  [lt("活跃用户", "活躍使用者", "Active users"), `${summary.value?.activeUserCount || 0}`],
  [lt("今日邮件", "今日郵件", "Mail today"), `${summary.value?.dailyMailCount || 0}`],
  [
    lt("安全评分", "安全評分", "Security score"),
    `${summary.value?.securityScore ?? risk.value?.riskScore ?? 0}`,
  ],
]);

const serviceStatusCards = computed(() => {
  if (system.value?.services?.length) {
    return system.value.services;
  }
  return [{ name: tr(lt("系统", "系統", "System")), status: statusCopy.value }];
});

const alertRows = computed(() => alerts.value.slice(0, 5));
const auditRows = computed(() => audit.value.slice(0, 5));
const entitlementRows = computed(() => entitlements.value.slice(0, 6));
const policyRows = computed(() => policies.value.slice(0, 4));

function openSection(section: AdminSection) {
  void router.push(section.path);
}

function clearAdminState() {
  summary.value = null;
  users.value = [];
  roles.value = [];
  domains.value = [];
  policies.value = [];
  audit.value = [];
  alerts.value = [];
  system.value = null;
  risk.value = null;
  billing.value = null;
  entitlements.value = [];
  loadError.value = "";
  adminLoading.value = false;
}

function resolveErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : tr(lt("读取管理后台失败。", "讀取管理後台失敗。", "Failed to load Admin."));
}

async function loadAdmin() {
  const requestId = ++latestAdminRequest;
  const requestToken = authStore.accessToken;
  const requestPath = route.fullPath;
  const scopeHeaders = requestHeaders.value;
  if (!requestToken) {
    clearAdminState();
    return;
  }

  adminLoading.value = true;
  loadError.value = "";

  try {
    const options = { scopeHeaders, token: requestToken };
    const nextState = await Promise.all([
      readAdminSummary(options),
      listAdminUsers(options),
      listAdminRoles(options),
      listAdminDomains(options),
      listAdminPolicies(options),
      listAdminAudit(options),
      listAdminAlerts(options),
      readAdminSystem(options),
      readAdminRisk(options),
      readBillingSummary(options),
      listEntitlements(options),
    ]);
    if (
      requestId !== latestAdminRequest ||
      requestToken !== authStore.accessToken ||
      requestPath !== route.fullPath
    ) {
      return;
    }
    applyAdminState(nextState);
  } catch (error) {
    if (
      requestId !== latestAdminRequest ||
      requestToken !== authStore.accessToken ||
      requestPath !== route.fullPath
    ) {
      return;
    }
    clearAdminState();
    loadError.value = resolveErrorMessage(error);
  } finally {
    if (
      requestId === latestAdminRequest &&
      requestToken === authStore.accessToken &&
      requestPath === route.fullPath
    ) {
      adminLoading.value = false;
    }
  }
}

function applyAdminState(nextState: Awaited<ReturnType<typeof Promise.all>>) {
  const [
    nextSummary,
    nextUsers,
    nextRoles,
    nextDomains,
    nextPolicies,
    nextAudit,
    nextAlerts,
    nextSystem,
    nextRisk,
    nextBilling,
    nextEntitlements,
  ] = nextState;
  summary.value = nextSummary as AdminSummary;
  users.value = Array.isArray(nextUsers) ? (nextUsers as AdminUser[]) : [];
  roles.value = Array.isArray(nextRoles) ? (nextRoles as AdminRole[]) : [];
  domains.value = Array.isArray(nextDomains) ? (nextDomains as AdminDomain[]) : [];
  policies.value = Array.isArray(nextPolicies) ? (nextPolicies as AdminPolicy[]) : [];
  audit.value = Array.isArray(nextAudit) ? (nextAudit as AdminAuditEntry[]) : [];
  alerts.value = Array.isArray(nextAlerts) ? (nextAlerts as AdminAlert[]) : [];
  system.value = nextSystem as AdminSystemStatus;
  risk.value = nextRisk as AdminRiskOverview;
  billing.value = nextBilling as BillingSummary;
  entitlements.value = Array.isArray(nextEntitlements)
    ? (nextEntitlements as EntitlementState[])
    : [];
}

watch(
  () => [route.fullPath, authStore.accessToken, JSON.stringify(requestHeaders.value)],
  () => {
    void loadAdmin();
  },
  { immediate: true },
);
</script>

<template>
  <section class="page-shell surface-grid admin-page">
    <compact-page-header
      :eyebrow="lt('管理后台', '管理後台', 'Admin')"
      :title="currentSection.label"
      :description="
        lt(
          '组织、用户、权限、安全、系统配置与商业边界都集中在一个可扫描的治理控制台。',
          '組織、使用者、權限、安全、系統設定與商業邊界都集中在一個可掃描的治理控制台。',
          'Organization, user, permission, security, system, and commercial boundaries in one scannable governance console.',
        )
      "
      :badge="lt('v2.1', 'v2.1', 'v2.1')"
    >
      <NButton class="admin-page__action" native-type="button">{{
        tr(lt("导出报告", "匯出報告", "Export report"))
      }}</NButton>
    </compact-page-header>

    <nav class="admin-nav">
      <NButton
        v-for="section in adminSections"
        :key="section.key"
        native-type="button"
        :class="{ 'admin-nav__active': section.key === currentSection.key }"
        @click="openSection(section)"
      >
        {{ section.label }}
      </NButton>
    </nav>

    <div class="admin-kpis">
      <V211MetricCard
        v-for="([label, value], index) in adminKpiCards"
        :key="index"
        class="surface-card admin-kpi"
        :title="label"
        :value="value"
        :status="index === 0 ? 'brand' : 'neutral'"
      />
    </div>

    <div class="admin-grid">
      <article class="surface-card admin-panel admin-panel--wide">
        <span class="section-label">{{
          tr(lt("系统资源使用情况", "系統資源使用情況", "System resource usage"))
        }}</span>
        <div class="admin-resource">
          <span
            >{{ tr(lt("CPU", "CPU", "CPU")) }} <strong>{{ system?.cpuPercent || 0 }}%</strong></span
          >
          <span
            >{{ tr(lt("内存", "記憶體", "Memory")) }}
            <strong>{{ system?.memoryPercent || 0 }}%</strong></span
          >
          <span
            >{{ tr(lt("存储", "儲存", "Storage")) }}
            <strong>{{ system?.storagePercent || billing?.quotaUsedPercent || 0 }}%</strong></span
          >
        </div>
        <p class="page-subtitle">{{ statusCopy }}</p>
      </article>

      <article class="surface-card admin-panel">
        <span class="section-label">{{ tr(lt("服务状态", "服務狀態", "Service status")) }}</span>
        <div v-for="service in serviceStatusCards" :key="service.name" class="admin-row">
          <strong>{{ service.name }}</strong>
          <span>{{ service.status }}</span>
        </div>
      </article>

      <article class="surface-card admin-panel">
        <span class="section-label">{{
          tr(lt("安全与告警", "安全與告警", "Security alerts"))
        }}</span>
        <p v-if="!alertRows.length" class="page-subtitle">{{ statusCopy }}</p>
        <div v-for="item in alertRows" :key="item.id" class="admin-row">
          <strong>{{ item.title }}</strong>
          <span>{{ item.severity }} · {{ item.createdAt }}</span>
        </div>
      </article>

      <article class="surface-card admin-panel">
        <span class="section-label">{{ tr(lt("审计", "稽核", "Audit")) }}</span>
        <p v-if="!auditRows.length" class="page-subtitle">{{ statusCopy }}</p>
        <div v-for="item in auditRows" :key="item.id" class="admin-row">
          <strong>{{ item.action }}</strong>
          <span>{{ item.actorEmail }} · {{ item.createdAt }}</span>
        </div>
      </article>

      <article class="surface-card admin-panel">
        <span class="section-label">{{
          tr(lt("计费与权益", "計費與權益", "Billing and entitlements"))
        }}</span>
        <div class="admin-billing">
          <strong>{{ billing?.planName || statusCopy }}</strong>
          <span>{{ billing?.status || tr(lt("未载入", "未載入", "Not loaded")) }}</span>
          <PremiumBadge compact />
          <HostedBadge compact />
        </div>
        <div v-for="item in entitlementRows" :key="item.key" class="admin-row">
          <strong>{{ item.label }}</strong>
          <span
            >{{ item.state
            }}<template v-if="item.requiredPlan"> · {{ item.requiredPlan }}</template></span
          >
        </div>
      </article>

      <article class="surface-card admin-panel">
        <span class="section-label">{{ tr(lt("策略", "政策", "Policies")) }}</span>
        <p class="page-subtitle">
          {{ domains.length }} domains · {{ roles.length }} roles · {{ users.length }} users
        </p>
        <div v-for="item in policyRows" :key="item.id" class="admin-row">
          <strong>{{ item.name }}</strong>
          <span>{{ item.enabled ? "enabled" : "disabled" }} · {{ item.updatedAt }}</span>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.admin-page__action,
.admin-nav button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 8px;
  background: var(--mm-card);
}

.admin-page__action,
.admin-nav button {
  flex: 0 0 auto;
}

.admin-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 0;
  padding-right: 4px;
}

.admin-nav__active {
  border-color: var(--mm-accent-border) !important;
  background: var(--mm-accent-soft) !important;
  color: var(--mm-primary);
}

.admin-kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.admin-kpi,
.admin-panel {
  display: grid;
  gap: 12px;
  padding: 18px;
}

.admin-kpi strong {
  font-size: 34px;
  line-height: 1;
}

.admin-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.8fr);
  gap: 16px;
}

.admin-panel--wide {
  grid-column: span 1;
}

.admin-resource {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.admin-resource span,
.admin-row,
.admin-billing {
  padding: 12px;
  border: 1px solid var(--mm-border);
  border-radius: 8px;
  background: var(--mm-card-muted);
}

.admin-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.admin-row span,
.admin-billing span {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.admin-row strong,
.admin-row span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.admin-billing {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

@media (max-width: 980px) {
  .admin-kpis,
  .admin-grid,
  .admin-resource {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .admin-nav {
    overflow-x: auto;
    flex-wrap: nowrap;
    position: relative;
    padding: 0 26px 8px 0;
    scrollbar-color: var(--mm-border-strong) transparent;
    scrollbar-width: thin;
    -webkit-overflow-scrolling: touch;
  }

  .admin-nav::after {
    content: "";
    position: sticky;
    right: -26px;
    flex: 0 0 30px;
    margin-left: -30px;
    pointer-events: none;
    background: linear-gradient(90deg, rgba(255, 255, 255, 0), var(--mm-bg));
  }

  .admin-nav::-webkit-scrollbar {
    height: 8px;
  }

  .admin-nav::-webkit-scrollbar-thumb {
    border-radius: 999px;
    background: var(--mm-border-strong);
  }

  .admin-nav button {
    flex: 0 0 auto;
  }

  .admin-row {
    align-items: start;
    flex-direction: column;
  }
}
</style>
