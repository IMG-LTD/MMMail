<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { LumoWorkspaceMetrics } from '~/utils/lumo-workspace'

interface Props {
  includeArchived: boolean
  summary: LumoWorkspaceMetrics
}

const props = defineProps<Props>()
const emit = defineEmits<{
  refresh: []
  updateIncludeArchived: [value: boolean]
}>()
const { t } = useI18n()

const privacyPills = computed(() => [
  t('lumo.hero.privacy.zeroAccess'),
  t('lumo.hero.privacy.noTraining'),
  t('lumo.hero.privacy.projectScoped')
])

const capabilityPills = computed(() => [
  t('lumo.capability.webSearch'),
  t('lumo.translate.title'),
  t('lumo.capability.citations')
])

function onArchivedChange(value: string | number | boolean): void {
  emit('updateIncludeArchived', Boolean(value))
}
</script>

<template>
  <section class="mm-card lumo-hero">
    <div class="lumo-hero-main">
      <div class="hero-copy">
        <span class="hero-eyebrow">{{ t('lumo.hero.badge') }}</span>
        <h1 class="hero-title">{{ t('page.lumo.title') }}</h1>
        <p class="hero-subtitle">{{ t('lumo.hero.subtitle') }}</p>
        <div class="hero-pill-row">
          <el-tag
            v-for="pill in privacyPills"
            :key="pill"
            class="hero-pill"
            effect="dark"
            round
          >
            {{ pill }}
          </el-tag>
        </div>
        <div class="hero-pill-row capability-pill-row">
          <el-tag
            v-for="pill in capabilityPills"
            :key="pill"
            class="hero-capability-pill"
            effect="plain"
            round
          >
            {{ pill }}
          </el-tag>
        </div>
      </div>

      <div class="hero-actions">
        <el-switch
          :model-value="props.includeArchived"
          :active-text="t('lumo.hero.includeArchived')"
          @change="onArchivedChange"
        />
        <el-button @click="emit('refresh')">
          {{ t('common.actions.refresh') }}
        </el-button>
      </div>
    </div>

    <div class="hero-metrics">
      <article class="metric-card">
        <span class="metric-label">{{ t('lumo.hero.metrics.projects') }}</span>
        <strong class="metric-value">{{ props.summary.projectCount }}</strong>
      </article>
      <article class="metric-card">
        <span class="metric-label">{{ t('lumo.hero.metrics.liveConversations') }}</span>
        <strong class="metric-value">{{ props.summary.liveConversationCount }}</strong>
      </article>
      <article class="metric-card">
        <span class="metric-label">{{ t('lumo.hero.metrics.archivedConversations') }}</span>
        <strong class="metric-value">{{ props.summary.archivedConversationCount }}</strong>
      </article>
      <article class="metric-card">
        <span class="metric-label">{{ t('lumo.hero.metrics.knowledge') }}</span>
        <strong class="metric-value">{{ props.summary.knowledgeCount }}</strong>
      </article>
    </div>
  </section>
</template>

<style scoped>
.lumo-hero {
  padding: 24px;
  border: 1px solid rgba(82, 101, 255, 0.18);
  background:
    radial-gradient(circle at top left, rgba(131, 105, 255, 0.18), transparent 42%),
    radial-gradient(circle at bottom right, rgba(44, 189, 255, 0.14), transparent 36%),
    linear-gradient(135deg, rgba(245, 247, 255, 0.98), rgba(255, 255, 255, 0.96));
  box-shadow: 0 24px 60px rgba(38, 57, 122, 0.12);
}

.lumo-hero-main {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 720px;
}

.hero-eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #5c56d8;
}

.hero-title {
  margin: 0;
  font-size: clamp(2rem, 3vw, 2.8rem);
  line-height: 1.06;
  color: #18213d;
}

.hero-subtitle {
  margin: 0;
  max-width: 60ch;
  font-size: 15px;
  line-height: 1.65;
  color: #57627d;
}

.hero-pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-pill {
  border: none;
  background: rgba(28, 41, 77, 0.92);
}

.capability-pill-row {
  margin-top: 4px;
}

.hero-capability-pill {
  border-color: rgba(76, 89, 184, 0.16);
  color: #40507e;
  background: rgba(255, 255, 255, 0.82);
}

.hero-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.hero-metrics {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  border-radius: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(95, 107, 170, 0.12);
  backdrop-filter: blur(10px);
}

.metric-label {
  display: block;
  font-size: 12px;
  color: #6f7894;
}

.metric-value {
  display: block;
  margin-top: 10px;
  font-size: 28px;
  line-height: 1;
  color: #1e2745;
}

@media (max-width: 1080px) {
  .hero-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .lumo-hero-main {
    flex-direction: column;
  }

  .hero-actions {
    width: 100%;
    align-items: flex-start;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
