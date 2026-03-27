<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { OrgCustomDomain } from '~/types/organization-admin'
import { domainStatusLabel } from '~/utils/organization-admin'

defineProps<{
  domains: OrgCustomDomain[]
  loading: boolean
  canManage: boolean
  domainInput: string
  mutationId: string
}>()

const emit = defineEmits<{
  'update:domain-input': [value: string]
  create: []
  verify: [domainId: string]
  'set-default': [domainId: string]
  remove: [domainId: string]
}>()

const { t } = useI18n()
</script>

<template>
  <article class="mm-card panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-title">{{ t('organizations.domains.title') }}</h2>
        <p class="mm-muted">{{ t('organizations.domains.description') }}</p>
      </div>
    </div>

    <div v-if="canManage" class="create-row">
      <el-input
        :model-value="domainInput"
        :placeholder="t('organizations.domains.placeholder')"
        @update:model-value="emit('update:domain-input', $event)"
      />
      <el-button type="primary" @click="emit('create')">{{ t('organizations.domains.add') }}</el-button>
    </div>

    <div v-else class="read-only-banner">
      {{ t('organizations.domains.readOnly') }}
    </div>

    <el-empty v-if="!loading && domains.length === 0" :description="t('organizations.domains.empty')" />

    <div v-else class="domain-list">
      <article v-for="domain in domains" :key="domain.id" class="domain-card">
        <div class="domain-copy">
          <div class="title-row">
            <div class="domain-name">{{ domain.domain }}</div>
            <el-tag :type="domain.status === 'VERIFIED' ? 'success' : 'warning'">{{ domainStatusLabel(domain.status, t) }}</el-tag>
            <el-tag v-if="domain.defaultDomain" effect="dark">{{ t('organizations.states.default') }}</el-tag>
          </div>
          <div class="domain-meta">
            {{ t('organizations.domains.tokenUpdated', { token: domain.verificationToken, value: domain.updatedAt }) }}
          </div>
        </div>
        <div v-if="canManage" class="domain-actions">
          <el-button
            v-if="domain.status !== 'VERIFIED'"
            size="small"
            :loading="mutationId === domain.id"
            @click="emit('verify', domain.id)"
          >
            {{ t('organizations.domains.verify') }}
          </el-button>
          <el-button
            v-if="!domain.defaultDomain && domain.status === 'VERIFIED'"
            size="small"
            @click="emit('set-default', domain.id)"
          >
            {{ t('organizations.domains.setDefault') }}
          </el-button>
          <el-button size="small" type="danger" plain :disabled="domain.defaultDomain" @click="emit('remove', domain.id)">
            {{ t('organizations.domains.remove') }}
          </el-button>
        </div>
      </article>
    </div>
  </article>
</template>

<style scoped>
.panel {
  padding: 20px;
}

.panel-head,
.title-row,
.create-row,
.domain-actions {
  display: flex;
  gap: 12px;
}

.panel-head,
.domain-card {
  justify-content: space-between;
}

.domain-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.domain-card {
  display: flex;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(15, 110, 110, 0.05);
}

.domain-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--mm-primary-dark);
}

.domain-meta,
.mm-muted {
  color: var(--mm-muted);
}

.read-only-banner {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(250, 173, 20, 0.12);
  color: #8a5b00;
  font-weight: 600;
}

@media (max-width: 768px) {
  .panel-head,
  .create-row,
  .domain-card,
  .domain-actions,
  .title-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
