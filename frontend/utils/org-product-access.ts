import type { DefaultNavItem } from '~/utils/default-nav'
import type { MailAddressMode } from '~/types/api'
import type { OrgProductKey } from '~/types/organization-admin'
import type { ProductEnabledResolver } from '~/types/org-access'
import { COMMUNITY_V1_HOME_ROUTE_CANDIDATES } from '../constants/module-maturity'

type RouteProductBinding = {
  prefix: string
  productKey: OrgProductKey
}

const ROUTE_BINDINGS: RouteProductBinding[] = [
  { prefix: '/archive', productKey: 'MAIL' },
  { prefix: '/authenticator', productKey: 'AUTHENTICATOR' },
  { prefix: '/calendar', productKey: 'CALENDAR' },
  { prefix: '/compose', productKey: 'MAIL' },
  { prefix: '/contacts', productKey: 'MAIL' },
  { prefix: '/conversations', productKey: 'MAIL' },
  { prefix: '/docs', productKey: 'DOCS' },
  { prefix: '/drafts', productKey: 'MAIL' },
  { prefix: '/drive', productKey: 'DRIVE' },
  { prefix: '/folders/', productKey: 'MAIL' },
  { prefix: '/inbox', productKey: 'MAIL' },
  { prefix: '/labels', productKey: 'MAIL' },
  { prefix: '/lumo', productKey: 'LUMO' },
  { prefix: '/mail/', productKey: 'MAIL' },
  { prefix: '/meet', productKey: 'MEET' },
  { prefix: '/outbox', productKey: 'MAIL' },
  { prefix: '/pass', productKey: 'PASS' },
  { prefix: '/scheduled', productKey: 'MAIL' },
  { prefix: '/search', productKey: 'MAIL' },
  { prefix: '/sheets', productKey: 'SHEETS' },
  { prefix: '/simplelogin', productKey: 'SIMPLELOGIN' },
  { prefix: '/snoozed', productKey: 'MAIL' },
  { prefix: '/spam', productKey: 'MAIL' },
  { prefix: '/standard-notes', productKey: 'STANDARD_NOTES' },
  { prefix: '/starred', productKey: 'MAIL' },
  { prefix: '/sent', productKey: 'MAIL' },
  { prefix: '/trash', productKey: 'MAIL' },
  { prefix: '/unread', productKey: 'MAIL' },
  { prefix: '/vpn', productKey: 'VPN' },
  { prefix: '/wallet', productKey: 'WALLET' }
]

const API_ROUTE_BINDINGS: RouteProductBinding[] = [
  { prefix: '/api/v1/authenticator', productKey: 'AUTHENTICATOR' },
  { prefix: '/api/v1/calendar', productKey: 'CALENDAR' },
  { prefix: '/api/v1/contact-groups', productKey: 'MAIL' },
  { prefix: '/api/v1/contacts', productKey: 'MAIL' },
  { prefix: '/api/v1/docs', productKey: 'DOCS' },
  { prefix: '/api/v1/drive', productKey: 'DRIVE' },
  { prefix: '/api/v1/labels', productKey: 'MAIL' },
  { prefix: '/api/v1/lumo', productKey: 'LUMO' },
  { prefix: '/api/v1/mail-easy-switch', productKey: 'MAIL' },
  { prefix: '/api/v1/mail-filters', productKey: 'MAIL' },
  { prefix: '/api/v1/mail-folders', productKey: 'MAIL' },
  { prefix: '/api/v1/mails', productKey: 'MAIL' },
  { prefix: '/api/v1/meet', productKey: 'MEET' },
  { prefix: '/api/v1/search-history', productKey: 'MAIL' },
  { prefix: '/api/v1/search-presets', productKey: 'MAIL' },
  { prefix: '/api/v1/sheets', productKey: 'SHEETS' },
  { prefix: '/api/v1/simplelogin', productKey: 'SIMPLELOGIN' },
  { prefix: '/api/v1/standard-notes', productKey: 'STANDARD_NOTES' },
  { prefix: '/api/v1/vpn', productKey: 'VPN' },
  { prefix: '/api/v1/wallet', productKey: 'WALLET' }
]

const HOME_ROUTE_CANDIDATES: Array<{ to: string; productKey?: OrgProductKey }> = [
  ...COMMUNITY_V1_HOME_ROUTE_CANDIDATES
]

const PROTON_ADDRESS_REQUIRED_PRODUCTS = new Set<OrgProductKey>(['MAIL', 'CALENDAR'])

export function resolveProductKeyFromPath(path: string): OrgProductKey | null {
  for (const binding of ROUTE_BINDINGS) {
    if (path.startsWith(binding.prefix)) {
      return binding.productKey
    }
  }
  return null
}

export function resolveProductKeyFromApiPath(path: string): OrgProductKey | null {
  for (const binding of API_ROUTE_BINDINGS) {
    if (path.startsWith(binding.prefix)) {
      return binding.productKey
    }
  }
  return null
}

export function resolveProductLabelKey(productKey: OrgProductKey): string {
  return `organizations.products.${productKey}`
}

export function isProductEnabledForMailAddressMode(
  productKey: OrgProductKey,
  mailAddressMode?: MailAddressMode | null
): boolean {
  if (mailAddressMode !== 'EXTERNAL_ACCOUNT') {
    return true
  }
  return !PROTON_ADDRESS_REQUIRED_PRODUCTS.has(productKey)
}

export function isProductAccessible(
  productKey: OrgProductKey,
  isEnabled: ProductEnabledResolver,
  mailAddressMode?: MailAddressMode | null
): boolean {
  return isEnabled(productKey) && isProductEnabledForMailAddressMode(productKey, mailAddressMode)
}

export function filterNavItemsByAccess(
  navItems: DefaultNavItem[],
  isEnabled: ProductEnabledResolver,
  mailAddressMode?: MailAddressMode | null
): DefaultNavItem[] {
  return navItems.filter(item => !item.productKey || isProductAccessible(item.productKey, isEnabled, mailAddressMode))
}

export function resolveHomeRoute(
  isEnabled: ProductEnabledResolver,
  mailAddressMode?: MailAddressMode | null
): string {
  const candidate = HOME_ROUTE_CANDIDATES.find(item => !item.productKey || isProductAccessible(item.productKey, isEnabled, mailAddressMode))
  return candidate?.to || '/organizations'
}
