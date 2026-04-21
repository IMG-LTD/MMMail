<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { lt, useLocaleText } from '@/locales'
import { findSurface, suiteSections } from '@/shared/content/route-surfaces'

const route = useRoute()
const router = useRouter()
const { tr } = useLocaleText()

const current = computed(() => findSurface(suiteSections, String(route.meta.surfaceKey ?? 'overview'), 'overview'))

function openSection(key: string) {
  const pathMap: Record<string, string> = {
    overview: '/suite',
    plans: '/suite/plans',
    billing: '/suite/billing',
    operations: '/suite/operations',
    boundary: '/suite/boundary'
  }
  router.push(pathMap[key] ?? '/suite')
}

const cards = [
  [lt('邮件', '郵件', 'Mail'), '8 GB / 10 GB'],
  [lt('日历', '日曆', 'Calendar'), lt('4 场会议', '4 場會議', '4 meetings')],
  [lt('云盘', '雲端硬碟', 'Drive'), lt('62% 配额', '62% 配額', '62% quota')],
  [lt('密码库', '密碼庫', 'Pass'), lt('3 条告警', '3 則告警', '3 alerts')]
]
</script>

<template>
  <section class="page-shell surface-grid suite-section">
    <header class="surface-card suite-section__hero">
      <div>
        <span class="section-label">{{ tr(lt('套件', '套件', 'Suite')) }}</span>
        <h1>{{ tr(current.label) }}</h1>
        <p class="page-subtitle">{{ tr(current.description) }}</p>
      </div>
      <nav class="suite-section__nav">
        <button
          v-for="item in suiteSections"
          :key="item.key"
          type="button"
          :class="{ 'suite-section__nav--active': item.key === current.key }"
          @click="openSection(item.key)"
        >
          {{ tr(item.label) }}
        </button>
      </nav>
    </header>

    <article class="surface-card suite-section__main">
      <span class="section-label">{{ tr(lt('当前焦点', '目前焦點', 'Current focus')) }}</span>
      <strong>{{ `${tr(current.label)} ${tr(lt('界面', '介面', 'surface'))}` }}</strong>
      <p class="page-subtitle">{{ tr(lt('Suite 壳层会为当前焦点保留专属 Hero、产品卡片和分区面板。', 'Suite 殼層會為目前焦點保留專屬 Hero、產品卡片與分區面板。', 'The suite shell keeps a dedicated hero, product cards, and a section-specific panel for the active focus area.')) }}</p>
    </article>

    <div class="suite-section__cards">
      <article v-for="([label, value], index) in cards" :key="index" class="surface-card suite-section__card">
        <span class="section-label">{{ tr(label) }}</span>
        <strong>{{ tr(value) }}</strong>
      </article>
    </div>
  </section>
</template>

<style scoped>
.suite-section__hero,
.suite-section__cards {
  display: grid;
  gap: 16px;
}

.suite-section__hero {
  grid-template-columns: minmax(0, 1fr) auto;
  padding: 18px;
}

.suite-section__hero h1 {
  margin: 8px 0 0;
  font-size: 28px;
  letter-spacing: -0.04em;
}

.suite-section__nav {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.suite-section__nav button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--mm-border);
  border-radius: 999px;
  background: var(--mm-card);
}

.suite-section__nav--active {
  border-color: var(--mm-accent-border) !important;
  background: var(--mm-accent-soft) !important;
  color: var(--mm-primary);
}

.suite-section__main,
.suite-section__card {
  display: grid;
  gap: 10px;
  padding: 18px;
}

.suite-section__cards {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

@media (max-width: 980px) {
  .suite-section__hero,
  .suite-section__cards {
    grid-template-columns: 1fr;
  }
}
</style>
