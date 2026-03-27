import type { ApiResponse, AuthenticatorCodePayload, OrgAuditEvent } from '~/types/api'
import type {
  AddPassSharedVaultMemberRequest,
  CreatePassAliasContactRequest,
  CreatePassItemShareRequest,
  CreatePassMailboxRequest,
  CreatePassMailAliasRequest,
  CreatePassSecureLinkRequest,
  CreatePassSharedVaultRequest,
  CreatePassWorkspaceItemRequest,
  GeneratePassPasswordRequest,
  PassAliasContact,
  PassBusinessOverview,
  PassMailbox,
  PassBusinessPolicy,
  PassGeneratedPassword,
  PassIncomingSharedItemDetail,
  PassIncomingSharedItemSummary,
  PassItemShare,
  PassMonitorOverview,
  PassMailAlias,
  PassSecureLinkDashboardEntry,
  UpsertPassItemTwoFactorRequest,
  PassPublicSecureLink,
  PassSecureLink,
  PassSharedVault,
  PassSharedVaultMember,
  PassWorkspaceItemDetail,
  PassWorkspaceItemSummary,
  UpdatePassAliasContactRequest,
  UpdatePassMailAliasRequest,
  VerifyPassMailboxRequest,
  UpdatePassBusinessPolicyRequest,
  UpdatePassWorkspaceItemRequest
} from '~/types/pass-business'

