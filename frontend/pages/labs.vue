<script setup lang="ts">
import { computed } from 'vue'
import { COMMUNITY_V1_PREVIEW_MODULES } from '~/constants/module-maturity'
import { useI18n } from '~/composables/useI18n'

definePageMeta({ layout: 'default' })

const { t } = useI18n()
const runtimeConfig = useRuntimeConfig()

const previewModulesEnabled = computed(() => runtimeConfig.public.enablePreviewModules === true)
const previewModules = computed(() => COMMUNITY_V1_PREVIEW_MODULES)

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
        <el-tag :type="previewModulesEnabled ? 'warning' : 'info'" effect="dark">
          {{ previewModulesEnabled ? t('labs.flag.enabled') : t('labs.flag.disabled') }}
        </el-tag>
      </section>

      <section v-if="previewModules.length" class="labs-grid">
        <article v-for="module in previewModules" :key="module.code" class="mm-card labs-card">
          <div class="labs-card__head">
            <div>
              <h2>{{ t(module.labelKey) }}</h2>
              <p class="mm-muted">{{ t('labs.card.description', { name: t(module.labelKey) }) }}</p>
            </div>
            <el-tag type="info" effect="plain">{{ t('labs.maturity.PREVIEW') }}</el-tag>
          </div>
          <dl class="labs-meta">
            <div>
              <dt>{{ t('labs.card.route') }}</dt>
              <dd><code>{{ module.route }}</code></dd>
            </div>
            <div>
              <dt>{{ t('labs.card.flag') }}</dt>
              <dd>{{ previewModulesEnabled ? t('labs.flag.enabled') : t('labs.flag.disabled') }}</dd>
            </div>
          </dl>
        </article>
      </section>

      <section v-else class="mm-card">
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
.labs-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
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
