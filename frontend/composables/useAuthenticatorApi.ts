import type {
  ApiResponse,
  AuthenticatorBackupPayload,
  AuthenticatorCodePayload,
  AuthenticatorEntryDetail,
  AuthenticatorExportPayload,
  AuthenticatorImportResult,
  AuthenticatorEntrySummary,
  CreateAuthenticatorBackupRequest,
  CreateAuthenticatorEntryRequest,
  ImportAuthenticatorBackupRequest,
  ImportAuthenticatorEntriesRequest,
  UpdateAuthenticatorEntryRequest
} from '~/types/api'
import type {
  AuthenticatorSecurityPinVerification,
  AuthenticatorSecurityPreference,
  UpdateAuthenticatorSecurityRequest
} from '~/types/authenticator-security'

export function useAuthenticatorApi() {
  const { $apiClient } = useNuxtApp()

  async function listEntries(keyword = '', limit = 100): Promise<AuthenticatorEntrySummary[]> {
    const response = await $apiClient.get<ApiResponse<AuthenticatorEntrySummary[]>>('/api/v1/authenticator/entries', {
      params: { keyword, limit }
    })
    return response.data.data
  }

  async function createEntry(payload: CreateAuthenticatorEntryRequest): Promise<AuthenticatorEntryDetail> {
    const response = await $apiClient.post<ApiResponse<AuthenticatorEntryDetail>>('/api/v1/authenticator/entries', payload)
    return response.data.data
  }

  async function getEntry(entryId: string): Promise<AuthenticatorEntryDetail> {
    const response = await $apiClient.get<ApiResponse<AuthenticatorEntryDetail>>(`/api/v1/authenticator/entries/${entryId}`)
    return response.data.data
  }

  async function updateEntry(entryId: string, payload: UpdateAuthenticatorEntryRequest): Promise<AuthenticatorEntryDetail> {
    const response = await $apiClient.put<ApiResponse<AuthenticatorEntryDetail>>(
      `/api/v1/authenticator/entries/${entryId}`,
      payload
    )
    return response.data.data
  }

  async function deleteEntry(entryId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/authenticator/entries/${entryId}`)
  }

  async function generateCode(entryId: string): Promise<AuthenticatorCodePayload> {
    const response = await $apiClient.post<ApiResponse<AuthenticatorCodePayload>>(
      `/api/v1/authenticator/entries/${entryId}/code`
    )
    return response.data.data
  }

  async function importEntries(payload: ImportAuthenticatorEntriesRequest): Promise<AuthenticatorImportResult> {
    const response = await $apiClient.post<ApiResponse<AuthenticatorImportResult>>('/api/v1/authenticator/import', payload)
    return response.data.data
  }

  async function exportEntries(): Promise<AuthenticatorExportPayload> {
    const response = await $apiClient.get<ApiResponse<AuthenticatorExportPayload>>('/api/v1/authenticator/export')
    return response.data.data
  }

  async function exportEncryptedBackup(payload: CreateAuthenticatorBackupRequest): Promise<AuthenticatorBackupPayload> {
    const response = await $apiClient.post<ApiResponse<AuthenticatorBackupPayload>>('/api/v1/authenticator/backup/export', payload)
    return response.data.data
  }

  async function importEncryptedBackup(payload: ImportAuthenticatorBackupRequest): Promise<AuthenticatorImportResult> {
    const response = await $apiClient.post<ApiResponse<AuthenticatorImportResult>>('/api/v1/authenticator/backup/import', payload)
    return response.data.data
  }

  async function getSecurityPreference(): Promise<AuthenticatorSecurityPreference> {
    const response = await $apiClient.get<ApiResponse<AuthenticatorSecurityPreference>>('/api/v1/authenticator/security')
    return response.data.data
  }

  async function updateSecurityPreference(
    payload: UpdateAuthenticatorSecurityRequest
  ): Promise<AuthenticatorSecurityPreference> {
    const response = await $apiClient.put<ApiResponse<AuthenticatorSecurityPreference>>('/api/v1/authenticator/security', payload)
    return response.data.data
  }

  async function verifyPin(pin: string): Promise<AuthenticatorSecurityPinVerification> {
    const response = await $apiClient.post<ApiResponse<AuthenticatorSecurityPinVerification>>(
      '/api/v1/authenticator/security/verify-pin',
      { pin }
    )
    return response.data.data
  }

  async function importQrImage(dataUrl: string): Promise<AuthenticatorImportResult> {
    const response = await $apiClient.post<ApiResponse<AuthenticatorImportResult>>(
      '/api/v1/authenticator/import/qr-image',
      { dataUrl }
    )
    return response.data.data
  }

  return {
    listEntries,
    createEntry,
    getEntry,
    updateEntry,
    deleteEntry,
    generateCode,
    importEntries,
    exportEntries,
    exportEncryptedBackup,
    importEncryptedBackup,
    getSecurityPreference,
    updateSecurityPreference,
    verifyPin,
    importQrImage
  }
}
