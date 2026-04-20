import type { ApiResponse, AuthPayload, UserSession } from '@/shared/types/api'
import { httpClient } from '@/service/request/http'

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest extends LoginRequest {
  displayName: string
}

function unwrapResponse<T>(response: ApiResponse<T>) {
  return response.data
}

export async function login(payload: LoginRequest) {
  const response = await httpClient.post<ApiResponse<AuthPayload>>('/api/v1/auth/login', { body: payload })
  return unwrapResponse(response)
}

export async function register(payload: RegisterRequest) {
  const response = await httpClient.post<ApiResponse<AuthPayload>>('/api/v1/auth/register', { body: payload })
  return unwrapResponse(response)
}

export async function refreshSession(refreshToken?: string) {
  const response = await httpClient.post<ApiResponse<AuthPayload>>('/api/v1/auth/refresh', {
    body: refreshToken ? { refreshToken } : {}
  })
  return unwrapResponse(response)
}

export async function logoutAll(token?: string) {
  await httpClient.post<void>('/api/v1/auth/logout-all', { token })
}

export async function listSessions(token?: string) {
  const response = await httpClient.get<ApiResponse<UserSession[]>>('/api/v1/auth/sessions', { token })
  return unwrapResponse(response)
}

export async function revokeSession(sessionId: string, token?: string) {
  await httpClient.post<void>(`/api/v1/auth/sessions/${sessionId}/revoke`, { token })
}
