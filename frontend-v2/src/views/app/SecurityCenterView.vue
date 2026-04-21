<script setup lang="ts">
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, type TextLike, useLocaleText } from '@/locales'

interface RegisteredDevice {
  id: string
  name: string
  meta: TextLike
  actionLabel: TextLike
}

interface AuthMethod {
  id: string
  name: TextLike
  status: TextLike
}

interface AuditItem {
  id: string
  title: TextLike
  meta: TextLike
  tone: 'critical' | 'info' | 'muted'
}

interface ActiveSession {
  id: string
  device: TextLike
  location: string
  ip: string
  activity: TextLike
  status: TextLike
}

const { tr } = useLocaleText()
const registeredDevices: RegisteredDevice[] = [
  {
    id: 'macbook',
    name: 'MacBook Pro 14"',
    meta: lt('macOS · Chrome 132.0', 'macOS · Chrome 132.0', 'macOS · Chrome 132.0'),
    actionLabel: lt('撤销', '撤銷', 'Revoke')
  },
  {
    id: 'iphone',
    name: 'iPhone 15 Pro',
    meta: lt('iOS · MMMail 原生应用', 'iOS · MMMail 原生 App', 'iOS · MMMail native app'),
    actionLabel: lt('移除', '移除', 'Remove')
  }
]

const authMethods: AuthMethod[] = [
  {
    id: 'totp',
    name: lt('TOTP 验证器', 'TOTP 驗證器', 'TOTP authenticator'),
    status: lt('已启用', '已啟用', 'Enabled')
  },
  {
    id: 'fido2',
    name: lt('硬件密钥（FIDO2）', '硬體金鑰（FIDO2）', 'Hardware keys (FIDO2)'),
    status: lt('待设置', '待設定', 'Set up')
  },
  {
    id: 'recovery-codes',
    name: lt('恢复代码', '復原代碼', 'Recovery codes'),
    status: lt('剩余 5 组代码', '剩餘 5 組代碼', '5 codes remaining')
  }
]

const auditItems: AuditItem[] = [
  {
    id: 'blocked-sign-in',
    title: lt('可疑登录已拦截', '可疑登入已攔截', 'Suspicious sign-in blocked'),
    meta: lt('昨天 · IP 185.52.214.3', '昨天 · IP 185.52.214.3', 'Yesterday · IP 185.52.214.3'),
    tone: 'critical'
  },
  {
    id: 'device-registered',
    title: lt('新设备已注册', '新裝置已註冊', 'New device registered'),
    meta: lt('昨天 · 设备：iPhone 15 Pro', '昨天 · 裝置：iPhone 15 Pro', 'Yesterday · Device: iPhone 15 Pro'),
    tone: 'info'
  },
  {
    id: 'root-key-rotated',
    title: lt('信任变更：根密钥已更新', '信任變更：根金鑰已更新', 'Trust change: root key updated'),
    meta: lt('3 月 12 日 · 轮换已成功完成', '3 月 12 日 · 輪換已成功完成', 'Mar 12 · Rotation completed successfully'),
    tone: 'muted'
  }
]

const activeSessions: ActiveSession[] = [
  {
    id: 'web-brave',
    device: lt('网页 / Brave（Windows）', '網頁 / Brave（Windows）', 'Web / Brave (Windows)'),
    location: 'Zurich, CH',
    ip: '172.16.24.1',
    activity: lt('在线中', '線上中', 'Online now'),
    status: lt('安全', '安全', 'Secure')
  },
  {
    id: 'ios-app',
    device: lt('iOS / 移动应用', 'iOS / 行動 App', 'iOS / Mobile app'),
    location: 'Geneva, CH',
    ip: '84.22.166.12',
    activity: lt('14 分钟前', '14 分鐘前', '14 mins ago'),
    status: lt('活跃', '活躍', 'Active')
  },
  {
    id: 'android-tablet',
    device: lt('Android / 平板', 'Android / 平板', 'Android / Tablet'),
    location: 'London, UK',
    ip: '2.122.44.12',
    activity: lt('3 天前', '3 天前', '3 days ago'),
    status: lt('空闲', '閒置', 'Idle')
  }
]
</script>

