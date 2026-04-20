<script setup lang="ts">
import { onMounted } from 'vue'
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, useLocaleText } from '@/locales'
import { useCopilotPanel } from '@/shared/composables/useCopilotPanel'

const { tr } = useLocaleText()
const copilotPanel = useCopilotPanel()
const copilotOpen = copilotPanel.open

onMounted(() => {
  void copilotPanel.loadCapabilities()
})

function toggleCopilotPanel() {
  copilotPanel.toggle()
}

const docs = [
  {
    id: 'security-charter',
    title: lt('季度安全章程', '季度安全章程', 'Quarterly Security Charter'),
    meta: lt('12 分钟前更新', '12 分鐘前更新', 'Updated 12 min ago'),
    state: lt('已与法务共享', '已與法務共享', 'Shared with Legal')
  },
  {
    id: 'hosting-review',
    title: lt('瑞士托管评审', '瑞士託管評審', 'Swiss Hosting Review'),
    meta: lt('昨天编辑', '昨天編輯', 'Edited yesterday'),
    state: lt('私有草稿', '私人草稿', 'Private draft')
  },
  {
    id: 'recovery-rollout',
    title: lt('恢复包发布计划', '復原套件發佈計畫', 'Recovery Kit Rollout'),
    meta: lt('3 天前编辑', '3 天前編輯', 'Edited 3 days ago'),
    state: lt('工作区备注', '工作區備註', 'Workspace note')
  }
]
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      :eyebrow="lt('文档', '文件', 'Docs')"
      :title="lt('轻量文档工作区', '輕量文件工作區', 'Lightweight document workspace')"
      :description="lt('面向最近、共享和个人写作的 Beta 界面，但不承诺完整套件对等能力。', '面向最近、共享與個人寫作的 Beta 介面，但不承諾完整套件對等能力。', 'Beta surfaces for recent, shared, and personal writing without promising full-suite parity.')"
      :badge="lt('Beta', 'Beta', 'Beta')"
      badge-tone="beta"
    >
      <div class="docs-actions">
        <button class="page-action" type="button">{{ tr(lt('新建文档', '新增文件', 'New document')) }}</button>
        <button class="page-action page-action--secondary" type="button" @click="toggleCopilotPanel()">
          {{ copilotOpen ? tr(lt('Copilot 已打开', 'Copilot 已開啟', 'Copilot open')) : tr(lt('切换 Copilot', '切換 Copilot', 'Toggle Copilot')) }}
        </button>
      </div>
    </compact-page-header>

    <article class="surface-card docs-shell">
      <aside class="docs-shell__nav">
        <button type="button">{{ tr(lt('最近', '最近', 'Recent')) }}</button>
        <button type="button">{{ tr(lt('我的文档', '我的文件', 'My docs')) }}</button>
        <button type="button">{{ tr(lt('共享', '共享', 'Shared')) }}</button>
      </aside>
      <div class="docs-shell__list">
        <article v-for="doc in docs" :key="doc.id" class="docs-row">
          <div>
            <strong>{{ tr(doc.title) }}</strong>
            <p>{{ tr(doc.meta) }}</p>
          </div>
          <span>{{ tr(doc.state) }}</span>
        </article>
      </div>
    </article>
  </section>
</template>

<style scoped>
.docs-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.page-action {
  min-height: 34px;
  padding: 0 14px;
  border: 0;
  border-radius: 10px;
  background: var(--mm-docs);
  color: #fff;
}

.page-action--secondary {
  border: 1px solid var(--mm-border);
  background: var(--mm-card);
  color: var(--mm-text);
}

.docs-shell {
  display: grid;
  grid-template-columns: 220px 1fr;
  overflow: hidden;
}

.docs-shell__nav {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-right: 1px solid var(--mm-border);
  background: color-mix(in srgb, var(--mm-docs) 6%, white);
}

.docs-shell__nav button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: var(--mm-text-secondary);
  text-align: left;
}

.docs-shell__nav button:first-child {
  border-color: color-mix(in srgb, var(--mm-docs) 24%, white);
  background: #fff;
  color: var(--mm-ink);
}

.docs-shell__list {
  display: grid;
  gap: 0;
  padding: 10px 18px;
}

.docs-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid var(--mm-border);
}

.docs-row p,
.docs-row span {
  margin: 4px 0 0;
  color: var(--mm-text-secondary);
  font-size: 12px;
}

@media (max-width: 900px) {
  .docs-shell {
    grid-template-columns: 1fr;
  }

  .docs-shell__nav {
    border-right: 0;
    border-bottom: 1px solid var(--mm-border);
  }
}
</style>
