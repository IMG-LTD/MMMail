import type { AuthenticatorAlgorithm } from './api'

export type PassWorkspaceMode = 'PERSONAL' | 'SHARED'
export type PassItemType = 'LOGIN' | 'PASSWORD' | 'NOTE' | 'CARD' | 'ALIAS' | 'PASSKEY'
export type PassVaultRole = 'MANAGER' | 'MEMBER'
export type PassOrgRole = 'OWNER' | 'ADMIN' | 'MEMBER'

export interface PassWorkspaceItemSummary {
  id: string
  title: string
  website: string | null
  username: string | null
  favorite: boolean
  updatedAt: string
  scopeType: PassWorkspaceMode
  itemType: PassItemType
  sharedVaultId: string | null
  secureLinkCount: number
}

export interface PassWorkspaceItemDetail extends PassWorkspaceItemSummary {
  secretCiphertext: string | null
  note: string
  createdAt: string
  orgId: string | null
  ownerEmail: string | null
  twoFactor: PassItemTwoFactor
}

export interface CreatePassWorkspaceItemRequest {
  title: string
  itemType?: PassItemType
  website?: string
  username?: string
  secretCiphertext?: string
  note?: string
}

export interface UpdatePassWorkspaceItemRequest extends CreatePassWorkspaceItemRequest {}

export interface PassItemTwoFactor {
  enabled: boolean
  issuer: string | null
  accountName: string | null
  algorithm: AuthenticatorAlgorithm | null
  digits: number | null
  periodSeconds: number | null
  updatedAt: string | null
}

export interface UpsertPassItemTwoFactorRequest {
  issuer: string
  accountName: string
  secretCiphertext: string
  algorithm?: AuthenticatorAlgorithm
  digits?: number
  periodSeconds?: number
}

export interface GeneratePassPasswordRequest {
  orgId?: string
  length?: number
  includeLowercase?: boolean
  includeUppercase?: boolean
  includeDigits?: boolean
  includeSymbols?: boolean
  memorable?: boolean
}

export interface PassPasswordGeneratorForm {
  length: number
  includeLowercase: boolean
  includeUppercase: boolean
  includeDigits: boolean
  includeSymbols: boolean
  memorable: boolean
}

export interface PassGeneratedPassword {
  password: string
}

export interface PassBusinessOverview {
  orgId: string
  currentRole: PassOrgRole
  sharedVaultCount: number
  memberCount: number
  sharedItemCount: number
  secureLinkCount: number
  weakPasswordItemCount: number
  passkeyItemCount: number
  aliasItemCount: number
  allowSecureLinks: boolean
  allowExternalSharing: boolean
  forceTwoFactor: boolean
  allowPasskeys: boolean
  allowAliases: boolean
  lastActivityAt: string | null
  policyUpdatedAt: string | null
}

export interface PassSharedVault {
  id: string
  orgId: string
  name: string
  description: string | null
  accessRole: PassVaultRole
  memberCount: number
  itemCount: number
  createdByEmail: string | null
  updatedAt: string
}

export interface PassSharedVaultMember {
  id: string
  userId: string
  userEmail: string
  role: PassVaultRole
  updatedAt: string
}

export interface CreatePassSharedVaultRequest {
  name: string
  description?: string
}

export interface AddPassSharedVaultMemberRequest {
  email: string
  role: PassVaultRole
}

export type PassMailAliasStatus = 'ENABLED' | 'DISABLED'
export type PassMailboxStatus = 'PENDING' | 'VERIFIED'

export interface PassMailbox {
  id: string
  mailboxEmail: string
  status: PassMailboxStatus
  defaultMailbox: boolean
  primaryMailbox: boolean
  createdAt: string
  updatedAt: string
  verifiedAt: string | null
}

export interface CreatePassMailboxRequest {
  mailboxEmail: string
}

export interface VerifyPassMailboxRequest {
  verificationCode: string
}

export interface PassMailAlias {
  id: string
  aliasEmail: string
  title: string
  note: string | null
  forwardToEmail: string
  forwardToEmails: string[]
  status: PassMailAliasStatus
  createdAt: string
  updatedAt: string
}

export interface CreatePassMailAliasRequest {
  title: string
  note?: string
  forwardToEmail?: string
  forwardToEmails?: string[]
  prefix?: string
}

export interface UpdatePassMailAliasRequest {
  title: string
  note?: string
  forwardToEmail?: string
  forwardToEmails?: string[]
}

export interface PassAliasContact {
  id: string
  aliasId: string
  targetUserId: string
  targetEmail: string
  displayName: string | null
  note: string | null
  reverseAliasEmail: string
  createdAt: string
  updatedAt: string
}

export interface CreatePassAliasContactRequest {
  targetEmail: string
  displayName?: string
  note?: string
}

export interface UpdatePassAliasContactRequest {
  targetEmail: string
  displayName?: string
  note?: string
}

