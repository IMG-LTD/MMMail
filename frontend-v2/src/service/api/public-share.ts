import { httpClient } from '@/service/request/http'
import type { ApiResponse } from '@/shared/types/api'

export interface PublicShareCapabilities {
  auditedActions: string[]
  passwordHeader: string
  states: string[]
}

export function readPublicShareCapabilities() {
  return httpClient.get<ApiResponse<PublicShareCapabilities>>('/api/v2/public-share/capabilities')
}
