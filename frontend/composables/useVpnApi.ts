import type { ApiResponse } from '~/types/api'
import type {
  ConnectVpnSessionRequest,
  CreateVpnProfileRequest,
  QuickConnectVpnSessionRequest,
  UpdateVpnProfileRequest,
  UpdateVpnSettingsRequest,
  VpnProfileItem,
  VpnServerItem,
  VpnSessionItem,
  VpnSettingsItem
} from '~/types/vpn'

type VpnApiResponse<T> = ApiResponse<T>

export function useVpnApi() {
  const { $apiClient } = useNuxtApp()

  async function listServers(): Promise<VpnServerItem[]> {
    const response = await $apiClient.get<VpnApiResponse<VpnServerItem[]>>('/api/v1/vpn/servers')
    return response.data.data
  }

  async function getSettings(): Promise<VpnSettingsItem> {
    const response = await $apiClient.get<VpnApiResponse<VpnSettingsItem>>('/api/v1/vpn/settings')
    return response.data.data
  }

  async function updateSettings(payload: UpdateVpnSettingsRequest): Promise<VpnSettingsItem> {
    const response = await $apiClient.put<VpnApiResponse<VpnSettingsItem>>('/api/v1/vpn/settings', payload)
    return response.data.data
  }

  async function listProfiles(): Promise<VpnProfileItem[]> {
    const response = await $apiClient.get<VpnApiResponse<VpnProfileItem[]>>('/api/v1/vpn/profiles')
    return response.data.data
  }

  async function createProfile(payload: CreateVpnProfileRequest): Promise<VpnProfileItem> {
    const response = await $apiClient.post<VpnApiResponse<VpnProfileItem>>('/api/v1/vpn/profiles', payload)
    return response.data.data
  }

  async function updateProfile(profileId: string, payload: UpdateVpnProfileRequest): Promise<VpnProfileItem> {
    const response = await $apiClient.put<VpnApiResponse<VpnProfileItem>>(`/api/v1/vpn/profiles/${profileId}`, payload)
    return response.data.data
  }

  async function deleteProfile(profileId: string): Promise<boolean> {
    const response = await $apiClient.delete<VpnApiResponse<boolean>>(`/api/v1/vpn/profiles/${profileId}`)
    return response.data.data
  }

  async function getCurrentSession(): Promise<VpnSessionItem | null> {
    const response = await $apiClient.get<VpnApiResponse<VpnSessionItem | null>>('/api/v1/vpn/sessions/current')
    return response.data.data
  }

  async function listHistory(limit = 20): Promise<VpnSessionItem[]> {
    const response = await $apiClient.get<VpnApiResponse<VpnSessionItem[]>>('/api/v1/vpn/sessions/history', {
      params: { limit }
    })
    return response.data.data
  }

  async function connect(payload: ConnectVpnSessionRequest): Promise<VpnSessionItem> {
    const response = await $apiClient.post<VpnApiResponse<VpnSessionItem>>('/api/v1/vpn/sessions/connect', payload)
    return response.data.data
  }

  async function quickConnect(profileId?: string): Promise<VpnSessionItem> {
    const payload: QuickConnectVpnSessionRequest | undefined = profileId ? { profileId } : undefined
    const response = await $apiClient.post<VpnApiResponse<VpnSessionItem>>('/api/v1/vpn/sessions/quick-connect', payload)
    return response.data.data
  }

  async function disconnect(): Promise<VpnSessionItem> {
    const response = await $apiClient.post<VpnApiResponse<VpnSessionItem>>('/api/v1/vpn/sessions/disconnect')
    return response.data.data
  }

  return {
    listServers,
    getSettings,
    updateSettings,
    listProfiles,
    createProfile,
    updateProfile,
    deleteProfile,
    getCurrentSession,
    listHistory,
    connect,
    quickConnect,
    disconnect
  }
}
