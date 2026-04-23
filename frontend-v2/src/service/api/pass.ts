import { httpClient } from '@/service/request/http'
import type { ApiResponse } from '@/shared/types/api'

export type PassWorkspaceMode = 'PERSONAL' | 'SHARED'
export type PassItemType = 'LOGIN' | 'PASSWORD' | 'NOTE' | 'CARD' | 'ALIAS' | 'PASSKEY'
export type PassMailboxStatus = 'PENDING' | 'VERIFIED'

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
  updatedAt: string
}

export interface PassMonitorOverview {
  scopeType?: PassWorkspaceMode
  orgId?: string | null
  currentRole?: string | null
  totalItemCount: number
  trackedItemCount: number
  weakPasswordCount: number
  reusedPasswordCount: number
  inactiveTwoFactorCount: number
  excludedItemCount: number
  generatedAt?: string
  weakPasswords: PassMonitorItem[]
  reusedPasswords: PassMonitorItem[]
  inactiveTwoFactorItems: PassMonitorItem[]
  excludedItems: PassMonitorItem[]
}

export function listPassItems(token: string, query: Record<string, string | undefined> = {}) {
  return httpClient.get<ApiResponse<PassWorkspaceItemSummary[]>>('/api/v1/pass/items', {
    token,
    query
  })
}

export function listPassMailboxes(token: string) {
  return httpClient.get<ApiResponse<PassMailbox[]>>('/api/v1/pass/mailboxes', { token })
}

export function readPassMonitor(token: string) {
  return httpClient.get<ApiResponse<PassMonitorOverview>>('/api/v1/pass/monitor', { token })
}
