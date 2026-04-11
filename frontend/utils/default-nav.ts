import type { SystemMailFolder } from '~/types/api'
import type { OrgProductKey } from '~/types/organization-admin'

export interface DefaultNavItem {
  labelKey: string
  to: string
  folder?: SystemMailFolder
  unread?: boolean
  starred?: boolean
  productKey?: OrgProductKey
}

export const DEFAULT_NAV_ITEMS: DefaultNavItem[] = [
  { labelKey: 'nav.inbox', to: '/inbox', folder: 'INBOX', productKey: 'MAIL' },
  { labelKey: 'nav.unread', to: '/unread', unread: true, productKey: 'MAIL' },
  { labelKey: 'nav.sent', to: '/sent', folder: 'SENT', productKey: 'MAIL' },
  { labelKey: 'nav.drafts', to: '/drafts', folder: 'DRAFTS', productKey: 'MAIL' },
  { labelKey: 'nav.search', to: '/search', productKey: 'MAIL' },
  { labelKey: 'nav.compose', to: '/compose', productKey: 'MAIL' },
  { labelKey: 'nav.calendar', to: '/calendar', productKey: 'CALENDAR' },
  { labelKey: 'nav.drive', to: '/drive', productKey: 'DRIVE' },
  { labelKey: 'nav.pass', to: '/pass', productKey: 'PASS' },
  { labelKey: 'nav.suite', to: '/suite' },
  { labelKey: 'nav.settings', to: '/settings' },
  { labelKey: 'nav.security', to: '/security' },
  { labelKey: 'nav.labs', to: '/labs' }
]
