import { httpClient } from '@/service/request/http'
import type { ApiResponse } from '@/shared/types/api'

export interface AiPlatformCapabilities {
  runStates: string[]
  supportsApproval: boolean
  supportsAudit: boolean
  supportsPreview: boolean
}

export function readAiPlatformCapabilities(token?: string) {
  return httpClient.get<ApiResponse<AiPlatformCapabilities>>('/api/v2/ai-platform/capabilities', { token })
}
