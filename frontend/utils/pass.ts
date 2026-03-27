import type {
  GeneratePassPasswordRequest,
  PassBusinessPolicy,
  PassItemType,
  PassMailboxStatus,
  PassMailAlias,
  PassMailAliasStatus,
  PassOrgRole,
  PassPasswordGeneratorForm,
  PassSecureLinkFilter,
  PassSecureLink,
  PassSharedVault,
  PassVaultRole,
  PassWorkspaceMode
} from '~/types/pass-business'

type PassSecureLinkState = 'active' | 'revoked' | 'expired' | 'spent'
type PassSecureLinkLike = Pick<PassSecureLink, 'active' | 'revokedAt' | 'expiresAt' | 'currentViews' | 'maxViews'>
type TranslateFn = (key: string, params?: Record<string, string | number>) => string
export const DEFAULT_SECURE_LINK_VIEWS = 10
export const DEFAULT_SECURE_LINK_EXPIRE_DAYS = 7
export const MAX_SECURE_LINK_EXPIRE_DAYS = 30

const PASS_ITEM_TYPE_LABELS: Record<PassItemType, string> = {
  LOGIN: 'Login',
  PASSWORD: 'Password',
  NOTE: 'Secure Note',
  CARD: 'Card',
  ALIAS: 'Alias',
  PASSKEY: 'Passkey'
}

export const PASS_ITEM_TYPE_OPTIONS: Array<{ label: string; value: PassItemType }> = [
  { label: 'Login', value: 'LOGIN' },
  { label: 'Password', value: 'PASSWORD' },
  { label: 'Secure Note', value: 'NOTE' },
  { label: 'Card', value: 'CARD' },
  { label: 'Passkey', value: 'PASSKEY' }
]

export function formatPassTime(value: string | null | undefined): string {
  return value || '-'
}

export function formatPassItemType(itemType: PassItemType | string | null | undefined): string {
  if (!itemType) {
    return 'Credential'
  }
  return PASS_ITEM_TYPE_LABELS[itemType as PassItemType] || 'Credential'
}

