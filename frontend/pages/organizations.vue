<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from '~/composables/useI18n'
import { useOrganizationsAdmin } from '~/composables/useOrganizationsAdmin'

const {
  loading,
  organizations,
  selectedOrgId,
  incomingInvites,
  createOrgDialogVisible,
  createOrgForm,
  inviteForm,
  createDomainForm,
  createIdentityForm,
  policyForm,
  auditFilter,
  respondingInviteId,
  selectedOrgRole,
  canManageProducts,
  canManageAuthenticationSecurity,
  canManageMemberRoles,
  canEditPolicy,
  canInvite,
  inviteRoleOptions,
  policy,
  summaryCards,
  authSecurity,
  authSecurityCards,
  authSecurityReminderMemberIds,
  policyChips,
  governanceAuditEvents,
  memberSessions,
  monitorStatus,
  memberSessionCards,
  members,
  domains,
  mailIdentities,
  productAccessRows,
  memberSessionFilter,
  canViewMemberSessions,
  openCreateOrgDialog,
  onOrganizationChange,
  onRefreshAll,
  onRefreshAudit,
  onRefreshAuthenticationSecurity,
  onRefreshMemberSessions,
  onExportAudit,
  onCreateOrganization,
  onSavePolicy,
  onSendAuthenticationSecurityReminders,
  onInviteMember,
  onRespondInvite,
  onUpdateMemberRole,
  onBatchUpdateMemberRole,
  onRemoveMember,
  onBatchRemoveMembers,
  onCreateDomain,
  onVerifyDomain,
  onSetDefaultDomain,
  onRemoveDomain,
  onCreateMailIdentity,
  onSetDefaultMailIdentity,
  onEnableMailIdentity,
  onDisableMailIdentity,
  onRemoveMailIdentity,
  onToggleProduct,
  onRevokeMemberSession,
  bootstrapPage
} = useOrganizationsAdmin()

const { t } = useI18n()
const auditDateRange = computed({
  get: () => (auditFilter.fromDate && auditFilter.toDate ? [auditFilter.fromDate, auditFilter.toDate] : []),
  set: (value: string[]) => {
    auditFilter.fromDate = value[0] || ''
    auditFilter.toDate = value[1] || ''
  }
})

useHead(() => ({
  title: t('organizations.page.title')
}))

onMounted(() => {
  void bootstrapPage()
})
</script>

