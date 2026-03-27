import { describe, expect, it } from 'vitest'
import type { OrgAuditEvent, OrgPolicy } from '../types/api'
import type { OrgAdminConsoleSummary } from '../types/organization-admin'
import { messages } from '../locales'
import { translate } from '../utils/i18n'
import {
  buildOrganizationInviteRoleOptions,
  buildOrganizationPolicyChips,
  buildOrganizationProductCatalog,
  buildOrganizationSummaryCards,
  canInviteOrganizationRole,
  canManageOrganizationMemberRole,
  canRemoveOrganizationMember,
  domainStatusLabel,
  filterOrganizationAuditEvents,
  formatOrganizationAuditType,
  isOrganizationManager,
  mailIdentityStatusLabel,
  organizationRoleLabel,
  productAccessLabel
} from '../utils/organization-admin'

const summary: OrgAdminConsoleSummary = {
  orgId: '1',
  orgName: 'Parity Org',
  currentRole: 'OWNER',
  memberCount: 12,
  adminCount: 3,
  domainCount: 2,
  verifiedDomainCount: 1,
  mailIdentityCount: 4,
  enabledMailIdentityCount: 3,
  enabledProductCount: 9,
  defaultDomain: 'mail.example.com',
  defaultSenderAddress: 'ops@mail.example.com',
  generatedAt: '2026-03-07T01:40:00'
}

const policy: OrgPolicy = {
  orgId: '1',
  allowedEmailDomains: ['example.com', 'example.org'],
  memberLimit: 250,
  governanceReviewSlaHours: 12,
  adminCanInviteAdmin: true,
  adminCanRemoveAdmin: false,
  adminCanReviewGovernance: true,
  adminCanExecuteGovernance: true,
  requireDualReviewGovernance: true,
  updatedAt: '2026-03-07T01:40:00'
}

const auditEvents: OrgAuditEvent[] = [
  {
    id: '1',
    orgId: '1',
    actorId: '10',
    actorEmail: 'owner@example.com',
    eventType: 'ORG_DOMAIN_ADD',
    ipAddress: '127.0.0.1',
    detail: 'orgId=1,domain=example.com',
    createdAt: '2026-03-07T01:40:00'
  },
  {
    id: '2',
    orgId: '1',
    actorId: '10',
    actorEmail: 'owner@example.com',
    eventType: 'MAIL_SEND',
    ipAddress: '127.0.0.1',
    detail: 'mailId=1',
    createdAt: '2026-03-07T01:41:00'
  }
]

const tEn = (key: string, params?: Record<string, string | number>) => translate(messages, 'en', key, params)
const tZhCN = (key: string, params?: Record<string, string | number>) => translate(messages, 'zh-CN', key, params)

describe('organization admin utils', () => {
  it('builds summary cards from admin console data', () => {
    const cards = buildOrganizationSummaryCards(summary, tEn)
    expect(cards).toHaveLength(5)
    expect(cards[0]).toEqual({ label: 'Members', value: '12', hint: 'Admins 3' })
    expect(cards[2]).toEqual({ label: 'Mail identities', value: '4', hint: '3 enabled' })
    expect(cards[4]).toEqual({
      label: 'Default sender',
      value: 'ops@mail.example.com',
      hint: 'Generated 2026-03-07T01:40:00'
    })
  })

  it('builds governance chips from organization policy', () => {
    expect(buildOrganizationPolicyChips(policy, tEn)).toEqual([
      'Allowed domains 2',
      'Member limit 250',
      'Governance SLA 12h',
      'Dual review required'
    ])
  })

  it('formats translated audit, role, product, and permission labels', () => {
    expect(formatOrganizationAuditType('ORG_PRODUCT_ACCESS_UPDATE', tZhCN)).toBe('更新产品访问')
    expect(domainStatusLabel('VERIFIED', tEn)).toBe('Verified')
    expect(domainStatusLabel('PENDING_VERIFICATION', tEn)).toBe('Pending verification')
    expect(mailIdentityStatusLabel('ENABLED', tEn)).toBe('Enabled')
    expect(mailIdentityStatusLabel('DISABLED', tEn)).toBe('Disabled')
    expect(productAccessLabel('ENABLED', tEn)).toBe('Enabled')
    expect(productAccessLabel('DISABLED', tEn)).toBe('Disabled')
    expect(organizationRoleLabel('ADMIN', tZhCN)).toBe('管理员')
    expect(buildOrganizationProductCatalog(tZhCN).find(item => item.key === 'AUTHENTICATOR')?.label).toBe('身份验证器')
  })

  it('detects managers and filters org audit events', () => {
    expect(isOrganizationManager('OWNER')).toBe(true)
    expect(isOrganizationManager('ADMIN')).toBe(true)
    expect(isOrganizationManager('MEMBER')).toBe(false)
    expect(filterOrganizationAuditEvents(auditEvents)).toEqual([auditEvents[0]])
  })

  it('resolves invite and removal permissions from role and policy', () => {
    expect(canManageOrganizationMemberRole('OWNER')).toBe(true)
    expect(canManageOrganizationMemberRole('ADMIN')).toBe(false)
    expect(canInviteOrganizationRole('OWNER', 'ADMIN', policy)).toBe(true)
    expect(canInviteOrganizationRole('ADMIN', 'ADMIN', policy)).toBe(true)
    expect(canInviteOrganizationRole('ADMIN', 'ADMIN', { ...policy, adminCanInviteAdmin: false })).toBe(false)
    expect(buildOrganizationInviteRoleOptions('ADMIN', { ...policy, adminCanInviteAdmin: false })).toEqual(['MEMBER'])
    expect(buildOrganizationInviteRoleOptions('OWNER', policy)).toEqual(['MEMBER', 'ADMIN'])
    expect(canRemoveOrganizationMember('OWNER', {
      memberId: '2',
      userId: '2',
      userEmail: 'member@example.com',
      role: 'MEMBER',
      currentUser: false,
      enabledProductCount: 10,
      products: []
    }, policy)).toBe(true)
    expect(canRemoveOrganizationMember('ADMIN', {
      memberId: '3',
      userId: '3',
      userEmail: 'admin@example.com',
      role: 'ADMIN',
      currentUser: false,
      enabledProductCount: 10,
      products: []
    }, { ...policy, adminCanRemoveAdmin: false })).toBe(false)
  })
})
