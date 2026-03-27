import type { ApiResponse } from '~/types/api'
import type {
  CreateSimpleLoginRelayPolicyRequest,
  SimpleLoginOverview,
  SimpleLoginRelayPolicy,
  UpdateSimpleLoginRelayPolicyRequest
} from '~/types/simplelogin'

export function useSimpleLoginApi() {
  const { $apiClient } = useNuxtApp()

  async function getOverview(orgId?: string): Promise<SimpleLoginOverview> {
    const response = await $apiClient.get<ApiResponse<SimpleLoginOverview>>('/api/v1/simplelogin/overview', {
      params: { orgId: orgId || undefined }
    })
    return response.data.data
  }

  async function listRelayPolicies(orgId: string): Promise<SimpleLoginRelayPolicy[]> {
    const response = await $apiClient.get<ApiResponse<SimpleLoginRelayPolicy[]>>(`/api/v1/simplelogin/orgs/${orgId}/relay-policies`)
    return response.data.data
  }

  async function createRelayPolicy(
    orgId: string,
    payload: CreateSimpleLoginRelayPolicyRequest
  ): Promise<SimpleLoginRelayPolicy> {
    const response = await $apiClient.post<ApiResponse<SimpleLoginRelayPolicy>>(
      `/api/v1/simplelogin/orgs/${orgId}/relay-policies`,
      payload
    )
    return response.data.data
  }

  async function updateRelayPolicy(
    orgId: string,
    policyId: string,
    payload: UpdateSimpleLoginRelayPolicyRequest
  ): Promise<SimpleLoginRelayPolicy> {
    const response = await $apiClient.put<ApiResponse<SimpleLoginRelayPolicy>>(
      `/api/v1/simplelogin/orgs/${orgId}/relay-policies/${policyId}`,
      payload
    )
    return response.data.data
  }

  async function removeRelayPolicy(orgId: string, policyId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/simplelogin/orgs/${orgId}/relay-policies/${policyId}`)
  }

  return {
    getOverview,
    listRelayPolicies,
    createRelayPolicy,
    updateRelayPolicy,
    removeRelayPolicy
  }
}
