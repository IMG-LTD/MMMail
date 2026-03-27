import { describe, expect, it } from 'vitest'
import { messages } from '../locales'
import type { PassMonitorOverview } from '../types/pass-business'
import { translate } from '../utils/i18n'
import {
  buildPassMonitorMetricCards,
  buildPassMonitorSections,
  resolveDefaultMonitorOrgId
} from '../utils/pass-monitor'

const tEn = (key: string, params?: Record<string, string | number>) => translate(messages, 'en', key, params)

const overview: PassMonitorOverview = {
  scopeType: 'SHARED',
  orgId: 'org-1',
  currentRole: 'MEMBER',
  totalItemCount: 5,
  trackedItemCount: 4,
  weakPasswordCount: 1,
  reusedPasswordCount: 2,
  inactiveTwoFactorCount: 1,
  excludedItemCount: 1,
  generatedAt: '2026-03-11T15:00:00',
  weakPasswords: [
    {
      id: 'item-1',
      title: 'Legacy VPN',
      website: 'vpn.example.com',
      username: 'legacy',
      itemType: 'LOGIN',
      scopeType: 'SHARED',
      orgId: 'org-1',
      sharedVaultId: 'vault-1',
      sharedVaultName: 'Ops',
      excluded: false,
      weakPassword: true,
      reusedPassword: false,
      inactiveTwoFactor: false,
      reusedGroupSize: 0,
      canToggleExclusion: false,
      canManageTwoFactor: false,
      twoFactor: {
        enabled: true,
        issuer: 'Legacy VPN',
        accountName: 'legacy',
        algorithm: 'SHA1',
        digits: 6,
        periodSeconds: 30,
        updatedAt: '2026-03-11T13:55:00'
      },
      updatedAt: '2026-03-11T14:00:00'
    }
  ],
  reusedPasswords: [
    {
      id: 'item-2',
      title: 'CRM Primary',
      website: 'crm.example.com',
      username: 'crm-a',
      itemType: 'LOGIN',
      scopeType: 'SHARED',
      orgId: 'org-1',
      sharedVaultId: 'vault-1',
      sharedVaultName: 'Ops',
      excluded: false,
      weakPassword: false,
      reusedPassword: true,
      inactiveTwoFactor: true,
      reusedGroupSize: 2,
      canToggleExclusion: false,
      canManageTwoFactor: false,
      twoFactor: {
        enabled: false,
        issuer: null,
        accountName: null,
        algorithm: null,
        digits: null,
        periodSeconds: null,
        updatedAt: null
      },
      updatedAt: '2026-03-11T14:30:00'
    },
    {
      id: 'item-3',
      title: 'CRM Backup',
      website: 'crm.example.com',
      username: 'crm-b',
      itemType: 'LOGIN',
      scopeType: 'SHARED',
      orgId: 'org-1',
      sharedVaultId: 'vault-1',
      sharedVaultName: 'Ops',
      excluded: false,
      weakPassword: false,
      reusedPassword: true,
      inactiveTwoFactor: false,
      reusedGroupSize: 2,
      canToggleExclusion: false,
      canManageTwoFactor: false,
      twoFactor: {
        enabled: true,
        issuer: 'CRM',
        accountName: 'crm-b',
        algorithm: 'SHA256',
        digits: 6,
        periodSeconds: 30,
        updatedAt: '2026-03-11T14:10:00'
      },
      updatedAt: '2026-03-11T14:40:00'
    }
  ],
  inactiveTwoFactorItems: [
    {
      id: 'item-2',
      title: 'CRM Primary',
      website: 'crm.example.com',
      username: 'crm-a',
      itemType: 'LOGIN',
      scopeType: 'SHARED',
      orgId: 'org-1',
      sharedVaultId: 'vault-1',
      sharedVaultName: 'Ops',
      excluded: false,
      weakPassword: false,
      reusedPassword: true,
      inactiveTwoFactor: true,
      reusedGroupSize: 2,
      canToggleExclusion: false,
      canManageTwoFactor: false,
      twoFactor: {
        enabled: false,
        issuer: null,
        accountName: null,
        algorithm: null,
        digits: null,
        periodSeconds: null,
        updatedAt: null
      },
      updatedAt: '2026-03-11T14:30:00'
    }
  ],
  excludedItems: [
    {
      id: 'item-4',
      title: 'Recovery Portal',
      website: null,
      username: null,
      itemType: 'PASSWORD',
      scopeType: 'SHARED',
      orgId: 'org-1',
      sharedVaultId: 'vault-2',
      sharedVaultName: 'Recovery',
      excluded: true,
      weakPassword: false,
      reusedPassword: false,
      inactiveTwoFactor: false,
      reusedGroupSize: 0,
      canToggleExclusion: true,
      canManageTwoFactor: true,
      twoFactor: {
        enabled: true,
        issuer: 'Recovery',
        accountName: 'breakglass@example.com',
        algorithm: 'SHA512',
        digits: 8,
        periodSeconds: 30,
        updatedAt: '2026-03-11T14:20:00'
      },
      updatedAt: '2026-03-11T14:50:00'
    }
  ]
}

describe('pass monitor utils', () => {
  it('builds translated metric cards', () => {
    expect(buildPassMonitorMetricCards(overview, tEn)).toEqual([
      {
        key: 'total',
        label: 'Eligible items',
        value: '5',
        hint: 'Password items monitored by this view.'
      },
      {
        key: 'tracked',
        label: 'Tracked items',
        value: '4',
        hint: 'Items still included in password health checks.'
      },
      {
        key: 'weak',
        label: 'Weak passwords',
        value: '1',
        hint: 'Credentials that are short or lack enough complexity.'
      },
      {
        key: 'reused',
        label: 'Reused passwords',
        value: '2',
        hint: 'Credentials reused across multiple items in this scope.'
      },
      {
        key: 'inactiveTwoFactor',
        label: 'Inactive 2FA',
        value: '1',
        hint: 'Login or password items that still do not store a built-in TOTP secret.'
      }
    ])
  })

  it('builds sections and empty fallbacks', () => {
    const sections = buildPassMonitorSections(overview, tEn)
    expect(sections[0].title).toBe('Weak passwords')
    expect(sections[0].items[0].title).toBe('Legacy VPN')
    expect(sections[1].items).toHaveLength(2)
    expect(sections[2].title).toBe('Inactive 2FA')
    expect(sections[2].items[0].inactiveTwoFactor).toBe(true)
    expect(sections[3].emptyText).toBe('No excluded items in this scope.')
    expect(buildPassMonitorSections(null, tEn)[0].items).toEqual([])
  })

  it('resolves a safe shared organization selection', () => {
    expect(resolveDefaultMonitorOrgId(['org-1', 'org-2'], 'org-2')).toBe('org-2')
    expect(resolveDefaultMonitorOrgId(['org-1', 'org-2'], 'missing')).toBe('org-1')
    expect(resolveDefaultMonitorOrgId([], 'missing')).toBe('')
  })
})
