<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from '~/composables/useI18n'
import type { OrgMember, OrgRole } from '~/types/api'
import type { OrgCustomDomain, OrgMailIdentity } from '~/types/organization-admin'
import { mailIdentityStatusLabel, organizationRoleLabel } from '~/utils/organization-admin'

const props = defineProps<{
  mailIdentities: OrgMailIdentity[]
  members: OrgMember[]
  domains: OrgCustomDomain[]
  selectedOrgRole: OrgRole | null
  canManage: boolean
  loading: boolean
  mutationId: string
  memberId: string
  customDomainId: string
  localPart: string
  displayName: string
}>()

const emit = defineEmits<{
  'update:member-id': [value: string]
  'update:custom-domain-id': [value: string]
  'update:local-part': [value: string]
  'update:display-name': [value: string]
  create: []
  'set-default': [identityId: string]
  enable: [identityId: string]
  disable: [identityId: string]
  remove: [identityId: string]
}>()

const verifiedDomains = computed(() => props.domains.filter((domain) => domain.status === 'VERIFIED'))
const { t } = useI18n()
</script>

<template>
  <article class="mm-card panel">
    <div class="panel-head">
      <div>
        <h2 class="mm-section-title">{{ t('organizations.identities.title') }}</h2>
        <p class="mm-muted">{{ t('organizations.identities.description') }}</p>
      </div>
      <el-tag type="info" effect="plain">{{ organizationRoleLabel(selectedOrgRole || 'MEMBER', t) }}</el-tag>
    </div>

    <div v-if="canManage" class="form-grid">
      <el-select :model-value="memberId" :placeholder="t('organizations.identities.assignMember')" @update:model-value="emit('update:member-id', $event)">
        <el-option
          v-for="member in members"
          :key="member.id"
          :label="`${member.userEmail} · ${organizationRoleLabel(member.role, t)}`"
          :value="member.id"
        />
      </el-select>
      <el-select :model-value="customDomainId" :placeholder="t('organizations.identities.verifiedDomain')" @update:model-value="emit('update:custom-domain-id', $event)">
        <el-option v-for="domain in verifiedDomains" :key="domain.id" :label="domain.domain" :value="domain.id" />
      </el-select>
      <el-input :model-value="localPart" :placeholder="t('organizations.identities.localPart')" @update:model-value="emit('update:local-part', $event)" />
      <el-input :model-value="displayName" :placeholder="t('organizations.identities.displayName')" @update:model-value="emit('update:display-name', $event)" />
      <el-button type="primary" :loading="mutationId === 'create'" @click="emit('create')">{{ t('organizations.identities.create') }}</el-button>
    </div>
    <p v-else class="mm-muted helper">{{ t('organizations.identities.viewOnly') }}</p>

    <el-empty v-if="mailIdentities.length === 0 && !loading" :description="t('organizations.identities.empty')" />
    <div v-else class="identity-list">
      <article v-for="identity in mailIdentities" :key="identity.id" class="identity-card">
        <div class="identity-head">
          <div>
            <div class="identity-email">{{ identity.emailAddress }}</div>
            <div class="identity-meta">
              {{ identity.memberEmail || t('organizations.identities.unassigned') }}
              <span v-if="identity.displayName">· {{ identity.displayName }}</span>
            </div>
          </div>
          <div class="identity-tags">
            <el-tag v-if="identity.defaultIdentity" type="success" effect="dark">{{ t('organizations.states.default') }}</el-tag>
            <el-tag :type="identity.status === 'ENABLED' ? 'primary' : 'info'" effect="plain">{{ mailIdentityStatusLabel(identity.status, t) }}</el-tag>
          </div>
        </div>
        <div v-if="canManage" class="identity-actions">
          <el-button
            v-if="!identity.defaultIdentity && identity.status === 'ENABLED'"
            size="small"
            :loading="mutationId === identity.id"
            @click="emit('set-default', identity.id)"
          >
            {{ t('organizations.identities.makeDefault') }}
          </el-button>
          <el-button
            v-if="identity.status === 'DISABLED'"
            size="small"
            :loading="mutationId === identity.id"
            @click="emit('enable', identity.id)"
          >
            {{ t('organizations.identities.enable') }}
          </el-button>
          <el-button
            v-else
            size="small"
            :loading="mutationId === identity.id"
            @click="emit('disable', identity.id)"
          >
            {{ t('organizations.identities.disable') }}
          </el-button>
          <el-button size="small" type="danger" plain :loading="mutationId === identity.id" @click="emit('remove', identity.id)">
            {{ t('organizations.identities.delete') }}
          </el-button>
        </div>
      </article>
    </div>
  </article>
</template>

<style scoped>
.panel,
.identity-list,
.identity-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  padding: 20px;
}

.panel-head,
.identity-head,
.identity-actions,
.identity-tags {
  display: flex;
  gap: 12px;
}

.panel-head,
.identity-head {
  justify-content: space-between;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.identity-card {
  padding: 16px;
  border-radius: 18px;
  background: rgba(15, 110, 110, 0.05);
}

.identity-email {
  font-weight: 700;
  color: var(--mm-primary-dark);
}

.identity-meta,
.mm-muted,
.helper {
  color: var(--mm-muted);
}

.identity-actions {
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .panel-head,
  .identity-head,
  .identity-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
