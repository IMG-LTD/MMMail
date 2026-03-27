import type { OrgRole } from '~/types/api'

export type OrgTeamSpaceItemType = 'FOLDER' | 'FILE'
export type OrgTeamSpaceAccessRole = 'MANAGER' | 'EDITOR' | 'VIEWER'
export type OrgTeamSpaceActivityCategory = 'MEMBER' | 'FILE' | 'VERSION' | 'TRASH'

export interface OrgBusinessOverview {
  orgId: string
  orgName: string
  currentRole: OrgRole
  memberCount: number
  adminCount: number
  pendingInviteCount: number
  teamSpaceCount: number
  storageBytes: number
  storageLimitBytes: number
  allowedEmailDomains: string[]
  governanceReviewSlaHours: number
  requireDualReviewGovernance: boolean
  generatedAt: string
}

export interface OrgTeamSpace {
  id: string
  orgId: string
  name: string
  slug: string
  description: string | null
  rootItemId: string
  storageBytes: number
  storageLimitBytes: number
  itemCount: number
  createdBy: string | null
  currentAccessRole: OrgTeamSpaceAccessRole | null
  canWrite: boolean
  canManage: boolean
  updatedAt: string
}

export interface OrgTeamSpaceItem {
  id: string
  teamSpaceId: string
  parentId: string | null
  itemType: OrgTeamSpaceItemType
  name: string
  mimeType: string | null
  sizeBytes: number
  ownerEmail: string | null
  updatedAt: string
}

export interface OrgTeamSpaceMember {
  id: string
  orgId: string
  teamSpaceId: string
  userId: string
  userEmail: string
  role: OrgTeamSpaceAccessRole
  currentUser: boolean
  updatedAt: string
}

export interface OrgTeamSpaceFileVersion {
  id: string
  itemId: string
  versionNo: number
  mimeType: string | null
  sizeBytes: number
  checksum: string | null
  ownerEmail: string | null
  createdAt: string
}

export interface OrgTeamSpaceTrashItem {
  id: string
  parentId: string | null
  itemType: OrgTeamSpaceItemType
  name: string
  mimeType: string | null
  sizeBytes: number
  ownerEmail: string | null
  trashedAt: string | null
  purgeAfterAt: string | null
  updatedAt: string
}

export interface OrgTeamSpaceActivity {
  id: string
  teamSpaceId: string
  actorEmail: string | null
  eventType: string
  detail: string
  createdAt: string
}

export interface CreateOrgTeamSpaceRequest {
  name: string
  description?: string
  storageLimitMb?: number
}

export interface CreateOrgTeamSpaceFolderRequest {
  name: string
  parentId?: string | null
}

export interface CreateOrgTeamSpaceMemberRequest {
  userEmail: string
  role: OrgTeamSpaceAccessRole
}

export interface UpdateOrgTeamSpaceMemberRoleRequest {
  role: OrgTeamSpaceAccessRole
}

export interface ListOrgTeamSpaceItemsParams {
  parentId?: string | null
  keyword?: string
  itemType?: OrgTeamSpaceItemType | ''
  limit?: number
}
