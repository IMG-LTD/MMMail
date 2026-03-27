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
  { labelKey: 'nav.outbox', to: '/outbox', folder: 'OUTBOX', productKey: 'MAIL' },
  { labelKey: 'nav.starred', to: '/starred', starred: true, productKey: 'MAIL' },
  { labelKey: 'nav.conversations', to: '/conversations', productKey: 'MAIL' },
  { labelKey: 'nav.scheduled', to: '/scheduled', folder: 'SCHEDULED', productKey: 'MAIL' },
  { labelKey: 'nav.snoozed', to: '/snoozed', folder: 'SNOOZED', productKey: 'MAIL' },
  { labelKey: 'nav.archive', to: '/archive', folder: 'ARCHIVE', productKey: 'MAIL' },
  { labelKey: 'nav.spam', to: '/spam', folder: 'SPAM', productKey: 'MAIL' },
  { labelKey: 'nav.trash', to: '/trash', folder: 'TRASH', productKey: 'MAIL' },
  { labelKey: 'nav.search', to: '/search', productKey: 'MAIL' },
  { labelKey: 'nav.compose', to: '/compose', productKey: 'MAIL' },
  { labelKey: 'nav.calendar', to: '/calendar', productKey: 'CALENDAR' },
  { labelKey: 'nav.contacts', to: '/contacts', productKey: 'MAIL' },
  { labelKey: 'nav.drive', to: '/drive', productKey: 'DRIVE' },
  { labelKey: 'nav.docs', to: '/docs', productKey: 'DOCS' },
  { labelKey: 'nav.sheets', to: '/sheets', productKey: 'SHEETS' },
  { labelKey: 'nav.suite', to: '/suite' },
  { labelKey: 'nav.business', to: '/business' },
  { labelKey: 'nav.organizations', to: '/organizations' },
  { labelKey: 'nav.labels', to: '/labels', productKey: 'MAIL' },
  { labelKey: 'nav.settings', to: '/settings' },
  { labelKey: 'nav.security', to: '/security' },
  { labelKey: 'nav.labs', to: '/labs' }
]
