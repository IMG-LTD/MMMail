<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, useLocaleText } from '@/locales'
import { readPassMonitor, type PassMonitorItem, type PassMonitorOverview } from '@/service/api/pass'
import { useAuthStore } from '@/store/modules/auth'

const MAX_SECURITY_SCORE = 100
const ISSUE_PREVIEW_LIMIT = 3

const { tr } = useLocaleText()
const authStore = useAuthStore()
const passMonitor = ref<PassMonitorOverview | null>(null)
const passMonitorLoading = ref(false)
const loadError = ref('')
let latestPassMonitorRequest = 0

const totalSignals = computed(() => {
  if (!passMonitor.value) {
    return 0
  }

  return passMonitor.value.weakPasswordCount
    + passMonitor.value.reusedPasswordCount
    + passMonitor.value.inactiveTwoFactorCount
})

const securityScore = computed(() => {
  const totalItems = passMonitor.value?.totalItemCount || 0
  if (!totalItems) {
    return 0
  }

  const cleanItems = Math.max(totalItems - totalSignals.value, 0)
  return Math.round((cleanItems / totalItems) * MAX_SECURITY_SCORE)
})

const trackedCoverage = computed(() => {
  const totalItems = passMonitor.value?.totalItemCount || 0
  if (!totalItems) {
    return 0
  }

  return Math.min(MAX_SECURITY_SCORE, Math.round((passMonitor.value!.trackedItemCount / totalItems) * MAX_SECURITY_SCORE))
})

const statusCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可读取安全监控。', '登入後即可讀取安全監控。', 'Sign in to load security monitoring.'))
  }

  if (loadError.value) {
    return loadError.value
  }

  return passMonitorLoading.value
    ? tr(lt('正在读取密码安全监控。', '正在讀取密碼安全監控。', 'Loading pass security monitor.'))
    : `${passMonitor.value?.trackedItemCount || 0} ${tr(lt('个项目已纳入监控', '個項目已納入監控', 'items tracked'))}`
})

const issueColumns = computed(() => [
  createIssueColumn('weak-passwords', lt('弱密码', '弱密碼', 'Weak passwords'), passMonitor.value?.weakPasswordCount || 0, passMonitor.value?.weakPasswords || []),
  createIssueColumn('reused-passwords', lt('重复使用的密码', '重複使用的密碼', 'Reused passwords'), passMonitor.value?.reusedPasswordCount || 0, passMonitor.value?.reusedPasswords || []),
  createIssueColumn('missing-2fa', lt('缺少 2FA', '缺少 2FA', 'Missing 2FA'), passMonitor.value?.inactiveTwoFactorCount || 0, passMonitor.value?.inactiveTwoFactorItems || [])
])

function createIssueColumn(id: string, title: ReturnType<typeof lt>, count: number, items: PassMonitorItem[]) {
  return {
    count,
    id,
    items: items.slice(0, ISSUE_PREVIEW_LIMIT),
    title
  }
}

function clearPassMonitorState() {
  passMonitor.value = null
  loadError.value = ''
  passMonitorLoading.value = false
}

function resolveErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : tr(lt('读取安全监控失败。', '讀取安全監控失敗。', 'Failed to load security monitor.'))
}

async function loadPassMonitor() {
  const requestId = ++latestPassMonitorRequest
  const requestToken = authStore.accessToken
  if (!requestToken) {
    clearPassMonitorState()
    return
  }

  passMonitorLoading.value = true
  loadError.value = ''

  try {
    const response = await readPassMonitor(requestToken)
    if (requestId !== latestPassMonitorRequest || requestToken !== authStore.accessToken) {
      return
    }
    passMonitor.value = response.data || null
  } catch (error) {
    if (requestId !== latestPassMonitorRequest || requestToken !== authStore.accessToken) {
      return
    }
    passMonitor.value = null
    loadError.value = resolveErrorMessage(error)
  } finally {
    if (requestId === latestPassMonitorRequest && requestToken === authStore.accessToken) {
      passMonitorLoading.value = false
    }
  }
}

