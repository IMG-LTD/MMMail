<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { OrgWorkspace } from '~/types/api'
import { organizationRoleLabel } from '~/utils/organization-admin'

const props = defineProps<{
  organizations: OrgWorkspace[]
  selectedOrgId: string
  loading: boolean
  policyChips: string[]
}>()

const emit = defineEmits<{
  'change-org': [orgId: string]
  refresh: []
  'open-create-org': []
}>()

const selectedOrgModel = computed({
  get: () => props.selectedOrgId,
  set: (value: string) => emit('change-org', value)
})

const { t } = useI18n()
</script>

<template>
  <section class="mm-card admin-hero">
    <div class="hero-copy">
      <p class="eyebrow">{{ t('organizations.hero.eyebrow') }}</p>
      <h1>{{ t('organizations.hero.title') }}</h1>
      <p class="hero-text">{{ t('organizations.hero.description') }}</p>
      <div class="policy-strip">
        <span v-for="chip in policyChips" :key="chip" class="policy-chip">{{ chip }}</span>
      </div>
    </div>
    <div class="hero-tools">
      <el-select v-model="selectedOrgModel" :placeholder="t('organizations.hero.selectPlaceholder')">
        <el-option
          v-for="org in organizations"
          :key="org.id"
          :label="`${org.name} · ${organizationRoleLabel(org.role, t)}`"
          :value="org.id"
        />
      </el-select>
      <div class="tool-actions">
        <el-button :loading="loading" @click="emit('refresh')">{{ t('organizations.hero.refresh') }}</el-button>
        <el-button type="primary" @click="emit('open-create-org')">{{ t('organizations.hero.create') }}</el-button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.admin-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(300px, 0.9fr);
  gap: 20px;
  padding: 20px;
  background:
    radial-gradient(circle at top left, rgba(15, 110, 110, 0.18), transparent 40%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(240, 249, 250, 0.94));
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

.hero-text {
  color: var(--mm-muted);
}

.hero-tools,
.tool-actions,
.policy-strip {
  display: flex;
  gap: 10px;
}

.hero-tools {
  flex-direction: column;
}

.policy-strip {
  flex-wrap: wrap;
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
  .admin-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  h1 {
    font-size: 28px;
  }

  .tool-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
