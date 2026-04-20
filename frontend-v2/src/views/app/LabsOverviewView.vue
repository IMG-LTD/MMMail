<script setup lang="ts">
import CompactPageHeader from '@/shared/components/CompactPageHeader.vue'
import { lt, useLocaleText } from '@/locales'

const { tr } = useLocaleText()

const curated = ['Authenticator', 'SimpleLogin', 'Standard Notes']
const hidden = ['VPN', 'Meet', 'Wallet', 'Lumo']
</script>

<template>
  <section class="page-shell surface-grid">
    <compact-page-header
      :eyebrow="lt('Labs', 'Labs', 'Labs')"
      :title="lt('实验模块', '實驗模組', 'Experimental modules')"
      :description="lt('实验能力可以被看见，但必须在视觉上从属于主功能，并明确标记为不稳定。', '實驗能力可以被看見，但必須在視覺上從屬於主要功能，並明確標記為不穩定。', 'Experimental capabilities are visible, but they stay visually subordinate and clearly marked as unstable.')"
      :badge="lt('预览', '預覽', 'Preview')"
      badge-tone="preview"
    />

    <article class="surface-card labs-banner">
      <strong>{{ tr(lt('实验能力不保证稳定性或兼容性。', '實驗能力不保證穩定性或相容性。', 'Experimental capabilities do not guarantee stability or compatibility.')) }}</strong>
      <p class="page-subtitle">{{ tr(lt('Authenticator、SimpleLogin 和 Standard Notes 保持精选显示。其他预览能力在被明确展开前仍保持隐藏。', 'Authenticator、SimpleLogin 與 Standard Notes 保持精選顯示。其他預覽能力在被明確展開前仍保持隱藏。', 'Authenticator, SimpleLogin, and Standard Notes stay curated. Other previews remain hidden until explicitly expanded.')) }}</p>
    </article>

    <div class="labs-grid">
      <article class="surface-card labs-card">
        <span class="section-label">{{ tr(lt('精选', '精選', 'Curated')) }}</span>
        <div class="labs-card__stack">
          <span v-for="item in curated" :key="item" class="metric-chip">{{ item }}</span>
        </div>
      </article>
      <article class="surface-card labs-card">
        <span class="section-label">{{ tr(lt('显示全部实验', '顯示全部實驗', 'Show all experiments')) }}</span>
        <div class="labs-card__stack labs-card__stack--muted">
          <span v-for="item in hidden" :key="item" class="metric-chip">{{ item }}</span>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.labs-banner,
.labs-card {
  padding: 20px;
}

.labs-banner strong {
  display: block;
  margin-bottom: 8px;
}

.labs-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.labs-card__stack {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.labs-card__stack--muted :deep(.metric-chip) {
  opacity: 0.76;
}

@media (max-width: 820px) {
  .labs-grid {
    grid-template-columns: 1fr;
  }
}
</style>
