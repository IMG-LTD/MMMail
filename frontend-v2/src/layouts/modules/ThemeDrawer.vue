<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { NDrawer, NDrawerContent } from 'naive-ui'
import { lt, useLocaleText } from '@/locales'
import { densityModes, radiusOptions, themePresets, themeSchemes } from '@/theme/settings'
import { useThemeStore } from '@/store/modules/theme'

const themeStore = useThemeStore()
const { density, drawerOpen, radius, resolvedScheme, themePreset, themeScheme } = storeToRefs(themeStore)
const { tr } = useLocaleText()

const previewMetrics = [
  [lt('壳层', '殼層', 'Shell'), lt('共享顶栏、侧栏和内容区一起变化。', '共享頂欄、側欄和內容區一起變化。', 'Shared header, side nav, and content surfaces')],
  [lt('密度', '密度', 'Density'), lt('邮件、云盘和密码库工作区即时响应。', '郵件、雲端硬碟和密碼庫工作區即時回應。', 'Mail, Drive, and Pass workspaces respond immediately')],
  [lt('圆角', '圓角', 'Radius'), lt('卡片、抽屉和控件边界保持同步。', '卡片、抽屜和控制項邊界保持同步。', 'Cards, drawers, and control borders stay synchronized')]
]

function handleDrawerVisibility(value: boolean) {
  themeStore.setDrawerOpen(value)
}
</script>

<template>
  <n-drawer :show="drawerOpen" :width="384" placement="right" @update:show="handleDrawerVisibility">
    <n-drawer-content :title="tr(lt('主题设置', '主題設定', 'Theme settings'))" closable>
      <div class="theme-drawer">
        <section class="theme-drawer__section">
          <span class="section-label">{{ tr(lt('模式', '模式', 'Mode')) }}</span>
          <div class="theme-drawer__option-grid">
            <button
              v-for="option in themeSchemes"
              :key="option.value"
              type="button"
              class="theme-drawer__option"
              :class="{ 'theme-drawer__option--active': themeScheme === option.value }"
              @click="themeStore.setThemeScheme(option.value)"
            >
              {{ tr(option.label) }}
            </button>
          </div>
        </section>

        <section class="theme-drawer__section">
          <span class="section-label">{{ tr(lt('预设', '預設', 'Preset')) }}</span>
          <div class="theme-drawer__preset-grid">
            <button
              v-for="preset in themePresets"
              :key="preset.id"
              type="button"
              class="theme-drawer__preset"
              :class="{ 'theme-drawer__preset--active': themePreset === preset.id }"
              @click="themeStore.setThemePreset(preset.id)"
            >
              <span class="theme-drawer__swatch" :style="{ background: preset.accent }" />
              <strong>{{ tr(preset.label) }}</strong>
              <span>{{ tr(preset.description) }}</span>
            </button>
          </div>
        </section>

        <section class="theme-drawer__section">
          <span class="section-label">{{ tr(lt('密度', '密度', 'Density')) }}</span>
          <div class="theme-drawer__option-grid">
            <button
              v-for="option in densityModes"
              :key="option.value"
              type="button"
              class="theme-drawer__option"
              :class="{ 'theme-drawer__option--active': density === option.value }"
              @click="themeStore.setDensity(option.value)"
            >
              {{ tr(option.label) }}
            </button>
          </div>
        </section>

        <section class="theme-drawer__section">
          <span class="section-label">{{ tr(lt('圆角', '圓角', 'Corner radius')) }}</span>
          <div class="theme-drawer__option-grid">
            <button
              v-for="value in radiusOptions"
              :key="value"
              type="button"
              class="theme-drawer__option"
              :class="{ 'theme-drawer__option--active': radius === value }"
              @click="themeStore.setRadius(value)"
            >
              {{ value }} px
            </button>
          </div>
        </section>

        <article class="theme-drawer__preview surface-card">
          <div class="theme-drawer__preview-head">
            <div>
              <span class="section-label">{{ tr(lt('预览', '預覽', 'Preview')) }}</span>
              <strong>{{ resolvedScheme === 'dark' ? tr(lt('深色壳层', '深色殼層', 'Dark shell')) : tr(lt('浅色壳层', '淺色殼層', 'Light shell')) }}</strong>
            </div>
            <span class="theme-drawer__preview-badge">{{ tr(densityModes.find(option => option.value === density)?.label ?? density) }}</span>
          </div>

          <div class="theme-drawer__preview-strip">
            <span class="theme-drawer__preview-rail" />
            <div class="theme-drawer__preview-card">
              <span class="theme-drawer__preview-pill">{{ tr(lt('已加密', '已加密', 'Encrypted')) }}</span>
              <span class="theme-drawer__preview-line" />
              <span class="theme-drawer__preview-line theme-drawer__preview-line--short" />
            </div>
          </div>

          <div class="theme-drawer__preview-metrics">
            <div v-for="([label, meta], index) in previewMetrics" :key="index">
              <strong>{{ tr(label) }}</strong>
              <span>{{ tr(meta) }}</span>
            </div>
          </div>
        </article>
      </div>
    </n-drawer-content>
  </n-drawer>
