import { describe, expect, it } from 'vitest'
import type { MailSenderIdentity } from '../types/api'
import {
  formatMailSenderLabel,
  resolveDefaultSenderEmail,
  sortMailSenderIdentities
} from '../utils/mail-identities'

const identities: MailSenderIdentity[] = [
  {
    identityId: '1',
    orgId: '10',
    orgName: 'Parity Org',
    memberId: '100',
    emailAddress: 'ops@example.com',
    displayName: 'Ops',
    source: 'ORG_CUSTOM_DOMAIN',
    status: 'ENABLED',
    defaultIdentity: true
  },
  {
    identityId: '2',
    orgId: null,
    orgName: null,
    memberId: null,
    emailAddress: 'shield@passmail.mmmail.local',
    displayName: 'Shield Alias',
    source: 'PASS_ALIAS',
    status: 'ENABLED',
    defaultIdentity: false
  },
  {
    identityId: null,
    orgId: null,
    orgName: null,
    memberId: null,
    emailAddress: 'primary@mmmail.local',
    displayName: 'Primary',
    source: 'PRIMARY',
    status: 'ENABLED',
    defaultIdentity: false
  }
]

describe('mail identity utils', () => {
  it('formats sender labels with source-aware badges', () => {
    expect(formatMailSenderLabel(identities[0])).toBe('Domain · Ops <ops@example.com>')
    expect(formatMailSenderLabel(identities[1])).toBe('Alias · Shield Alias <shield@passmail.mmmail.local>')
  })

  it('resolves default sender email', () => {
    expect(resolveDefaultSenderEmail(identities, 'fallback@mmmail.local')).toBe('ops@example.com')
    expect(resolveDefaultSenderEmail([], 'fallback@mmmail.local')).toBe('fallback@mmmail.local')
  })

  it('sorts default, primary, domain, and alias identities deterministically', () => {
    const sorted = sortMailSenderIdentities([identities[1], identities[2], identities[0]])
    expect(sorted[0].emailAddress).toBe('ops@example.com')
    expect(sorted[1].source).toBe('PRIMARY')
    expect(sorted[2].source).toBe('PASS_ALIAS')
  })
})
