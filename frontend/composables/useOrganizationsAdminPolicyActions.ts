import { ElMessage } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import type { useOrganizationsAdminCore } from '~/composables/useOrganizationsAdminCore'
import type { UpdateOrganizationPolicyRequest } from '~/types/organization-policy'

export function useOrganizationsAdminPolicyActions(workspace: ReturnType<typeof useOrganizationsAdminCore>) {
  const { t } = useI18n()

  function resolveSelectedOrgId(): string | null {
    if (workspace.selectedOrgId.value) {
      return workspace.selectedOrgId.value
    }
    ElMessage.warning(t('organizations.messages.selectOrgFirst'))
    return null
  }

  function resolveErrorMessage(error: unknown): string {
    return error instanceof Error && error.message
      ? error.message
      : t('organizations.messages.policyUpdateFailed')
  }

  async function onSavePolicy(): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    workspace.loading.savePolicy = true
    try {
      const payload: UpdateOrganizationPolicyRequest = {
        allowedEmailDomains: workspace.policyForm.allowedEmailDomainsText
          .split(/[\n,]/)
          .map(item => item.trim())
          .filter(Boolean),
        memberLimit: workspace.policyForm.memberLimit,
        governanceReviewSlaHours: workspace.policyForm.governanceReviewSlaHours,
        adminCanInviteAdmin: workspace.policyForm.adminCanInviteAdmin,
        adminCanRemoveAdmin: workspace.policyForm.adminCanRemoveAdmin,
        adminCanReviewGovernance: workspace.policyForm.adminCanReviewGovernance,
        adminCanExecuteGovernance: workspace.policyForm.adminCanExecuteGovernance,
        requireDualReviewGovernance: workspace.policyForm.requireDualReviewGovernance,
        twoFactorEnforcementLevel: workspace.policyForm.twoFactorEnforcementLevel,
        twoFactorGracePeriodDays: workspace.policyForm.twoFactorGracePeriodDays
      }
      workspace.policy.value = await workspace.api.updateOrgPolicy(orgId, payload)
      workspace.applyPolicyToForm(workspace.policy.value)
      await Promise.all([workspace.refreshAudit(), workspace.refreshAuthenticationSecurity()])
      ElMessage.success(t('organizations.messages.policyUpdated'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error))
    } finally {
      workspace.loading.savePolicy = false
    }
  }

  return {
    onSavePolicy
  }
}