</template>

<style scoped>
.theme-drawer {
  display: grid;
  gap: 20px;
}

.theme-drawer__section {
  display: grid;
  gap: 10px;
}

.theme-drawer__option-grid,
.theme-drawer__preset-grid {
  display: grid;
  gap: 10px;
}

.theme-drawer__option-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.theme-drawer__option,
.theme-drawer__preset {
  padding: 12px 14px;
  border: 1px solid var(--mm-border);
  border-radius: var(--mm-radius);
  background: var(--mm-card);
  color: var(--mm-text-secondary);
  text-align: left;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease,
    color 0.18s ease,
    transform 0.18s ease;
}

.theme-drawer__option--active,
.theme-drawer__preset--active {
  border-color: var(--mm-accent-border);
  background: var(--mm-accent-soft);
  color: var(--mm-primary);
  transform: translateY(-1px);
}

.theme-drawer__preset {
  display: grid;
  gap: 8px;
}

.theme-drawer__preset strong,
.theme-drawer__preview strong,
.theme-drawer__preview-metrics strong {
  color: var(--mm-ink);
}

.theme-drawer__preset span:last-child,
.theme-drawer__preview-metrics span {
  color: var(--mm-text-secondary);
  font-size: 12px;
  line-height: 1.55;
}

.theme-drawer__swatch {
  width: 28px;
  height: 28px;
  border-radius: 10px;
}

.theme-drawer__preview {
  display: grid;
  gap: 16px;
  padding: 18px;
}

.theme-drawer__preview-head {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.theme-drawer__preview-badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--mm-card-muted);
  border: 1px solid var(--mm-border);
  color: var(--mm-text-secondary);
  font-size: 11px;
  text-transform: none;
}

.theme-drawer__preview-strip {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 12px;
}

.theme-drawer__preview-rail,
.theme-drawer__preview-card {
  border-radius: var(--mm-radius);
}

.theme-drawer__preview-rail {
  background: linear-gradient(180deg, var(--mm-card-muted) 0%, var(--mm-side-surface) 100%);
  border: 1px solid var(--mm-border);
}

.theme-drawer__preview-card {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--mm-border);
  background: var(--mm-card-muted);
}

.theme-drawer__preview-pill {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  width: fit-content;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--mm-accent-soft);
  color: var(--mm-primary);
  font-size: 11px;
  font-weight: 600;
}

.theme-drawer__preview-line {
  height: 8px;
  border-radius: 999px;
  background: var(--mm-border);
}

.theme-drawer__preview-line--short {
  width: 64%;
}

.theme-drawer__preview-metrics {
  display: grid;
  gap: 12px;
}

.theme-drawer__preview-metrics div {
  display: grid;
  gap: 4px;
}
</style>
