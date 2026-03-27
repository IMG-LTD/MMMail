import type { LocationQuery, LocationQueryRaw } from 'vue-router'
import type { OrgProductKey } from '~/types/organization-admin'

export const ORG_TWO_FACTOR_REQUIRED_CODE = 30046
export const ORG_TWO_FACTOR_REASON = 'ORG_TWO_FACTOR_REQUIRED'
export const ACCOUNT_MAIL_ADDRESS_REQUIRED_CODE = 30047
export const ACCOUNT_MAIL_ADDRESS_REQUIRED_REASON = 'ACCOUNT_MAIL_ADDRESS_REQUIRED'

export interface BlockedRecoveryContext {
  reason: string
  productKey: OrgProductKey | null
  from: string
  orgId: string
  orgName: string
}

export interface AuthenticatorRecoveryContext {
  enabled: boolean
  productKey: OrgProductKey | null
  returnTo: string
  restoreOrgId: string
  orgName: string
}

export function buildTwoFactorBlockedQuery(options: {
  from: string
  orgId: string
  orgName: string
  productKey: OrgProductKey
}): LocationQueryRaw {
  return {
    reason: ORG_TWO_FACTOR_REASON,
    from: options.from,
    orgId: options.orgId,
    orgName: options.orgName,
    productKey: options.productKey
  }
}

export function buildAuthenticatorRecoveryQuery(context: BlockedRecoveryContext): LocationQueryRaw {
  return {
    recovery: context.reason,
    returnTo: context.from,
    restoreOrgId: context.orgId,
    orgName: context.orgName,
    productKey: context.productKey || undefined
  }
}

export function buildMailAddressBlockedQuery(options: {
  from: string
  productKey: OrgProductKey
}): LocationQueryRaw {
  return {
    reason: ACCOUNT_MAIL_ADDRESS_REQUIRED_REASON,
    from: options.from,
    productKey: options.productKey
  }
}

export function resolveBlockedRecoveryContext(query: LocationQuery): BlockedRecoveryContext {
  return {
    reason: readQueryValue(query.reason),
    productKey: parseProductKey(readQueryValue(query.productKey)),
    from: readQueryValue(query.from) || '/inbox',
    orgId: readQueryValue(query.orgId),
    orgName: readQueryValue(query.orgName)
  }
}

export function resolveAuthenticatorRecoveryContext(query: LocationQuery): AuthenticatorRecoveryContext {
  return {
    enabled: readQueryValue(query.recovery) === ORG_TWO_FACTOR_REASON,
    productKey: parseProductKey(readQueryValue(query.productKey)),
    returnTo: readQueryValue(query.returnTo) || '/inbox',
    restoreOrgId: readQueryValue(query.restoreOrgId),
    orgName: readQueryValue(query.orgName)
  }
}

function readQueryValue(value: LocationQuery[string]): string {
  if (typeof value === 'string') {
    return value
  }
  if (Array.isArray(value) && typeof value[0] === 'string') {
    return value[0]
  }
  return ''
}

function parseProductKey(value: string): OrgProductKey | null {
  if (!value) {
    return null
  }
  return value as OrgProductKey
}
