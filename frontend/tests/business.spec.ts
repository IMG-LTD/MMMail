import { describe, expect, it } from 'vitest'
import {
  calculateBusinessStoragePercent,
  canManageBusinessTeamSpace,
  canWriteBusinessTeamSpace,
  formatBusinessActivityType,
  formatBusinessBytes,
  formatBusinessTime,
  isBusinessManager
} from '../utils/business'

describe('business utils', () => {
  it('formats storage bytes for business dashboard', () => {
    expect(formatBusinessBytes(512)).toBe('512 B')
    expect(formatBusinessBytes(1536)).toBe('1.50 KB')
    expect(formatBusinessBytes(12 * 1024 * 1024)).toBe('12.0 MB')
  })

  it('caps storage percentage and guards invalid limits', () => {
    expect(calculateBusinessStoragePercent(0, 0)).toBe(0)
    expect(calculateBusinessStoragePercent(512, 1024)).toBe(50)
    expect(calculateBusinessStoragePercent(4096, 1024)).toBe(100)
  })

  it('detects manager roles and formats timestamps', () => {
    expect(isBusinessManager('OWNER')).toBe(true)
    expect(isBusinessManager('ADMIN')).toBe(true)
    expect(isBusinessManager('MEMBER')).toBe(false)
    expect(formatBusinessTime(null)).toBe('-')
    expect(formatBusinessTime('2026-03-06T18:00:00')).toBe('2026-03-06T18:00:00')
  })

  it('evaluates Team Space write and manage roles', () => {
    expect(canWriteBusinessTeamSpace('MANAGER')).toBe(true)
    expect(canWriteBusinessTeamSpace('EDITOR')).toBe(true)
    expect(canWriteBusinessTeamSpace('VIEWER')).toBe(false)
    expect(canManageBusinessTeamSpace('MANAGER')).toBe(true)
    expect(canManageBusinessTeamSpace('EDITOR')).toBe(false)
  })

  it('formats Team Space activity labels', () => {
    expect(formatBusinessActivityType('ORG_TEAM_SPACE_FILE_VERSION_RESTORE')).toBe('FILE VERSION RESTORE')
    expect(formatBusinessActivityType('ORG_TEAM_SPACE_MEMBER_ADD')).toBe('MEMBER ADD')
  })
})
