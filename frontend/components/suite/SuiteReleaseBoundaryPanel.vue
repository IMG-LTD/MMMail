<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import {
  buildCommunityBoundarySections,
  COMMUNITY_BOUNDARY_DOC_PATHS,
  COMMUNITY_HOSTED_ONLY_ITEM_KEYS,
  COMMUNITY_SELF_HOSTED_ITEM_KEYS,
  countCommunityModulesByMaturity,
  countCommunityModulesBySurface
} from '~/utils/community-boundary'

const { t } = useI18n()

const sections = computed(() => buildCommunityBoundarySections())
const defaultNavCount = computed(() => countCommunityModulesBySurface('DEFAULT_NAV'))
const labsCount = computed(() => countCommunityModulesBySurface('LABS'))
const gaCount = computed(() => countCommunityModulesByMaturity('GA'))
const betaCount = computed(() => countCommunityModulesByMaturity('BETA'))
const previewCount = computed(() => countCommunityModulesByMaturity('PREVIEW'))
</script>

<template>
  <section class="mm-card boundary-panel">
    <div class="boundary-head">
      <div>
        <p class="eyebrow">{{ t('community.boundary.badge') }}</p>
        <h2 class="mm-section-title">{{ t('community.boundary.title') }}</h2>
        <p class="mm-muted">{{ t('community.boundary.subtitle') }}</p>
      </div>
      <div class="boundary-metrics">
        <el-tag effect="plain" type="success">{{ t('community.boundary.metrics.ga', { count: gaCount }) }}</el-tag>
        <el-tag effect="plain" type="warning">{{ t('community.boundary.metrics.beta', { count: betaCount }) }}</el-tag>
        <el-tag effect="plain" type="info">{{ t('community.boundary.metrics.preview', { count: previewCount }) }}</el-tag>
      </div>
    </div>

    <div class="boundary-grid">
      <article class="boundary-card">
        <div class="card-head">
          <div>
            <h3 class="card-title">{{ t('community.boundary.sections.community') }}</h3>
            <p class="mm-muted">
              {{ t('community.boundary.communitySummary', { defaultNav: defaultNavCount, labs: labsCount }) }}
            </p>
          </div>
        </div>
        <div class="maturity-grid">
          <section v-for="section in sections" :key="section.maturity" class="maturity-card">
            <div class="maturity-head">
              <el-tag :type="section.maturity === 'GA' ? 'success' : section.maturity === 'BETA' ? 'warning' : 'info'">
                {{ t(`community.boundary.maturity.${section.maturity}`) }}
              </el-tag>
              <span class="mm-muted">{{ t('community.boundary.metrics.modules', { count: section.modules.length }) }}</span>
            </div>
            <ul class="module-list">
              <li v-for="module in section.modules" :key="module.code">
                <strong>{{ t(module.labelKey) }}</strong>
                <span class="mm-muted">{{ module.route }} · {{ t(`community.boundary.surface.${module.surface}`) }}</span>
              </li>
            </ul>
          </section>
        </div>
      </article>

      <article class="boundary-card">
        <div class="card-head">
          <div>
            <h3 class="card-title">{{ t('community.boundary.sections.hostedOnly') }}</h3>
            <p class="mm-muted">{{ t('community.boundary.hostedOnlySummary') }}</p>
          </div>
        </div>
        <ul class="bullet-list">
          <li v-for="itemKey in COMMUNITY_HOSTED_ONLY_ITEM_KEYS" :key="itemKey">{{ t(itemKey) }}</li>
        </ul>
      </article>

      <article class="boundary-card">
        <div class="card-head">
          <div>
            <h3 class="card-title">{{ t('community.boundary.sections.selfHosted') }}</h3>
            <p class="mm-muted">{{ t('community.boundary.selfHostedSummary') }}</p>
          </div>
        </div>
        <ul class="bullet-list">
          <li v-for="itemKey in COMMUNITY_SELF_HOSTED_ITEM_KEYS" :key="itemKey">{{ t(itemKey) }}</li>
        </ul>
      </article>

      <article class="boundary-card">
        <div class="card-head">
          <div>
            <h3 class="card-title">{{ t('community.boundary.sections.docs') }}</h3>
            <p class="mm-muted">{{ t('community.boundary.docsSummary') }}</p>
          </div>
        </div>
        <ul class="doc-list">
          <li v-for="docPath in COMMUNITY_BOUNDARY_DOC_PATHS" :key="docPath">
            <code>{{ docPath }}</code>
          </li>
        </ul>
      </article>
    </div>
  </section>
</template>

<style scoped>
.boundary-panel {
  padding: 20px;
}

.boundary-head,
.card-head,
.maturity-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.boundary-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.boundary-grid,
.maturity-grid {
  display: grid;
  gap: 16px;
}

.boundary-grid {
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  margin-top: 16px;
}

.maturity-grid {
  margin-top: 12px;
}

.boundary-card,
.maturity-card {
  border: 1px solid rgba(18, 54, 58, 0.08);
  border-radius: 16px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.82);
}

.card-title {
  margin: 0;
}

.eyebrow {
  margin: 0 0 6px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #0f6e6e;
}

.module-list,
.bullet-list,
.doc-list {
  margin: 12px 0 0;
  padding-left: 18px;
  display: grid;
  gap: 8px;
}

.module-list li,
.bullet-list li,
.doc-list li {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
</style>