export interface PassBusinessPolicy {
  orgId: string
  minimumPasswordLength: number
  maximumPasswordLength: number
  requireUppercase: boolean
  requireDigits: boolean
  requireSymbols: boolean
  allowMemorablePasswords: boolean
  allowExternalSharing: boolean
  allowItemSharing: boolean
  allowSecureLinks: boolean
  allowMemberVaultCreation: boolean
  allowExport: boolean
  forceTwoFactor: boolean
  allowPasskeys: boolean
  allowAliases: boolean
  updatedAt: string | null
}

export interface PassBusinessPolicyForm {
  minimumPasswordLength: number
  maximumPasswordLength: number
  requireUppercase: boolean
  requireDigits: boolean
  requireSymbols: boolean
  allowMemorablePasswords: boolean
  allowExternalSharing: boolean
  allowItemSharing: boolean
  allowSecureLinks: boolean
  allowMemberVaultCreation: boolean
  allowExport: boolean
  forceTwoFactor: boolean
  allowPasskeys: boolean
  allowAliases: boolean
}

export interface UpdatePassBusinessPolicyRequest {
  minimumPasswordLength?: number
  maximumPasswordLength?: number
  requireUppercase?: boolean
  requireDigits?: boolean
  requireSymbols?: boolean
  allowMemorablePasswords?: boolean
  allowExternalSharing?: boolean
  allowItemSharing?: boolean
  allowSecureLinks?: boolean
  allowMemberVaultCreation?: boolean
  allowExport?: boolean
  forceTwoFactor?: boolean
  allowPasskeys?: boolean
  allowAliases?: boolean
}

export interface PassSecureLink {
  id: string
  itemId: string
  sharedVaultId: string
  token: string
  publicUrl: string
  maxViews: number
  currentViews: number
  expiresAt: string | null
  revokedAt: string | null
  createdAt: string
  active: boolean
}

export type PassSecureLinkStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED' | 'SPENT'
export type PassSecureLinkFilter = 'ALL' | PassSecureLinkStatus

export interface PassSecureLinkDashboardEntry {
  id: string
  itemId: string
  sharedVaultId: string
  sharedVaultName: string | null
  itemTitle: string | null
  itemWebsite: string | null
  itemUsername: string | null
  publicUrl: string
  maxViews: number
  currentViews: number
  expiresAt: string | null
  revokedAt: string | null
  createdAt: string
  active: boolean
  status: PassSecureLinkStatus
}

export interface CreatePassSecureLinkRequest {
  maxViews?: number
  expiresAt?: string | null
}

export interface PassItemShare {
  id: string
  itemId: string
  collaboratorUserId: string
  collaboratorEmail: string
  createdByEmail: string | null
  createdAt: string
  updatedAt: string
}

export interface CreatePassItemShareRequest {
  email: string
}

export interface PassIncomingSharedItemSummary {
  shareId: string
  itemId: string
  title: string
  website: string | null
  username: string | null
  itemType: PassItemType
  sourceVaultId: string
  sourceVaultName: string
  ownerEmail: string | null
  updatedAt: string
  readOnly: boolean
}

export interface PassIncomingSharedItemDetail {
  shareId: string
  itemId: string
  title: string
  website: string | null
  username: string | null
  secretCiphertext: string | null
  note: string
  itemType: PassItemType
  sourceVaultId: string
  sourceVaultName: string
  ownerEmail: string | null
  createdAt: string
  updatedAt: string
  readOnly: boolean
}

export interface PassMonitorItem {
  id: string
  title: string
  website: string | null
  username: string | null
  itemType: PassItemType
  scopeType: PassWorkspaceMode
  orgId: string | null
  sharedVaultId: string | null
  sharedVaultName: string | null
  excluded: boolean
  weakPassword: boolean
  reusedPassword: boolean
  inactiveTwoFactor: boolean
  reusedGroupSize: number
  canToggleExclusion: boolean
  canManageTwoFactor: boolean
  twoFactor: PassItemTwoFactor
  updatedAt: string
}

export interface PassMonitorOverview {
  scopeType: PassWorkspaceMode
  orgId: string | null
  currentRole: PassOrgRole | null
  totalItemCount: number
  trackedItemCount: number
  weakPasswordCount: number
  reusedPasswordCount: number
  inactiveTwoFactorCount: number
  excludedItemCount: number
  generatedAt: string
  weakPasswords: PassMonitorItem[]
  reusedPasswords: PassMonitorItem[]
  inactiveTwoFactorItems: PassMonitorItem[]
  excludedItems: PassMonitorItem[]
}

export interface PassPublicSecureLink {
  itemId: string
  sharedVaultId: string
  sharedVaultName: string
  itemType: PassItemType
  title: string
  website: string | null
  username: string | null
  secretCiphertext: string | null
  note: string
  maxViews: number
  currentViews: number
  remainingViews: number | null
  expiresAt: string | null
}
