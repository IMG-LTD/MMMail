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

const sheets = [
  {
    id: 'org-access-matrix',
    name: lt('组织访问矩阵', '組織存取矩陣', 'Org Access Matrix'),
    people: lt('15 位协作者', '15 位協作者', '15 collaborators'),
    scope: lt('共享', '共享', 'Shared')
  },
  {
    id: 'storage-budget',
    name: lt('FY26 存储预算', 'FY26 儲存預算', 'Storage Budget FY26'),
    people: lt('4 位协作者', '4 位協作者', '4 collaborators'),
    scope: lt('私有', '私人', 'Private')
  },
  {
    id: 'support-queue',
    name: lt('支持队列地图', '支援佇列地圖', 'Support Queue Map'),
    people: lt('7 位协作者', '7 位協作者', '7 collaborators'),
    scope: lt('工作区', '工作區', 'Workspace')
  }
]
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      :eyebrow="lt('表格', '試算表', 'Sheets')"
      :title="lt('面向轻量分析的表格界面', '面向輕量分析的試算表介面', 'Sheet surfaces for lightweight analysis')"
      :description="lt('以网格为中心的规划与报表能力，采用克制的 Beta 呈现。', '以網格為中心的規劃與報表能力，採用克制的 Beta 呈現。', 'Grid-oriented planning and reporting in a restrained beta presentation.')"
      :badge="lt('Beta', 'Beta', 'Beta')"
      badge-tone="beta"
    >
      <div class="sheets-actions">
        <button class="page-action" type="button">{{ tr(lt('新建表格', '新增試算表', 'New sheet')) }}</button>
        <button class="page-action page-action--secondary" type="button" @click="toggleCopilotPanel()">
          {{ copilotOpen ? tr(lt('Copilot 已打开', 'Copilot 已開啟', 'Copilot open')) : tr(lt('切换 Copilot', '切換 Copilot', 'Toggle Copilot')) }}
        </button>
      </div>
    </compact-page-header>

    <article class="surface-card sheets-table">
      <header class="sheets-table__head">
        <span>{{ tr(lt('名称', '名稱', 'Name')) }}</span>
        <span>{{ tr(lt('协作者', '協作者', 'Collaborators')) }}</span>
        <span>{{ tr(lt('可见性', '可見性', 'Visibility')) }}</span>
      </header>
      <div v-for="sheet in sheets" :key="sheet.id" class="sheets-table__row">
        <strong>{{ tr(sheet.name) }}</strong>
        <span>{{ tr(sheet.people) }}</span>
        <span>{{ tr(sheet.scope) }}</span>
      </div>
    </article>
  </section>
</template>

<style scoped>
.sheets-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.page-action {
  min-height: 34px;
  padding: 0 14px;
  border: 0;
  border-radius: 10px;
  background: var(--mm-sheets);
  color: #fff;
}

.page-action--secondary {
  border: 1px solid var(--mm-border);
  background: var(--mm-card);
  color: var(--mm-text);
}

.sheets-table {
  overflow: hidden;
}

.sheets-table__head,
.sheets-table__row {
  display: grid;
  grid-template-columns: 1.4fr 0.9fr 0.7fr;
  gap: 16px;
  padding: 16px 18px;
}

.sheets-table__head {
  border-bottom: 1px solid var(--mm-border);
  color: var(--mm-text-secondary);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.sheets-table__row {
  border-bottom: 1px solid var(--mm-border);
  font-size: 13px;
}

.sheets-table__row span {
  color: var(--mm-text-secondary);
}

@media (max-width: 720px) {
  .sheets-table__head,
  .sheets-table__row {
    grid-template-columns: 1fr;
  }
}
</style>
