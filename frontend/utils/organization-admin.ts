import type { MailIdentityStatus, OrgAuditEvent, OrgPolicy, OrgRole } from '~/types/api'
import type {
  OrgAdminConsoleSummary,
  OrgCustomDomainStatus,
  OrgMemberProductAccess,
  OrganizationSummaryCard,
  OrgProductAccessState,
  OrgProductKey
} from '~/types/organization-admin'

export type OrganizationTranslate = (key: string, params?: Record<string, string | number>) => string

const PRODUCT_KEYS: OrgProductKey[] = [
  'MAIL',
  'CALENDAR',
  'DRIVE',
  'DOCS',
  'SHEETS',
  'PASS',
  'SIMPLELOGIN',
  'STANDARD_NOTES',
  'VPN',
  'WALLET',
  'AUTHENTICATOR',
  'MEET',
  'LUMO'
]

const PRODUCT_FALLBACK_LABELS: Record<OrgProductKey, string> = {
  MAIL: 'Mail',
  CALENDAR: 'Calendar',
  DRIVE: 'Drive',
  DOCS: 'Docs',
  SHEETS: 'Sheets',
  PASS: 'Pass',
  SIMPLELOGIN: 'SimpleLogin',
  STANDARD_NOTES: 'Standard Notes',
  VPN: 'VPN',
  WALLET: 'Wallet',
  AUTHENTICATOR: 'Authenticator',
  MEET: 'Meet',
  LUMO: 'Lumo'
}

function translatedOrFallback(t: OrganizationTranslate, key: string, fallback: string): string {
  const translated = t(key)
  return translated === key ? fallback : translated
}

function humanizeEventType(eventType: string): string {
  return eventType.replace(/^ORG_/, '').replace(/_/g, ' ').trim()
}

export function buildOrganizationProductCatalog(t: OrganizationTranslate): Array<{ key: OrgProductKey, label: string }> {
  return PRODUCT_KEYS.map(key => ({
    key,
    label: organizationProductLabel(key, t)
  }))
}

export function buildOrganizationSummaryCards(
  summary: OrgAdminConsoleSummary | null,
  t: OrganizationTranslate
): OrganizationSummaryCard[] {
  if (!summary) {
    return []
  }
  return [
    {
      label: t('organizations.summary.members.label'),
      value: String(summary.memberCount),
      hint: t('organizations.summary.members.hint', { count: summary.adminCount })
    },
    {
      label: t('organizations.summary.domains.label'),
      value: String(summary.domainCount),
      hint: t('organizations.summary.domains.hint', { count: summary.verifiedDomainCount })
    },
    {
      label: t('organizations.summary.identities.label'),
      value: String(summary.mailIdentityCount),
      hint: t('organizations.summary.identities.hint', { count: summary.enabledMailIdentityCount })
    },
    {
      label: t('organizations.summary.products.label'),
      value: String(summary.enabledProductCount),
      hint: summary.defaultDomain
        ? t('organizations.summary.products.hintDefaultDomain', { value: summary.defaultDomain })
        : t('organizations.summary.products.hintMissingDomain')
    },
    {
      label: t('organizations.summary.sender.label'),
      value: summary.defaultSenderAddress || t('organizations.summary.sender.primaryMailbox'),
      hint: t('organizations.summary.sender.generatedAt', { value: summary.generatedAt })
    }
  ]
}

export function buildOrganizationPolicyChips(policy: OrgPolicy | null, t: OrganizationTranslate): string[] {
  if (!policy) {
    return []
  }
  return [
    policy.allowedEmailDomains.length === 0
      ? t('organizations.policy.chips.domainsUnrestricted')
      : t('organizations.policy.chips.allowedDomains', { count: policy.allowedEmailDomains.length }),
    t('organizations.policy.chips.memberLimit', { count: policy.memberLimit }),
    t('organizations.policy.chips.governanceSla', { count: policy.governanceReviewSlaHours }),
    policy.requireDualReviewGovernance
      ? t('organizations.policy.chips.dualReviewRequired')
      : t('organizations.policy.chips.singleReviewAllowed')
  ]
}

export function formatOrganizationAuditType(eventType: string, t: OrganizationTranslate): string {
  return translatedOrFallback(t, `organizations.audit.eventTypes.${eventType}`, humanizeEventType(eventType))
}

export function isOrganizationManager(role: OrgRole | null | undefined): boolean {
  return role === 'OWNER' || role === 'ADMIN'
}

export function canManageOrganizationMemberRole(role: OrgRole | null | undefined): boolean {
  return role === 'OWNER'
}

export function canInviteOrganizationRole(
  actorRole: OrgRole | null | undefined,
  targetRole: Exclude<OrgRole, 'OWNER'>,
  policy: OrgPolicy | null
): boolean {
  if (actorRole === 'OWNER') {
    return true
  }
  if (actorRole !== 'ADMIN') {
    return false
  }
  if (targetRole === 'MEMBER') {
    return true
  }
  return Boolean(policy?.adminCanInviteAdmin)
}

export function buildOrganizationInviteRoleOptions(
  actorRole: OrgRole | null | undefined,
  policy: OrgPolicy | null
): Array<Exclude<OrgRole, 'OWNER'>> {
  return (['MEMBER', 'ADMIN'] as const).filter((targetRole): targetRole is Exclude<OrgRole, 'OWNER'> => {
    return canInviteOrganizationRole(actorRole, targetRole, policy)
  })
}

export function canRemoveOrganizationMember(
  actorRole: OrgRole | null | undefined,
  row: OrgMemberProductAccess,
  policy: OrgPolicy | null
): boolean {
  if (!actorRole || row.role === 'OWNER' || row.currentUser) {
    return false
  }
  if (actorRole === 'OWNER') {
    return true
  }
  if (actorRole === 'ADMIN') {
    return row.role === 'MEMBER' || (row.role === 'ADMIN' && Boolean(policy?.adminCanRemoveAdmin))
  }
  return false
}

export function organizationRoleLabel(role: OrgRole, t: OrganizationTranslate): string {
  return translatedOrFallback(t, `organizations.roles.${role}`, role)
}

export function organizationProductLabel(productKey: OrgProductKey, t: OrganizationTranslate): string {
  return translatedOrFallback(t, `organizations.products.${productKey}`, PRODUCT_FALLBACK_LABELS[productKey])
}

export function domainStatusLabel(status: OrgCustomDomainStatus, t: OrganizationTranslate): string {
  return status === 'VERIFIED'
    ? t('organizations.states.verified')
    : t('organizations.states.pendingVerification')
}

export function mailIdentityStatusLabel(status: MailIdentityStatus, t: OrganizationTranslate): string {
  return status === 'ENABLED'
    ? t('organizations.states.enabled')
    : t('organizations.states.disabled')
}

export function productAccessLabel(state: OrgProductAccessState, t: OrganizationTranslate): string {
  return state === 'ENABLED'
    ? t('organizations.states.enabled')
    : t('organizations.states.disabled')
}

export function filterOrganizationAuditEvents(events: OrgAuditEvent[]): OrgAuditEvent[] {
  return events.filter((event) => event.eventType.startsWith('ORG_'))
}
