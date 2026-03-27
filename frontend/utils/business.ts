import type { OrgRole } from '~/types/api'
import type { OrgTeamSpaceAccessRole } from '~/types/business'

const BYTE_BASE = 1024
const MAX_PERCENT = 100
const DECIMAL_SWITCH = 10
const BYTE_UNITS = ['KB', 'MB', 'GB', 'TB'] as const
const TEAM_SPACE_ACTIVITY_PREFIX = 'ORG_TEAM_SPACE_'

export function formatBusinessBytes(bytes: number): string {
  const value = Math.max(0, bytes)
  if (value < BYTE_BASE) {
    return `${value} B`
  }
  let scaled = value / BYTE_BASE
  let unitIndex = 0
  while (scaled >= BYTE_BASE && unitIndex < BYTE_UNITS.length - 1) {
    scaled /= BYTE_BASE
    unitIndex += 1
  }
  const digits = scaled >= DECIMAL_SWITCH ? 1 : 2
  return `${scaled.toFixed(digits)} ${BYTE_UNITS[unitIndex]}`
}

export function formatBusinessTime(value: string | null): string {
  return value || '-'
}

export function calculateBusinessStoragePercent(storageBytes: number, storageLimitBytes: number): number {
  if (storageLimitBytes <= 0) {
    return 0
  }
  const normalizedBytes = Math.max(0, storageBytes)
  return Math.min(MAX_PERCENT, Math.round((normalizedBytes / storageLimitBytes) * MAX_PERCENT))
}

export function isBusinessManager(role: OrgRole | null | undefined): boolean {
  return role === 'OWNER' || role === 'ADMIN'
}

export function canWriteBusinessTeamSpace(role: OrgTeamSpaceAccessRole | null | undefined): boolean {
  return role === 'MANAGER' || role === 'EDITOR'
}

export function canManageBusinessTeamSpace(role: OrgTeamSpaceAccessRole | null | undefined): boolean {
  return role === 'MANAGER'
}

export function formatBusinessActivityType(value: string): string {
  return value.replace(TEAM_SPACE_ACTIVITY_PREFIX, '').replaceAll('_', ' ')
}
