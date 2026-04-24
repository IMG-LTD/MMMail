<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { readSystemHealth } from '@/service/api/system-health'
import { lt, type TextLike, useLocaleText } from '@/locales'
import { useScopeGuard } from '@/shared/composables/useScopeGuard'
import { useMcpRegistry } from '@/shared/composables/useMcpRegistry'
import type { SystemHealthOverview } from '@/shared/types/system-health'
import { useAuthStore } from '@/store/modules/auth'
import { useOnboardingStore } from '@/store/modules/onboarding'

interface SettingsNavItem {
  key: SettingsPanelKey
  label: TextLike
}

interface RegisteredDevice {
  id: string
  name: string
  location: string
  activity: TextLike
}

type SettingsPanelKey = 'getting-started' | 'privacy-telemetry' | 'system-health' | 'integrations'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const onboardingStore = useOnboardingStore()
const { requestHeaders } = useScopeGuard()
const { tr } = useLocaleText()
const mcpRegistry = useMcpRegistry()
const registryCapabilities = mcpRegistry.capabilities
const systemHealth = ref<SystemHealthOverview | null>(null)
const systemHealthFailed = ref(false)

const settingsPanelKeys: SettingsPanelKey[] = ['getting-started', 'privacy-telemetry', 'system-health', 'integrations']
const navItems: SettingsNavItem[] = [
  { key: 'getting-started', label: lt('新手引导', '新手引導', 'Getting Started') },
  { key: 'privacy-telemetry', label: lt('隐私与遥测', '隱私與遙測', 'Privacy & Telemetry') },
  { key: 'system-health', label: lt('系统健康', '系統健康', 'System Health') },
  { key: 'integrations', label: lt('集成', '整合', 'Integrations') }
]

const devices: RegisteredDevice[] = [
  {
    id: 'macbook-zurich',
    name: 'MacBook Pro 14"',
    location: 'Zurich, CH',
    activity: lt('当前会话', '目前工作階段', 'Current session')
  },
  {
    id: 'iphone-geneva',
    name: 'iPhone 15 Pro',
    location: 'Geneva, CH',
    activity: lt('2 小时前', '2 小時前', '2 hours ago')
  },
  {
    id: 'firefox-berlin',
    name: 'Firefox 132',
    location: 'Berlin, DE',
    activity: lt('3 天前', '3 天前', '3 days ago')
  }
]

function isSettingsPanelKey(value: string): value is SettingsPanelKey {
  return settingsPanelKeys.includes(value as SettingsPanelKey)
}

const activePanelKey = computed<SettingsPanelKey>(() => {
  const panelQuery = Array.isArray(route.query.panel) ? route.query.panel[0] : route.query.panel
  const requestedPanel = typeof panelQuery === 'string' ? panelQuery : ''
  return isSettingsPanelKey(requestedPanel) ? requestedPanel : 'privacy-telemetry'
})

function openPanel(panelKey: SettingsPanelKey) {
  void router.replace({
    path: route.path,
    query: {
      ...route.query,
      panel: panelKey
    }
  })
}

