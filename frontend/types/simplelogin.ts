export type SimpleLoginSubdomainMode = 'DISABLED' | 'TEAM_PREFIX' | 'ANY_PREFIX'

export interface SimpleLoginOverview {
  orgId: string | null
  aliasCount: number
  enabledAliasCount: number
  disabledAliasCount: number
  mailboxCount: number
  verifiedMailboxCount: number
  defaultMailboxEmail: string | null
  reverseAliasContactCount: number
  customDomainCount: number
  verifiedCustomDomainCount: number
  defaultDomain: string | null
  relayPolicyCount: number
  catchAllDomainCount: number
  subdomainPolicyCount: number
  defaultRelayMailboxEmail: string | null
  generatedAt: string
}

export interface SimpleLoginRelayPolicy {
  id: string
  orgId: string
  customDomainId: string
  domain: string | null
  catchAllEnabled: boolean
  subdomainMode: SimpleLoginSubdomainMode
  defaultMailboxId: string
  defaultMailboxEmail: string
  note: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateSimpleLoginRelayPolicyRequest {
  customDomainId: string
  catchAllEnabled: boolean
  subdomainMode: SimpleLoginSubdomainMode
  defaultMailboxId: string
  note?: string
}

export interface UpdateSimpleLoginRelayPolicyRequest {
  catchAllEnabled: boolean
  subdomainMode: SimpleLoginSubdomainMode
  defaultMailboxId: string
  note?: string
}