export function usePassApi() {
  const { $apiClient } = useNuxtApp()

  async function listItems(
    keyword = '',
    favoriteOnly = false,
    limit = 100,
    itemType?: string
  ): Promise<PassWorkspaceItemSummary[]> {
    const response = await $apiClient.get<ApiResponse<PassWorkspaceItemSummary[]>>('/api/v1/pass/items', {
      params: { keyword, favoriteOnly, limit, itemType: itemType || undefined }
    })
    return response.data.data
  }

  async function createItem(payload: CreatePassWorkspaceItemRequest): Promise<PassWorkspaceItemDetail> {
    const response = await $apiClient.post<ApiResponse<PassWorkspaceItemDetail>>('/api/v1/pass/items', payload)
    return response.data.data
  }

  async function getItem(itemId: string): Promise<PassWorkspaceItemDetail> {
    const response = await $apiClient.get<ApiResponse<PassWorkspaceItemDetail>>(`/api/v1/pass/items/${itemId}`)
    return response.data.data
  }

  async function updateItem(itemId: string, payload: UpdatePassWorkspaceItemRequest): Promise<PassWorkspaceItemDetail> {
    const response = await $apiClient.put<ApiResponse<PassWorkspaceItemDetail>>(`/api/v1/pass/items/${itemId}`, payload)
    return response.data.data
  }

  async function deleteItem(itemId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/pass/items/${itemId}`)
  }

  async function favoriteItem(itemId: string): Promise<PassWorkspaceItemDetail> {
    const response = await $apiClient.post<ApiResponse<PassWorkspaceItemDetail>>(`/api/v1/pass/items/${itemId}/favorite`)
    return response.data.data
  }

  async function unFavoriteItem(itemId: string): Promise<PassWorkspaceItemDetail> {
    const response = await $apiClient.delete<ApiResponse<PassWorkspaceItemDetail>>(`/api/v1/pass/items/${itemId}/favorite`)
    return response.data.data
  }

  async function listMailboxes(): Promise<PassMailbox[]> {
    const response = await $apiClient.get<ApiResponse<PassMailbox[]>>('/api/v1/pass/mailboxes')
    return response.data.data
  }

  async function createMailbox(payload: CreatePassMailboxRequest): Promise<PassMailbox> {
    const response = await $apiClient.post<ApiResponse<PassMailbox>>('/api/v1/pass/mailboxes', payload)
    return response.data.data
  }

  async function verifyMailbox(mailboxId: string, payload: VerifyPassMailboxRequest): Promise<PassMailbox> {
    const response = await $apiClient.post<ApiResponse<PassMailbox>>(`/api/v1/pass/mailboxes/${mailboxId}/verify`, payload)
    return response.data.data
  }

  async function setDefaultMailbox(mailboxId: string): Promise<PassMailbox> {
    const response = await $apiClient.post<ApiResponse<PassMailbox>>(`/api/v1/pass/mailboxes/${mailboxId}/default`)
    return response.data.data
  }

  async function deleteMailbox(mailboxId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/pass/mailboxes/${mailboxId}`)
  }

  async function listAliases(): Promise<PassMailAlias[]> {
    const response = await $apiClient.get<ApiResponse<PassMailAlias[]>>('/api/v1/pass/aliases')
    return response.data.data
  }

  async function createAlias(payload: CreatePassMailAliasRequest): Promise<PassMailAlias> {
    const response = await $apiClient.post<ApiResponse<PassMailAlias>>('/api/v1/pass/aliases', payload)
    return response.data.data
  }

  async function updateAlias(aliasId: string, payload: UpdatePassMailAliasRequest): Promise<PassMailAlias> {
    const response = await $apiClient.put<ApiResponse<PassMailAlias>>(`/api/v1/pass/aliases/${aliasId}`, payload)
    return response.data.data
  }

  async function enableAlias(aliasId: string): Promise<PassMailAlias> {
    const response = await $apiClient.post<ApiResponse<PassMailAlias>>(`/api/v1/pass/aliases/${aliasId}/enable`)
    return response.data.data
  }

  async function disableAlias(aliasId: string): Promise<PassMailAlias> {
    const response = await $apiClient.post<ApiResponse<PassMailAlias>>(`/api/v1/pass/aliases/${aliasId}/disable`)
    return response.data.data
  }

  async function deleteAlias(aliasId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/pass/aliases/${aliasId}`)
  }

  async function listAliasContacts(aliasId: string): Promise<PassAliasContact[]> {
    const response = await $apiClient.get<ApiResponse<PassAliasContact[]>>(`/api/v1/pass/aliases/${aliasId}/contacts`)
    return response.data.data
  }

  async function createAliasContact(aliasId: string, payload: CreatePassAliasContactRequest): Promise<PassAliasContact> {
    const response = await $apiClient.post<ApiResponse<PassAliasContact>>(`/api/v1/pass/aliases/${aliasId}/contacts`, payload)
    return response.data.data
  }

  async function updateAliasContact(
    aliasId: string,
    contactId: string,
    payload: UpdatePassAliasContactRequest
  ): Promise<PassAliasContact> {
    const response = await $apiClient.put<ApiResponse<PassAliasContact>>(
      `/api/v1/pass/aliases/${aliasId}/contacts/${contactId}`,
      payload
    )
    return response.data.data
  }

  async function deleteAliasContact(aliasId: string, contactId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/pass/aliases/${aliasId}/contacts/${contactId}`)
  }

  async function suggestAliasContacts(
    senderEmail: string,
    keyword = '',
    limit = 8
  ): Promise<PassAliasContact[]> {
    const response = await $apiClient.get<ApiResponse<PassAliasContact[]>>('/api/v1/pass/alias-contacts/suggestions', {
      params: { senderEmail, keyword, limit }
    })
    return response.data.data
  }

  async function generatePassword(payload: GeneratePassPasswordRequest): Promise<PassGeneratedPassword> {
    const response = await $apiClient.post<ApiResponse<PassGeneratedPassword>>('/api/v1/pass/password/generate', payload)
    return response.data.data
  }

  async function getBusinessOverview(orgId: string): Promise<PassBusinessOverview> {
    const response = await $apiClient.get<ApiResponse<PassBusinessOverview>>(`/api/v1/pass/orgs/${orgId}/overview`)
    return response.data.data
  }

  async function getPersonalMonitor(): Promise<PassMonitorOverview> {
    const response = await $apiClient.get<ApiResponse<PassMonitorOverview>>('/api/v1/pass/monitor')
    return response.data.data
  }

  async function getSharedMonitor(orgId: string): Promise<PassMonitorOverview> {
    const response = await $apiClient.get<ApiResponse<PassMonitorOverview>>(`/api/v1/pass/orgs/${orgId}/monitor`)
    return response.data.data
  }

  async function excludePersonalMonitorItem(itemId: string): Promise<void> {
    await $apiClient.post(`/api/v1/pass/items/${itemId}/monitor/exclude`)
  }

  async function includePersonalMonitorItem(itemId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/pass/items/${itemId}/monitor/exclude`)
  }

  async function excludeSharedMonitorItem(orgId: string, itemId: string): Promise<void> {
    await $apiClient.post(`/api/v1/pass/orgs/${orgId}/items/${itemId}/monitor/exclude`)
  }

  async function includeSharedMonitorItem(orgId: string, itemId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/pass/orgs/${orgId}/items/${itemId}/monitor/exclude`)
  }

  async function upsertPersonalItemTwoFactor(
    itemId: string,
    payload: UpsertPassItemTwoFactorRequest
  ): Promise<PassWorkspaceItemDetail> {
    const response = await $apiClient.put<ApiResponse<PassWorkspaceItemDetail>>(`/api/v1/pass/items/${itemId}/two-factor`, payload)
    return response.data.data
  }

  async function deletePersonalItemTwoFactor(itemId: string): Promise<PassWorkspaceItemDetail> {
    const response = await $apiClient.delete<ApiResponse<PassWorkspaceItemDetail>>(`/api/v1/pass/items/${itemId}/two-factor`)
    return response.data.data
  }

  async function generatePersonalItemTwoFactorCode(itemId: string): Promise<AuthenticatorCodePayload> {
    const response = await $apiClient.post<ApiResponse<AuthenticatorCodePayload>>(`/api/v1/pass/items/${itemId}/two-factor/code`)
    return response.data.data
  }

  async function upsertSharedItemTwoFactor(
    orgId: string,
    itemId: string,
    payload: UpsertPassItemTwoFactorRequest
  ): Promise<PassWorkspaceItemDetail> {
    const response = await $apiClient.put<ApiResponse<PassWorkspaceItemDetail>>(
      `/api/v1/pass/orgs/${orgId}/items/${itemId}/two-factor`,
      payload
    )
    return response.data.data
  }

  async function deleteSharedItemTwoFactor(orgId: string, itemId: string): Promise<PassWorkspaceItemDetail> {
    const response = await $apiClient.delete<ApiResponse<PassWorkspaceItemDetail>>(`/api/v1/pass/orgs/${orgId}/items/${itemId}/two-factor`)
    return response.data.data
  }

  async function generateSharedItemTwoFactorCode(orgId: string, itemId: string): Promise<AuthenticatorCodePayload> {
    const response = await $apiClient.post<ApiResponse<AuthenticatorCodePayload>>(`/api/v1/pass/orgs/${orgId}/items/${itemId}/two-factor/code`)
    return response.data.data
  }

  async function getBusinessPolicy(orgId: string): Promise<PassBusinessPolicy> {
    const response = await $apiClient.get<ApiResponse<PassBusinessPolicy>>(`/api/v1/pass/orgs/${orgId}/policy`)
    return response.data.data
  }

  async function updateBusinessPolicy(
    orgId: string,
    payload: UpdatePassBusinessPolicyRequest
  ): Promise<PassBusinessPolicy> {
    const response = await $apiClient.put<ApiResponse<PassBusinessPolicy>>(`/api/v1/pass/orgs/${orgId}/policy`, payload)
    return response.data.data
  }

  async function listSharedVaults(orgId: string, keyword = ''): Promise<PassSharedVault[]> {
    const response = await $apiClient.get<ApiResponse<PassSharedVault[]>>(`/api/v1/pass/orgs/${orgId}/shared-vaults`, {
      params: { keyword: keyword || undefined }
    })
    return response.data.data
  }

  async function createSharedVault(orgId: string, payload: CreatePassSharedVaultRequest): Promise<PassSharedVault> {
    const response = await $apiClient.post<ApiResponse<PassSharedVault>>(`/api/v1/pass/orgs/${orgId}/shared-vaults`, payload)
    return response.data.data
  }

  async function listSharedVaultMembers(orgId: string, vaultId: string): Promise<PassSharedVaultMember[]> {
    const response = await $apiClient.get<ApiResponse<PassSharedVaultMember[]>>(
      `/api/v1/pass/orgs/${orgId}/shared-vaults/${vaultId}/members`
    )
    return response.data.data
  }

  async function addSharedVaultMember(
    orgId: string,
    vaultId: string,
    payload: AddPassSharedVaultMemberRequest
  ): Promise<PassSharedVaultMember> {
    const response = await $apiClient.post<ApiResponse<PassSharedVaultMember>>(
      `/api/v1/pass/orgs/${orgId}/shared-vaults/${vaultId}/members`,
      payload
    )
    return response.data.data
  }

  async function removeSharedVaultMember(orgId: string, vaultId: string, memberId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/pass/orgs/${orgId}/shared-vaults/${vaultId}/members/${memberId}`)
  }

  async function listSharedItems(
    orgId: string,
    vaultId: string,
    keyword = '',
    favoriteOnly = false,
    limit = 100,
    itemType?: string
  ): Promise<PassWorkspaceItemSummary[]> {
    const response = await $apiClient.get<ApiResponse<PassWorkspaceItemSummary[]>>(
      `/api/v1/pass/orgs/${orgId}/shared-vaults/${vaultId}/items`,
      {
        params: {
          keyword,
          favoriteOnly,
          limit,
          itemType: itemType || undefined
        }
      }
    )
    return response.data.data
  }

  async function createSharedItem(
    orgId: string,
    vaultId: string,
    payload: CreatePassWorkspaceItemRequest
  ): Promise<PassWorkspaceItemDetail> {
    const response = await $apiClient.post<ApiResponse<PassWorkspaceItemDetail>>(
      `/api/v1/pass/orgs/${orgId}/shared-vaults/${vaultId}/items`,
      payload
    )
    return response.data.data
  }

  async function getSharedItem(orgId: string, vaultId: string, itemId: string): Promise<PassWorkspaceItemDetail> {
    const response = await $apiClient.get<ApiResponse<PassWorkspaceItemDetail>>(
      `/api/v1/pass/orgs/${orgId}/shared-vaults/${vaultId}/items/${itemId}`
    )
    return response.data.data
  }

  async function updateSharedItem(
    orgId: string,
    vaultId: string,
    itemId: string,
    payload: UpdatePassWorkspaceItemRequest
  ): Promise<PassWorkspaceItemDetail> {
    const response = await $apiClient.put<ApiResponse<PassWorkspaceItemDetail>>(
      `/api/v1/pass/orgs/${orgId}/shared-vaults/${vaultId}/items/${itemId}`,
      payload
    )
    return response.data.data
  }

  async function deleteSharedItem(orgId: string, vaultId: string, itemId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/pass/orgs/${orgId}/shared-vaults/${vaultId}/items/${itemId}`)
  }

  async function listActivity(orgId: string, limit = 50): Promise<OrgAuditEvent[]> {
    const response = await $apiClient.get<ApiResponse<OrgAuditEvent[]>>(`/api/v1/pass/orgs/${orgId}/activity`, {
      params: { limit }
    })
    return response.data.data
  }

  async function listSecureLinks(orgId: string, itemId: string): Promise<PassSecureLink[]> {
    const response = await $apiClient.get<ApiResponse<PassSecureLink[]>>(`/api/v1/pass/orgs/${orgId}/items/${itemId}/secure-links`)
    return response.data.data
  }

  async function listOrgSecureLinks(orgId: string): Promise<PassSecureLinkDashboardEntry[]> {
    const response = await $apiClient.get<ApiResponse<PassSecureLinkDashboardEntry[]>>(`/api/v1/pass/orgs/${orgId}/secure-links`)
    return response.data.data
  }

  async function listItemShares(orgId: string, itemId: string): Promise<PassItemShare[]> {
    const response = await $apiClient.get<ApiResponse<PassItemShare[]>>(`/api/v1/pass/orgs/${orgId}/items/${itemId}/item-shares`)
    return response.data.data
  }

  async function createItemShare(
    orgId: string,
    itemId: string,
    payload: CreatePassItemShareRequest
  ): Promise<PassItemShare> {
    const response = await $apiClient.post<ApiResponse<PassItemShare>>(
      `/api/v1/pass/orgs/${orgId}/items/${itemId}/item-shares`,
      payload
    )
    return response.data.data
  }

  async function removeItemShare(orgId: string, itemId: string, shareId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/pass/orgs/${orgId}/items/${itemId}/item-shares/${shareId}`)
  }

  async function listIncomingItemShares(
    orgId: string,
    keyword = '',
    favoriteOnly = false,
    limit = 100,
    itemType?: string
  ): Promise<PassIncomingSharedItemSummary[]> {
    const response = await $apiClient.get<ApiResponse<PassIncomingSharedItemSummary[]>>(
      `/api/v1/pass/orgs/${orgId}/incoming-item-shares`,
      {
        params: {
          keyword,
          favoriteOnly,
          limit,
          itemType: itemType || undefined
        }
      }
    )
    return response.data.data
  }

  async function getIncomingItemShare(orgId: string, itemId: string): Promise<PassIncomingSharedItemDetail> {
    const response = await $apiClient.get<ApiResponse<PassIncomingSharedItemDetail>>(
      `/api/v1/pass/orgs/${orgId}/incoming-item-shares/${itemId}`
    )
    return response.data.data
  }

  async function createSecureLink(
    orgId: string,
    itemId: string,
    payload: CreatePassSecureLinkRequest
  ): Promise<PassSecureLink> {
    const response = await $apiClient.post<ApiResponse<PassSecureLink>>(
      `/api/v1/pass/orgs/${orgId}/items/${itemId}/secure-links`,
      payload
    )
    return response.data.data
  }

  async function revokeSecureLink(orgId: string, linkId: string): Promise<PassSecureLink> {
    const response = await $apiClient.delete<ApiResponse<PassSecureLink>>(`/api/v1/pass/orgs/${orgId}/secure-links/${linkId}`)
    return response.data.data
  }

  async function getPublicSecureLink(token: string): Promise<PassPublicSecureLink> {
    const response = await $apiClient.get<ApiResponse<PassPublicSecureLink>>(`/api/v1/public/pass/secure-links/${token}`)
    return response.data.data
  }

  return {
    listItems,
    listMailboxes,
    createMailbox,
    verifyMailbox,
    setDefaultMailbox,
    deleteMailbox,
    listAliases,
    createAlias,
    updateAlias,
    enableAlias,
    disableAlias,
    deleteAlias,
    listAliasContacts,
    createAliasContact,
    updateAliasContact,
    deleteAliasContact,
    suggestAliasContacts,
    createItem,
    getItem,
    updateItem,
    deleteItem,
    favoriteItem,
    unFavoriteItem,
    generatePassword,
    getBusinessOverview,
    getPersonalMonitor,
    getSharedMonitor,
    excludePersonalMonitorItem,
    includePersonalMonitorItem,
    excludeSharedMonitorItem,
    includeSharedMonitorItem,
    upsertPersonalItemTwoFactor,
    deletePersonalItemTwoFactor,
    generatePersonalItemTwoFactorCode,
    upsertSharedItemTwoFactor,
    deleteSharedItemTwoFactor,
    generateSharedItemTwoFactorCode,
    getBusinessPolicy,
    updateBusinessPolicy,
    listSharedVaults,
    createSharedVault,
    listSharedVaultMembers,
    addSharedVaultMember,
    removeSharedVaultMember,
    listSharedItems,
    createSharedItem,
    getSharedItem,
    updateSharedItem,
    deleteSharedItem,
    listActivity,
    listSecureLinks,
    listOrgSecureLinks,
    listItemShares,
    createItemShare,
    removeItemShare,
    listIncomingItemShares,
    getIncomingItemShare,
    createSecureLink,
    revokeSecureLink,
    getPublicSecureLink
  }
}
