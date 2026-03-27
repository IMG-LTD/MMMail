import { describe, expect, it } from 'vitest'
import type { PassBusinessPolicy, PassMailAlias, PassSecureLink } from '../types/pass-business'
import {
  buildDefaultSecureLinkExpiryValue,
  buildGeneratorPayload,
  buildAliasComposeQuery,
  canCreateSharedVault,
  canManagePassPolicy,
  DEFAULT_SECURE_LINK_EXPIRE_DAYS,
  formatAliasRouteSummary,
  formatPassAliasStatus,
  formatPassMailboxStatus,
  formatPassItemType,
  formatVaultRole,
  generatorLengthBounds,
  generatorPresetFromPolicy,
  isSecureLinkActive,
  isSecureLinkExpiryDisabled,
  limitRecentAliases,
  placeholderSecretFromPolicy,
  resolvePassSecureLinkErrorKey,
  resolveAliasRouteEmails,
  resolvePreferredAliasRouteSelection,
  secureLinkMatchesFilter,
  secureLinkStatusKey,
  secureLinkStatusLabel,
  secureLinkStatusTone,
  workspaceTitle
} from '../utils/pass'

const basePolicy: PassBusinessPolicy = {
  orgId: '1',
  minimumPasswordLength: 18,
  maximumPasswordLength: 24,
  requireUppercase: true,
  requireDigits: true,
  requireSymbols: true,
  allowMemorablePasswords: true,
  allowExternalSharing: true,
  allowItemSharing: true,
  allowSecureLinks: true,
  allowMemberVaultCreation: false,
  allowExport: false,
  forceTwoFactor: true,
  allowPasskeys: true,
  allowAliases: true,
  updatedAt: '2026-03-06T20:00:00'
}

const routeAlias: PassMailAlias = {
  id: 'alias-1',
  aliasEmail: 'shield@passmail.mmmail.local',
  title: 'Shield',
  note: null,
  forwardToEmail: 'owner@mmmail.local',
  forwardToEmails: ['owner@mmmail.local', 'ops@mmmail.local', 'owner@mmmail.local'],
  status: 'ENABLED',
  createdAt: '2026-03-06T20:00:00',
  updatedAt: '2026-03-06T20:30:00'
}

const activeLink: PassSecureLink = {
  id: '1',
  itemId: '2',
  sharedVaultId: '3',
  token: 'abc',
  publicUrl: 'https://example.com/share/pass/abc',
  maxViews: 5,
  currentViews: 1,
  expiresAt: '2099-01-01T00:00:00',
  revokedAt: null,
  createdAt: '2026-03-06T20:00:00',
  active: true
}

