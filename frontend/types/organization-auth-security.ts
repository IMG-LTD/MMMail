import type { TwoFactorEnforcementLevel } from '~/types/organization-policy'

export interface OrganizationAuthenticationSecurityMember {
  memberId: string
  userId: string | null
  memberEmail: string
  role: 'OWNER' | 'ADMIN' | 'MEMBER'
  twoFactorEnabled: boolean
  authenticatorEntryCount: number
  activeSessionCount: number
  lastAuthenticatorAt: string | null
  lastReminderAt: string | null
  inGracePeriod: boolean
  gracePeriodEndsAt: string | null
  blockedByPolicy: boolean
}

export interface OrganizationAuthenticationSecurity {
  orgId: string
  twoFactorEnforcementLevel: TwoFactorEnforcementLevel
  twoFactorGracePeriodDays: number
  totalActiveMembers: number
  protectedMembers: number
  unprotectedMembers: number
  protectedManagerSeats: number
  unprotectedManagerSeats: number
  enforcementBlockedMembers: number
  members: OrganizationAuthenticationSecurityMember[]
  generatedAt: string
}

export interface SendOrganizationAuthenticationSecurityReminderRequest {
  memberIds: string[]
}

export interface OrganizationAuthenticationSecurityReminderResult {
  requestedCount: number
  deliveredCount: number
  skippedProtectedCount: number
  skippedMissingCount: number
  deliveredMemberIds: string[]
}