onMounted(async () => {
  if (!authStore.accessToken) {
    return
  }

  void mcpRegistry.loadCapabilities().catch(() => {})

  try {
    systemHealth.value = await readSystemHealth(authStore.accessToken, requestHeaders.value)
  } catch {
    systemHealthFailed.value = true
  }
})
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      :eyebrow="lt('设置', '設定', 'Settings')"
      :title="lt('设置', '設定', 'Settings')"
      :description="lt('分层设置面板让高密度控制项保持可读，同时保留平静、感知范围的壳层。', '分層設定面板讓高密度控制項保持可讀，同時保留平靜、感知範圍的殼層。', 'Nested settings panels keep dense controls readable while preserving a calm, scope-aware shell.')"
    />

    <article class="surface-card settings-shell">
      <aside class="settings-shell__nav">
        <button
          v-for="item in navItems"
          :key="item.key"
          type="button"
          :class="{ 'settings-shell__nav-active': item.key === activePanelKey }"
          @click="openPanel(item.key)"
        >
          {{ tr(item.label) }}
        </button>
      </aside>

      <div class="settings-shell__content">
        <section v-if="activePanelKey === 'getting-started'" class="settings-panel">
          <span class="section-label">{{ tr(lt('新手引导', '新手引導', 'Getting Started')) }}</span>
          <strong>{{ tr(lt('重新打开新手引导', '重新開啟新手引導', 'Reopen the onboarding guide')) }}</strong>
          <p class="page-subtitle">{{ tr(lt('随时回到快速开始流程，复习套件、收件箱、日历和云盘的核心入口。', '隨時回到快速開始流程，複習套件、收件匣、日曆和雲端硬碟的核心入口。', 'Return to the quick-start flow anytime to review the suite, inbox, calendar, and drive essentials.')) }}</p>

          <div class="settings-choice">
            <div class="settings-choice__item settings-choice__item--active">
              <strong>{{ tr(lt('入门指南', '入門指南', 'Getting Started')) }}</strong>
              <p>{{ tr(lt('打开欢迎引导，继续浏览 MMMail 的关键工作区。', '開啟歡迎引導，繼續瀏覽 MMMail 的關鍵工作區。', 'Open the welcome guide and continue browsing the key MMMail workspaces.')) }}</p>
              <button type="button" @click="onboardingStore.openGuide()">{{ tr(lt('打开引导', '開啟引導', 'Open guide')) }}</button>
            </div>
          </div>
        </section>

        <section v-if="activePanelKey === 'privacy-telemetry'" class="settings-panel">
          <span class="section-label">{{ tr(lt('隐私与遥测', '隱私與遙測', 'Privacy & Telemetry')) }}</span>
          <strong>{{ tr(lt('隐私与遥测', '隱私與遙測', 'Privacy & Telemetry')) }}</strong>
          <p class="page-subtitle">{{ tr(lt('管理 MMMail 如何收集诊断数据，以在多设备上保护并强化你的隐私。', '管理 MMMail 如何收集診斷資料，以在多裝置上保護並強化你的隱私。', 'Manage how MMMail collects diagnostic data to defend and harden your privacy across devices.')) }}</p>

          <div class="settings-choice">
            <div class="settings-choice__item settings-choice__item--active">
              <strong>{{ tr(lt('最小化（推荐）', '最小化（推薦）', 'Minimal (Recommended)')) }}</strong>
              <p>{{ tr(lt('仅采集关键 MMMail 页面上的匿名诊断数据，不包含文件数据，也不检查内容。', '僅收集關鍵 MMMail 頁面上的匿名診斷資料，不包含檔案資料，也不檢查內容。', 'Anonymous diagnostic data on key MMMail screens, no file data, and no content inspection.')) }}</p>
            </div>
            <div class="settings-choice__item">
              <strong>{{ tr(lt('标准', '標準', 'Standard')) }}</strong>
              <p>{{ tr(lt('共享服务事件与健康信号，以提升问题定位效率。', '共享服務事件與健康訊號，以提升問題定位效率。', 'Share service events and health signals to improve issue resolution.')) }}</p>
            </div>
            <div class="settings-choice__item">
              <strong>{{ tr(lt('完整企业洞察', '完整企業洞察', 'Full enterprise insight')) }}</strong>
              <p>{{ tr(lt('仅在自托管或企业托管场景中启用更详细的诊断载荷。', '僅在自託管或企業代管場景中啟用更詳細的診斷負載。', 'Detailed diagnostic payloads enabled only in self-host or enterprise-managed contexts.')) }}</p>
            </div>
          </div>
        </section>

        <section v-if="activePanelKey === 'system-health'" class="settings-panel settings-panel--grid">
          <span class="section-label">{{ tr(lt('系统健康', '系統健康', 'System Health')) }}</span>
          <strong>{{ tr(lt('系统健康', '系統健康', 'System Health')) }}</strong>
          <p class="page-subtitle">{{ tr(lt('当前工作区的服务状态、请求指标、错误追踪和作业运行概览。', '目前工作區的服務狀態、請求指標、錯誤追蹤與工作執行總覽。', 'A minimal overview of service status, request metrics, error tracking, and job activity for the current workspace.')) }}</p>

          <div v-if="systemHealth" class="settings-health-grid">
            <article class="settings-choice__item">
              <span class="section-label">{{ tr(lt('状态', '狀態', 'Status')) }}</span>
              <strong>{{ systemHealth.status }}</strong>
              <p>{{ systemHealth.applicationName }} · {{ systemHealth.applicationVersion }}</p>
            </article>
            <article class="settings-choice__item">
              <span class="section-label">{{ tr(lt('请求指标', '請求指標', 'Request Metrics')) }}</span>
              <strong>{{ systemHealth.metrics.totalRequests }}</strong>
              <p>{{ tr(lt('失败请求', '失敗請求', 'Failed requests')) }}: {{ systemHealth.metrics.failedRequests }}</p>
            </article>
            <article class="settings-choice__item">
              <span class="section-label">{{ tr(lt('错误追踪', '錯誤追蹤', 'Error Tracking')) }}</span>
              <strong>{{ systemHealth.errorTracking.totalEvents }}</strong>
              <p>{{ tr(lt('服务端 / 客户端', '服務端 / 用戶端', 'Server / Client')) }}: {{ systemHealth.errorTracking.serverEvents }} / {{ systemHealth.errorTracking.clientEvents }}</p>
            </article>
            <article class="settings-choice__item">
              <span class="section-label">{{ tr(lt('作业运行', '工作執行', 'Jobs')) }}</span>
              <strong>{{ systemHealth.jobs.totalRuns }}</strong>
              <p>{{ tr(lt('活跃 / 失败', '活躍 / 失敗', 'Active / Failed')) }}: {{ systemHealth.jobs.activeRuns }} / {{ systemHealth.jobs.failedRuns }}</p>
            </article>
            <article class="settings-choice__item settings-health-grid__wide">
              <span class="section-label">{{ tr(lt('Prometheus', 'Prometheus', 'Prometheus')) }}</span>
              <strong>{{ systemHealth.prometheusPath }}</strong>
            </article>
          </div>

          <p v-else-if="systemHealthFailed" class="page-subtitle">{{ tr(lt('系统健康暂时不可用。', '系統健康暫時無法使用。', 'System health is temporarily unavailable.')) }}</p>
          <p v-else class="page-subtitle">{{ tr(lt('登录后即可查看系统健康概览。', '登入後即可查看系統健康總覽。', 'Sign in to view the system health overview.')) }}</p>
        </section>

        <section v-if="activePanelKey === 'integrations'" class="settings-panel">
          <span class="section-label">{{ tr(lt('集成', '整合', 'Integrations')) }}</span>
          <strong>{{ tr(lt('MCP 能力', 'MCP 能力', 'MCP Capabilities')) }}</strong>
          <p class="page-subtitle">{{ tr(lt('已接入的 MCP 注册能力会在这里集中呈现。', '已接入的 MCP 註冊能力會在這裡集中呈現。', 'Available MCP registry capabilities are collected here.')) }}</p>

          <div v-if="registryCapabilities.length" class="settings-capabilities">
            <span v-for="capability in registryCapabilities" :key="capability" class="metric-chip">{{ capability }}</span>
          </div>
          <p v-else class="page-subtitle">{{ tr(lt('当前没有可展示的集成能力。', '目前沒有可顯示的整合能力。', 'No integration capabilities are currently available.')) }}</p>
        </section>

        <section v-if="activePanelKey === 'privacy-telemetry'" class="settings-panel">
          <div class="settings-panel__head">
            <span class="section-label">{{ tr(lt('已注册设备', '已註冊裝置', 'Registered Devices')) }}</span>
            <button type="button">{{ tr(lt('全部撤销', '全部撤銷', 'Revoke all')) }}</button>
          </div>
          <div class="settings-table">
            <div class="settings-table__head">
              <span>{{ tr(lt('设备', '裝置', 'Device')) }}</span>
              <span>{{ tr(lt('位置 / IP', '位置 / IP', 'Location / IP')) }}</span>
              <span>{{ tr(lt('最近活跃', '最近活躍', 'Last active')) }}</span>
            </div>
            <div v-for="device in devices" :key="device.id" class="settings-table__row">
              <strong>{{ device.name }}</strong>
              <span>{{ device.location }}</span>
              <span>{{ tr(device.activity) }}</span>
            </div>
          </div>
        </section>

        <section v-if="activePanelKey === 'privacy-telemetry'" class="settings-actions">
          <article class="settings-panel">
            <span class="section-label">{{ tr(lt('导出工作区数据', '匯出工作區資料', 'Export Workspace Data')) }}</span>
            <p class="page-subtitle">{{ tr(lt('以 JSON 格式下载包含 MMMail 设置、遥测日志和偏好历史的账户归档。', '以 JSON 格式下載包含 MMMail 設定、遙測日誌與偏好歷史的帳號封存檔。', 'Download an account archive of your MMMail settings, telemetry logs, and preference history in JSON format.')) }}</p>
            <button type="button">{{ tr(lt('申请导出', '申請匯出', 'Request export')) }}</button>
          </article>

          <article class="settings-panel settings-panel--danger">
            <span class="section-label">{{ tr(lt('删除账户', '刪除帳戶', 'Delete Account')) }}</span>
            <p class="page-subtitle">{{ tr(lt('永久删除你的 MMMail 账户，并清除所有相关遥测数据。', '永久刪除你的 MMMail 帳戶，並清除所有相關遙測資料。', 'Permanently delete your MMMail account and wipe all associated telemetry data.')) }}</p>
            <button type="button">{{ tr(lt('删除账户', '刪除帳戶', 'Delete account')) }}</button>
          </article>
        </section>

        <div v-if="activePanelKey === 'privacy-telemetry'" class="settings-save">
          <button type="button">{{ tr(lt('保存更改', '儲存變更', 'Save changes')) }}</button>
        </div>
      </div>
    </article>
  </section>
