<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, useLocaleText } from '@/locales'
import { useScopeGuard } from '@/shared/composables/useScopeGuard'
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
  type NotificationTemplate
} from '@/service/api/notifications'
import { useAuthStore } from '@/store/modules/auth'

const { tr } = useLocaleText()
const authStore = useAuthStore()
const { requestHeaders } = useScopeGuard()
const notifications = ref<NotificationItem[]>([])
const rules = ref<NotificationRule[]>([])
const subscriptions = ref<NotificationSubscription[]>([])
const templates = ref<NotificationTemplate[]>([])
const analytics = ref<NotificationAnalytics | null>(null)
const notificationsLoading = ref(false)
const loadError = ref('')
let latestNotificationsRequest = 0

const unreadCount = computed(() => notifications.value.filter(item => item.status === 'UNREAD').length)
const criticalCount = computed(() => notifications.value.filter(item => item.severity === 'CRITICAL').length)
const enabledRules = computed(() => rules.value.filter(rule => rule.enabled).length)
const enabledSubscriptions = computed(() => subscriptions.value.filter(item => item.enabled).length)
const statusCopy = computed(() => {
  if (!authStore.accessToken) {
    return tr(lt('登录后即可读取通知。', '登入後即可讀取通知。', 'Sign in to load notifications.'))
  }

  if (loadError.value) {
    return loadError.value
  }

  return notificationsLoading.value
    ? tr(lt('正在读取通知运行时。', '正在讀取通知執行期。', 'Loading notification runtime.'))
    : `${notifications.value.length} ${tr(lt('条通知已载入', '則通知已載入', 'notifications loaded'))}`
})

const analyticsCards = computed(() => [
  [lt('未读', '未讀', 'Unread'), `${analytics.value?.unreadCount ?? unreadCount.value}`],
  [lt('严重', '嚴重', 'Critical'), `${analytics.value?.criticalCount ?? criticalCount.value}`],
  [lt('送达率', '送達率', 'Delivery'), `${analytics.value?.deliveryRate ?? 0}%`]
])

function clearNotificationsState() {
  notifications.value = []
  rules.value = []
  subscriptions.value = []
  templates.value = []
  analytics.value = null
  loadError.value = ''
  notificationsLoading.value = false
}

function resolveErrorMessage(error: unknown) {
  return error instanceof Error
    ? error.message
    : tr(lt('读取通知失败。', '讀取通知失敗。', 'Failed to load notifications.'))
}

async function loadNotifications() {
  const requestId = ++latestNotificationsRequest
  const requestToken = authStore.accessToken
  const scopeHeaders = requestHeaders.value
  if (!requestToken) {
    clearNotificationsState()
    return
  }

  notificationsLoading.value = true
  loadError.value = ''

  try {
    const options = { scopeHeaders, token: requestToken }
    const [nextNotifications, nextRules, nextSubscriptions, nextTemplates, nextAnalytics] = await Promise.all([
      listNotifications(options),
      listNotificationRules(options),
      listNotificationSubscriptions(options),
      listNotificationTemplates(options),
      readNotificationAnalytics(options)
    ])
    if (requestId !== latestNotificationsRequest || requestToken !== authStore.accessToken) {
      return
    }
    notifications.value = Array.isArray(nextNotifications) ? nextNotifications : []
    rules.value = Array.isArray(nextRules) ? nextRules : []
    subscriptions.value = Array.isArray(nextSubscriptions) ? nextSubscriptions : []
    templates.value = Array.isArray(nextTemplates) ? nextTemplates : []
    analytics.value = nextAnalytics || null
  } catch (error) {
    if (requestId !== latestNotificationsRequest || requestToken !== authStore.accessToken) {
      return
    }
    clearNotificationsState()
    loadError.value = resolveErrorMessage(error)
  } finally {
    if (requestId === latestNotificationsRequest && requestToken === authStore.accessToken) {
      notificationsLoading.value = false
    }
  }
}

