<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { lt, useLocaleText } from '@/locales'
import { useCopilotPanel } from '@/shared/composables/useCopilotPanel'

const route = useRoute()
const { tr } = useLocaleText()
const copilotPanel = useCopilotPanel()
const copilotOpen = copilotPanel.open

onMounted(() => {
  void copilotPanel.loadCapabilities()
})

function toggleCopilotPanel() {
  copilotPanel.toggle()
}

const title = computed(() => {
  return String(route.params.id ?? 'org-access-matrix').replace(/-/g, ' ')
})

const columns = ['A', 'B', 'C', 'D', 'E']
const rows = [1, 2, 3, 4, 5]
</script>

<template>
  <section class="page-shell surface-grid sheets-editor">
    <article class="surface-card sheets-editor__top">
      <div>
        <span class="section-label">{{ tr(lt('表格编辑器', '試算表編輯器', 'Sheets editor')) }}</span>
        <h1>{{ title }}</h1>
        <p class="page-subtitle">{{ tr(lt('公式栏、工作表标签和上下文引用共同提供一个网格优先的 Beta 编辑界面。', '公式列、工作表分頁與內容脈絡引用共同提供一個網格優先的 Beta 編輯介面。', 'Formula bar, sheet tabs, and contextual references deliver a grid-first beta editing surface.')) }}</p>
      </div>
      <div class="sheets-editor__actions">
        <button type="button">{{ tr(lt('格式', '格式', 'Format')) }}</button>
        <button type="button">{{ tr(lt('共享', '共享', 'Share')) }}</button>
        <button type="button" @click="toggleCopilotPanel()">
          {{ copilotOpen ? tr(lt('Copilot 已打开', 'Copilot 已開啟', 'Copilot open')) : tr(lt('切换 Copilot', '切換 Copilot', 'Toggle Copilot')) }}
        </button>
      </div>
    </article>

    <section class="sheets-editor__layout">
      <article class="surface-card sheets-editor__grid">
        <header class="sheets-editor__formula">fx =IF(B2="Enabled","Grant","Hold")</header>
        <div class="sheets-editor__table">
          <div class="sheets-editor__corner" />
          <div v-for="column in columns" :key="column" class="sheets-editor__cell sheets-editor__cell--head">{{ column }}</div>
          <template v-for="row in rows" :key="row">
            <div class="sheets-editor__cell sheets-editor__cell--head">{{ row }}</div>
            <div v-for="column in columns" :key="`${row}-${column}`" class="sheets-editor__cell">
              {{ row === 1 ? `${column}1` : '' }}
            </div>
          </template>
        </div>
      </article>
      <aside class="surface-card sheets-editor__side">
        <span class="section-label">{{ tr(lt('函数参考', '函數參考', 'Function reference')) }}</span>
        <strong>{{ tr(lt('逻辑与访问控制辅助函数', '邏輯與存取控制輔助函數', 'Logical and access-control helpers')) }}</strong>
        <p class="page-subtitle">{{ tr(lt('评论、辅助公式与协作备注会留在独立的 Beta 侧栏中。', '評論、輔助公式與協作備註會留在獨立的 Beta 側欄中。', 'Keep comments, helper formulas, and collaboration notes in a dedicated beta side panel.')) }}</p>
      </aside>
    </section>
  </section>
</template>

<style scoped>
.sheets-editor__top,
.sheets-editor__layout {
  display: grid;
  gap: 16px;
}

.sheets-editor__top {
  grid-template-columns: minmax(0, 1fr) auto;
  padding: 18px;
}

.sheets-editor__top h1 {
  margin: 8px 0 0;
  font-size: 24px;
  letter-spacing: -0.04em;
  text-transform: capitalize;
}

.sheets-editor__actions {
  display: flex;
  gap: 10px;
}

.sheets-editor__actions button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  background: var(--mm-card);
}

.sheets-editor__layout {
  grid-template-columns: minmax(0, 1fr) 280px;
}

.sheets-editor__grid,
.sheets-editor__side {
  display: grid;
  gap: 12px;
  padding: 18px;
}

.sheets-editor__formula {
  min-height: 42px;
  padding: 10px 12px;
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  background: var(--mm-card-muted);
}

.sheets-editor__table {
  display: grid;
  grid-template-columns: 56px repeat(5, minmax(96px, 1fr));
}

.sheets-editor__cell {
  min-height: 58px;
  padding: 10px;
  border: 1px solid var(--mm-border);
  color: var(--mm-text-secondary);
  font-size: 12px;
}

.sheets-editor__cell--head {
  background: var(--mm-card-muted);
  color: var(--mm-ink);
  font-weight: 600;
}

@media (max-width: 980px) {
  .sheets-editor__layout,
  .sheets-editor__top {
    grid-template-columns: 1fr;
  }
}
</style>
