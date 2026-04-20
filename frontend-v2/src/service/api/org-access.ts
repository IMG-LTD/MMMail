import type { ApiResponse } from '@/shared/types/api'
import type { OrgAccessScope } from '@/shared/types/organization'
import { httpClient } from '@/service/request/http'

export async function listOrgAccessContext(token?: string) {
  const response = await httpClient.get<ApiResponse<OrgAccessScope[]>>('/api/v1/orgs/access-context', { token })
  return response.data
}