async function markAllRead() {
  const requestToken = authStore.accessToken
  if (!requestToken) {
    return
  }

  const options = { scopeHeaders: requestHeaders.value, token: requestToken }
  const unreadNotifications = notifications.value.filter(item => item.status === 'UNREAD')
  await Promise.all(unreadNotifications.map(item => patchNotification(item.id, { status: 'READ' }, options)))
  await loadNotifications()
}

watch(
  () => [authStore.accessToken, JSON.stringify(requestHeaders.value)],
  () => {
    void loadNotifications()
  },
  { immediate: true }
)
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      :eyebrow="lt('聚合', '聚合', 'Aggregation')"
      :title="lt('通知', '通知', 'Notifications')"
      :description="lt('基于范围过滤模块告警，并支持按未读、严重程度和来源控制。', '依據範圍過濾模組告警，並支援依未讀、嚴重程度與來源控制。', 'Scope-filtered module alerts with unread, severity, and source-based control.')"
      :badge="lt('预览', '預覽', 'Preview')"
      badge-tone="preview"
    />

    <div class="notification-filters">
      <span class="metric-chip">{{ tr(lt('未读', '未讀', 'Unread')) }} {{ unreadCount }}</span>
      <span class="metric-chip">{{ tr(lt('严重程度', '嚴重程度', 'Severity')) }} {{ criticalCount }}</span>
      <span class="metric-chip">{{ tr(lt('规则', '規則', 'Rules')) }} {{ enabledRules }}</span>
      <span class="metric-chip">{{ tr(lt('订阅', '訂閱', 'Subscriptions')) }} {{ enabledSubscriptions }}</span>
      <button class="notifications-mark-all" type="button" @click="markAllRead">{{ tr(lt('全部标为已读', '全部標示為已讀', 'Mark all read')) }}</button>
    </div>

    <div class="notifications-layout">
      <article class="surface-card notifications-list">
        <p v-if="!notifications.length" class="page-subtitle">{{ statusCopy }}</p>
        <div v-for="item in notifications" :key="item.id" class="notifications-row">
          <div class="notifications-row__dot" :class="{ 'notifications-row__dot--read': item.status === 'READ' }" />
          <div>
            <span class="section-label">{{ item.severity }}</span>
            <strong>{{ item.title }}</strong>
            <p>{{ item.product }} · {{ item.createdAt }}</p>
          </div>
          <span>{{ item.status }}</span>
        </div>
      </article>

      <aside class="notifications-side">
        <article class="surface-card notifications-panel">
          <span class="section-label">{{ tr(lt('分析', '分析', 'Analytics')) }}</span>
          <div v-for="([label, value], index) in analyticsCards" :key="index" class="notifications-stat">
            <span>{{ tr(label) }}</span>
            <strong>{{ value }}</strong>
          </div>
        </article>

        <article class="surface-card notifications-panel">
          <span class="section-label">{{ tr(lt('规则与模板', '規則與範本', 'Rules and templates')) }}</span>
          <p class="page-subtitle">{{ rules.length }} {{ tr(lt('条规则', '條規則', 'rules')) }} · {{ templates.length }} {{ tr(lt('个模板', '個範本', 'templates')) }}</p>
          <span v-for="item in templates" :key="item.id" class="metric-chip">{{ item.name }}</span>
        </article>
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

.notifications-list {
  padding: 0 18px;
}

.notifications-row {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  align-items: start;
  gap: 14px;
  padding: 18px 0;
  border-bottom: 1px solid var(--mm-border);
}

.notifications-row__dot {
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 999px;
  background: var(--mm-primary);
}

.notifications-row__dot--read {
  background: var(--mm-border);
}

.notifications-row strong {
  display: block;
  margin-top: 8px;
}

.notifications-row p,
.notifications-row span:last-child {
  margin: 6px 0 0;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.notifications-side {
  display: grid;
  gap: 16px;
}

.notifications-panel {
  display: grid;
  gap: 12px;
  padding: 18px;
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
}

@media (max-width: 720px) {
  .notifications-mark-all {
    margin-left: 0;
  }

  .notifications-row {
    grid-template-columns: 10px minmax(0, 1fr);
  }

  .notifications-row span:last-child {
    grid-column: 2;
  }
}
</style>
