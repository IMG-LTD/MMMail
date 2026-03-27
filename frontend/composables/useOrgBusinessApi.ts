import type { ApiResponse } from '~/types/api'
import type {
  CreateOrgTeamSpaceFolderRequest,
  CreateOrgTeamSpaceMemberRequest,
  CreateOrgTeamSpaceRequest,
  ListOrgTeamSpaceItemsParams,
  OrgBusinessOverview,
  OrgTeamSpace,
  OrgTeamSpaceActivity,
  OrgTeamSpaceActivityCategory,
  OrgTeamSpaceFileVersion,
  OrgTeamSpaceItem,
  OrgTeamSpaceMember,
  OrgTeamSpaceTrashItem,
  UpdateOrgTeamSpaceMemberRoleRequest
} from '~/types/business'

interface DownloadedOrgTeamSpaceFile {
  blob: Blob
  fileName: string
}

export function useOrgBusinessApi() {
  const { $apiClient } = useNuxtApp()

  async function getBusinessOverview(orgId: string): Promise<OrgBusinessOverview> {
    const response = await $apiClient.get<ApiResponse<OrgBusinessOverview>>(`/api/v1/orgs/${orgId}/business/overview`)
    return response.data.data
  }

  async function listTeamSpaces(orgId: string): Promise<OrgTeamSpace[]> {
    const response = await $apiClient.get<ApiResponse<OrgTeamSpace[]>>(`/api/v1/orgs/${orgId}/team-spaces`)
    return response.data.data
  }

  async function createTeamSpace(orgId: string, payload: CreateOrgTeamSpaceRequest): Promise<OrgTeamSpace> {
    const response = await $apiClient.post<ApiResponse<OrgTeamSpace>>(`/api/v1/orgs/${orgId}/team-spaces`, payload)
    return response.data.data
  }

  async function listTeamSpaceItems(
    orgId: string,
    teamSpaceId: string,
    params: ListOrgTeamSpaceItemsParams = {}
  ): Promise<OrgTeamSpaceItem[]> {
    const response = await $apiClient.get<ApiResponse<OrgTeamSpaceItem[]>>(`/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/items`, {
      params: {
        parentId: params.parentId ?? undefined,
        keyword: params.keyword || undefined,
        itemType: params.itemType || undefined,
        limit: params.limit ?? undefined
      }
    })
    return response.data.data
  }

  async function createTeamSpaceFolder(
    orgId: string,
    teamSpaceId: string,
    payload: CreateOrgTeamSpaceFolderRequest
  ): Promise<OrgTeamSpaceItem> {
    const response = await $apiClient.post<ApiResponse<OrgTeamSpaceItem>>(
      `/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/folders`,
      payload
    )
    return response.data.data
  }

  async function uploadTeamSpaceFile(
    orgId: string,
    teamSpaceId: string,
    file: File,
    parentId?: string | null
  ): Promise<OrgTeamSpaceItem> {
    const formData = new FormData()
    formData.append('file', file)
    if (parentId) {
      formData.append('parentId', parentId)
    }
    const response = await $apiClient.post<ApiResponse<OrgTeamSpaceItem>>(
      `/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/files/upload`,
      formData
    )
    return response.data.data
  }

  async function downloadTeamSpaceFile(
    orgId: string,
    teamSpaceId: string,
    itemId: string
  ): Promise<DownloadedOrgTeamSpaceFile> {
    const response = await $apiClient.get<Blob>(`/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/files/${itemId}/download`, {
      responseType: 'blob'
    })
    const header = String(response.headers['content-disposition'] || '')
    return {
      blob: response.data,
      fileName: extractFileName(header) || `team-space-file-${itemId}`
    }
  }

  async function listTeamSpaceMembers(orgId: string, teamSpaceId: string): Promise<OrgTeamSpaceMember[]> {
    const response = await $apiClient.get<ApiResponse<OrgTeamSpaceMember[]>>(`/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/members`)
    return response.data.data
  }

  async function addTeamSpaceMember(
    orgId: string,
    teamSpaceId: string,
    payload: CreateOrgTeamSpaceMemberRequest
  ): Promise<OrgTeamSpaceMember> {
    const response = await $apiClient.post<ApiResponse<OrgTeamSpaceMember>>(
      `/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/members`,
      payload
    )
    return response.data.data
  }

  async function updateTeamSpaceMemberRole(
    orgId: string,
    teamSpaceId: string,
    memberId: string,
    payload: UpdateOrgTeamSpaceMemberRoleRequest
  ): Promise<OrgTeamSpaceMember> {
    const response = await $apiClient.put<ApiResponse<OrgTeamSpaceMember>>(
      `/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/members/${memberId}`,
      payload
    )
    return response.data.data
  }

  async function removeTeamSpaceMember(orgId: string, teamSpaceId: string, memberId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/members/${memberId}`)
  }

  async function listTeamSpaceFileVersions(
    orgId: string,
    teamSpaceId: string,
    itemId: string,
    limit = 50
  ): Promise<OrgTeamSpaceFileVersion[]> {
    const response = await $apiClient.get<ApiResponse<OrgTeamSpaceFileVersion[]>>(
      `/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/files/${itemId}/versions`,
      { params: { limit } }
    )
    return response.data.data
  }

  async function uploadTeamSpaceFileVersion(
    orgId: string,
    teamSpaceId: string,
    itemId: string,
    file: File
  ): Promise<OrgTeamSpaceItem> {
    const formData = new FormData()
    formData.append('file', file)
    const response = await $apiClient.post<ApiResponse<OrgTeamSpaceItem>>(
      `/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/files/${itemId}/versions`,
      formData
    )
    return response.data.data
  }

  async function restoreTeamSpaceFileVersion(
    orgId: string,
    teamSpaceId: string,
    itemId: string,
    versionId: string
  ): Promise<OrgTeamSpaceItem> {
    const response = await $apiClient.post<ApiResponse<OrgTeamSpaceItem>>(
      `/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/files/${itemId}/versions/${versionId}/restore`
    )
    return response.data.data
  }

  async function deleteTeamSpaceItem(orgId: string, teamSpaceId: string, itemId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/items/${itemId}`)
  }

  async function listTeamSpaceTrashItems(
    orgId: string,
    teamSpaceId: string,
    limit = 100
  ): Promise<OrgTeamSpaceTrashItem[]> {
    const response = await $apiClient.get<ApiResponse<OrgTeamSpaceTrashItem[]>>(
      `/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/trash`,
      { params: { limit } }
    )
    return response.data.data
  }

  async function restoreTeamSpaceTrashItem(
    orgId: string,
    teamSpaceId: string,
    itemId: string
  ): Promise<OrgTeamSpaceTrashItem> {
    const response = await $apiClient.post<ApiResponse<OrgTeamSpaceTrashItem>>(
      `/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/trash/${itemId}/restore`
    )
    return response.data.data
  }

  async function purgeTeamSpaceTrashItem(orgId: string, teamSpaceId: string, itemId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/trash/${itemId}`)
  }

  async function listTeamSpaceActivity(
    orgId: string,
    teamSpaceId: string,
    params: { category?: OrgTeamSpaceActivityCategory | ''; limit?: number } = {}
  ): Promise<OrgTeamSpaceActivity[]> {
    const response = await $apiClient.get<ApiResponse<OrgTeamSpaceActivity[]>>(
      `/api/v1/orgs/${orgId}/team-spaces/${teamSpaceId}/activity`,
      {
        params: {
          category: params.category || undefined,
          limit: params.limit ?? undefined
        }
      }
    )
    return response.data.data
  }

  return {
    getBusinessOverview,
    listTeamSpaces,
    createTeamSpace,
    listTeamSpaceItems,
    createTeamSpaceFolder,
    uploadTeamSpaceFile,
    downloadTeamSpaceFile,
    listTeamSpaceMembers,
    addTeamSpaceMember,
    updateTeamSpaceMemberRole,
    removeTeamSpaceMember,
    listTeamSpaceFileVersions,
    uploadTeamSpaceFileVersion,
    restoreTeamSpaceFileVersion,
    deleteTeamSpaceItem,
    listTeamSpaceTrashItems,
    restoreTeamSpaceTrashItem,
    purgeTeamSpaceTrashItem,
    listTeamSpaceActivity
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
