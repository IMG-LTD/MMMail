import { describe, expect, it } from 'vitest'
import {
  ORG_TWO_FACTOR_REASON,
  resolveAuthenticatorRecoveryContext
} from '../utils/org-access-recovery'

describe('authenticator recovery context', () => {
  it('enables recovery mode and preserves restore target', () => {
    expect(resolveAuthenticatorRecoveryContext({
      recovery: ORG_TWO_FACTOR_REASON,
      returnTo: '/vpn',
      restoreOrgId: 'org-2',
      orgName: 'Parity Org',
      productKey: 'VPN'
    })).toEqual({
      enabled: true,
      returnTo: '/vpn',
      restoreOrgId: 'org-2',
      orgName: 'Parity Org',
      productKey: 'VPN'
    })
  })

  it('falls back to default target when recovery params are missing', () => {
    expect(resolveAuthenticatorRecoveryContext({})).toEqual({
      enabled: false,
      returnTo: '/inbox',
      restoreOrgId: '',
      orgName: '',
      productKey: null
    })
  })
})
