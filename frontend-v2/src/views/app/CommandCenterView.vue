<script setup lang="ts">
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, useLocaleText } from '@/locales'
import { useAutomationRunbook } from '@/shared/composables/useAutomationRunbook'

const { tr } = useLocaleText()
const { currentView, setView } = useAutomationRunbook()

const routes = [lt('收件箱', '收件匣', 'Inbox'), lt('日历', '日曆', 'Calendar'), lt('云盘', '雲端硬碟', 'Drive'), lt('密码监控', '密碼監控', 'Pass Monitor')]
const history = [lt('组织访问矩阵', '組織存取矩陣', 'org access matrix'), lt('恢复包', '復原包', 'recovery kit'), lt('季度审计', '季度稽核', 'quarterly audit')]
const feed = [
  [lt('邮件', '郵件', 'Mail'), lt('安全运营已更新凤凰交付包', '安全營運已更新鳳凰交付包', 'Secure Operations updated Phoenix handoff'), lt('2 分钟前', '2 分鐘前', '2 min ago')],
  [lt('云盘', '雲端硬碟', 'Drive'), lt('节点拓扑图已重新共享给法务', '節點拓撲圖已重新共享給法務', 'Node topology map reshared with legal'), lt('14 分钟前', '14 分鐘前', '14 min ago')],
  [lt('密码库', '密碼庫', 'Pass'), lt('弱密码审阅已分派', '弱密碼審閱已分派', 'Weak password review assigned'), lt('1 小时前', '1 小時前', '1 h ago')]
]
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      :eyebrow="lt('聚合', '聚合', 'Aggregation')"
      :title="lt('指挥中心', '指揮中心', 'Command Center')"
      :description="lt('快速入口、固定搜索、最近关键词与聚合动态都集中在一个键盘优先的界面中。', '快速入口、固定搜尋、最近關鍵字與聚合動態都集中在一個鍵盤優先的介面中。', 'Quick routes, pinned search, recent keywords, and aggregated activity in one keyboard-first surface.')"
      :badge="lt('预览', '預覽', 'Preview')"
      badge-tone="preview"
    />

    <div class="command-grid">
      <article class="surface-card command-card" :class="{ 'command-card--active': currentView === 'overview' }">
        <button
          type="button"
          class="section-label command-card__label"
          :class="{ 'command-card__label--active': currentView === 'overview' }"
          :aria-pressed="currentView === 'overview'"
          @click="setView('overview')"
        >
          {{ tr(lt('快速入口', '快速入口', 'Quick routes')) }}
        </button>
        <div class="command-card__chips">
          <span v-for="(item, index) in routes" :key="index" class="metric-chip">{{ tr(item) }}</span>
        </div>
      </article>

      <article class="surface-card command-card" :class="{ 'command-card--active': currentView === 'automation' }">
        <button
          type="button"
          class="section-label command-card__label"
          :class="{ 'command-card__label--active': currentView === 'automation' }"
          :aria-pressed="currentView === 'automation'"
          @click="setView('automation')"
        >
          {{ tr(lt('最近关键词', '最近關鍵字', 'Recent keywords')) }}
        </button>
        <div class="command-card__stack">
          <strong v-for="(item, index) in history" :key="index">{{ tr(item) }}</strong>
        </div>
      </article>

      <article class="surface-card command-card command-card--feed" :class="{ 'command-card--active': currentView === 'runs' }">
        <button
          type="button"
          class="section-label command-card__label"
          :class="{ 'command-card__label--active': currentView === 'runs' }"
          :aria-pressed="currentView === 'runs'"
          @click="setView('runs')"
        >
          {{ tr(lt('动态', '動態', 'Feed')) }}
        </button>
        <div v-for="([module, title, time], index) in feed" :key="index" class="command-feed__row">
          <div>
            <strong>{{ tr(module) }}</strong>
            <p>{{ tr(title) }}</p>
          </div>
          <span>{{ tr(time) }}</span>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.command-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.command-card {
  padding: 20px;
}

.command-card--active {
  border-color: var(--mm-accent-border);
  box-shadow: 0 0 0 1px var(--mm-accent-border), var(--mm-shadow);
}

.command-card__label {
  display: inline-flex;
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.command-card__label--active {
  color: var(--mm-primary);
}

.command-card__chips,
.command-card__stack {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.command-card__stack strong {
  display: inline-flex;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  align-items: center;
}

.command-card--feed {
  grid-column: span 1;
}

.command-feed__row {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--mm-border);
}

.command-feed__row p,
.command-feed__row span {
  margin: 4px 0 0;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

@media (max-width: 920px) {
  .command-grid {
    grid-template-columns: 1fr;
  }
}
</style>
