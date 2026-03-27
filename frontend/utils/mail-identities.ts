import type { MailSenderIdentity } from '~/types/api'

function sourceLabel(source: MailSenderIdentity['source']): string {
  if (source === 'PASS_ALIAS') {
    return 'Alias'
  }
  if (source === 'ORG_CUSTOM_DOMAIN') {
    return 'Domain'
  }
  return 'Primary'
}

function sourceRank(source: MailSenderIdentity['source']): number {
  if (source === 'PRIMARY') {
    return 0
  }
  if (source === 'ORG_CUSTOM_DOMAIN') {
    return 1
  }
  if (source === 'PASS_ALIAS') {
    return 2
  }
  return 3
}

export function formatMailSenderLabel(identity: MailSenderIdentity): string {
  const prefix = identity.source === 'PRIMARY' ? '' : `${sourceLabel(identity.source)} · `
  if (identity.displayName) {
    return `${prefix}${identity.displayName} <${identity.emailAddress}>`
  }
  return `${prefix}${identity.emailAddress}`
}

export function resolveDefaultSenderEmail(identities: MailSenderIdentity[], fallbackEmail = ''): string {
  return identities.find((identity) => identity.defaultIdentity)?.emailAddress || fallbackEmail
}

export function sortMailSenderIdentities(identities: MailSenderIdentity[]): MailSenderIdentity[] {
  return [...identities].sort((left, right) => {
    if (left.defaultIdentity !== right.defaultIdentity) {
      return left.defaultIdentity ? -1 : 1
    }
    if (left.source !== right.source) {
      return sourceRank(left.source) - sourceRank(right.source)
    }
    return left.emailAddress.localeCompare(right.emailAddress)
  })
}
