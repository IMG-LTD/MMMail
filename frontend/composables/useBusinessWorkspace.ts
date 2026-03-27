import { createBusinessWorkspaceCore } from '~/composables/useBusinessWorkspaceCore'
import { useBusinessWorkspaceActions } from '~/composables/useBusinessWorkspaceActions'

export function useBusinessWorkspace() {
  const core = createBusinessWorkspaceCore()
  const actions = useBusinessWorkspaceActions(core)
  return {
    ...core,
    ...actions
  }
}
