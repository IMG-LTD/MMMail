<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { OrgWorkspace } from '~/types/api'
import type { PassBusinessOverview, PassWorkspaceMode } from '~/types/pass-business'
import { formatPassTime } from '~/utils/pass'

const props = defineProps<{
  workspaceMode: PassWorkspaceMode
  organizations: OrgWorkspace[]
  selectedOrgId: string
  overview: PassBusinessOverview | null
  selectedVaultName?: string
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:workspaceMode': [mode: PassWorkspaceMode]
  'update:selectedOrgId': [orgId: string]
  refresh: []
}>()
const { t } = useI18n()

const metricCards = computed(() => {
  if (!props.overview) {
    return [
      { label: t('pass.hero.metrics.sharedVaults'), value: '--' },
      { label: t('pass.hero.metrics.members'), value: '--' },
      { label: t('pass.hero.metrics.secureLinks'), value: '--' },
      { label: t('pass.hero.metrics.weakPasswords'), value: '--' }
    ]
  }
  return [
    { label: t('pass.hero.metrics.sharedVaults'), value: String(props.overview.sharedVaultCount) },
    { label: t('pass.hero.metrics.members'), value: String(props.overview.memberCount) },
    { label: t('pass.hero.metrics.secureLinks'), value: String(props.overview.secureLinkCount) },
    { label: t('pass.hero.metrics.weakPasswords'), value: String(props.overview.weakPasswordItemCount) }
  ]
})

const selectedOrgName = computed(() => props.organizations.find(item => item.id === props.selectedOrgId)?.name || t('pass.hero.empty.org'))
const modeLabel = computed(() => props.workspaceMode === 'PERSONAL' ? t('pass.hero.mode.personal') : t('pass.hero.mode.shared'))
const statusChips = computed(() => {
  if (!props.overview) {
    return [
      t('pass.hero.status.workspacePosture'),
      t('pass.hero.status.policyAware'),
      t('pass.hero.status.sharedReady')
    ]
  }
  return [
    props.overview.allowSecureLinks ? t('pass.hero.status.secureLinksOn') : t('pass.hero.status.secureLinksOff'),
    props.overview.forceTwoFactor ? t('pass.hero.status.twoFactorEnforced') : t('pass.hero.status.twoFactorOptional'),
    props.overview.allowPasskeys ? t('pass.hero.status.passkeysEnabled') : t('pass.hero.status.passkeysBlocked'),
    props.overview.allowAliases ? t('pass.hero.status.aliasesEnabled') : t('pass.hero.status.aliasesBlocked')
  ]
})
</script>

<template>
  <section class="pass-hero">
    <div class="hero-copy">
      <p class="hero-eyebrow">{{ t('pass.hero.eyebrow') }}</p>
      <h1>{{ modeLabel }}</h1>
      <p class="hero-subtitle">{{ t('pass.hero.subtitle') }}</p>
      <div class="hero-meta">
        <span class="hero-pill">{{ t('pass.hero.meta.org', { value: selectedOrgName }) }}</span>
        <span class="hero-pill">{{ t('pass.hero.meta.vault', { value: selectedVaultName || t('pass.hero.empty.vault') }) }}</span>
        <span class="hero-pill">{{ t('pass.hero.meta.activity', { value: formatPassTime(overview?.lastActivityAt || null) }) }}</span>
      </div>
      <div class="hero-actions">
        <el-segmented
          :model-value="workspaceMode"
          :options="[
            { label: t('pass.hero.segment.personal'), value: 'PERSONAL' },
            { label: t('pass.hero.segment.shared'), value: 'SHARED' }
          ]"
          @update:model-value="emit('update:workspaceMode', $event as PassWorkspaceMode)"
        />
        <el-select
          v-if="workspaceMode === 'SHARED'"
          :model-value="selectedOrgId"
          :placeholder="t('pass.hero.orgPlaceholder')"
          class="org-select"
          @change="emit('update:selectedOrgId', $event)"
        >
          <el-option v-for="org in organizations" :key="org.id" :label="org.name" :value="org.id" />
        </el-select>
        <el-button :loading="loading" @click="emit('refresh')">{{ t('pass.hero.actions.refresh') }}</el-button>
      </div>
    </div>
    <div class="hero-metrics">
      <article v-for="metric in metricCards" :key="metric.label" class="metric-card">
        <span class="metric-label">{{ metric.label }}</span>
        <strong class="metric-value">{{ metric.value }}</strong>
      </article>
      <div class="metric-strip">
        <span v-for="chip in statusChips" :key="chip" class="chip">{{ chip }}</span>
      </div>
    </div>
  </section>
</template>

<style scoped>
.pass-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(360px, 1fr);
  gap: 18px;
  padding: 24px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(95, 247, 215, 0.22), transparent 32%),
    radial-gradient(circle at bottom right, rgba(142, 92, 255, 0.28), transparent 38%),
    linear-gradient(135deg, #08111f 0%, #111a31 52%, #0b2330 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #f8fbff;
  box-shadow: 0 24px 72px rgba(4, 12, 26, 0.38);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero-eyebrow {
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 12px;
  color: rgba(165, 245, 233, 0.84);
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(32px, 4vw, 52px);
  line-height: 0.95;
}

.hero-subtitle {
  margin: 0;
  max-width: 56ch;
  color: rgba(235, 244, 255, 0.78);
}

.hero-meta,
.hero-actions,
.metric-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-pill,
.chip {
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(244, 248, 255, 0.84);
  font-size: 12px;
}

.org-select {
  min-width: 220px;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  align-content: start;
}

.metric-card {
  min-height: 120px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(13, 23, 41, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.metric-label {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  color: rgba(167, 186, 219, 0.76);
}

.metric-value {
  font-size: 30px;
  color: #faffff;
}

.metric-strip {
  grid-column: 1 / -1;
}

:deep(.el-segmented) {
  --el-segmented-item-selected-bg-color: rgba(95, 247, 215, 0.18);
  --el-segmented-item-selected-color: #f6ffff;
  --el-border-radius-base: 999px;
}

@media (max-width: 1100px) {
  .pass-hero {
    grid-template-columns: 1fr;
  }

  .hero-metrics {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .hero-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
