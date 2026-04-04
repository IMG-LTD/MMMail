import type { OrgProductKey } from '~/types/organization-admin'

export type ModuleMaturity = 'GA' | 'BETA' | 'PREVIEW'
export type ModuleSurface = 'DEFAULT_NAV' | 'SUITE' | 'LABS'

export interface CommunityModuleDefinition {
  code: string
  route: string
  labelKey: string
  maturity: ModuleMaturity
  surface: ModuleSurface
  productKey?: OrgProductKey
}

export const COMMUNITY_V1_MODULES: ReadonlyArray<CommunityModuleDefinition> = [
  { code: 'MAIL', route: '/inbox', labelKey: 'nav.inbox', maturity: 'GA', surface: 'DEFAULT_NAV', productKey: 'MAIL' },
  { code: 'CALENDAR', route: '/calendar', labelKey: 'nav.calendar', maturity: 'GA', surface: 'DEFAULT_NAV', productKey: 'CALENDAR' },
  { code: 'DRIVE', route: '/drive', labelKey: 'nav.drive', maturity: 'GA', surface: 'DEFAULT_NAV', productKey: 'DRIVE' },
  { code: 'SUITE', route: '/suite', labelKey: 'nav.suite', maturity: 'GA', surface: 'DEFAULT_NAV' },
  { code: 'BUSINESS', route: '/business', labelKey: 'nav.business', maturity: 'GA', surface: 'DEFAULT_NAV' },
  { code: 'ORGANIZATIONS', route: '/organizations', labelKey: 'nav.organizations', maturity: 'GA', surface: 'DEFAULT_NAV' },
  { code: 'SETTINGS', route: '/settings', labelKey: 'nav.settings', maturity: 'GA', surface: 'DEFAULT_NAV' },
  { code: 'SECURITY', route: '/security', labelKey: 'nav.security', maturity: 'GA', surface: 'DEFAULT_NAV' },
  { code: 'DOCS', route: '/docs', labelKey: 'nav.docs', maturity: 'BETA', surface: 'DEFAULT_NAV', productKey: 'DOCS' },
  { code: 'SHEETS', route: '/sheets', labelKey: 'nav.sheets', maturity: 'BETA', surface: 'DEFAULT_NAV', productKey: 'SHEETS' },
  { code: 'BILLING_CENTER', route: '/suite', labelKey: 'suite.billing.center.badge', maturity: 'BETA', surface: 'SUITE' },
  { code: 'PASS', route: '/pass', labelKey: 'nav.pass', maturity: 'BETA', surface: 'LABS', productKey: 'PASS' },
  { code: 'AUTHENTICATOR', route: '/authenticator', labelKey: 'nav.authenticator', maturity: 'PREVIEW', surface: 'LABS', productKey: 'AUTHENTICATOR' },
  { code: 'SIMPLELOGIN', route: '/simplelogin', labelKey: 'nav.simpleLogin', maturity: 'PREVIEW', surface: 'LABS', productKey: 'SIMPLELOGIN' },
  { code: 'STANDARD_NOTES', route: '/standard-notes', labelKey: 'nav.standardNotes', maturity: 'PREVIEW', surface: 'LABS', productKey: 'STANDARD_NOTES' },
  { code: 'VPN', route: '/vpn', labelKey: 'nav.vpn', maturity: 'PREVIEW', surface: 'LABS', productKey: 'VPN' },
  { code: 'MEET', route: '/meet', labelKey: 'nav.meet', maturity: 'PREVIEW', surface: 'LABS', productKey: 'MEET' },
  { code: 'WALLET', route: '/wallet', labelKey: 'nav.wallet', maturity: 'PREVIEW', surface: 'LABS', productKey: 'WALLET' },
  { code: 'LUMO', route: '/lumo', labelKey: 'nav.lumo', maturity: 'PREVIEW', surface: 'LABS', productKey: 'LUMO' },
  { code: 'COLLABORATION', route: '/collaboration', labelKey: 'nav.collaboration', maturity: 'PREVIEW', surface: 'LABS' },
  { code: 'COMMAND_CENTER', route: '/command-center', labelKey: 'nav.commandCenter', maturity: 'PREVIEW', surface: 'LABS' },
  { code: 'NOTIFICATIONS', route: '/notifications', labelKey: 'nav.notifications', maturity: 'PREVIEW', surface: 'LABS' }
]

export const COMMUNITY_V1_PREVIEW_MODULES = COMMUNITY_V1_MODULES.filter(item => item.maturity === 'PREVIEW')
export const COMMUNITY_V1_LABS_MODULES = COMMUNITY_V1_MODULES.filter(item => item.surface === 'LABS')

export const COMMUNITY_V1_HOME_ROUTE_CANDIDATES: ReadonlyArray<{ to: string; productKey?: OrgProductKey }> = [
  { to: '/inbox', productKey: 'MAIL' },
  { to: '/calendar', productKey: 'CALENDAR' },
  { to: '/drive', productKey: 'DRIVE' },
  { to: '/docs', productKey: 'DOCS' },
  { to: '/sheets', productKey: 'SHEETS' },
  { to: '/suite' },
  { to: '/business' },
  { to: '/organizations' }
]

export function findCommunityModuleByRoute(route: string): CommunityModuleDefinition | null {
  return COMMUNITY_V1_MODULES.find(item => item.route === route) || null
}