watch(() => authStore.accessToken, () => {
  void loadPassMonitor()
}, { immediate: true })
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      :eyebrow="lt('密码库', '密碼庫', 'Pass')"
      :title="lt('安全监控', '安全監控', 'Security Monitor')"
      :description="lt('优先处理弱密码、重复密码和缺少二次验证的凭据。', '優先處理弱密碼、重複密碼和缺少第二因素驗證的憑證。', 'Weak, reused, and missing-factor credentials prioritized for immediate remediation.')"
      :badge="lt('Beta', 'Beta', 'Beta')"
      badge-tone="beta"
    />

    <article class="surface-card pass-monitor__hero">
      <div class="pass-monitor__score">
        <span class="section-label">{{ tr(lt('全局安全指标', '全域安全指標', 'Global security metric')) }}</span>
        <strong>{{ securityScore }} <small>/ {{ MAX_SECURITY_SCORE }}</small></strong>
        <p class="page-subtitle">{{ statusCopy }}</p>
      </div>
      <div class="pass-monitor__rails">
        <div class="pass-monitor__rail">
          <span>{{ tr(lt('已监控项目', '已監控項目', 'Tracked items')) }}</span>
          <strong>{{ passMonitor?.trackedItemCount || 0 }} / {{ passMonitor?.totalItemCount || 0 }}</strong>
        </div>
        <div class="pass-monitor__bar"><span :style="{ width: `${trackedCoverage}%` }" /></div>
        <div class="pass-monitor__rail">
          <span>{{ tr(lt('重点信号', '重點訊號', 'Priority signals')) }}</span>
          <strong>{{ totalSignals }}</strong>
        </div>
        <div class="pass-monitor__bar pass-monitor__bar--risk"><span :style="{ width: `${Math.min(MAX_SECURITY_SCORE, totalSignals * 10)}%` }" /></div>
      </div>
    </article>

    <div class="pass-monitor__grid">
      <article v-for="column in issueColumns" :key="column.id" class="surface-card pass-monitor__column">
        <div class="pass-monitor__column-head">
          <span class="section-label">{{ tr(column.title) }}</span>
          <strong>{{ column.count }}</strong>
        </div>
        <p v-if="!column.items.length" class="page-subtitle">{{ statusCopy }}</p>
        <div v-for="item in column.items" :key="item.id" class="pass-monitor__row">
          <div>
            <strong>{{ item.website || item.title }}</strong>
            <p>{{ item.username || item.itemType }}</p>
          </div>
          <a href="/pass">{{ tr(lt('修复', '修復', 'Fix')) }}</a>
        </div>
        <a class="pass-monitor__link" href="/pass">{{ tr(lt('查看全部', '查看全部', 'View all')) }}</a>
      </article>
    </div>
  </section>
</template>

<style scoped>
.pass-monitor__hero {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 24px;
  padding: 24px;
}

.pass-monitor__score {
  padding-left: 18px;
  border-left: 3px solid var(--mm-pass);
}

.pass-monitor__score strong {
  display: block;
  margin-top: 12px;
  font-size: 64px;
  line-height: 0.92;
  letter-spacing: 0;
  color: var(--mm-pass);
}

.pass-monitor__score small {
  font-size: 22px;
  color: var(--mm-text-secondary);
}

.pass-monitor__rails {
  align-self: center;
  display: grid;
  gap: 10px;
}

.pass-monitor__rail {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.pass-monitor__bar {
  height: 4px;
  border-radius: 999px;
  background: #eceff1;
}

.pass-monitor__bar span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #12a66a 0%, #0b8f5b 100%);
}

.pass-monitor__bar--risk span {
  background: linear-gradient(90deg, #ff8b38 0%, #ff7d1a 100%);
}

.pass-monitor__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.pass-monitor__column {
  padding: 18px;
}

.pass-monitor__column-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pass-monitor__column-head strong {
  color: var(--mm-pass);
}

.pass-monitor__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--mm-border);
}

.pass-monitor__row p {
  margin: 4px 0 0;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.pass-monitor__row a {
  min-height: 28px;
  padding: 5px 10px;
  border: 1px solid var(--mm-border);
  border-radius: 8px;
  background: #fff;
  color: var(--mm-text-primary);
}

.pass-monitor__link {
  display: inline-flex;
  margin-top: 14px;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

@media (max-width: 960px) {
  .pass-monitor__hero,
  .pass-monitor__grid {
    grid-template-columns: 1fr;
  }
}
</style>
