import { describe, expect, it } from 'vitest'
import {
  buildSimpleLoginHealthChips,
  resolvePreferredSimpleLoginAliasId,
  resolvePreferredSimpleLoginOrgId,
  resolveSimpleLoginPolicy,
  simpleLoginSubdomainModeLabel
} from '../utils/simplelogin'

describe('simplelogin utils', () => {
  it('builds fallback health chips without overview', () => {
    expect(buildSimpleLoginHealthChips(null)).toEqual([
      'Relay workspace',
      'Domain controls pending',
      'Mailbox baseline pending'
    ])
  })

  it('builds health chips from overview state', () => {
    expect(buildSimpleLoginHealthChips({
      orgId: '1',
      aliasCount: 2,
      enabledAliasCount: 2,
      disabledAliasCount: 0,
      mailboxCount: 1,
      verifiedMailboxCount: 1,
      defaultMailboxEmail: 'me@mmmail.local',
      reverseAliasContactCount: 0,
      customDomainCount: 1,
      verifiedCustomDomainCount: 1,
      defaultDomain: 'acme.test',
      relayPolicyCount: 1,
      catchAllDomainCount: 1,
      subdomainPolicyCount: 1,
      defaultRelayMailboxEmail: 'me@mmmail.local',
      generatedAt: '2026-03-08T01:00:00'
    })).toEqual([
      '2 aliases',
      '1 verified mailbox',
      '1 relay policy',
      '1 catch-all domains',
      'Default domain acme.test'
    ])
  })

  it('resolves preferred org and alias selections', () => {
    expect(resolvePreferredSimpleLoginOrgId([{ id: 'org-1' }, { id: 'org-2' }], 'org-2')).toBe('org-2')
    expect(resolvePreferredSimpleLoginOrgId([{ id: 'org-1' }], 'missing')).toBe('org-1')
    expect(resolvePreferredSimpleLoginAliasId([{ id: 'a-1' }, { id: 'a-2' }], 'a-2')).toBe('a-2')
    expect(resolvePreferredSimpleLoginAliasId([], 'a-2')).toBe('')
  })

  it('resolves relay policy and subdomain labels', () => {
    const policy = {
      id: 'p-1',
      orgId: 'org-1',
      customDomainId: 'd-1',
      domain: 'relay.test',
      catchAllEnabled: true,
      subdomainMode: 'TEAM_PREFIX' as const,
      defaultMailboxId: 'm-1',
      defaultMailboxEmail: 'ops@mmmail.local',
      note: 'Ops route',
      createdAt: '2026-03-08T01:00:00',
      updatedAt: '2026-03-08T01:00:00'
    }
    expect(resolveSimpleLoginPolicy([policy], 'd-1')).toEqual(policy)
    expect(resolveSimpleLoginPolicy([policy], 'missing')).toBeNull()
    expect(simpleLoginSubdomainModeLabel('TEAM_PREFIX')).toBe('Team prefix')
  })
})
