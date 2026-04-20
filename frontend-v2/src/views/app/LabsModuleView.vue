<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { lt, useLocaleText } from '@/locales'
import { findSurface, labsModules } from '@/shared/content/route-surfaces'

const route = useRoute()
const { tr } = useLocaleText()
const current = computed(() => findSurface(labsModules, String(route.params.moduleKey ?? 'authenticator'), 'authenticator'))
</script>

<template>
  <section class="page-shell surface-grid labs-module">
    <article class="surface-card labs-module__hero">
      <span class="section-label">{{ tr(lt('预览模块', '預覽模組', 'Preview module')) }}</span>
      <h1>{{ tr(current.label) }}</h1>
      <p class="page-subtitle">{{ tr(current.description) }}</p>
      <div class="labs-module__actions">
        <span class="metric-chip">{{ tr(current.badge ?? lt('预览', '預覽', 'Preview')) }}</span>
        <span class="metric-chip">{{ tr(lt('已启用反馈', '已啟用回饋', 'Feedback enabled')) }}</span>
      </div>
    </article>
    <article class="surface-card labs-module__panel">
      <span class="section-label">{{ tr(lt('当前范围', '目前範圍', 'Current scope')) }}</span>
      <strong>{{ tr(lt('模块占位外壳', '模組佔位外殼', 'Module placeholder shell')) }}</strong>
      <p class="page-subtitle">{{ tr(lt('这个预览界面刻意保持轻量：一个能力预告、一条成熟度声明，以及一个反馈入口。', '這個預覽介面刻意保持輕量：一個能力預告、一條成熟度聲明，以及一個回饋入口。', 'This preview surface deliberately stays light: one capability teaser, one maturity statement, and one feedback path.')) }}</p>
    </article>
  </section>
</template>

<style scoped>
.labs-module__hero,
.labs-module__panel {
  display: grid;
  gap: 12px;
  padding: 20px;
}

.labs-module__hero h1 {
  margin: 0;
  font-size: 28px;
  letter-spacing: -0.04em;
}

.labs-module__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
