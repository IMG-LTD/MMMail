import type {
  ApiResponse,
  BatchRemoveOrgMembersRequest,
  BatchUpdateOrgMemberRoleRequest,
  CreateOrgRequest,
  InviteOrgMemberRequest,
  OrgAuditEvent,
  OrgBatchActionResult,
  OrgIncomingInvite,
  OrgMember,
  OrgWorkspace,
  RespondOrgInviteRequest,
  UpdateOrgMemberRoleRequest,
  UpdateOrgMemberStatusRequest
} from '~/types/api'
import type {
  OrganizationAuthenticationSecurity,
  OrganizationAuthenticationSecurityReminderResult,
  SendOrganizationAuthenticationSecurityReminderRequest
} from '~/types/organization-auth-security'
import type {
  CreateOrgCustomDomainRequest,
  CreateOrgMailIdentityRequest,
  OrgAuditSortDirection,
  OrgAdminConsoleSummary,
  OrgCustomDomain,
  OrgMailIdentity,
  OrgMemberSession,
  OrgMonitorStatus,
  OrgMemberProductAccess,
  UpdateOrgMemberProductAccessRequest
} from '~/types/organization-admin'
import type { OrganizationPolicy, UpdateOrganizationPolicyRequest } from '~/types/organization-policy'

export interface OrgAuditFilterParams {
  limit?: number
  eventType?: string
  actorEmail?: string
  keyword?: string
  fromDate?: string
  toDate?: string
  sortDirection?: OrgAuditSortDirection
}

export interface OrgAuditExportFile {
  content: string
  fileName: string
}

export interface OrgMemberSessionFilterParams {
  limit?: number
  memberEmail?: string
}

