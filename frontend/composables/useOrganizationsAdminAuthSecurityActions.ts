import { ElMessage } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import type { useOrganizationsAdminCore } from '~/composables/useOrganizationsAdminCore'

export function useOrganizationsAdminAuthSecurityActions(workspace: ReturnType<typeof useOrganizationsAdminCore>) {
  const { t } = useI18n()

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

  async function onRefreshAuthenticationSecurity(): Promise<void> {
    try {
      await workspace.refreshAuthenticationSecurity()
      ElMessage.success(t('organizations.messages.authSecurityRefreshed'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.authSecurityRefreshFailed'))
    }
  }

  async function onSendAuthenticationSecurityReminders(): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    if (workspace.authSecurityReminderMemberIds.value.length === 0) {
      ElMessage.warning(t('organizations.messages.authSecurityReminderRequired'))
      return
    }
    workspace.loading.authSecurityReminder = true
    try {
      await workspace.api.sendOrgAuthenticationSecurityReminders(orgId, {
        memberIds: workspace.authSecurityReminderMemberIds.value
      })
      await Promise.all([workspace.refreshAuthenticationSecurity(), workspace.refreshAudit()])
      ElMessage.success(t('organizations.messages.authSecurityRemindersSent'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.authSecurityReminderFailed'))
    } finally {
      workspace.loading.authSecurityReminder = false
    }
  }

  return {
    onRefreshAuthenticationSecurity,
    onSendAuthenticationSecurityReminders
  }
}
