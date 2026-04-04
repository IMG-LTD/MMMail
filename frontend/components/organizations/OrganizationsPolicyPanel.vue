<script setup lang="ts">
import { useI18n } from '~/composables/useI18n'
import type { OrgIncomingInvite, OrgRole } from '~/types/api'
import { organizationRoleLabel } from '~/utils/organization-admin'

defineProps<{
  selectedOrgRole: OrgRole | null
  canInvite: boolean
  canEditPolicy: boolean
  inviteRoleOptions: Array<Exclude<OrgRole, 'OWNER'>>
  incomingInvites: OrgIncomingInvite[]
  inviting: boolean
  savingPolicy: boolean
  respondingInviteId: string
  inviteEmail: string
  inviteRole: Exclude<OrgRole, 'OWNER'>
  allowedEmailDomainsText: string
  memberLimit: number
  governanceReviewSlaHours: number
  adminCanInviteAdmin: boolean
  adminCanRemoveAdmin: boolean
  adminCanReviewGovernance: boolean
  adminCanExecuteGovernance: boolean
  requireDualReviewGovernance: boolean
}>()

const emit = defineEmits<{
  'update:invite-email': [value: string]
  'update:invite-role': [value: Exclude<OrgRole, 'OWNER'>]
  'update:allowed-email-domains-text': [value: string]
  'update:member-limit': [value: number]
  'update:governance-review-sla-hours': [value: number]
  'update:admin-can-invite-admin': [value: boolean]
  'update:admin-can-remove-admin': [value: boolean]
  'update:admin-can-review-governance': [value: boolean]
  'update:admin-can-execute-governance': [value: boolean]
  'update:require-dual-review-governance': [value: boolean]
  invite: []
  'save-policy': []
  'respond-invite': [inviteId: string, response: 'ACCEPT' | 'DECLINE']
}>()

const { t } = useI18n()
</script>

<template>
  <article class="mm-card panel policy-panel">
    <section class="section">
      <div class="section-head">
        <div>
          <h2 class="mm-section-title">{{ t('organizations.policy.title') }}</h2>
          <p class="mm-muted">{{ t('organizations.policy.description') }}</p>
        </div>
        <el-tag v-if="selectedOrgRole" effect="dark">{{ organizationRoleLabel(selectedOrgRole, t) }}</el-tag>
      </div>
      <el-form label-position="top" class="policy-form">
        <el-form-item :label="t('organizations.policy.allowedDomains')">
          <el-input
            type="textarea"
            :rows="3"
            :disabled="!canEditPolicy"
            :model-value="allowedEmailDomainsText"
            :placeholder="t('organizations.policy.allowedDomainsPlaceholder')"
            @update:model-value="emit('update:allowed-email-domains-text', $event)"
          />
        </el-form-item>
        <div class="policy-grid">
          <el-form-item :label="t('organizations.policy.memberLimit')">
            <el-input-number :disabled="!canEditPolicy" :model-value="memberLimit" :min="1" @update:model-value="emit('update:member-limit', Number($event))" />
          </el-form-item>
          <el-form-item :label="t('organizations.policy.governanceSla')">
            <el-input-number :disabled="!canEditPolicy" :model-value="governanceReviewSlaHours" :min="1" @update:model-value="emit('update:governance-review-sla-hours', Number($event))" />
          </el-form-item>
        </div>
        <div class="check-grid">
          <el-checkbox :disabled="!canEditPolicy" :model-value="adminCanInviteAdmin" @update:model-value="emit('update:admin-can-invite-admin', Boolean($event))">{{ t('organizations.policy.adminCanInviteAdmin') }}</el-checkbox>
          <el-checkbox :disabled="!canEditPolicy" :model-value="adminCanRemoveAdmin" @update:model-value="emit('update:admin-can-remove-admin', Boolean($event))">{{ t('organizations.policy.adminCanRemoveAdmin') }}</el-checkbox>
          <el-checkbox :disabled="!canEditPolicy" :model-value="adminCanReviewGovernance" @update:model-value="emit('update:admin-can-review-governance', Boolean($event))">{{ t('organizations.policy.adminCanReviewGovernance') }}</el-checkbox>
          <el-checkbox :disabled="!canEditPolicy" :model-value="adminCanExecuteGovernance" @update:model-value="emit('update:admin-can-execute-governance', Boolean($event))">{{ t('organizations.policy.adminCanExecuteGovernance') }}</el-checkbox>
          <el-checkbox :disabled="!canEditPolicy" :model-value="requireDualReviewGovernance" @update:model-value="emit('update:require-dual-review-governance', Boolean($event))">{{ t('organizations.policy.requireDualReview') }}</el-checkbox>
        </div>
        <el-button v-if="canEditPolicy" type="primary" :loading="savingPolicy" @click="emit('save-policy')">{{ t('organizations.policy.save') }}</el-button>
      </el-form>
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <h3 class="mm-section-title">{{ t('organizations.invites.title') }}</h3>
          <p class="mm-muted">{{ t('organizations.invites.description') }}</p>
        </div>
      </div>
      <div v-if="canInvite" class="invite-row">
        <el-input :model-value="inviteEmail" :placeholder="t('organizations.invites.emailPlaceholder')" @update:model-value="emit('update:invite-email', $event)" />
        <el-select :model-value="inviteRole" @update:model-value="emit('update:invite-role', $event)">
          <el-option
            v-for="role in inviteRoleOptions"
            :key="role"
            :label="organizationRoleLabel(role, t)"
            :value="role"
          />
        </el-select>
        <el-button type="primary" :loading="inviting" @click="emit('invite')">{{ t('organizations.invites.invite') }}</el-button>
      </div>
      <el-empty v-if="incomingInvites.length === 0" :description="t('organizations.invites.empty')" />
      <div v-else class="invite-list">
        <article v-for="invite in incomingInvites" :key="invite.inviteId" class="invite-card">
          <div>
            <div class="invite-name">{{ invite.orgName }} · {{ organizationRoleLabel(invite.role, t) }}</div>
            <div class="domain-meta">{{ invite.invitedByEmail || t('organizations.invites.unknownInviter') }} · {{ invite.updatedAt }}</div>
          </div>
          <div class="invite-actions">
            <el-button :loading="respondingInviteId === invite.inviteId" size="small" @click="emit('respond-invite', invite.inviteId, 'ACCEPT')">{{ t('organizations.invites.accept') }}</el-button>
            <el-button :loading="respondingInviteId === invite.inviteId" size="small" type="danger" plain @click="emit('respond-invite', invite.inviteId, 'DECLINE')">{{ t('organizations.invites.decline') }}</el-button>
          </div>
        </article>
      </div>
    </section>
  </article>
</template>

<style scoped>
.panel {
  padding: 20px;
}

.policy-panel,
.policy-form,
.section,
.invite-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-head,
.invite-row,
.invite-card,
.invite-actions,
.policy-grid {
  display: flex;
  gap: 12px;
}

.section-head,
.invite-card {
  justify-content: space-between;
}

.check-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.invite-card {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(15, 110, 110, 0.05);
}

.invite-name,
.mm-muted {
  color: var(--mm-primary-dark);
}

.domain-meta {
  color: var(--mm-muted);
}

@media (max-width: 768px) {
  .section-head,
  .invite-row,
  .invite-card,
  .invite-actions,
  .policy-grid {
    flex-direction: column;
    align-items: stretch;
  }

  .check-grid {
    grid-template-columns: 1fr;
  }
}
</style>
