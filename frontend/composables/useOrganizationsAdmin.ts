import { useOrganizationsAdminActions } from '~/composables/useOrganizationsAdminActions'
import { useOrganizationsAdminAuthSecurityActions } from '~/composables/useOrganizationsAdminAuthSecurityActions'
import { useOrganizationsAdminCore } from '~/composables/useOrganizationsAdminCore'
import { useOrganizationsAdminMonitorActions } from '~/composables/useOrganizationsAdminMonitorActions'
import { useOrganizationsAdminPolicyActions } from '~/composables/useOrganizationsAdminPolicyActions'

export function useOrganizationsAdmin() {
  const core = useOrganizationsAdminCore()
  const actions = useOrganizationsAdminActions(core)
  const policyActions = useOrganizationsAdminPolicyActions(core)
  const authSecurityActions = useOrganizationsAdminAuthSecurityActions(core)
  const monitorActions = useOrganizationsAdminMonitorActions(core)
  return {
    ...core,
    ...actions,
    ...policyActions,
    ...authSecurityActions,
    ...monitorActions
  }
}
