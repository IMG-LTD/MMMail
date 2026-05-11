<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { lt, type TextLike, useLocaleText } from '@/locales'
import { useShellStore } from '@/store/modules/shell'

const shellStore = useShellStore()
const { activeContextPanel, contextPanelOpen, mobileMorePanelOpen } = storeToRefs(shellStore)
const { tr } = useLocaleText()

interface ContextPanelCopy {
  body: TextLike
  meta: TextLike
  title: TextLike
}

const panelCopy: Record<string, ContextPanelCopy> = {
  activity: {
    title: lt('活动', '活動', 'Activity'),
    meta: lt('最近动态', '最近動態', 'Recent activity'),
    body: lt('查看邮件、文件和团队空间的最新协作信号。', '查看郵件、檔案和團隊空間的最新協作信號。', 'Review the latest collaboration signals across mail, files, and team spaces.')
  },
  details: {
    title: lt('详情', '詳情', 'Details'),
    meta: lt('当前对象', '目前物件', 'Current object'),
    body: lt('保留所选邮件、文件或成员的关键属性摘要。', '保留所選郵件、檔案或成員的關鍵屬性摘要。', 'Keep the key properties for the selected mail, file, or member in view.')
  },
  help: {
    title: lt('帮助', '幫助', 'Help'),
    meta: lt('上下文指南', '上下文指南', 'Context guide'),
    body: lt('展示与当前工作区相关的简短操作提示和支持入口。', '展示與目前工作區相關的簡短操作提示和支援入口。', 'Show short workflow tips and support entry points for the current workspace.')
  },
  notifications: {
    title: lt('通知', '通知', 'Notifications'),
    meta: lt('统一提醒', '統一提醒', 'Unified alerts'),
    body: lt('汇总邮件、审批、安全和协作提醒，方便快速处理。', '彙總郵件、審批、安全和協作提醒，方便快速處理。', 'Summarize mail, approval, security, and collaboration alerts for quick triage.')
  },
  risk: {
    title: lt('风控', '風控', 'Risk'),
    meta: lt('安全态势', '安全態勢', 'Security posture'),
    body: lt('突出账户、组织和共享内容的风险线索与建议动作。', '突出帳戶、組織和共享內容的風險線索與建議動作。', 'Highlight risk signals and suggested actions for accounts, organizations, and shared content.')
  }
}

const activePanelCopy = computed(() => panelCopy[activeContextPanel.value] ?? panelCopy.activity)

const panelVisible = computed(() => contextPanelOpen.value || mobileMorePanelOpen.value)

function closeContextPanel() {
  shellStore.closeContextPanel()
  shellStore.closeMobileMorePanel()
}
</script>

<template>
  <aside
    v-if="panelVisible"
    class="context-panel shell-panel-surface"
    :class="{ 'context-panel--mobile': mobileMorePanelOpen }"
    aria-label="Context panel"
  >
    <header class="context-panel__header">
      <div>
        <p class="context-panel__eyebrow">MMMail v2.1</p>
        <h2>{{ tr(activePanelCopy.title) }}</h2>
      </div>
      <button type="button" class="context-panel__close" :aria-label="tr(lt('关闭', '關閉', 'Close'))" @click="closeContextPanel()">
        ×
      </button>
    </header>

    <div class="context-panel__content">
      <p class="context-panel__meta">{{ tr(activePanelCopy.meta) }}</p>
      <p>{{ tr(activePanelCopy.body) }}</p>
    </div>
  </aside>
</template>

<style scoped>
.context-panel {
  width: var(--base-layout-context-panel-width, 320px);
  border-left: 1px solid var(--mm-border);
  background: var(--mm-side-surface);
  padding: 16px;
  display: grid;
  gap: 16px;
}

.context-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.context-panel__eyebrow {
  margin: 0 0 4px;
  font-size: 11px;
  color: var(--mm-text-secondary);
}

.context-panel__header h2 {
  margin: 0;
  color: var(--mm-ink);
  font-size: 16px;
}

.context-panel__close {
  width: 28px;
  height: 28px;
  border: 1px solid var(--mm-border);
  border-radius: 8px;
  background: var(--mm-card);
  color: var(--mm-text-secondary);
}

.context-panel__content {
  display: grid;
  gap: 8px;
}

.context-panel__content p {
  margin: 0;
  color: var(--mm-text-secondary);
  line-height: 1.55;
}

.context-panel__meta {
  font-size: 12px;
  font-weight: 600;
  color: var(--mm-ink);
}

@media (max-width: 820px) {
  .context-panel {
    display: none;
  }

  .context-panel--mobile {
    display: grid;
    width: 100%;
    border-left: 0;
    border-top: 1px solid var(--mm-border);
  }
}
</style>
