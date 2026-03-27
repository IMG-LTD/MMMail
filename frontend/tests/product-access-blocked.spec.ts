import { describe, expect, it } from 'vitest'
import {
  ACCOUNT_MAIL_ADDRESS_REQUIRED_REASON,
  buildAuthenticatorRecoveryQuery,
  buildMailAddressBlockedQuery,
  buildTwoFactorBlockedQuery,
  ORG_TWO_FACTOR_REASON,
  resolveBlockedRecoveryContext
} from '../utils/org-access-recovery'

describe('product access blocked recovery helpers', () => {
  it('builds two-factor blocked query payload', () => {
    expect(buildTwoFactorBlockedQuery({
      from: '/drive?view=grid',
      orgId: 'org-1',
      orgName: 'Parity Org',
      productKey: 'DRIVE'
    })).toEqual({
      reason: ORG_TWO_FACTOR_REASON,
      from: '/drive?view=grid',
      orgId: 'org-1',
      orgName: 'Parity Org',
      productKey: 'DRIVE'
    })
  })

  it('translates blocked query into authenticator recovery query', () => {
    const blocked = resolveBlockedRecoveryContext({
      reason: ORG_TWO_FACTOR_REASON,
      from: '/pass',
      orgId: 'org-9',
      orgName: 'Secure Org',
      productKey: 'PASS'
    })

    expect(buildAuthenticatorRecoveryQuery(blocked)).toEqual({
      recovery: ORG_TWO_FACTOR_REASON,
      returnTo: '/pass',
      restoreOrgId: 'org-9',
      orgName: 'Secure Org',
      productKey: 'PASS'
    })
  })

  it('builds account mail address blocked query payload', () => {
    expect(buildMailAddressBlockedQuery({
      from: '/calendar',
      productKey: 'CALENDAR'
    })).toEqual({
      reason: ACCOUNT_MAIL_ADDRESS_REQUIRED_REASON,
      from: '/calendar',
      productKey: 'CALENDAR'
    })
  })
})