function translateMessage(
  t: TranslateFn | undefined,
  key: string,
  fallback: string,
  params?: Record<string, string | number>
): string {
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

export function formatPassAliasStatus(
  status: PassMailAliasStatus | string | null | undefined,
  t?: TranslateFn
): string {
  if (status === 'ENABLED') {
    return translateMessage(t, 'pass.shared.status.enabled', 'Enabled')
  }
  if (status === 'DISABLED') {
    return translateMessage(t, 'pass.shared.status.disabled', 'Disabled')
  }
  return translateMessage(t, 'pass.shared.status.unknown', 'Unknown')
}

export function formatPassMailboxStatus(
  status: PassMailboxStatus | string | null | undefined,
  t?: TranslateFn
): string {
  if (status === 'VERIFIED') {
    return translateMessage(t, 'pass.shared.mailboxStatus.verified', 'Verified')
  }
  if (status === 'PENDING') {
    return translateMessage(t, 'pass.shared.mailboxStatus.pending', 'Pending')
  }
  return translateMessage(t, 'pass.shared.mailboxStatus.unknown', 'Unknown')
}

export function resolveAliasRouteEmails(
  alias: Pick<PassMailAlias, 'forwardToEmail' | 'forwardToEmails'> | null | undefined
): string[] {
  if (!alias) {
    return []
  }
  const routes = alias.forwardToEmails?.length ? alias.forwardToEmails : (alias.forwardToEmail ? [alias.forwardToEmail] : [])
  return Array.from(new Set(routes.filter(Boolean)))
}

export function formatAliasRouteSummary(
  alias: Pick<PassMailAlias, 'forwardToEmail' | 'forwardToEmails'> | null | undefined,
  t?: TranslateFn
): string {
  const routes = resolveAliasRouteEmails(alias)
  if (!routes.length) {
    return translateMessage(t, 'pass.shared.routes.none', 'No routes')
  }
  if (routes.length === 1) {
    return routes[0]
  }
  return translateMessage(t, 'pass.shared.routes.summary', '{email} +{count}', {
    email: routes[0],
    count: routes.length - 1
  })
}

export function resolvePreferredAliasRouteSelection(availableEmails: string[], currentUserEmail?: string | null): string[] {
  if (!availableEmails.length) {
    return []
  }
  const normalizedUserEmail = currentUserEmail?.trim().toLowerCase() || ''
  const matched = availableEmails.find(email => email.trim().toLowerCase() === normalizedUserEmail)
  return matched ? [matched] : [availableEmails[0]]
}

export function limitRecentAliases(aliases: PassMailAlias[], limit = 3): PassMailAlias[] {
  const safeLimit = Math.max(0, limit)
  return aliases.slice(0, safeLimit)
}

export function formatVaultRole(role: PassVaultRole | PassOrgRole | string | null | undefined): string {
  if (role === 'OWNER') return 'Owner'
  if (role === 'ADMIN') return 'Admin'
  if (role === 'MANAGER') return 'Vault Manager'
  if (role === 'MEMBER') return 'Member'
  return 'Member'
}

export function isSecureLinkActive(link: PassSecureLinkLike): boolean {
  if (!link.active) {
    return false
  }
  if (link.revokedAt) {
    return false
  }
  if (link.expiresAt && new Date(link.expiresAt).getTime() <= Date.now()) {
    return false
  }
  return link.currentViews < link.maxViews
}

function resolveSecureLinkState(link: PassSecureLinkLike): PassSecureLinkState {
  if (link.revokedAt) {
    return 'revoked'
  }
  if (link.expiresAt && new Date(link.expiresAt).getTime() <= Date.now()) {
    return 'expired'
  }
  if (link.currentViews >= link.maxViews) {
    return 'spent'
  }
  return 'active'
}

export function secureLinkMatchesFilter(link: PassSecureLinkLike, filter: PassSecureLinkFilter): boolean {
  if (filter === 'ALL') {
    return true
  }
  return resolveSecureLinkState(link).toUpperCase() === filter
}

export function buildDefaultSecureLinkExpiryValue(now = new Date()): string {
  const next = new Date(now.getTime())
  next.setDate(next.getDate() + DEFAULT_SECURE_LINK_EXPIRE_DAYS)
  return formatDateTimeLocalValue(next)
}

export function isSecureLinkExpiryDisabled(date: Date, now = new Date()): boolean {
  const min = now.getTime()
  const max = new Date(now.getTime())
  max.setDate(max.getDate() + MAX_SECURE_LINK_EXPIRE_DAYS)
  const candidate = date.getTime()
  return candidate <= min || candidate > max.getTime()
}

export function secureLinkStatusLabel(link: PassSecureLinkLike): string {
  const state = resolveSecureLinkState(link)
  if (state === 'revoked') {
    return 'Revoked'
  }
  if (state === 'expired') {
    return 'Expired'
  }
  if (state === 'spent') {
    return 'Spent'
  }
  return 'Active'
}

export function secureLinkStatusKey(link: PassSecureLinkLike): string {
  return `pass.secureLinks.status.${resolveSecureLinkState(link)}`
}

export function secureLinkStatusTone(link: PassSecureLinkLike): 'success' | 'danger' | 'warning' | 'info' {
  const state = resolveSecureLinkState(link)
  if (state === 'active') {
    return 'success'
  }
  if (state === 'revoked') {
    return 'info'
  }
  return 'warning'
}

const PASS_SECURE_LINK_ERROR_KEY_MAP: Record<string, string> = {
  'Pass secure link is not found': 'pass.publicShare.errors.notFound',
  'Pass secure link has been revoked': 'pass.publicShare.errors.revoked',
  'Pass secure link has expired': 'pass.publicShare.errors.expired',
  'Pass secure link has reached the maximum views': 'pass.publicShare.errors.spent'
}

export function resolvePassSecureLinkErrorKey(message: string): string | null {
  return PASS_SECURE_LINK_ERROR_KEY_MAP[message] || null
}

function formatDateTimeLocalValue(date: Date): string {
  const year = String(date.getFullYear())
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hour}:${minute}:${second}`
}

export function canCreateSharedVault(role: PassOrgRole | null | undefined, policy: PassBusinessPolicy | null): boolean {
  if (!role) {
    return false
  }
  if (role === 'OWNER' || role === 'ADMIN') {
    return true
  }
  return Boolean(policy?.allowMemberVaultCreation)
}

export function canManagePassPolicy(role: PassOrgRole | null | undefined): boolean {
  return role === 'OWNER' || role === 'ADMIN'
}

const PLACEHOLDER_SECRET_LOWERCASE = 'vault'
const PLACEHOLDER_SECRET_UPPERCASE = 'Q'
const PLACEHOLDER_SECRET_DIGIT = '7'
const PLACEHOLDER_SECRET_SYMBOL = '#'
const PLACEHOLDER_SECRET_FILLER = 'vaultQ7#'

export function generatorLengthBounds(policy: PassBusinessPolicy | null): { min: number; max: number } {
  const min = Math.max(policy?.minimumPasswordLength || 14, 8)
  const max = Math.max(policy?.maximumPasswordLength || 64, min)
  return { min, max }
}

export function generatorPresetFromPolicy(policy: PassBusinessPolicy | null): PassPasswordGeneratorForm {
  const { min, max } = generatorLengthBounds(policy)
  return {
    length: Math.min(Math.max(min, 14), max),
    includeLowercase: true,
    includeUppercase: policy?.requireUppercase ?? true,
    includeDigits: policy?.requireDigits ?? true,
    includeSymbols: policy?.requireSymbols ?? true,
    memorable: false
  }
}

export function placeholderSecretFromPolicy(policy: PassBusinessPolicy | null): string {
  const preset = generatorPresetFromPolicy(policy)
  const segments = [PLACEHOLDER_SECRET_LOWERCASE]

  if (preset.includeUppercase) {
    segments.push(PLACEHOLDER_SECRET_UPPERCASE)
  }
  if (preset.includeDigits) {
    segments.push(PLACEHOLDER_SECRET_DIGIT)
  }
  if (preset.includeSymbols) {
    segments.push(PLACEHOLDER_SECRET_SYMBOL)
  }

  let secret = segments.join('')
  while (secret.length < preset.length) {
    secret += PLACEHOLDER_SECRET_FILLER
  }

  return secret.slice(0, preset.length)
}

export function buildGeneratorPayload(
  orgId: string | null | undefined,
  form: PassPasswordGeneratorForm,
  policy: PassBusinessPolicy | null
): GeneratePassPasswordRequest {
  return {
    orgId: orgId?.trim() || undefined,
    length: form.length,
    includeLowercase: form.includeLowercase,
    includeUppercase: policy?.requireUppercase ? true : form.includeUppercase,
    includeDigits: policy?.requireDigits ? true : form.includeDigits,
    includeSymbols: policy?.requireSymbols ? true : form.includeSymbols,
    memorable: form.memorable
  }
}

export function resolveSelectedVault(vaults: PassSharedVault[], vaultId: string): PassSharedVault | null {
  return vaults.find(item => item.id === vaultId) || null
}

export function workspaceTitle(mode: PassWorkspaceMode): string {
  return mode === 'PERSONAL' ? 'Personal Vault' : 'Shared Vaults'
}

export function buildAliasComposeQuery(senderEmail: string, reverseAliasEmail: string, subject?: string): Record<string, string> {
  const query: Record<string, string> = {
    from: senderEmail,
    to: reverseAliasEmail
  }
  if (subject) {
    query.subject = subject
  }
  return query
}
