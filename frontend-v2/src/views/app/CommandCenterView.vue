<script setup lang="ts">
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, useLocaleText } from '@/locales'
import { useAutomationRunbook } from '@/shared/composables/useAutomationRunbook'

const { tr } = useLocaleText()
const automationRunbook = useAutomationRunbook()
const currentView = automationRunbook.currentView
const setView = automationRunbook.setView

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

    <div class="command-switcher surface-card">
      <button
        type="button"
        :class="{ 'command-switcher__button--active': currentView === 'overview' }"
        @click="setView('overview')"
      >
        {{ tr(lt('总览', '總覽', 'Overview')) }}
      </button>
      <button
        type="button"
        :class="{ 'command-switcher__button--active': currentView === 'automation' }"
        @click="setView('automation')"
      >
        {{ tr(lt('自动化', '自動化', 'Automation')) }}
      </button>
      <button
        type="button"
        :class="{ 'command-switcher__button--active': currentView === 'runs' }"
        @click="setView('runs')"
      >
        {{ tr(lt('运行记录', '執行紀錄', 'Runs')) }}
      </button>
    </div>

    <div class="command-grid">
      <article class="surface-card command-card">
        <span class="section-label">{{ tr(lt('快速入口', '快速入口', 'Quick routes')) }}</span>
        <div class="command-card__chips">
          <span v-for="(item, index) in routes" :key="index" class="metric-chip">{{ tr(item) }}</span>
        </div>
      </article>

      <article class="surface-card command-card">
        <span class="section-label">{{ tr(lt('最近关键词', '最近關鍵字', 'Recent keywords')) }}</span>
        <div class="command-card__stack">
          <strong v-for="(item, index) in history" :key="index">{{ tr(item) }}</strong>
        </div>
      </article>

      <article class="surface-card command-card command-card--feed">
        <span class="section-label">{{ tr(lt('动态', '動態', 'Feed')) }}</span>
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
.command-switcher {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 16px;
}

.command-switcher button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 999px;
  background: var(--mm-card);
}

.command-switcher__button--active {
  border-color: var(--mm-accent-border) !important;
  background: var(--mm-accent-soft) !important;
  color: var(--mm-primary);
}

.command-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.command-card {
  padding: 20px;
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
