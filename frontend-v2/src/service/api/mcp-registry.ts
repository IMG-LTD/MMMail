import { httpClient } from '@/service/request/http'
import type { ApiResponse } from '@/shared/types/api'

export interface McpRegistryCapabilities {
  supportsAudit: boolean
  supportsGrantMatrix: boolean
  supportsHealthChecks: boolean
  supportsSecretMasking: boolean
}

export function readMcpRegistryCapabilities(token?: string) {
  return httpClient.get<ApiResponse<McpRegistryCapabilities>>('/api/v2/mcp/registry', { token })
}
