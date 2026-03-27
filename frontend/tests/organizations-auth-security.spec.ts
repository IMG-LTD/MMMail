import { describe, expect, it } from 'vitest'
import { messages } from '../locales'
import type { OrganizationAuthenticationSecurity } from '../types/organization-auth-security'
import { translate } from '../utils/i18n'
import {
  authenticationCoveragePercent,
  authenticationReminderCandidates,
  buildAuthenticationSecurityCards,
  buildTwoFactorEnforcementOptions,
  twoFactorEnforcementLabel
} from '../utils/organization-auth-security'

const overview: OrganizationAuthenticationSecurity = {
  orgId: '1',
  twoFactorEnforcementLevel: 'ADMINS',
  twoFactorGracePeriodDays: 7,
  totalActiveMembers: 5,
  protectedMembers: 3,
  unprotectedMembers: 2,
  protectedManagerSeats: 1,
  unprotectedManagerSeats: 1,
  enforcementBlockedMembers: 1,
  generatedAt: '2026-03-10T21:00:00',
  members: [
    {
      memberId: 'owner',
      userId: '101',
      memberEmail: 'owner@example.com',
      role: 'OWNER',
      twoFactorEnabled: true,
      authenticatorEntryCount: 1,
      activeSessionCount: 2,
      lastAuthenticatorAt: '2026-03-09T20:00:00',
      lastReminderAt: null,
      inGracePeriod: false,
      gracePeriodEndsAt: null,
      blockedByPolicy: false
    },
    {
      memberId: 'member',
      userId: '102',
      memberEmail: 'member@example.com',
      role: 'MEMBER',
      twoFactorEnabled: false,
      authenticatorEntryCount: 0,
      activeSessionCount: 1,
      lastAuthenticatorAt: null,
      lastReminderAt: '2026-03-10T18:00:00',
      inGracePeriod: true,
      gracePeriodEndsAt: '2026-03-17T18:00:00',
      blockedByPolicy: false
    }
  ]
}

const tEn = (key: string, params?: Record<string, string | number>) => translate(messages, 'en', key, params)
const tZhCN = (key: string, params?: Record<string, string | number>) => translate(messages, 'zh-CN', key, params)

describe('organization authentication security utils', () => {
  it('builds authentication security summary cards', () => {
    expect(buildAuthenticationSecurityCards(overview, tEn)).toEqual([
      {
        label: 'Protected members',
        value: '3/5',
        hint: '2 still unprotected'
      },
      {
        label: 'Protected managers',
        value: '1',
        hint: '1 manager seats still at risk'
      },
      {
        label: 'Members blocked now',
        value: '1',
        hint: 'Current enforcement Admins only'
      },
      {
        label: 'Grace period',
        value: '7 day(s)',
        hint: '7 day(s) before organization access is restricted'
      }
    ])
  })

  it('builds enforcement labels and options', () => {
    expect(twoFactorEnforcementLabel('OFF', tZhCN)).toBe('关闭')
    expect(twoFactorEnforcementLabel('ADMINS', tEn)).toBe('Admins only')
    expect(buildTwoFactorEnforcementOptions(tEn)).toEqual([
      { value: 'OFF', label: 'Off' },
      { value: 'ADMINS', label: 'Admins only' },
      { value: 'ALL', label: 'All members' }
    ])
  })

  it('derives reminder candidates and coverage percent', () => {
    expect(authenticationReminderCandidates(overview.members)).toEqual(['member'])
    expect(authenticationCoveragePercent(overview)).toBe(60)
    expect(authenticationCoveragePercent(null)).toBe(0)
  })
})
