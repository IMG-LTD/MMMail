import type { OrganizationSummaryCard } from '~/types/organization-admin'
import type {
  OrganizationAuthenticationSecurity,
  OrganizationAuthenticationSecurityMember
} from '~/types/organization-auth-security'
import type { TwoFactorEnforcementLevel } from '~/types/organization-policy'

type Translate = (key: string, params?: Record<string, string | number>) => string

export interface TwoFactorEnforcementOption {
  value: TwoFactorEnforcementLevel
  label: string
}

const ENFORCEMENT_LEVELS: TwoFactorEnforcementLevel[] = ['OFF', 'ADMINS', 'ALL']

export function buildAuthenticationSecurityCards(
  overview: OrganizationAuthenticationSecurity | null,
  t: Translate
): OrganizationSummaryCard[] {
  if (!overview) {
    return []
  }
  return [
    {
      label: t('organizations.authSecurity.summary.coverage'),
      value: `${overview.protectedMembers}/${overview.totalActiveMembers}`,
      hint: t('organizations.authSecurity.summary.coverageHint', { count: overview.unprotectedMembers })
    },
    {
      label: t('organizations.authSecurity.summary.managers'),
      value: String(overview.protectedManagerSeats),
      hint: t('organizations.authSecurity.summary.managersHint', { count: overview.unprotectedManagerSeats })
    },
    {
      label: t('organizations.authSecurity.summary.blocked'),
      value: String(overview.enforcementBlockedMembers),
      hint: t('organizations.authSecurity.summary.blockedHint', { level: twoFactorEnforcementLabel(overview.twoFactorEnforcementLevel, t) })
    },
    {
      label: t('organizations.authSecurity.summary.gracePeriod'),
      value: gracePeriodLabel(overview.twoFactorGracePeriodDays, t),
      hint: gracePeriodHint(overview.twoFactorGracePeriodDays, t)
    }
  ]
}

export function buildTwoFactorEnforcementOptions(t: Translate): TwoFactorEnforcementOption[] {
  return ENFORCEMENT_LEVELS.map((value) => ({
    value,
    label: twoFactorEnforcementLabel(value, t)
  }))
}

export function twoFactorEnforcementLabel(level: TwoFactorEnforcementLevel, t: Translate): string {
  return t(`organizations.authSecurity.enforcement.${level}`)
}

export function authenticationReminderCandidates(
  members: OrganizationAuthenticationSecurityMember[]
): string[] {
  return members
    .filter((member) => !member.twoFactorEnabled)
    .map((member) => member.memberId)
}

export function authenticationCoveragePercent(overview: OrganizationAuthenticationSecurity | null): number {
  if (!overview || overview.totalActiveMembers === 0) {
    return 0
  }
  return Math.round((overview.protectedMembers / overview.totalActiveMembers) * 100)
}

export function gracePeriodLabel(days: number, t: Translate): string {
  return days === 0
    ? t('organizations.authSecurity.gracePeriod.none')
    : t('organizations.authSecurity.gracePeriod.days', { count: days })
}

function gracePeriodHint(days: number, t: Translate): string {
  return days === 0
    ? t('organizations.authSecurity.summary.gracePeriodHint.none')
    : t('organizations.authSecurity.summary.gracePeriodHint.days', { count: days })
}
