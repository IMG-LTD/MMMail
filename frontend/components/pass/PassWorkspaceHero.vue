<script setup lang="ts">
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

const metricCards = computed(() => {
  if (!props.overview) {
    return [
      { label: 'Shared Vaults', value: '--' },
      { label: 'Members', value: '--' },
      { label: 'Secure Links', value: '--' },
      { label: 'Weak Passwords', value: '--' }
    ]
  }
  return [
    { label: 'Shared Vaults', value: String(props.overview.sharedVaultCount) },
    { label: 'Members', value: String(props.overview.memberCount) },
    { label: 'Secure Links', value: String(props.overview.secureLinkCount) },
    { label: 'Weak Passwords', value: String(props.overview.weakPasswordItemCount) }
  ]
})

const selectedOrgName = computed(() => props.organizations.find(item => item.id === props.selectedOrgId)?.name || 'No organization selected')
const modeLabel = computed(() => (props.workspaceMode === 'PERSONAL' ? 'Personal Vault' : 'Shared Vaults'))
const statusChips = computed(() => {
  if (!props.overview) {
    return ['Encrypted cockpit', 'Policy aware', 'Shared-ready']
  }
  return [
    props.overview.allowSecureLinks ? 'Secure links on' : 'Secure links off',
    props.overview.forceTwoFactor ? '2FA enforced' : '2FA optional',
    props.overview.allowPasskeys ? 'Passkeys enabled' : 'Passkeys blocked',
    props.overview.allowAliases ? 'Aliases enabled' : 'Aliases blocked'
  ]
})
</script>

<template>
  <section class="pass-hero">
    <div class="hero-copy">
      <p class="hero-eyebrow">Pass Workspace</p>
      <h1>{{ modeLabel }}</h1>
      <p class="hero-subtitle">Personal secrets, shared vaults, and policy controls in one encrypted cockpit.</p>
      <div class="hero-meta">
        <span class="hero-pill">Org: {{ selectedOrgName }}</span>
        <span class="hero-pill">Vault: {{ selectedVaultName || 'No vault selected' }}</span>
        <span class="hero-pill">Activity: {{ formatPassTime(overview?.lastActivityAt || null) }}</span>
      </div>
      <div class="hero-actions">
        <el-segmented
          :model-value="workspaceMode"
          :options="[
            { label: 'Personal', value: 'PERSONAL' },
            { label: 'Shared', value: 'SHARED' }
          ]"
          @update:model-value="emit('update:workspaceMode', $event as PassWorkspaceMode)"
        />
        <el-select
          v-if="workspaceMode === 'SHARED'"
          :model-value="selectedOrgId"
          placeholder="Select organization"
          class="org-select"
          @change="emit('update:selectedOrgId', $event)"
        >
          <el-option v-for="org in organizations" :key="org.id" :label="org.name" :value="org.id" />
        </el-select>
        <el-button :loading="loading" @click="emit('refresh')">Refresh workspace</el-button>
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
