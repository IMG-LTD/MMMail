import type { MailIdentityStatus, OrgAuditEvent, OrgRole } from '~/types/api'

export type OrgProductKey = 'MAIL' | 'CALENDAR' | 'DRIVE' | 'DOCS' | 'SHEETS' | 'PASS' | 'SIMPLELOGIN' | 'STANDARD_NOTES' | 'VPN' | 'WALLET' | 'AUTHENTICATOR' | 'MEET' | 'LUMO'
export type OrgProductAccessState = 'ENABLED' | 'DISABLED'
export type OrgCustomDomainStatus = 'PENDING_VERIFICATION' | 'VERIFIED'
export type OrgAuditSortDirection = 'ASC' | 'DESC'
export type OrgMonitorVisibilityScope = 'ALL_ADMINS'
export type OrgMonitorRetentionMode = 'PERMANENT'

export interface OrgAdminConsoleSummary {
  orgId: string
  orgName: string
  currentRole: OrgRole
  memberCount: number
  adminCount: number
  domainCount: number
  verifiedDomainCount: number
  mailIdentityCount: number
  enabledMailIdentityCount: number
  enabledProductCount: number
  defaultDomain: string | null
  defaultSenderAddress: string | null
  generatedAt: string
}

export interface OrgCustomDomain {
  id: string
  orgId: string
  domain: string
  verificationToken: string
  status: OrgCustomDomainStatus
  defaultDomain: boolean
  createdBy: string | null
  verifiedAt: string | null
  updatedAt: string
}

export interface OrgMailIdentity {
  id: string
  orgId: string
  memberId: string
  memberEmail: string | null
  customDomainId: string
  localPart: string
  emailAddress: string
  displayName: string | null
  status: MailIdentityStatus
  defaultIdentity: boolean
  createdBy: string | null
  updatedAt: string
}

export interface OrgMemberSession {
  sessionId: string
  memberId: string
  userId: string | null
  memberEmail: string
  role: OrgRole
  createdAt: string
  expiresAt: string
  current: boolean
}

export interface OrgProductAccessItem {
  productKey: OrgProductKey
  accessState: OrgProductAccessState
}

export interface OrgMemberProductAccess {
  memberId: string
  userId: string | null
  userEmail: string
  role: OrgRole
  currentUser: boolean
  enabledProductCount: number
  products: OrgProductAccessItem[]
}

export interface OrgMonitorStatus {
  orgId: string
  alwaysOn: boolean
  canDisable: boolean
  canDeleteEvents: boolean
  canEditEvents: boolean
  visibilityScope: OrgMonitorVisibilityScope
  retentionMode: OrgMonitorRetentionMode
  totalEvents: number
  coveredEventTypes: number
  activeSessions: number
  managerSessions: number
  protectedSessions: number
  maximumExportSize: number
  oldestEventAt: string | null
  latestEvent: OrgAuditEvent | null
  generatedAt: string
}

export interface CreateOrgCustomDomainRequest {
  domain: string
}

export interface CreateOrgMailIdentityRequest {
  memberId: string
  customDomainId: string
  localPart: string
  displayName?: string
}

export interface UpdateOrgMemberProductAccessRequest {
  products: OrgProductAccessItem[]
}

export interface OrganizationSummaryCard {
  label: string
  value: string
  hint: string
}
