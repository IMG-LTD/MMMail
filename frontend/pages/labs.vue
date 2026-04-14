<script setup lang="ts">
import { computed } from 'vue'
import {
  COMMUNITY_V1_CURATED_LABS_MODULES,
  COMMUNITY_V1_DEFERRED_LABS_MODULES,
  type CommunityLabsCatalogItem,
  type ModuleMaturity
} from '~/constants/module-maturity'
import { countPreviewRegistryByStrategy, type PreviewRegistryStrategy } from '~/constants/preview-registry'
import { useI18n } from '~/composables/useI18n'

definePageMeta({ layout: 'default' })

const { t } = useI18n()
const runtimeConfig = useRuntimeConfig()

const labsModulesEnabled = computed(() => runtimeConfig.public.enablePreviewModules === true)
const curatedLabsModules = computed(() => COMMUNITY_V1_CURATED_LABS_MODULES)
const deferredLabsModules = computed(() => COMMUNITY_V1_DEFERRED_LABS_MODULES)
const strategyCounts = computed(() => countPreviewRegistryByStrategy())

function resolveMaturityTagType(maturity: ModuleMaturity): 'success' | 'warning' | 'info' {
  if (maturity === 'GA') return 'success'
  if (maturity === 'BETA') return 'warning'
  return 'info'
}

function resolveStrategyTagType(strategy: PreviewRegistryStrategy): 'success' | 'warning' | 'info' {
  if (strategy === 'PLUGIN') return 'success'
  if (strategy === 'EXTERNALIZED') return 'info'
  return 'warning'
}

function resolveCatalogTagType(curatedDefault: boolean): 'success' | 'info' {
  return curatedDefault ? 'success' : 'info'
}

function strategyHintKey(item: CommunityLabsCatalogItem): string {
  return `labs.strategyHint.${item.previewStrategy}`
}

useHead(() => ({
  title: t('page.labs.title')
}))
</script>

<template>
  <div class="mm-page">
    <section class="labs-shell">
      <section class="mm-card labs-hero">
        <p class="eyebrow">{{ t('labs.hero.badge') }}</p>
        <h1>{{ t('labs.hero.title') }}</h1>
        <p class="mm-muted">{{ t('labs.hero.description') }}</p>
        <el-alert
          :closable="false"
          type="info"
          data-testid="labs-secondary-note"
          :title="t('labs.secondary.title')"
          :description="t('labs.secondary.description')"
        />
        <el-tag :type="labsModulesEnabled ? 'warning' : 'info'" effect="dark">
          {{ labsModulesEnabled ? t('labs.flag.enabled') : t('labs.flag.disabled') }}
        </el-tag>
        <div class="labs-hero__metrics">
          <el-tag type="success" effect="plain">{{ t('labs.strategy.PLUGIN') }} · {{ strategyCounts.PLUGIN }}</el-tag>
          <el-tag type="info" effect="plain">{{ t('labs.strategy.EXTERNALIZED') }} · {{ strategyCounts.EXTERNALIZED }}</el-tag>
          <el-tag type="warning" effect="plain">{{ t('labs.strategy.EXPERIMENT') }} · {{ strategyCounts.EXPERIMENT }}</el-tag>
        </div>
      </section>

      <section v-if="curatedLabsModules.length" class="labs-section">
        <header class="labs-section__header">
          <h2>{{ t('labs.section.curated.title') }}</h2>
          <p class="mm-muted">{{ t('labs.section.curated.description') }}</p>
        </header>
        <div class="labs-grid">
          <article v-for="module in curatedLabsModules" :key="module.code" class="mm-card labs-card">
            <div class="labs-card__head">
              <div>
                <h2>{{ t(module.labelKey) }}</h2>
                <p class="mm-muted">{{ t('labs.card.description', { name: t(module.labelKey) }) }}</p>
              </div>
              <el-tag :type="resolveMaturityTagType(module.maturity)" effect="plain">
                {{ t(`labs.maturity.${module.maturity}`) }}
              </el-tag>
            </div>
            <div class="labs-card__tags">
              <el-tag :type="resolveCatalogTagType(module.curatedDefault)" effect="dark">
                {{ t('labs.catalog.curated') }}
              </el-tag>
              <el-tag :type="resolveStrategyTagType(module.previewStrategy)" effect="plain">
                {{ t(`labs.strategy.${module.previewStrategy}`) }}
              </el-tag>
            </div>
            <dl class="labs-meta">
              <div>
                <dt>{{ t('labs.card.route') }}</dt>
                <dd><code>{{ module.route }}</code></dd>
              </div>
              <div>
                <dt>{{ t('labs.card.flag') }}</dt>
                <dd>{{ labsModulesEnabled ? t('labs.flag.enabled') : t('labs.flag.disabled') }}</dd>
              </div>
              <div>
                <dt>{{ t('labs.card.strategy') }}</dt>
                <dd>{{ t(strategyHintKey(module)) }}</dd>
              </div>
            </dl>
          </article>
        </div>
      </section>

      <section v-if="deferredLabsModules.length" class="labs-section">
        <header class="labs-section__header">
          <h2>{{ t('labs.section.registry.title') }}</h2>
          <p class="mm-muted">{{ t('labs.section.registry.description') }}</p>
        </header>
        <div class="labs-grid">
          <article v-for="module in deferredLabsModules" :key="module.code" class="mm-card labs-card labs-card--deferred">
            <div class="labs-card__head">
              <div>
                <h2>{{ t(module.labelKey) }}</h2>
                <p class="mm-muted">{{ t('labs.card.description', { name: t(module.labelKey) }) }}</p>
              </div>
              <el-tag :type="resolveMaturityTagType(module.maturity)" effect="plain">
                {{ t(`labs.maturity.${module.maturity}`) }}
              </el-tag>
            </div>
            <div class="labs-card__tags">
              <el-tag :type="resolveCatalogTagType(module.curatedDefault)" effect="plain">
                {{ t('labs.catalog.deferred') }}
              </el-tag>
              <el-tag :type="resolveStrategyTagType(module.previewStrategy)" effect="plain">
                {{ t(`labs.strategy.${module.previewStrategy}`) }}
              </el-tag>
            </div>
            <dl class="labs-meta">
              <div>
                <dt>{{ t('labs.card.route') }}</dt>
                <dd><code>{{ module.route }}</code></dd>
              </div>
              <div>
                <dt>{{ t('labs.card.strategy') }}</dt>
                <dd>{{ t(strategyHintKey(module)) }}</dd>
              </div>
            </dl>
          </article>
        </div>
      </section>

      <section v-if="!curatedLabsModules.length && !deferredLabsModules.length" class="mm-card">
        <el-empty :description="t('labs.empty')" :image-size="72" />
      </section>
    </section>
  </div>
</template>

<style scoped>
.labs-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.labs-hero,
.labs-card,
.labs-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.labs-hero__metrics,
.labs-card__tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.labs-section__header h2 {
  margin: 0 0 6px;
}

.eyebrow {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #0f6e6e;
}

.labs-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.labs-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.labs-card__head h2 {
  margin: 0 0 6px;
}

.labs-card--deferred {
  border-style: dashed;
}

.labs-meta {
  display: grid;
  gap: 12px;
  margin: 0;
}

.labs-meta dt {
  font-size: 12px;
  color: #5d7277;
}

.labs-meta dd {
  margin: 4px 0 0;
  color: #12363a;
}
</style>
