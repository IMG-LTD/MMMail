<script setup lang="ts">
import { computed } from 'vue'
import type { OrgWorkspace } from '~/types/api'
import { useI18n } from '~/composables/useI18n'

interface SummaryCard {
  label: string
  value: string
  hint: string
}

const props = defineProps<{
  organizations: OrgWorkspace[]
  selectedOrgId: string
  loading: boolean
  canManageTeamSpaces: boolean
  summaryCards: SummaryCard[]
  policyChips: string[]
}>()

const emit = defineEmits<{
  'change-org': [orgId: string]
  refresh: []
  'create-team-space': []
}>()
const { t } = useI18n()

const selectedOrgModel = computed({
  get: () => props.selectedOrgId,
  set: (value: string) => emit('change-org', value)
})
</script>

<template>
  <div class="overview-shell">
    <section class="mm-card business-hero">
      <div class="hero-copy">
        <p class="eyebrow">{{ t('business.hero.eyebrow') }}</p>
        <h1>{{ t('business.hero.title') }}</h1>
        <p class="hero-text">{{ t('business.hero.subtitle') }}</p>
        <div class="hero-links">
          <NuxtLink class="hero-link" to="/organizations">{{ t('business.hero.openOrganizations') }}</NuxtLink>
          <NuxtLink class="hero-link hero-link--muted" to="/drive">{{ t('business.hero.openPersonalDrive') }}</NuxtLink>
        </div>
      </div>
      <div class="hero-tools">
        <el-select v-model="selectedOrgModel" :placeholder="t('business.hero.selectOrganization')">
          <el-option
            v-for="org in organizations"
            :key="org.id"
            :label="`${org.name} · ${org.role}`"
            :value="org.id"
          />
        </el-select>
        <div class="tool-actions">
          <el-button :loading="loading" @click="emit('refresh')">{{ t('common.actions.refresh') }}</el-button>
          <el-button v-if="canManageTeamSpaces" type="primary" @click="emit('create-team-space')">{{ t('business.hero.newTeamSpace') }}</el-button>
        </div>
      </div>
    </section>

    <section class="summary-grid">
      <article v-for="card in summaryCards" :key="card.label" class="mm-card summary-card">
        <div class="summary-label">{{ card.label }}</div>
        <div class="summary-value">{{ card.value }}</div>
        <div class="summary-hint">{{ card.hint }}</div>
      </article>
    </section>

    <section class="policy-strip">
      <span v-for="chip in policyChips" :key="chip" class="policy-chip">{{ chip }}</span>
    </section>
  </div>
</template>

<style scoped>
.overview-shell {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.business-hero,
.summary-card {
  padding: 20px;
}

.business-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(300px, 0.9fr);
  gap: 20px;
  background:
    radial-gradient(circle at top left, rgba(15, 110, 110, 0.18), transparent 40%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(240, 249, 250, 0.94));
}

.eyebrow {
  margin: 0 0 10px;
  color: var(--mm-primary);
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  font-weight: 700;
}

h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.1;
}

.hero-text,
.summary-hint {
  color: var(--mm-muted);
}

.hero-tools,
.tool-actions,
.policy-strip,
.hero-links {
  display: flex;
  gap: 10px;
}

.hero-tools {
  flex-direction: column;
}

.hero-links,
.policy-strip {
  flex-wrap: wrap;
}

.hero-link {
  color: var(--mm-primary-dark);
  font-weight: 600;
}

.hero-link--muted {
  color: var(--mm-muted);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--mm-muted);
}

.summary-value {
  margin-top: 10px;
  font-size: 26px;
  font-weight: 700;
  color: var(--mm-primary-dark);
}

.policy-strip {
  gap: 10px;
}

.policy-chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(15, 110, 110, 0.1);
  color: var(--mm-primary-dark);
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 1280px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .business-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  h1 {
    font-size: 28px;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .tool-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
