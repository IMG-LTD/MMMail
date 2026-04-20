<script setup lang="ts">
import { onMounted } from 'vue'
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, type TextLike, useLocaleText } from '@/locales'
import { useMcpRegistry } from '@/shared/composables/useMcpRegistry'

const { tr } = useLocaleText()
const mcpRegistry = useMcpRegistry()
const registryCapabilities = mcpRegistry.capabilities

onMounted(() => {
  void mcpRegistry.loadCapabilities()
})

interface SettingsNavItem {
  key: string
  label: TextLike
}

interface RegisteredDevice {
  id: string
  name: string
  location: string
  activity: TextLike
}

const activeNavKey = 'privacy-telemetry'
const navItems: SettingsNavItem[] = [
  { key: 'general', label: lt('常规', '一般', 'General') },
  { key: 'appearance', label: lt('外观', '外觀', 'Appearance') },
  { key: 'language', label: lt('语言', '語言', 'Language') },
  { key: 'mail', label: lt('邮件', '郵件', 'Mail') },
  { key: 'calendar', label: lt('日历', '日曆', 'Calendar') },
  { key: 'drive', label: lt('云盘', '雲端硬碟', 'Drive') },
  { key: 'pass', label: lt('密码库', '密碼庫', 'Pass') },
  { key: 'notifications', label: lt('通知', '通知', 'Notifications') },
  { key: 'privacy-telemetry', label: lt('隐私与遥测', '隱私與遙測', 'Privacy & Telemetry') },
  { key: 'accessibility', label: lt('无障碍', '無障礙', 'Accessibility') },
  { key: 'keyboard', label: lt('键盘', '鍵盤', 'Keyboard') },
  { key: 'labs', label: lt('Labs', 'Labs', 'Labs') },
  { key: 'developer', label: lt('开发者', '開發者', 'Developer') },
  { key: 'adoption-readiness', label: lt('采用准备度', '採用準備度', 'Adoption readiness') },
  { key: 'pwa', label: lt('PWA', 'PWA', 'PWA') },
  { key: 'mail-e2ee', label: lt('邮件 E2EE', '郵件 E2EE', 'Mail E2EE') },
  { key: 'about', label: lt('关于', '關於', 'About') }
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
          :class="{ 'settings-shell__nav-active': item.key === activeNavKey }"
        >
          {{ tr(item.label) }}
        </button>
      </aside>

      <div class="settings-shell__content">
        <section class="settings-panel">
          <span class="section-label">{{ tr(lt('隐私与遥测', '隱私與遙測', 'Privacy & Telemetry')) }}</span>
          <strong>{{ tr(lt('隐私与遥测', '隱私與遙測', 'Privacy & Telemetry')) }}</strong>
          <p class="page-subtitle">{{ tr(lt('管理 MMMail 如何收集诊断数据，以在多设备上保护并强化你的隐私。', '管理 MMMail 如何收集診斷資料，以在多裝置上保護並強化你的隱私。', 'Manage how MMMail collects diagnostic data to defend and harden your privacy across devices.')) }}</p>

          <div v-if="registryCapabilities.length" class="settings-capabilities">
            <span v-for="capability in registryCapabilities" :key="capability" class="metric-chip">{{ capability }}</span>
          </div>

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

        <section class="settings-panel">
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

        <section class="settings-actions">
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

        <div class="settings-save">
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