<template>
  <section class="page-shell surface-grid security-page">
    <compact-page-header
      :eyebrow="lt('安全', '安全', 'Security')"
      :title="lt('安全中心', '安全中心', 'Security Center')"
      :description="lt('管理加密密钥、认证方式与活跃会话治理。', '管理加密金鑰、驗證方式與活躍工作階段治理。', 'Manage encryption keys, authentication methods, and active session governance.')"
    >
      <div class="security-page__actions">
        <button type="button">{{ tr(lt('下载恢复包', '下載復原包', 'Download Recovery Kit')) }}</button>
        <button type="button">{{ tr(lt('查看会话', '查看工作階段', 'Review Sessions')) }}</button>
      </div>
    </compact-page-header>

    <div class="security-cards">
      <article class="surface-card security-card">
        <div class="security-card__head">
          <span class="section-label">{{ tr(lt('已注册设备', '已註冊裝置', 'Registered Devices')) }}</span>
          <span class="security-card__meta">{{ tr(lt('3 个活跃', '3 個活躍', '3 Active')) }}</span>
        </div>
        <div v-for="device in registeredDevices" :key="device.id" class="security-card__list-row">
          <div>
            <strong>{{ device.name }}</strong>
            <span>{{ tr(device.meta) }}</span>
          </div>
          <button type="button">{{ tr(device.actionLabel) }}</button>
        </div>
      </article>

      <article class="surface-card security-card">
        <div class="security-card__head">
          <span class="section-label">{{ tr(lt('认证方式', '驗證方式', 'Authentication')) }}</span>
        </div>
        <div v-for="method in authMethods" :key="method.id" class="security-card__list-row">
          <div>
            <strong>{{ tr(method.name) }}</strong>
          </div>
          <span class="security-card__status">{{ tr(method.status) }}</span>
        </div>
      </article>

      <article class="surface-card security-card">
        <span class="section-label">{{ tr(lt('加密基础设施', '加密基礎設施', 'Encryption Infrastructure')) }}</span>
        <strong class="security-card__title">{{ tr(lt('密钥健康状态最佳', '金鑰健康狀態最佳', 'Key Health Optimal')) }}</strong>
        <p class="page-subtitle">{{ tr(lt('最近一次轮换：2 天前。你的加密身份已在可信端点间保持同步。', '最近一次輪換：2 天前。你的加密身分已在可信端點間保持同步。', 'Last rotation: 2 days ago. Your cryptographic identity is synchronized across trusted endpoints.')) }}</p>
        <button class="security-card__link" type="button">{{ tr(lt('查看技术详情', '查看技術詳情', 'View Technical Details')) }}</button>
      </article>

      <article class="surface-card security-card security-card--dark">
        <span class="section-label">{{ tr(lt('恢复包', '復原包', 'Recovery Kit')) }}</span>
        <strong class="security-card__title">{{ tr(lt('请保护好本地恢复记录。', '請保護好本機復原紀錄。', 'Protect local recovery records.')) }}</strong>
        <p>{{ tr(lt('一旦丢失恢复包，历史加密数据可能永久锁定。MMMail 无法代你恢复。', '一旦遺失復原包，歷史加密資料可能永久鎖定。MMMail 無法代你復原。', 'Losing your recovery kit means historical encrypted data may stay locked. MMMail cannot recover this for you.')) }}</p>
        <div class="security-card__dark-actions">
          <button type="button">{{ tr(lt('下载', '下載', 'Download')) }}</button>
          <button type="button">{{ tr(lt('轮换恢复包', '輪換復原包', 'Rotate Kit')) }}</button>
        </div>
      </article>
    </div>

    <article class="surface-card security-audit">
      <div class="security-audit__head">
        <span class="section-label">{{ tr(lt('安全活动审计', '安全活動稽核', 'Security Activity Audit')) }}</span>
        <div class="security-audit__filters">
          <button type="button">{{ tr(lt('全部', '全部', 'All')) }}</button>
          <button type="button">{{ tr(lt('高严重度', '高嚴重度', 'High Severity')) }}</button>
        </div>
      </div>

      <div v-for="item in auditItems" :key="item.id" class="security-audit__row">
        <span class="security-audit__dot" :class="`security-audit__dot--${item.tone}`" />
        <div>
          <strong>{{ tr(item.title) }}</strong>
          <span>{{ tr(item.meta) }}</span>
        </div>
      </div>
    </article>

    <article class="surface-card security-sessions">
      <div class="security-sessions__head">
        <span class="section-label">{{ tr(lt('活跃会话治理', '活躍工作階段治理', 'Active Sessions Governance')) }}</span>
        <button type="button">{{ tr(lt('刷新列表', '重新整理清單', 'Refresh List')) }}</button>
      </div>

      <div class="security-sessions__table-head">
        <span>{{ tr(lt('设备类型', '裝置類型', 'Device Type')) }}</span>
        <span>{{ tr(lt('位置', '位置', 'Location')) }}</span>
        <span>{{ tr(lt('IP 地址', 'IP 位址', 'IP Address')) }}</span>
        <span>{{ tr(lt('最近活跃', '最近活躍', 'Last Active')) }}</span>
        <span>{{ tr(lt('状态', '狀態', 'Status')) }}</span>
      </div>

      <div v-for="session in activeSessions" :key="session.id" class="security-sessions__row">
        <strong>{{ tr(session.device) }}</strong>
        <span>{{ session.location }}</span>
        <span>{{ session.ip }}</span>
        <span>{{ tr(session.activity) }}</span>
        <span class="security-sessions__badge">{{ tr(session.status) }}</span>
      </div>
    </article>
  </section>