</template>

<style scoped>
.settings-shell {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  overflow: hidden;
}

.settings-shell__nav {
  display: grid;
  gap: 6px;
  padding: 16px;
  border-right: 1px solid var(--mm-border);
  background: #f7f8fa;
}

.settings-shell__nav button {
  min-height: 32px;
  padding: 0 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--mm-text-secondary);
  text-align: left;
  font-size: 12px;
}

.settings-shell__nav-active {
  background: #e9edf1 !important;
  border-color: var(--mm-border) !important;
  color: var(--mm-ink) !important;
  font-weight: 600;
}

.settings-shell__content {
  display: grid;
  gap: 16px;
  padding: 18px;
}

.settings-panel {
  padding: 18px;
  border: 1px solid var(--mm-border);
  border-radius: 12px;
}

.settings-panel--grid,
.settings-health-grid {
  display: grid;
  gap: 12px;
}

.settings-health-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.settings-health-grid__wide {
  grid-column: 1 / -1;
}

.settings-panel strong {
  display: block;
  margin-top: 8px;
}

.settings-capabilities {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.settings-choice {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.settings-choice__item {
  padding: 14px;
  border: 1px solid var(--mm-border);
  border-radius: 10px;
}

.settings-choice__item p {
  margin: 6px 0 0;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.settings-choice__item--active {
  border-color: #c8d5df;
  background: #fbfdff;
}

.settings-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.settings-panel__head button,
.settings-panel button,
.settings-save button {
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  background: #fff;
}

.settings-table {
  margin-top: 14px;
}

.settings-table__head,
.settings-table__row {
  display: grid;
  grid-template-columns: 1.2fr 0.9fr 0.8fr;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid var(--mm-border);
}

.settings-table__head {
  color: var(--mm-text-secondary);
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.settings-table__row span {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.settings-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.settings-panel--danger button {
  color: #b43d3d;
}

.settings-save {
  display: flex;
  justify-content: end;
}

.settings-save button {
  background: #1f2937;
  color: #fff;
}

@media (max-width: 980px) {
  .settings-shell,
  .settings-actions,
  .settings-table__head,
  .settings-table__row {
    grid-template-columns: 1fr;
  }

  .settings-shell__nav {
    border-right: 0;
    border-bottom: 1px solid var(--mm-border);
  }
}
</style>
