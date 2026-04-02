import type {
  ApiResponse,
  BlockedDomain,
  BlockedSender,
  MailE2eeKeyProfile,
  RuleResolution,
  TrustedDomain,
  TrustedSender,
  UpdateMailE2eeKeyProfileRequest,
  UpdateUserPreferenceRequest,
  UserPreference
} from '~/types/api'

export function useSettingsApi() {
  const { $apiClient } = useNuxtApp()

  async function fetchProfile(): Promise<UserPreference> {
    const response = await $apiClient.get<ApiResponse<UserPreference>>('/api/v1/settings/profile')
    return response.data.data
  }

  async function updateProfile(payload: UpdateUserPreferenceRequest): Promise<UserPreference> {
    const response = await $apiClient.put<ApiResponse<UserPreference>>('/api/v1/settings/profile', payload)
    return response.data.data
  }

  async function fetchMailE2eeKeyProfile(): Promise<MailE2eeKeyProfile> {
    const response = await $apiClient.get<ApiResponse<MailE2eeKeyProfile>>('/api/v1/settings/mail-e2ee')
    return response.data.data
  }

  async function updateMailE2eeKeyProfile(payload: UpdateMailE2eeKeyProfileRequest): Promise<MailE2eeKeyProfile> {
    const response = await $apiClient.put<ApiResponse<MailE2eeKeyProfile>>('/api/v1/settings/mail-e2ee', payload)
    return response.data.data
  }

  async function listBlockedSenders(): Promise<BlockedSender[]> {
    const response = await $apiClient.get<ApiResponse<BlockedSender[]>>('/api/v1/settings/blocked-senders')
    return response.data.data
  }

  async function addBlockedSender(email: string): Promise<BlockedSender> {
    const response = await $apiClient.post<ApiResponse<BlockedSender>>('/api/v1/settings/blocked-senders', { email })
    return response.data.data
  }

  async function removeBlockedSender(blockedSenderId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/settings/blocked-senders/${blockedSenderId}`)
  }

  async function listTrustedSenders(): Promise<TrustedSender[]> {
    const response = await $apiClient.get<ApiResponse<TrustedSender[]>>('/api/v1/settings/trusted-senders')
    return response.data.data
  }

  async function addTrustedSender(email: string): Promise<TrustedSender> {
    const response = await $apiClient.post<ApiResponse<TrustedSender>>('/api/v1/settings/trusted-senders', { email })
    return response.data.data
  }

  async function removeTrustedSender(trustedSenderId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/settings/trusted-senders/${trustedSenderId}`)
  }

  async function listBlockedDomains(): Promise<BlockedDomain[]> {
    const response = await $apiClient.get<ApiResponse<BlockedDomain[]>>('/api/v1/settings/blocked-domains')
    return response.data.data
  }

  async function addBlockedDomain(domain: string): Promise<BlockedDomain> {
    const response = await $apiClient.post<ApiResponse<BlockedDomain>>('/api/v1/settings/blocked-domains', { domain })
    return response.data.data
  }

  async function removeBlockedDomain(blockedDomainId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/settings/blocked-domains/${blockedDomainId}`)
  }

  async function listTrustedDomains(): Promise<TrustedDomain[]> {
    const response = await $apiClient.get<ApiResponse<TrustedDomain[]>>('/api/v1/settings/trusted-domains')
    return response.data.data
  }

  async function addTrustedDomain(domain: string): Promise<TrustedDomain> {
    const response = await $apiClient.post<ApiResponse<TrustedDomain>>('/api/v1/settings/trusted-domains', { domain })
    return response.data.data
  }

  async function removeTrustedDomain(trustedDomainId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/settings/trusted-domains/${trustedDomainId}`)
  }

  async function resolveRule(senderEmail: string): Promise<RuleResolution> {
    const response = await $apiClient.get<ApiResponse<RuleResolution>>('/api/v1/settings/rule-resolution', {
      params: { senderEmail }
    })
    return response.data.data
  }

  return {
    fetchProfile,
    updateProfile,
    fetchMailE2eeKeyProfile,
    updateMailE2eeKeyProfile,
    listBlockedSenders,
    addBlockedSender,
    removeBlockedSender,
    listTrustedSenders,
    addTrustedSender,
    removeTrustedSender,
    listBlockedDomains,
    addBlockedDomain,
    removeBlockedDomain,
    listTrustedDomains,
    addTrustedDomain,
    removeTrustedDomain,
    resolveRule
  }
}