<template>
  <div class="mm-page organizations-page">
    <OrganizationsAdminHero
      :organizations="organizations"
      :selected-org-id="selectedOrgId"
      :loading="loading.organizations || loading.summary"
      :policy-chips="policyChips"
      @change-org="onOrganizationChange"
      @refresh="onRefreshAll"
      @open-create-org="openCreateOrgDialog"
    />

    <section v-if="organizations.length === 0" class="mm-card empty-state">
      <el-empty :description="t('organizations.empty.description')" />
    </section>

    <template v-else>
      <OrganizationsSummaryCards :cards="summaryCards" />

      <section class="organizations-grid organizations-grid--top">
        <OrganizationsDomainsPanel
          :domains="domains"
          :loading="loading.domains"
          :can-manage="canManageProducts"
          :domain-input="createDomainForm.domain"
          :mutation-id="loading.domainMutationId"
          @update:domain-input="createDomainForm.domain = $event"
          @create="onCreateDomain"
          @verify="onVerifyDomain"
          @set-default="onSetDefaultDomain"
          @remove="onRemoveDomain"
        />
        <OrganizationsMailIdentitiesPanel
          :mail-identities="mailIdentities"
          :members="members"
          :domains="domains"
          :selected-org-role="selectedOrgRole"
          :can-manage="canManageProducts"
          :loading="loading.identities"
          :mutation-id="loading.identityMutationId"
          :member-id="createIdentityForm.memberId"
          :custom-domain-id="createIdentityForm.customDomainId"
          :local-part="createIdentityForm.localPart"
          :display-name="createIdentityForm.displayName"
          @update:member-id="createIdentityForm.memberId = $event"
          @update:custom-domain-id="createIdentityForm.customDomainId = $event"
          @update:local-part="createIdentityForm.localPart = $event"
          @update:display-name="createIdentityForm.displayName = $event"
          @create="onCreateMailIdentity"
          @set-default="onSetDefaultMailIdentity"
          @enable="onEnableMailIdentity"
          @disable="onDisableMailIdentity"
          @remove="onRemoveMailIdentity"
        />
        <OrganizationsPolicyPanel
          :selected-org-role="selectedOrgRole"
          :can-invite="canInvite"
          :can-edit-policy="canEditPolicy"
          :invite-role-options="inviteRoleOptions"
          :incoming-invites="incomingInvites"
          :inviting="loading.invite"
          :saving-policy="loading.savePolicy"
          :responding-invite-id="respondingInviteId"
          :invite-email="inviteForm.email"
          :invite-role="inviteForm.role"
          :allowed-email-domains-text="policyForm.allowedEmailDomainsText"
          :member-limit="policyForm.memberLimit"
          :governance-review-sla-hours="policyForm.governanceReviewSlaHours"
          :admin-can-invite-admin="policyForm.adminCanInviteAdmin"
          :admin-can-remove-admin="policyForm.adminCanRemoveAdmin"
          :admin-can-review-governance="policyForm.adminCanReviewGovernance"
          :admin-can-execute-governance="policyForm.adminCanExecuteGovernance"
          :require-dual-review-governance="policyForm.requireDualReviewGovernance"
          @update:invite-email="inviteForm.email = $event"
          @update:invite-role="inviteForm.role = $event"
          @update:allowed-email-domains-text="policyForm.allowedEmailDomainsText = $event"
          @update:member-limit="policyForm.memberLimit = $event"
          @update:governance-review-sla-hours="policyForm.governanceReviewSlaHours = $event"
          @update:admin-can-invite-admin="policyForm.adminCanInviteAdmin = $event"
          @update:admin-can-remove-admin="policyForm.adminCanRemoveAdmin = $event"
          @update:admin-can-review-governance="policyForm.adminCanReviewGovernance = $event"
          @update:admin-can-execute-governance="policyForm.adminCanExecuteGovernance = $event"
          @update:require-dual-review-governance="policyForm.requireDualReviewGovernance = $event"
          @invite="onInviteMember"
          @save-policy="onSavePolicy"
          @respond-invite="onRespondInvite"
        />
      </section>

      <section class="organizations-grid organizations-grid--bottom">
        <OrganizationsAuthenticationSecurityPanel
          class="organizations-grid__full"
          :overview="authSecurity"
          :cards="authSecurityCards"
          :selected-org-role="selectedOrgRole"
          :can-manage="canManageAuthenticationSecurity"
          :can-edit-policy="canEditPolicy"
          :saving-policy="loading.savePolicy"
          :loading="loading.authSecurity"
          :sending-reminders="loading.authSecurityReminder"
          :reminder-member-ids="authSecurityReminderMemberIds"
          :two-factor-enforcement-level="policyForm.twoFactorEnforcementLevel"
          :two-factor-grace-period-days="policyForm.twoFactorGracePeriodDays"
          @update:reminder-member-ids="authSecurityReminderMemberIds = $event"
          @update:two-factor-enforcement-level="policyForm.twoFactorEnforcementLevel = $event"
          @update:two-factor-grace-period-days="policyForm.twoFactorGracePeriodDays = $event"
          @refresh="onRefreshAuthenticationSecurity"
          @save-policy="onSavePolicy"
          @send-reminders="onSendAuthenticationSecurityReminders"
        />
        <OrganizationsProductAccessMatrix
          class="organizations-grid__full"
          :rows="productAccessRows"
          :selected-org-role="selectedOrgRole"
          :policy="policy"
          :can-manage-products="canManageProducts"
          :can-manage-roles="canManageMemberRoles"
          :member-mutation-id="loading.memberMutationId"
          :product-mutation-id="loading.productMutationId"
          @toggle-product="onToggleProduct"
          @update-role="onUpdateMemberRole"
          @batch-update-role="onBatchUpdateMemberRole"
          @remove="onRemoveMember"
          @batch-remove="onBatchRemoveMembers"
        />
        <OrganizationsMonitorStatusPanel
          class="organizations-grid__full"
          :status="monitorStatus"
          :loading="loading.monitorStatus"
          :can-manage="canViewMemberSessions"
        />
        <OrganizationsSessionMonitorPanel
          :sessions="memberSessions"
          :cards="memberSessionCards"
          :loading="loading.memberSessions"
          :mutation-id="loading.sessionMutationId"
          :can-manage="canViewMemberSessions"
          :member-email="memberSessionFilter.memberEmail"
          :limit="memberSessionFilter.limit"
          @update:member-email="memberSessionFilter.memberEmail = $event"
          @update:limit="memberSessionFilter.limit = $event"
          @refresh="onRefreshMemberSessions"
          @revoke="onRevokeMemberSession"
        />
        <OrganizationsAuditPanel
          v-model:date-range="auditDateRange"
          :audit-events="governanceAuditEvents"
          :loading="loading.audit"
          :can-manage="canViewMemberSessions"
          :event-type="auditFilter.eventType"
          :actor-email="auditFilter.actorEmail"
          :keyword="auditFilter.keyword"
          :sort-direction="auditFilter.sortDirection"
          @update:event-type="auditFilter.eventType = $event"
          @update:actor-email="auditFilter.actorEmail = $event"
          @update:keyword="auditFilter.keyword = $event"
          @update:sort-direction="auditFilter.sortDirection = $event"
          @apply="onRefreshAudit"
          @refresh="onRefreshAudit"
          @export="onExportAudit"
        />
      </section>
    </template>

    <el-dialog v-model="createOrgDialogVisible" :title="t('organizations.dialog.create.title')" width="460px">
      <el-form label-position="top">
        <el-form-item :label="t('organizations.dialog.create.nameLabel')">
          <el-input v-model="createOrgForm.name" :placeholder="t('organizations.dialog.create.namePlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createOrgDialogVisible = false">{{ t('common.actions.cancel') }}</el-button>
        <el-button type="primary" :loading="loading.createOrg" @click="onCreateOrganization">{{ t('common.actions.create') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.organizations-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.organizations-grid {
  display: grid;
  gap: 18px;
}

.organizations-grid--top {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.organizations-grid--bottom {
  grid-template-columns: 1.25fr 0.95fr;
}

.organizations-grid__full {
  grid-column: 1 / -1;
}

.empty-state {
  padding: 28px;
}

@media (max-width: 1400px) {
  .organizations-grid--top {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1200px) {
  .organizations-grid--bottom {
    grid-template-columns: 1fr;
  }
}
</style>
