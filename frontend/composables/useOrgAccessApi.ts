import type { ApiResponse } from '~/types/api'
import type { OrgAccessScope } from '~/types/org-access'

export function useOrgAccessApi() {
  const { $apiClient } = useNuxtApp()

  async function listAccessContext(): Promise<OrgAccessScope[]> {
    const response = await $apiClient.get<ApiResponse<OrgAccessScope[]>>('/api/v1/orgs/access-context')
    return response.data.data
  }

  return {
    listAccessContext
  }
}
