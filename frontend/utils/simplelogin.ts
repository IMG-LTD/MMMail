import type {
  SimpleLoginOverview,
  SimpleLoginRelayPolicy,
  SimpleLoginSubdomainMode
} from '~/types/simplelogin'

type TranslateFn = (key: string, params?: Record<string, string | number>) => string

interface HasId {
  id: string
}

const SUBDOMAIN_LABELS: Record<SimpleLoginSubdomainMode, string> = {
  DISABLED: 'Subdomain off',
  TEAM_PREFIX: 'Team prefix',
  ANY_PREFIX: 'Any prefix'
}

function translateMessage(t: TranslateFn | undefined, key: string, fallback: string, params?: Record<string, string | number>): string {
  if (t) {
    return t(key, params)
  }
  if (!params) {
    return fallback
  }
  return Object.entries(params).reduce((message, [paramKey, value]) => {
    return message.replaceAll(`{${paramKey}}`, String(value))
  }, fallback)
}

export function buildSimpleLoginHealthChips(overview: SimpleLoginOverview | null, t?: TranslateFn): string[] {
  if (!overview) {
    return [
      translateMessage(t, 'simplelogin.health.relayWorkspace', 'Relay workspace'),
      translateMessage(t, 'simplelogin.health.domainControlsPending', 'Domain controls pending'),
      translateMessage(t, 'simplelogin.health.mailboxBaselinePending', 'Mailbox baseline pending')
    ]
  }
  const chips = [
    overview.aliasCount > 0
      ? translateMessage(t, 'simplelogin.health.aliasCount', '{count} aliases', { count: overview.aliasCount })
      : translateMessage(t, 'simplelogin.health.noAliases', 'No aliases yet'),
    overview.verifiedMailboxCount > 0
      ? overview.verifiedMailboxCount === 1
        ? translateMessage(t, 'simplelogin.health.verifiedMailboxSingle', '1 verified mailbox')
        : translateMessage(t, 'simplelogin.health.verifiedMailboxCount', '{count} verified mailboxes', { count: overview.verifiedMailboxCount })
      : translateMessage(t, 'simplelogin.health.noVerifiedMailbox', 'No verified mailbox'),
    overview.relayPolicyCount > 0
      ? overview.relayPolicyCount === 1
        ? translateMessage(t, 'simplelogin.health.relayPolicySingle', '1 relay policy')
        : translateMessage(t, 'simplelogin.health.relayPolicyCount', '{count} relay policies', { count: overview.relayPolicyCount })
      : translateMessage(t, 'simplelogin.health.noRelayPolicies', 'No relay policies')
  ]
  if (overview.orgId) {
    chips.push(
      overview.catchAllDomainCount > 0
        ? translateMessage(t, 'simplelogin.health.catchAllCount', '{count} catch-all domains', { count: overview.catchAllDomainCount })
        : translateMessage(t, 'simplelogin.health.catchAllMissing', 'Catch-all not configured')
    )
    chips.push(
      overview.defaultDomain
        ? translateMessage(t, 'simplelogin.health.defaultDomain', 'Default domain {domain}', { domain: overview.defaultDomain })
        : translateMessage(t, 'simplelogin.health.noDefaultDomain', 'No default domain')
    )
  }
  return chips
}

export function simpleLoginSubdomainModeLabel(mode: SimpleLoginSubdomainMode, t?: TranslateFn): string {
  if (!t) {
    return SUBDOMAIN_LABELS[mode]
  }
  if (mode === 'DISABLED') {
    return t('simplelogin.subdomain.disabled')
  }
  if (mode === 'TEAM_PREFIX') {
    return t('simplelogin.subdomain.teamPrefix')
  }
  return t('simplelogin.subdomain.anyPrefix')
}

export function resolvePreferredSimpleLoginOrgId<T extends HasId>(items: T[], requestedOrgId: string | null): string {
  if (!items.length) {
    return ''
  }
  if (requestedOrgId && items.some((item) => item.id === requestedOrgId)) {
    return requestedOrgId
  }
  return items[0].id
}

export function resolvePreferredSimpleLoginAliasId<T extends HasId>(items: T[], requestedAliasId: string | null): string {
  if (!items.length) {
    return ''
  }
  if (requestedAliasId && items.some((item) => item.id === requestedAliasId)) {
    return requestedAliasId
  }
  return items[0].id
}

export function resolveSimpleLoginPolicy<T extends SimpleLoginRelayPolicy>(items: T[], customDomainId: string): T | null {
  return items.find((item) => item.customDomainId === customDomainId) || null
}
