<script setup lang="ts">
import type { OrgWorkspace } from '~/types/api'
import type { PassMonitorOverview, PassWorkspaceMode } from '~/types/pass-business'
import type { PassMonitorMetricCard } from '~/utils/pass-monitor'
import { useI18n } from '~/composables/useI18n'

const props = defineProps<{
  workspaceMode: PassWorkspaceMode
  organizations: OrgWorkspace[]
  selectedOrgId: string
  overview: PassMonitorOverview | null
  metricCards: PassMonitorMetricCard[]
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:workspaceMode': [mode: PassWorkspaceMode]
  'update:selectedOrgId': [orgId: string]
  refresh: []
}>()

const { t } = useI18n()

const roleLabel = computed(() => {
  if (!props.overview?.currentRole) {
    return null
  }
  return t('pass.monitor.hero.role', { value: t(`pass.monitor.role.${props.overview.currentRole}`) })
})

const generatedAtLabel = computed(() => {
  if (!props.overview?.generatedAt) {
    return null
  }
  return t('pass.monitor.hero.generatedAt', { value: props.overview.generatedAt })
})
</script>

<template>
  <section class="monitor-hero">
    <div class="hero-copy">
      <p class="hero-eyebrow">{{ t('pass.monitor.hero.eyebrow') }}</p>
      <h1>{{ t('pass.monitor.hero.title') }}</h1>
      <p class="hero-description">{{ t('pass.monitor.hero.description') }}</p>
      <div class="hero-toolbar">
        <el-segmented
          :model-value="workspaceMode"
          :options="[
            { label: t('pass.monitor.hero.personal'), value: 'PERSONAL' },
            { label: t('pass.monitor.hero.shared'), value: 'SHARED' }
          ]"
          @update:model-value="emit('update:workspaceMode', $event as PassWorkspaceMode)"
        />
        <el-select
          v-if="workspaceMode === 'SHARED'"
          :model-value="selectedOrgId"
          :placeholder="t('pass.monitor.hero.orgPlaceholder')"
          class="org-select"
          @change="emit('update:selectedOrgId', $event)"
        >
          <el-option v-for="org in organizations" :key="org.id" :label="org.name" :value="org.id" />
        </el-select>
        <el-button :loading="loading" @click="emit('refresh')">
          {{ t('pass.monitor.hero.refresh') }}
        </el-button>
      </div>
      <div class="hero-meta">
        <span v-if="generatedAtLabel" class="hero-pill">{{ generatedAtLabel }}</span>
        <span v-if="roleLabel" class="hero-pill">{{ roleLabel }}</span>
      </div>
    </div>

    <div class="hero-metrics">
      <article v-for="card in metricCards" :key="card.key" class="metric-card">
        <span class="metric-label">{{ card.label }}</span>
        <strong class="metric-value">{{ card.value }}</strong>
        <span class="metric-hint">{{ card.hint }}</span>
      </article>
    </div>
  </section>
</template>

<style scoped>
.monitor-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 1fr);
  gap: 20px;
  padding: 24px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(110, 97, 255, 0.26), transparent 34%),
    radial-gradient(circle at bottom right, rgba(87, 225, 193, 0.18), transparent 38%),
    linear-gradient(145deg, #0e1731 0%, #162347 56%, #10263b 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 28px 80px rgba(7, 13, 28, 0.32);
  color: #f7fbff;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero-eyebrow,
.metric-label {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.16em;
  font-size: 12px;
  color: rgba(181, 232, 255, 0.76);
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(32px, 4vw, 52px);
  line-height: 1;
}

.hero-description,
.metric-hint {
  color: rgba(232, 240, 255, 0.74);
}

.hero-toolbar,
.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-pill {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: rgba(247, 251, 255, 0.82);
  font-size: 12px;
}

.org-select {
  min-width: 220px;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.metric-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 132px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(10, 18, 35, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.metric-value {
  font-size: 32px;
  color: #ffffff;
}

:deep(.el-segmented) {
  --el-segmented-item-selected-bg-color: rgba(110, 97, 255, 0.28);
  --el-segmented-item-selected-color: #ffffff;
  --el-border-radius-base: 999px;
}

@media (max-width: 1100px) {
  .monitor-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
