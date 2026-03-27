import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import type { useOrganizationsAdminCore } from '~/composables/useOrganizationsAdminCore'
import type { OrgBatchActionResult, OrgRole } from '~/types/api'
import { downloadTextFile } from '~/utils/sheets'

const CSV_MIME_TYPE = 'text/csv;charset=utf-8'

export function useOrganizationsAdminActions(workspace: ReturnType<typeof useOrganizationsAdminCore>) {
  const { t } = useI18n()

  function openCreateOrgDialog(): void {
    workspace.createOrgForm.name = ''
    workspace.createOrgDialogVisible.value = true
  }

  function resolveSelectedOrgId(): string | null {
    if (workspace.selectedOrgId.value) {
      return workspace.selectedOrgId.value
    }
    ElMessage.warning(t('organizations.messages.selectOrgFirst'))
    return null
  }

  function resolveErrorMessage(error: unknown, fallbackKey: string): string {
    return error instanceof Error && error.message
      ? error.message
      : t(fallbackKey)
  }

  async function refreshGovernanceWorkspace(): Promise<void> {
    await Promise.all([
      workspace.refreshMembers(),
      workspace.refreshIdentities(),
      workspace.refreshProductAccess(),
      workspace.refreshMonitorStatus(),
      workspace.refreshAudit(),
      workspace.refreshSummary()
    ])
  }

  async function confirmDestructiveAction(
    messageKey: string,
    titleKey: string,
    params: Record<string, string | number>
  ): Promise<boolean> {
    try {
      await ElMessageBox.confirm(t(messageKey, params), t(titleKey), {
        type: 'warning'
      })
      return true
    } catch {
      return false
    }
  }

  function buildBatchFailureDetails(result: OrgBatchActionResult): string {
    return result.failedItems
      .map(item => `${item.memberId}: ${item.reason}`)
      .join('\n')
  }

  async function notifyBatchResult(result: OrgBatchActionResult, successKey: string): Promise<void> {
    if (result.failedItems.length === 0) {
      ElMessage.success(t(successKey))
      return
    }
    await ElMessageBox.alert(buildBatchFailureDetails(result), t('organizations.messages.batchPartialTitle'), {
      type: 'warning'
    })
    ElMessage.warning(t('organizations.messages.batchPartialSummary', {
      success: result.successIds.length,
      requested: result.requestedCount,
      failed: result.failedItems.length
    }))
  }

  async function onOrganizationChange(orgId: string): Promise<void> {
    workspace.selectedOrgId.value = orgId
    await workspace.refreshCurrentOrganization()
  }

  async function onRefreshAll(): Promise<void> {
    await workspace.refreshOrganizations()
    await workspace.refreshCurrentOrganization()
    await workspace.refreshIncomingInvites()
  }

  async function onRefreshAudit(): Promise<void> {
    try {
      await Promise.all([workspace.refreshAudit(), workspace.refreshMonitorStatus()])
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.auditRefreshFailed'))
    }
  }

  async function onExportAudit(): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    try {
      const file = await workspace.api.exportOrgAuditEvents(orgId, workspace.auditFilter)
      downloadTextFile(file.content, file.fileName, CSV_MIME_TYPE)
      await Promise.all([workspace.refreshAudit(), workspace.refreshMonitorStatus()])
      ElMessage.success(t('organizations.messages.auditExported'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.auditExportFailed'))
    }
  }

  async function onCreateOrganization(): Promise<void> {
    const name = workspace.createOrgForm.name.trim()
    if (!name) {
      ElMessage.warning(t('organizations.messages.orgNameRequired'))
      return
    }
    workspace.loading.createOrg = true
    try {
      const created = await workspace.api.createOrganization({ name })
      workspace.createOrgDialogVisible.value = false
      workspace.selectedOrgId.value = created.id
      await workspace.refreshOrganizations()
      await workspace.refreshCurrentOrganization()
      ElMessage.success(t('organizations.messages.orgCreated'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.orgCreateFailed'))
    } finally {
      workspace.loading.createOrg = false
    }
  }

  async function onInviteMember(): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    const email = workspace.inviteForm.email.trim()
    if (!email) {
      ElMessage.warning(t('organizations.messages.inviteEmailRequired'))
      return
    }
    workspace.loading.invite = true
    try {
      await workspace.api.inviteMember(orgId, {
        email,
        role: workspace.inviteForm.role
      })
      workspace.inviteForm.email = ''
      await Promise.all([workspace.refreshMembers(), workspace.refreshAudit(), workspace.refreshMonitorStatus()])
      ElMessage.success(t('organizations.messages.inviteSent'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.inviteFailed'))
    } finally {
      workspace.loading.invite = false
    }
  }

  async function onRespondInvite(inviteId: string, response: 'ACCEPT' | 'DECLINE'): Promise<void> {
    workspace.respondingInviteId.value = inviteId
    try {
      await workspace.api.respondInvite(inviteId, { response })
      await Promise.all([workspace.refreshIncomingInvites(), workspace.refreshOrganizations(), workspace.refreshCurrentOrganization()])
      ElMessage.success(t(
        response === 'ACCEPT'
          ? 'organizations.messages.inviteAccepted'
          : 'organizations.messages.inviteDeclined'
      ))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.respondInviteFailed'))
    } finally {
      workspace.respondingInviteId.value = ''
    }
  }

  async function onUpdateMemberRole(memberId: string, role: Exclude<OrgRole, 'OWNER'>): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    workspace.loading.memberMutationId = memberId
    try {
      await workspace.api.updateMemberRole(orgId, memberId, { role })
      await refreshGovernanceWorkspace()
      ElMessage.success(t('organizations.messages.memberRoleUpdated'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.memberRoleUpdateFailed'))
    } finally {
      workspace.loading.memberMutationId = ''
    }
  }

  async function onBatchUpdateMemberRole(memberIds: string[], role: Exclude<OrgRole, 'OWNER'>): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId || memberIds.length === 0) {
      return
    }
    workspace.loading.memberMutationId = memberIds.join(',')
    try {
      const result = await workspace.api.batchUpdateMemberRole(orgId, { memberIds, role })
      await refreshGovernanceWorkspace()
      await notifyBatchResult(result, 'organizations.messages.batchRoleUpdated')
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.batchRoleFailed'))
    } finally {
      workspace.loading.memberMutationId = ''
    }
  }

  async function onRemoveMember(memberId: string): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    const member = workspace.members.value.find(item => item.id === memberId)
    const confirmed = await confirmDestructiveAction(
      'organizations.confirm.removeMember.message',
      'organizations.confirm.removeMember.title',
      { email: member?.userEmail || memberId }
    )
    if (!confirmed) {
      return
    }
    workspace.loading.memberMutationId = memberId
    try {
      await workspace.api.removeMember(orgId, memberId)
      await refreshGovernanceWorkspace()
      ElMessage.success(t('organizations.messages.memberRemoved'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.memberRemoveFailed'))
    } finally {
      workspace.loading.memberMutationId = ''
    }
  }

  async function onBatchRemoveMembers(memberIds: string[]): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId || memberIds.length === 0) {
      return
    }
    const confirmed = await confirmDestructiveAction(
      'organizations.confirm.removeMembers.message',
      'organizations.confirm.removeMembers.title',
      { count: memberIds.length }
    )
    if (!confirmed) {
      return
    }
    workspace.loading.memberMutationId = memberIds.join(',')
    try {
      const result = await workspace.api.batchRemoveMembers(orgId, { memberIds })
      await refreshGovernanceWorkspace()
      await notifyBatchResult(result, 'organizations.messages.batchRemoveCompleted')
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.batchRemoveFailed'))
    } finally {
      workspace.loading.memberMutationId = ''
    }
  }

  async function onCreateDomain(): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    const domain = workspace.createDomainForm.domain.trim()
    if (!domain) {
      ElMessage.warning(t('organizations.messages.domainRequired'))
      return
    }
    workspace.loading.domainMutationId = 'create'
    try {
      await workspace.api.createOrgCustomDomain(orgId, { domain })
      workspace.createDomainForm.domain = ''
      await Promise.all([workspace.refreshDomains(), workspace.refreshSummary(), workspace.refreshAudit()])
      ElMessage.success(t('organizations.messages.domainAdded'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.domainAddFailed'))
    } finally {
      workspace.loading.domainMutationId = ''
    }
  }

  async function onVerifyDomain(domainId: string): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    workspace.loading.domainMutationId = domainId
    try {
      await workspace.api.verifyOrgCustomDomain(orgId, domainId)
      await Promise.all([workspace.refreshDomains(), workspace.refreshSummary(), workspace.refreshAudit()])
      ElMessage.success(t('organizations.messages.domainVerified'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.domainVerifyFailed'))
    } finally {
      workspace.loading.domainMutationId = ''
    }
  }

  async function onSetDefaultDomain(domainId: string): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    workspace.loading.domainMutationId = domainId
    try {
      await workspace.api.setDefaultOrgCustomDomain(orgId, domainId)
      await Promise.all([workspace.refreshDomains(), workspace.refreshSummary(), workspace.refreshAudit()])
      ElMessage.success(t('organizations.messages.defaultDomainUpdated'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.defaultDomainUpdateFailed'))
    } finally {
      workspace.loading.domainMutationId = ''
    }
  }

  async function onRemoveDomain(domainId: string): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    workspace.loading.domainMutationId = domainId
    try {
      await workspace.api.removeOrgCustomDomain(orgId, domainId)
      await Promise.all([workspace.refreshDomains(), workspace.refreshSummary(), workspace.refreshAudit()])
      ElMessage.success(t('organizations.messages.domainRemoved'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.domainRemoveFailed'))
    } finally {
      workspace.loading.domainMutationId = ''
    }
  }

  async function onCreateMailIdentity(): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    if (!workspace.createIdentityForm.memberId || !workspace.createIdentityForm.customDomainId) {
      ElMessage.warning(t('organizations.messages.memberAndDomainRequired'))
      return
    }
    if (!workspace.createIdentityForm.localPart.trim()) {
      ElMessage.warning(t('organizations.messages.localPartRequired'))
      return
    }
    workspace.loading.identityMutationId = 'create'
    try {
      await workspace.api.createOrgMailIdentity(orgId, {
        memberId: workspace.createIdentityForm.memberId,
        customDomainId: workspace.createIdentityForm.customDomainId,
        localPart: workspace.createIdentityForm.localPart.trim(),
        displayName: workspace.createIdentityForm.displayName.trim() || undefined
      })
      workspace.createIdentityForm.localPart = ''
      workspace.createIdentityForm.displayName = ''
      await Promise.all([workspace.refreshIdentities(), workspace.refreshSummary(), workspace.refreshAudit()])
      ElMessage.success(t('organizations.messages.identityCreated'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.identityCreateFailed'))
    } finally {
      workspace.loading.identityMutationId = ''
    }
  }

  async function onSetDefaultMailIdentity(identityId: string): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    workspace.loading.identityMutationId = identityId
    try {
      await workspace.api.setDefaultOrgMailIdentity(orgId, identityId)
      await Promise.all([workspace.refreshIdentities(), workspace.refreshSummary(), workspace.refreshAudit()])
      ElMessage.success(t('organizations.messages.defaultSenderUpdated'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.defaultSenderUpdateFailed'))
    } finally {
      workspace.loading.identityMutationId = ''
    }
  }

  async function onEnableMailIdentity(identityId: string): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    workspace.loading.identityMutationId = identityId
    try {
      await workspace.api.enableOrgMailIdentity(orgId, identityId)
      await Promise.all([workspace.refreshIdentities(), workspace.refreshSummary(), workspace.refreshAudit()])
      ElMessage.success(t('organizations.messages.identityEnabled'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.identityEnableFailed'))
    } finally {
      workspace.loading.identityMutationId = ''
    }
  }

  async function onDisableMailIdentity(identityId: string): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    workspace.loading.identityMutationId = identityId
    try {
      await workspace.api.disableOrgMailIdentity(orgId, identityId)
      await Promise.all([workspace.refreshIdentities(), workspace.refreshSummary(), workspace.refreshAudit()])
      ElMessage.success(t('organizations.messages.identityDisabled'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.identityDisableFailed'))
    } finally {
      workspace.loading.identityMutationId = ''
    }
  }

  async function onRemoveMailIdentity(identityId: string): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    workspace.loading.identityMutationId = identityId
    try {
      await workspace.api.removeOrgMailIdentity(orgId, identityId)
      await Promise.all([workspace.refreshIdentities(), workspace.refreshSummary(), workspace.refreshAudit()])
      ElMessage.success(t('organizations.messages.identityRemoved'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.identityRemoveFailed'))
    } finally {
      workspace.loading.identityMutationId = ''
    }
  }

  async function onToggleProduct(
    memberId: string,
    productKey: Parameters<typeof workspace.buildProductPayload>[1],
    enabled: boolean
  ): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    workspace.loading.productMutationId = memberId
    try {
      const payload = workspace.buildProductPayload(memberId, productKey, enabled)
      const updatedRow = await workspace.api.updateOrgMemberProductAccess(orgId, memberId, payload)
      workspace.updateProductRow(updatedRow)
      await Promise.all([workspace.refreshSummary(), workspace.refreshAudit()])
      ElMessage.success(t('organizations.messages.productAccessUpdated'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.productAccessUpdateFailed'))
      await workspace.refreshProductAccess()
    } finally {
      workspace.loading.productMutationId = ''
    }
  }

  return {
    openCreateOrgDialog,
    onOrganizationChange,
    onRefreshAll,
    onRefreshAudit,
    onExportAudit,
    onCreateOrganization,
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
    onToggleProduct
  }
}
