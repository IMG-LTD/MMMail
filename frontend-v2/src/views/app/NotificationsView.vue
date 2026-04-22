<script setup lang="ts">
import { ref, watch } from 'vue'
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, useLocaleText } from '@/locales'
import { useScopeGuard } from '@/shared/composables/useScopeGuard'
import { httpClient } from '@/service/request/http'
import type { ApiResponse } from '@/shared/types/api'

interface WorkspaceAggregationSummary {
  surfaces: string[]
}

const { tr } = useLocaleText()
const { requestHeaders } = useScopeGuard()
const aggregationSurfaces = ref<string[]>([])
let aggregationRequestToken = 0
const notifications = [
  [lt('严重', '嚴重', 'Critical'), lt('云盘共享策略需要复核', '雲端硬碟共享政策需要複核', 'Drive sharing policy needs review'), lt('组织', '組織', 'Organizations'), lt('刚刚', '剛剛', 'Now')],
  [lt('未读', '未讀', 'Unread'), lt('凤凰会议室邀请已接受', '鳳凰會議室邀請已接受', 'Phoenix room invite accepted'), lt('日历', '日曆', 'Calendar'), lt('12 分钟前', '12 分鐘前', '12 min ago')],
  [lt('信息', '資訊', 'Info'), lt('恢复包导出已完成', '復原包匯出已完成', 'Recovery kit export completed'), lt('安全', '安全', 'Security'), lt('昨天', '昨天', 'Yesterday')]
]

async function loadAggregationSurfaces() {
  const response = await httpClient.get<ApiResponse<WorkspaceAggregationSummary>>('/api/v2/workspace/aggregation', {
    scopeHeaders: requestHeaders.value
  })
  return response.data.surfaces
}

watch(
  requestHeaders,
  () => {
    const requestToken = ++aggregationRequestToken

    void loadAggregationSurfaces()
      .then(surfaces => {
        if (requestToken !== aggregationRequestToken) {
          return
        }

        aggregationSurfaces.value = surfaces
      })
      .catch(() => {
        if (requestToken !== aggregationRequestToken) {
          return
        }

        aggregationSurfaces.value = []
      })
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
      <span class="metric-chip">{{ tr(lt('未读', '未讀', 'Unread')) }}</span>
      <span class="metric-chip">{{ tr(lt('严重程度', '嚴重程度', 'Severity')) }}</span>
      <span class="metric-chip">{{ tr(lt('来源模块', '來源模組', 'Source module')) }}</span>
      <span v-for="surface in aggregationSurfaces" :key="surface" class="metric-chip">{{ surface }}</span>
      <button class="notifications-mark-all" type="button">{{ tr(lt('全部标为已读', '全部標示為已讀', 'Mark all read')) }}</button>
    </div>

    <article class="surface-card notifications-list">
      <div v-for="([severity, title, module, time], index) in notifications" :key="index" class="notifications-row">
        <div>
          <span class="section-label">{{ tr(severity) }}</span>
          <strong>{{ tr(title) }}</strong>
          <p>{{ tr(module) }}</p>
        </div>
        <span>{{ tr(time) }}</span>
      </div>
    </article>
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
  border-radius: 10px;
  background: #fff;
}

.notifications-list {
  padding: 0 18px;
}

.notifications-row {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 0;
  border-bottom: 1px solid var(--mm-border);
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

@media (max-width: 720px) {
  .notifications-mark-all {
    margin-left: 0;
  }

  .notifications-row {
    flex-direction: column;
  }
}
</style>
