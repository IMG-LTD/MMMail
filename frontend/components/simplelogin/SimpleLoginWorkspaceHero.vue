<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { OrgWorkspace } from '~/types/api'
import type { SimpleLoginOverview } from '~/types/simplelogin'
import { buildSimpleLoginHealthChips } from '~/utils/simplelogin'

const props = defineProps<{
  organizations: OrgWorkspace[]
  selectedOrgId: string
  overview: SimpleLoginOverview | null
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:selectedOrgId': [orgId: string]
  refresh: []
}>()
const { t } = useI18n()

const metricCards = computed(() => {
  if (!props.overview) {
    return [
      { label: t('simplelogin.metric.aliases'), value: '--', hint: t('simplelogin.metric.hint.relayAddresses') },
      { label: t('simplelogin.metric.verifiedMailboxes'), value: '--', hint: t('simplelogin.metric.hint.deliveryTargets') },
      { label: t('simplelogin.metric.relayPolicies'), value: '--', hint: t('simplelogin.metric.hint.domainControlPlane') },
      { label: t('simplelogin.metric.catchAllDomains'), value: '--', hint: t('simplelogin.metric.hint.readyCoverage') },
      { label: t('simplelogin.metric.reverseContacts'), value: '--', hint: t('simplelogin.metric.hint.aliasSenderGraph') },
      { label: t('simplelogin.metric.domains'), value: '--', hint: t('simplelogin.metric.hint.orgScope') }
    ]
  }
  return [
    {
      label: t('simplelogin.metric.aliases'),
      value: String(props.overview.aliasCount),
      hint: t('simplelogin.metric.hint.enabled', { count: props.overview.enabledAliasCount })
    },
    {
      label: t('simplelogin.metric.verifiedMailboxes'),
      value: String(props.overview.verifiedMailboxCount),
      hint: props.overview.defaultMailboxEmail || t('simplelogin.metric.hint.noDefaultMailbox')
    },
    {
      label: t('simplelogin.metric.relayPolicies'),
      value: String(props.overview.relayPolicyCount),
      hint: props.overview.defaultRelayMailboxEmail || t('simplelogin.metric.hint.noDefaultRelayRoute')
    },
    {
      label: t('simplelogin.metric.catchAllDomains'),
      value: String(props.overview.catchAllDomainCount),
      hint: t('simplelogin.metric.hint.subdomainReady', { count: props.overview.subdomainPolicyCount })
    },
    {
      label: t('simplelogin.metric.reverseContacts'),
      value: String(props.overview.reverseAliasContactCount),
      hint: t('simplelogin.metric.hint.senderPrivacyReady')
    },
    {
      label: t('simplelogin.metric.domains'),
      value: String(props.overview.customDomainCount),
      hint: props.overview.defaultDomain || t('simplelogin.metric.hint.noDefaultDomain')
    }
  ]
})

const selectedOrgName = computed(() => props.organizations.find((item) => item.id === props.selectedOrgId)?.name || t('simplelogin.scope.personal'))
const healthChips = computed(() => buildSimpleLoginHealthChips(props.overview, t))
</script>

<template>
  <section class="simplelogin-hero">
    <div class="hero-copy">
      <p class="hero-eyebrow">{{ t('simplelogin.hero.eyebrow') }}</p>
      <h1>{{ t('simplelogin.hero.title') }}</h1>
      <p class="hero-subtitle">{{ t('simplelogin.hero.subtitle') }}</p>
      <div class="hero-actions">
        <el-select
          :model-value="selectedOrgId"
          clearable
          :placeholder="t('simplelogin.hero.orgScope')"
          class="org-select"
          @change="emit('update:selectedOrgId', $event || '')"
        >
          <el-option v-for="org in organizations" :key="org.id" :label="org.name" :value="org.id" />
        </el-select>
        <el-button :loading="loading" @click="emit('refresh')">{{ t('simplelogin.hero.refreshWorkspace') }}</el-button>
      </div>
      <div class="hero-meta">
        <span class="hero-pill">{{ t('simplelogin.hero.scope', { name: selectedOrgName }) }}</span>
        <span class="hero-pill">{{ t('simplelogin.hero.defaultMailbox', { email: overview?.defaultMailboxEmail || t('simplelogin.hero.primaryMailbox') }) }}</span>
        <span class="hero-pill">{{ t('simplelogin.hero.relayRoute', { email: overview?.defaultRelayMailboxEmail || t('simplelogin.hero.notConfigured') }) }}</span>
      </div>
      <div class="hero-strip">
        <span v-for="chip in healthChips" :key="chip" class="chip">{{ chip }}</span>
      </div>
    </div>

    <div class="hero-metrics">
      <article v-for="metric in metricCards" :key="metric.label" class="metric-card">
        <span class="metric-label">{{ metric.label }}</span>
        <strong class="metric-value">{{ metric.value }}</strong>
        <span class="metric-hint">{{ metric.hint }}</span>
      </article>
    </div>
  </section>
</template>

<style scoped>
.simplelogin-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(420px, 1fr);
  gap: 18px;
  padding: 28px;
  border-radius: 30px;
  color: #f8fbff;
  background:
    radial-gradient(circle at top left, rgba(88, 180, 255, 0.26), transparent 32%),
    radial-gradient(circle at bottom right, rgba(116, 118, 255, 0.18), transparent 40%),
    linear-gradient(135deg, #07121e 0%, #0c1f33 42%, #10253c 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 28px 80px rgba(3, 10, 24, 0.35);
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
  color: rgba(173, 236, 255, 0.82);
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(34px, 4vw, 54px);
  line-height: 0.94;
}

.hero-subtitle,
.metric-hint {
  color: rgba(236, 245, 255, 0.78);
}

.hero-subtitle {
  margin: 0;
  max-width: 60ch;
}

.hero-actions,
.hero-meta,
.hero-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-pill,
.chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  font-size: 12px;
}

.org-select {
  min-width: 240px;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  min-height: 128px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(8, 18, 34, 0.76);
  border: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.metric-label {
  text-transform: uppercase;
  letter-spacing: 0.18em;
  font-size: 11px;
  color: rgba(165, 193, 220, 0.74);
}

.metric-value {
  font-size: 30px;
}

@media (max-width: 1100px) {
  .simplelogin-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-metrics {
    grid-template-columns: 1fr 1fr;
  }

  .org-select {
    min-width: 100%;
  }
}
</style>
