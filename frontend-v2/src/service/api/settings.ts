import type { ApiResponse, UserPreference } from '@/shared/types/api'
import { httpClient } from '@/service/request/http'

function unwrapResponse<T>(response: ApiResponse<T>) {
  return response.data
}

export async function fetchProfile(token?: string) {
  const response = await httpClient.get<ApiResponse<UserPreference>>('/api/v1/settings/profile', { token })
  return unwrapResponse(response)
}

export async function updateProfile(payload: UserPreference, token?: string) {
  const response = await httpClient.put<ApiResponse<UserPreference>>('/api/v1/settings/profile', {
    body: payload,
    token
  })
  return unwrapResponse(response)
}