describe('pass business utils', () => {
  it('maps labels and titles', () => {
    expect(formatPassItemType('PASSKEY')).toBe('Passkey')
    expect(formatPassAliasStatus('ENABLED')).toBe('Enabled')
    expect(formatPassMailboxStatus('VERIFIED')).toBe('Verified')
    expect(formatVaultRole('MANAGER')).toBe('Vault Manager')
    expect(workspaceTitle('SHARED')).toBe('Shared Vaults')
  })

  it('uses translator when formatting shared workspace labels', () => {
    const tMock = (key: string, params?: Record<string, string | number>) => {
      if (key === 'pass.shared.status.enabled') {
        return '已启用'
      }
      if (key === 'pass.shared.mailboxStatus.verified') {
        return '已验证'
      }
      if (key === 'pass.shared.routes.summary') {
        return `${params?.email} +${params?.count}`
      }
      return key
    }

    expect(formatPassAliasStatus('ENABLED', tMock)).toBe('已启用')
    expect(formatPassMailboxStatus('VERIFIED', tMock)).toBe('已验证')
    expect(formatAliasRouteSummary(routeAlias, tMock)).toBe('owner@mmmail.local +1')
  })

  it('builds alias compose query', () => {
    expect(buildAliasComposeQuery('alias@passmail.mmmail.local', 'reply@reply.passmail.mmmail.local')).toEqual({
      from: 'alias@passmail.mmmail.local',
      to: 'reply@reply.passmail.mmmail.local'
    })
    expect(buildAliasComposeQuery('alias@passmail.mmmail.local', 'reply@reply.passmail.mmmail.local', 'Hello')).toEqual({
      from: 'alias@passmail.mmmail.local',
      to: 'reply@reply.passmail.mmmail.local',
      subject: 'Hello'
    })
  })

  it('normalizes alias routes', () => {
    expect(resolveAliasRouteEmails(routeAlias)).toEqual(['owner@mmmail.local', 'ops@mmmail.local'])
    expect(formatAliasRouteSummary(routeAlias)).toBe('owner@mmmail.local +1')
    expect(formatAliasRouteSummary({ ...routeAlias, forwardToEmails: [], forwardToEmail: 'single@mmmail.local' })).toBe('single@mmmail.local')
    expect(formatAliasRouteSummary(null)).toBe('No routes')
  })

  it('picks preferred alias routes and recent aliases', () => {
    expect(resolvePreferredAliasRouteSelection(['default@mmmail.local', 'user@mmmail.local'], 'USER@mmmail.local')).toEqual(['user@mmmail.local'])
    expect(resolvePreferredAliasRouteSelection(['default@mmmail.local', 'user@mmmail.local'], 'missing@mmmail.local')).toEqual(['default@mmmail.local'])
    expect(limitRecentAliases([routeAlias, { ...routeAlias, id: '2' }, { ...routeAlias, id: '3' }, { ...routeAlias, id: '4' }], 3).map(item => item.id)).toEqual(['alias-1', '2', '3'])
  })

  it('derives permissions from policy and role', () => {
    expect(canCreateSharedVault('ADMIN', basePolicy)).toBe(true)
    expect(canCreateSharedVault('MEMBER', basePolicy)).toBe(false)
    expect(canManagePassPolicy('OWNER')).toBe(true)
    expect(canManagePassPolicy('MEMBER')).toBe(false)
  })

  it('builds generator preset from policy', () => {
    expect(generatorLengthBounds(basePolicy)).toEqual({ min: 18, max: 24 })
    const preset = generatorPresetFromPolicy(basePolicy)
    expect(preset.length).toBe(18)
    expect(preset.includeUppercase).toBe(true)
    expect(preset.includeDigits).toBe(true)
    expect(preset.includeSymbols).toBe(true)
    expect(preset.memorable).toBe(false)
  })

  it('builds generator payload from policy and form', () => {
    const payload = buildGeneratorPayload('1', {
      length: 20,
      includeLowercase: true,
      includeUppercase: false,
      includeDigits: false,
      includeSymbols: false,
      memorable: true
    }, basePolicy)
    expect(payload.orgId).toBe('1')
    expect(payload.length).toBe(20)
    expect(payload.includeUppercase).toBe(true)
    expect(payload.includeDigits).toBe(true)
    expect(payload.includeSymbols).toBe(true)
    expect(payload.memorable).toBe(true)
  })

  it('preserves large org ids in generator payload', () => {
    const payload = buildGeneratorPayload('2031516656619614210', {
      length: 18,
      includeLowercase: true,
      includeUppercase: false,
      includeDigits: false,
      includeSymbols: false,
      memorable: true
    }, basePolicy)
    expect(payload.orgId).toBe('2031516656619614210')
  })

  it('builds placeholder secret from policy', () => {
    const secret = placeholderSecretFromPolicy(basePolicy)
    expect(secret.length).toBe(18)
    expect(/[A-Z]/.test(secret)).toBe(true)
    expect(/\d/.test(secret)).toBe(true)
    expect(/[^A-Za-z0-9]/.test(secret)).toBe(true)
  })

  it('derives secure link state and labels', () => {
    expect(isSecureLinkActive(activeLink)).toBe(true)
    expect(secureLinkStatusKey(activeLink)).toBe('pass.secureLinks.status.active')
    expect(secureLinkStatusLabel(activeLink)).toBe('Active')
    expect(secureLinkStatusTone(activeLink)).toBe('success')
    expect(secureLinkStatusLabel({ ...activeLink, revokedAt: '2026-03-06T20:10:00' })).toBe('Revoked')
    expect(secureLinkStatusKey({ ...activeLink, revokedAt: '2026-03-06T20:10:00' })).toBe('pass.secureLinks.status.revoked')
    expect(secureLinkStatusTone({ ...activeLink, revokedAt: '2026-03-06T20:10:00' })).toBe('info')
    expect(secureLinkStatusLabel({ ...activeLink, currentViews: 5, active: false })).toBe('Spent')
    expect(secureLinkStatusKey({ ...activeLink, currentViews: 5, active: false })).toBe('pass.secureLinks.status.spent')
    expect(secureLinkStatusTone({ ...activeLink, currentViews: 5, active: false })).toBe('warning')
  })

  it('maps public secure link backend errors to i18n keys', () => {
    expect(resolvePassSecureLinkErrorKey('Pass secure link is not found')).toBe('pass.publicShare.errors.notFound')
    expect(resolvePassSecureLinkErrorKey('Pass secure link has been revoked')).toBe('pass.publicShare.errors.revoked')
    expect(resolvePassSecureLinkErrorKey('Pass secure link has expired')).toBe('pass.publicShare.errors.expired')
    expect(resolvePassSecureLinkErrorKey('Pass secure link has reached the maximum views')).toBe('pass.publicShare.errors.spent')
    expect(resolvePassSecureLinkErrorKey('unknown')).toBeNull()
  })

  it('builds default secure link expiry and blocks dates outside the 30 day window', () => {
    const reference = new Date('2026-03-11T10:00:00')
    const value = buildDefaultSecureLinkExpiryValue(reference)
    expect(value.startsWith(`2026-03-${String(11 + DEFAULT_SECURE_LINK_EXPIRE_DAYS).padStart(2, '0')}`)).toBe(true)
    expect(isSecureLinkExpiryDisabled(new Date('2026-03-11T09:59:59'), reference)).toBe(true)
    expect(isSecureLinkExpiryDisabled(new Date('2026-03-18T10:00:00'), reference)).toBe(false)
    expect(isSecureLinkExpiryDisabled(new Date('2026-04-11T10:00:01'), reference)).toBe(true)
  })

  it('matches secure link filters', () => {
    expect(secureLinkMatchesFilter(activeLink, 'ALL')).toBe(true)
    expect(secureLinkMatchesFilter(activeLink, 'ACTIVE')).toBe(true)
    expect(secureLinkMatchesFilter({ ...activeLink, revokedAt: '2026-03-06T20:10:00' }, 'REVOKED')).toBe(true)
    expect(secureLinkMatchesFilter({ ...activeLink, currentViews: 5, active: false }, 'SPENT')).toBe(true)
    expect(secureLinkMatchesFilter({ ...activeLink, currentViews: 5, active: false }, 'ACTIVE')).toBe(false)
  })
})