</template>

<style scoped>
.security-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.security-page__actions button,
.security-card__list-row button,
.security-sessions__head button {
  min-height: 34px;
  padding: 0 14px;
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  background: var(--mm-card);
}

.security-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.security-card {
  display: grid;
  gap: 14px;
  padding: 18px;
}

.security-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.security-card__meta,
.security-card__status {
  color: var(--mm-text-secondary);
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.security-card__list-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid var(--mm-border);
}

.security-card__list-row strong,
.security-audit__row strong,
.security-sessions__row strong {
  display: block;
  color: var(--mm-ink);
}

.security-card__list-row span,
.security-audit__row span,
.security-sessions__row span {
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.security-card__title {
  font-size: 18px;
  line-height: 1.4;
}

.security-card__link {
  width: fit-content;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--mm-primary);
  font-size: 12px;
  font-weight: 600;
}

.security-card--dark {
  background: linear-gradient(180deg, #2a2f33 0%, #1f2529 100%);
  border-color: transparent;
  box-shadow: none;
}

.security-card--dark .section-label,
.security-card--dark p {
  color: rgba(255, 255, 255, 0.68);
}

.security-card--dark .security-card__title {
  color: #fff;
}

.security-card--dark p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
}

.security-card__dark-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.security-card__dark-actions button {
  min-height: 34px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.security-audit,
.security-sessions {
  padding: 18px;
}

.security-audit__head,
.security-sessions__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.security-audit__filters {
  display: flex;
  gap: 8px;
}

.security-audit__filters button {
  min-height: 30px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 999px;
  background: var(--mm-card-muted);
}

.security-audit__row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 12px;
  align-items: start;
  padding: 16px 0;
  border-top: 1px solid var(--mm-border);
}

.security-audit__dot {
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 999px;
  background: var(--mm-border-strong);
}

.security-audit__dot--critical {
  background: #e15e5e;
}

.security-audit__dot--info {
  background: var(--mm-primary);
}

.security-sessions__table-head,
.security-sessions__row {
  display: grid;
  grid-template-columns: 1.2fr 0.9fr 0.9fr 0.9fr 0.7fr;
  gap: 16px;
  align-items: center;
}

.security-sessions__table-head {
  margin-top: 18px;
  padding-bottom: 12px;
  color: var(--mm-text-secondary);
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.security-sessions__row {
  padding: 14px 0;
  border-top: 1px solid var(--mm-border);
}

.security-sessions__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid var(--mm-accent-border);
  background: var(--mm-accent-soft);
  color: var(--mm-primary) !important;
}

@media (max-width: 1024px) {
  .security-cards {
    grid-template-columns: 1fr;
  }

  .security-sessions {
    overflow-x: auto;
  }

  .security-sessions__table-head,
  .security-sessions__row {
    min-width: 840px;
  }
}

@media (max-width: 640px) {
  .security-page__actions,
  .security-card__dark-actions {
    grid-template-columns: 1fr;
  }
}
</style>