export function useOrganizationApi() {
  const { $apiClient } = useNuxtApp()

  async function createOrganization(payload: CreateOrgRequest): Promise<OrgWorkspace> {
    const response = await $apiClient.post<ApiResponse<OrgWorkspace>>('/api/v1/orgs', payload)
    return response.data.data
  }

  async function listOrganizations(): Promise<OrgWorkspace[]> {
    const response = await $apiClient.get<ApiResponse<OrgWorkspace[]>>('/api/v1/orgs')
    return response.data.data
  }

  async function listMembers(orgId: string): Promise<OrgMember[]> {
    const response = await $apiClient.get<ApiResponse<OrgMember[]>>(`/api/v1/orgs/${orgId}/members`)
    return response.data.data
  }

  async function getOrgPolicy(orgId: string): Promise<OrganizationPolicy> {
    const response = await $apiClient.get<ApiResponse<OrganizationPolicy>>(`/api/v1/orgs/${orgId}/policy`)
    return response.data.data
  }

  async function updateOrgPolicy(orgId: string, payload: UpdateOrganizationPolicyRequest): Promise<OrganizationPolicy> {
    const response = await $apiClient.put<ApiResponse<OrganizationPolicy>>(`/api/v1/orgs/${orgId}/policy`, payload)
    return response.data.data
  }

  async function inviteMember(orgId: string, payload: InviteOrgMemberRequest): Promise<OrgMember> {
    const response = await $apiClient.post<ApiResponse<OrgMember>>(`/api/v1/orgs/${orgId}/invites`, payload)
    return response.data.data
  }

  async function listIncomingInvites(): Promise<OrgIncomingInvite[]> {
    const response = await $apiClient.get<ApiResponse<OrgIncomingInvite[]>>('/api/v1/orgs/invites/incoming')
    return response.data.data
  }

  async function respondInvite(inviteId: string, payload: RespondOrgInviteRequest): Promise<OrgIncomingInvite> {
    const response = await $apiClient.post<ApiResponse<OrgIncomingInvite>>(`/api/v1/orgs/invites/${inviteId}/respond`, payload)
    return response.data.data
  }

  async function updateMemberRole(
    orgId: string,
    memberId: string,
    payload: UpdateOrgMemberRoleRequest
  ): Promise<OrgMember> {
    const response = await $apiClient.put<ApiResponse<OrgMember>>(`/api/v1/orgs/${orgId}/members/${memberId}/role`, payload)
    return response.data.data
  }

  async function updateMemberStatus(
    orgId: string,
    memberId: string,
    payload: UpdateOrgMemberStatusRequest
  ): Promise<OrgMember> {
    const response = await $apiClient.put<ApiResponse<OrgMember>>(`/api/v1/orgs/${orgId}/members/${memberId}/status`, payload)
    return response.data.data
  }

  async function removeMember(orgId: string, memberId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/orgs/${orgId}/members/${memberId}`)
  }

  async function batchUpdateMemberRole(
    orgId: string,
    payload: BatchUpdateOrgMemberRoleRequest
  ): Promise<OrgBatchActionResult> {
    const response = await $apiClient.post<ApiResponse<OrgBatchActionResult>>(`/api/v1/orgs/${orgId}/members/batch/role`, payload)
    return response.data.data
  }

  async function batchRemoveMembers(
    orgId: string,
    payload: BatchRemoveOrgMembersRequest
  ): Promise<OrgBatchActionResult> {
    const response = await $apiClient.post<ApiResponse<OrgBatchActionResult>>(`/api/v1/orgs/${orgId}/members/batch/remove`, payload)
    return response.data.data
  }

  async function getOrgAdminConsoleSummary(orgId: string): Promise<OrgAdminConsoleSummary> {
    const response = await $apiClient.get<ApiResponse<OrgAdminConsoleSummary>>(`/api/v1/orgs/${orgId}/admin-console/summary`)
    return response.data.data
  }

  async function listOrgCustomDomains(orgId: string): Promise<OrgCustomDomain[]> {
    const response = await $apiClient.get<ApiResponse<OrgCustomDomain[]>>(`/api/v1/orgs/${orgId}/domains`)
    return response.data.data
  }

  async function createOrgCustomDomain(orgId: string, payload: CreateOrgCustomDomainRequest): Promise<OrgCustomDomain> {
    const response = await $apiClient.post<ApiResponse<OrgCustomDomain>>(`/api/v1/orgs/${orgId}/domains`, payload)
    return response.data.data
  }

  async function verifyOrgCustomDomain(orgId: string, domainId: string): Promise<OrgCustomDomain> {
    const response = await $apiClient.post<ApiResponse<OrgCustomDomain>>(`/api/v1/orgs/${orgId}/domains/${domainId}/verify`)
    return response.data.data
  }

  async function setDefaultOrgCustomDomain(orgId: string, domainId: string): Promise<OrgCustomDomain> {
    const response = await $apiClient.post<ApiResponse<OrgCustomDomain>>(`/api/v1/orgs/${orgId}/domains/${domainId}/default`)
    return response.data.data
  }

  async function removeOrgCustomDomain(orgId: string, domainId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/orgs/${orgId}/domains/${domainId}`)
  }

  async function listOrgMailIdentities(orgId: string): Promise<OrgMailIdentity[]> {
    const response = await $apiClient.get<ApiResponse<OrgMailIdentity[]>>(`/api/v1/orgs/${orgId}/mail-identities`)
    return response.data.data
  }

  async function createOrgMailIdentity(orgId: string, payload: CreateOrgMailIdentityRequest): Promise<OrgMailIdentity> {
    const response = await $apiClient.post<ApiResponse<OrgMailIdentity>>(`/api/v1/orgs/${orgId}/mail-identities`, payload)
    return response.data.data
  }

  async function setDefaultOrgMailIdentity(orgId: string, identityId: string): Promise<OrgMailIdentity> {
    const response = await $apiClient.post<ApiResponse<OrgMailIdentity>>(`/api/v1/orgs/${orgId}/mail-identities/${identityId}/default`)
    return response.data.data
  }

  async function enableOrgMailIdentity(orgId: string, identityId: string): Promise<OrgMailIdentity> {
    const response = await $apiClient.post<ApiResponse<OrgMailIdentity>>(`/api/v1/orgs/${orgId}/mail-identities/${identityId}/enable`)
    return response.data.data
  }

  async function disableOrgMailIdentity(orgId: string, identityId: string): Promise<OrgMailIdentity> {
    const response = await $apiClient.post<ApiResponse<OrgMailIdentity>>(`/api/v1/orgs/${orgId}/mail-identities/${identityId}/disable`)
    return response.data.data
  }

  async function removeOrgMailIdentity(orgId: string, identityId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/orgs/${orgId}/mail-identities/${identityId}`)
  }

  async function listOrgProductAccess(orgId: string): Promise<OrgMemberProductAccess[]> {
    const response = await $apiClient.get<ApiResponse<OrgMemberProductAccess[]>>(`/api/v1/orgs/${orgId}/admin-console/product-access`)
    return response.data.data
  }

  async function updateOrgMemberProductAccess(
    orgId: string,
    memberId: string,
    payload: UpdateOrgMemberProductAccessRequest
  ): Promise<OrgMemberProductAccess> {
    const response = await $apiClient.put<ApiResponse<OrgMemberProductAccess>>(`/api/v1/orgs/${orgId}/admin-console/product-access/${memberId}`, payload)
    return response.data.data
  }

  async function listOrgAuditEvents(orgId: string, filters: OrgAuditFilterParams = {}): Promise<OrgAuditEvent[]> {
    const response = await $apiClient.get<ApiResponse<OrgAuditEvent[]>>(`/api/v1/orgs/${orgId}/audit/events`, {
      params: {
        limit: filters.limit ?? 100,
        eventType: filters.eventType || undefined,
        actorEmail: filters.actorEmail || undefined,
        keyword: filters.keyword || undefined,
        fromDate: filters.fromDate || undefined,
        toDate: filters.toDate || undefined,
        sortDirection: filters.sortDirection || undefined
      }
    })
    return response.data.data
  }

  async function listOrgMemberSessions(
    orgId: string,
    filters: OrgMemberSessionFilterParams = {}
  ): Promise<OrgMemberSession[]> {
    const response = await $apiClient.get<ApiResponse<OrgMemberSession[]>>(`/api/v1/orgs/${orgId}/admin-console/member-sessions`, {
      params: {
        limit: filters.limit ?? 60,
        memberEmail: filters.memberEmail || undefined
      }
    })
    return response.data.data
  }

  async function getOrgMonitorStatus(orgId: string): Promise<OrgMonitorStatus> {
    const response = await $apiClient.get<ApiResponse<OrgMonitorStatus>>(`/api/v1/orgs/${orgId}/admin-console/monitor-status`)
    return response.data.data
  }

  async function revokeOrgMemberSession(orgId: string, sessionId: string): Promise<void> {
    await $apiClient.post(`/api/v1/orgs/${orgId}/admin-console/member-sessions/${sessionId}/revoke`)
  }

  async function getOrgAuthenticationSecurity(orgId: string): Promise<OrganizationAuthenticationSecurity> {
    const response = await $apiClient.get<ApiResponse<OrganizationAuthenticationSecurity>>(
      `/api/v1/orgs/${orgId}/admin-console/authentication-security`
    )
    return response.data.data
  }

  async function sendOrgAuthenticationSecurityReminders(
    orgId: string,
    payload: SendOrganizationAuthenticationSecurityReminderRequest
  ): Promise<OrganizationAuthenticationSecurityReminderResult> {
    const response = await $apiClient.post<ApiResponse<OrganizationAuthenticationSecurityReminderResult>>(
      `/api/v1/orgs/${orgId}/admin-console/authentication-security/reminders`,
      {
        memberIds: payload.memberIds
      }
    )
    return response.data.data
  }

  async function exportOrgAuditEvents(orgId: string, filters: OrgAuditFilterParams = {}): Promise<OrgAuditExportFile> {
    const response = await $apiClient.get<string>(`/api/v1/orgs/${orgId}/audit/events/export`, {
      responseType: 'text',
      params: {
        limit: filters.limit ?? 100,
        eventType: filters.eventType || undefined,
        actorEmail: filters.actorEmail || undefined,
        keyword: filters.keyword || undefined,
        fromDate: filters.fromDate || undefined,
        toDate: filters.toDate || undefined,
        sortDirection: filters.sortDirection || undefined
      }
    })
    return {
      content: response.data,
      fileName: extractFileName(String(response.headers['content-disposition'] || '')) || `organization-audit-${orgId}.csv`
    }
  }

  return {
    createOrganization,
    listOrganizations,
    listMembers,
    getOrgPolicy,
    updateOrgPolicy,
    inviteMember,
    listIncomingInvites,
    respondInvite,
    updateMemberRole,
    updateMemberStatus,
    removeMember,
    batchUpdateMemberRole,
    batchRemoveMembers,
    getOrgAdminConsoleSummary,
    listOrgCustomDomains,
    createOrgCustomDomain,
    verifyOrgCustomDomain,
    setDefaultOrgCustomDomain,
    removeOrgCustomDomain,
    listOrgMailIdentities,
    createOrgMailIdentity,
    setDefaultOrgMailIdentity,
    enableOrgMailIdentity,
    disableOrgMailIdentity,
    removeOrgMailIdentity,
    listOrgProductAccess,
    updateOrgMemberProductAccess,
    listOrgMemberSessions,
    getOrgMonitorStatus,
    revokeOrgMemberSession,
    getOrgAuthenticationSecurity,
    sendOrgAuthenticationSecurityReminders,
    listOrgAuditEvents,
    exportOrgAuditEvents
  }
}

function extractFileName(contentDisposition: string): string | null {
  if (!contentDisposition) {
    return null
  }
  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1]).trim()
  }
  const plainMatch = contentDisposition.match(/filename=\"?([^\";]+)\"?/i)
  return plainMatch?.[1]?.trim() || null
}
