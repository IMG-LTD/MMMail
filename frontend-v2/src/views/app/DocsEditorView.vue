<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { lt, useLocaleText } from '@/locales'

const route = useRoute()
const { tr } = useLocaleText()

const title = computed(() => {
  return String(route.params.id ?? 'quarterly-security-charter').replace(/-/g, ' ')
})
</script>

<template>
  <section class="page-shell surface-grid docs-editor">
    <article class="surface-card docs-editor__top">
      <div>
        <span class="section-label">{{ tr(lt('文档编辑器', '文件編輯器', 'Docs editor')) }}</span>
        <h1>{{ title }}</h1>
        <p class="page-subtitle">{{ tr(lt('块级编辑、提纲导航、协作者上下文和 Beta 指引都保持可见，但不会夸大能力对等。', '區塊編輯、大綱導覽、協作者內容脈絡與 Beta 指引都保持可見，但不會誇大能力對等。', 'Block editing, outline navigation, collaborator context, and beta guidance remain visible without overstating parity.')) }}</p>
      </div>
      <div class="docs-editor__actions">
        <button type="button">{{ tr(lt('共享', '共享', 'Share')) }}</button>
        <button type="button">{{ tr(lt('导出', '匯出', 'Export')) }}</button>
      </div>
    </article>

    <section class="docs-editor__layout">
      <aside class="surface-card docs-editor__panel">
        <span class="section-label">{{ tr(lt('提纲', '大綱', 'Outline')) }}</span>
        <button type="button">{{ tr(lt('执行摘要', '執行摘要', 'Executive summary')) }}</button>
        <button type="button">{{ tr(lt('安全态势', '安全態勢', 'Security posture')) }}</button>
        <button type="button">{{ tr(lt('恢复清单', '復原清單', 'Recovery checklist')) }}</button>
      </aside>
      <article class="surface-card docs-editor__canvas">
        <p>{{ tr(lt('/ 命令可用', '/ 命令可用', '/ command available')) }}</p>
        <h2>{{ tr(lt('恢复章程', '復原章程', 'Recovery charter')) }}</h2>
        <p>{{ tr(lt('轻量编辑器会在克制的 Beta 外壳中保留层级、提示块、嵌入与云盘引用。', '輕量編輯器會在克制的 Beta 外殼中保留層級、提示塊、嵌入與雲端硬碟引用。', 'The lightweight editor keeps hierarchy, callouts, embeds, and Drive references within a restrained beta shell.')) }}</p>
        <blockquote>{{ tr(lt('云盘嵌入：凤凰项目发布表', '雲端硬碟嵌入：鳳凰專案發佈表', 'Drive embed: Phoenix rollout spreadsheet')) }}</blockquote>
        <pre>```mermaid\nflowchart LR\n  Mail --> Drive --> Calendar\n```</pre>
      </article>
      <aside class="surface-card docs-editor__panel">
        <span class="section-label">{{ tr(lt('评论', '評論', 'Comments')) }}</span>
        <strong>{{ tr(lt('2 条未解决评论', '2 則未解決評論', '2 open comments')) }}</strong>
        <p class="page-subtitle">{{ tr(lt('版本历史与协作者动态会以情境化 Beta 面板形式提供。', '版本歷史與協作者動態會以情境化 Beta 面板形式提供。', 'Version history and collaborator activity stay available as contextual beta panels.')) }}</p>
      </aside>
    </section>
  </section>
</template>

<style scoped>
.docs-editor__top,
.docs-editor__layout {
  display: grid;
  gap: 16px;
}

.docs-editor__top {
  grid-template-columns: minmax(0, 1fr) auto;
  padding: 18px;
}

.docs-editor__top h1 {
  margin: 8px 0 0;
  font-size: 24px;
  letter-spacing: -0.04em;
  text-transform: capitalize;
}

.docs-editor__actions {
  display: flex;
  gap: 10px;
}

.docs-editor__actions button,
.docs-editor__panel button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 10px;
  background: var(--mm-card);
}

.docs-editor__layout {
  grid-template-columns: 220px minmax(0, 1fr) 280px;
}

.docs-editor__panel,
.docs-editor__canvas {
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 18px;
}

.docs-editor__canvas h2,
.docs-editor__canvas p,
.docs-editor__canvas blockquote,
.docs-editor__canvas pre {
  margin: 0;
}

.docs-editor__canvas blockquote,
.docs-editor__canvas pre {
  padding: 14px;
  border-radius: 12px;
  background: var(--mm-card-muted);
}

@media (max-width: 980px) {
  .docs-editor__layout,
  .docs-editor__top {
    grid-template-columns: 1fr;
  }
}
</style>
