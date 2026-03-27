<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { VpnHeroMetric, VpnSessionItem, VpnSettingsDraft } from '~/types/vpn'

const props = defineProps<{
  metrics: VpnHeroMetric[]
  settings: VpnSettingsDraft
  defaultProfileName: string | null
  secureCoreProfileCount: number
  currentSession: VpnSessionItem | null
  refreshing?: boolean
  quickConnecting?: boolean
}>()

const emit = defineEmits<{
  refresh: []
  quickConnect: []
}>()

const { t } = useI18n()

function resolveMetricHint(metric: VpnHeroMetric): string {
  if (metric.key === 'route' && metric.hint.startsWith('PROFILE')) {
    return t(`vpn.policy.mode.${metric.hint}`)
  }
  if (metric.key === 'route' && ['MANUAL', 'QUICK_CONNECT', 'PROFILE'].includes(metric.hint)) {
    return t(`vpn.source.${metric.hint}`)
  }
  if (metric.key === 'route' && ['FASTEST', 'RANDOM', 'LAST_CONNECTION', 'PROFILE'].includes(metric.hint)) {
    return t(`vpn.policy.mode.${metric.hint}`)
  }
  if (metric.key === 'hardening') {
    return metric.hint === 'KILL_SWITCH_ON' ? t('vpn.session.values.enabled') : t('vpn.session.values.disabled')
  }
  return metric.hint
}
</script>

<template>
  <section class="vpn-hero">
    <div class="hero-copy">
      <p class="hero-eyebrow">{{ t('vpn.hero.eyebrow') }}</p>
      <h1>{{ t('vpn.hero.title') }}</h1>
      <p class="hero-description">{{ t('vpn.hero.description') }}</p>
      <div class="hero-actions">
        <el-button :loading="props.refreshing" @click="emit('refresh')">
          {{ t('vpn.hero.actions.refresh') }}
        </el-button>
        <el-button type="primary" :loading="props.quickConnecting" @click="emit('quickConnect')">
          {{ t('vpn.hero.actions.quickConnect') }}
        </el-button>
      </div>
      <div class="hero-pills">
        <span class="hero-pill">{{ t('vpn.hero.pills.defaultMode', { value: t(`vpn.policy.mode.${props.settings.defaultConnectionMode}`) }) }}</span>
        <span class="hero-pill">
          {{ t('vpn.hero.pills.defaultProfile', { value: props.defaultProfileName || '—' }) }}
        </span>
        <span class="hero-pill">
          {{ t('vpn.hero.pills.session', { value: props.currentSession?.serverId || 'standby' }) }}
        </span>
      </div>
      <div class="hero-ribbon">
        <span class="ribbon-chip">{{ props.secureCoreProfileCount }} Secure Core</span>
        <span class="ribbon-chip">{{ t(`vpn.netshield.${props.settings.netshieldMode}`) }}</span>
        <span class="ribbon-chip">
          {{ props.settings.killSwitchEnabled ? t('vpn.session.values.enabled') : t('vpn.session.values.disabled') }}
        </span>
      </div>
    </div>

    <div class="hero-metrics">
      <article v-for="metric in props.metrics" :key="metric.key" class="metric-card">
        <span class="metric-label">{{ t(`vpn.hero.metrics.${metric.key}`) }}</span>
        <strong class="metric-value">{{ metric.value }}</strong>
        <span class="metric-hint">{{ resolveMetricHint(metric) }}</span>
      </article>
    </div>
  </section>
</template>

<style scoped>
.vpn-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 1fr);
  gap: 18px;
  padding: 28px;
  border-radius: 30px;
  color: #eef6ff;
  background:
    linear-gradient(120deg, rgba(44, 99, 255, 0.18), transparent 35%),
    linear-gradient(180deg, rgba(12, 18, 30, 0.98), rgba(16, 23, 39, 0.98)),
    repeating-linear-gradient(90deg, rgba(255, 255, 255, 0.04), rgba(255, 255, 255, 0.04) 1px, transparent 1px, transparent 48px);
  border: 1px solid rgba(120, 145, 203, 0.2);
  box-shadow: 0 28px 80px rgba(2, 6, 23, 0.38);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero-eyebrow,
.metric-label {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(157, 191, 255, 0.76);
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(34px, 4vw, 54px);
  line-height: 0.92;
}

.hero-description,
.metric-hint {
  color: rgba(212, 225, 247, 0.76);
}

.hero-description {
  margin: 0;
  max-width: 58ch;
}

.hero-actions,
.hero-pills,
.hero-ribbon {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-pill,
.ribbon-chip {
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(148, 163, 184, 0.1);
  font-size: 12px;
}

.hero-ribbon {
  padding-top: 4px;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  min-height: 130px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(15, 23, 42, 0.68);
  border: 1px solid rgba(148, 163, 184, 0.12);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.metric-value {
  font-size: 30px;
}

@media (max-width: 1100px) {
  .vpn-hero {
    grid-template-columns: 1fr;
  }
}
</style>
