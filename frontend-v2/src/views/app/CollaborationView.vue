<script setup lang="ts">
import { onMounted, ref } from 'vue'
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, useLocaleText } from '@/locales'
import { httpClient } from '@/service/request/http'
import type { ApiResponse } from '@/shared/types/api'

interface WorkspaceAggregationSummary {
  surfaces: string[]
}

const { tr } = useLocaleText()
const aggregationSurfaces = ref<string[]>([])
const cards = [
  [lt('共享文档', '共享文件', 'Shared Docs'), '14', lt('Beta 写作空间中仍有待响应项。', 'Beta 寫作空間中仍有待回應項目。', 'Pending responses across beta writing spaces')],
  [lt('云盘审阅', '雲端硬碟審閱', 'Drive Reviews'), '8', lt('文件仍在等待协作者批准。', '檔案仍在等待協作者批准。', 'Files waiting for collaborator approval')],
  [lt('日历占位', '日曆保留', 'Calendar Holds'), '3', lt('时间提议仍需要回复。', '時間提議仍需要回覆。', 'Time proposals requiring a reply')]
]

async function loadAggregationSurfaces() {
  const response = await httpClient.get<ApiResponse<WorkspaceAggregationSummary>>('/api/v2/workspace/aggregation')
  return response.data.surfaces
}

onMounted(() => {
  void loadAggregationSurfaces()
    .then(surfaces => {
      aggregationSurfaces.value = surfaces
    })
    .catch(() => {
      aggregationSurfaces.value = []
    })
})
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      :eyebrow="lt('聚合', '聚合', 'Aggregation')"
      :title="lt('协作焦点', '協作焦點', 'Collaboration focus')"
      :description="lt('跨模块协作信号会按当前范围内可见产品进行过滤。', '跨模組協作訊號會依目前範圍內可見產品進行過濾。', 'Cross-module collaboration signals filtered to the products visible in the current scope.')"
      :badge="lt('预览', '預覽', 'Preview')"
      badge-tone="preview"
    />

    <div class="collaboration-filters">
      <span class="metric-chip">{{ tr(lt('当前筛选：共享工作', '目前篩選：共享工作', 'Current filter: Shared work')) }}</span>
      <span class="metric-chip">{{ tr(lt('感知组织访问范围', '感知組織存取範圍', 'Org access aware')) }}</span>
      <span class="metric-chip">{{ tr(lt('不含邮件的摘要', '不含郵件的摘要', 'Mail-free summaries')) }}</span>
      <span v-for="surface in aggregationSurfaces" :key="surface" class="metric-chip">{{ surface }}</span>
    </div>

    <div class="collaboration-grid">
      <article v-for="([title, value, copy], index) in cards" :key="index" class="surface-card collaboration-card">
        <span class="section-label">{{ tr(title) }}</span>
        <strong>{{ value }}</strong>
        <p class="page-subtitle">{{ tr(copy) }}</p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.collaboration-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.collaboration-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.collaboration-card {
  padding: 20px;
}

.collaboration-card strong {
  display: block;
  margin: 10px 0 8px;
  font-size: 40px;
  line-height: 1;
}

@media (max-width: 920px) {
  .collaboration-grid {
    grid-template-columns: 1fr;
  }
}
</style>
