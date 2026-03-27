import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from '~/composables/useI18n'
import type { useOrganizationsAdminCore } from '~/composables/useOrganizationsAdminCore'

export function useOrganizationsAdminMonitorActions(workspace: ReturnType<typeof useOrganizationsAdminCore>) {
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

  async function onRefreshMemberSessions(): Promise<void> {
    try {
      await workspace.refreshMemberSessions()
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.monitorRefreshFailed'))
    }
  }

  async function onRevokeMemberSession(sessionId: string): Promise<void> {
    const orgId = resolveSelectedOrgId()
    if (!orgId) {
      return
    }
    const session = workspace.memberSessions.value.find(item => item.sessionId === sessionId)
    try {
      await ElMessageBox.confirm(
        t('organizations.confirm.revokeSession.message', { email: session?.memberEmail || sessionId }),
        t('organizations.confirm.revokeSession.title'),
        { type: 'warning' }
      )
    } catch {
      return
    }
    workspace.loading.sessionMutationId = sessionId
    try {
      await workspace.api.revokeOrgMemberSession(orgId, sessionId)
      await Promise.all([workspace.refreshMemberSessions(), workspace.refreshAudit(), workspace.refreshMonitorStatus()])
      ElMessage.success(t('organizations.messages.memberSessionRevoked'))
    } catch (error) {
      ElMessage.error(resolveErrorMessage(error, 'organizations.messages.memberSessionRevokeFailed'))
    } finally {
      workspace.loading.sessionMutationId = ''
    }
  }

  return {
    onRefreshMemberSessions,
    onRevokeMemberSession
  }
}
