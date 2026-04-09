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
export const COMMUNITY_V1_CURATED_LABS_MODULE_CODES = [
  'PASS',
  'AUTHENTICATOR',
  'SIMPLELOGIN',
  'STANDARD_NOTES'
] as const
export const COMMUNITY_V1_CURATED_LABS_MODULES = COMMUNITY_V1_LABS_MODULES.filter(item => {
  return COMMUNITY_V1_CURATED_LABS_MODULE_CODES.includes(
    item.code as typeof COMMUNITY_V1_CURATED_LABS_MODULE_CODES[number]
  )
})

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

export const COMMUNITY_V1_CORE_PRODUCT_CODES = [
  'MAIL',
  'CALENDAR',
  'DRIVE',
  'DOCS',
  'SHEETS',
  'PASS'
] as const satisfies readonly OrgProductKey[]

export type CommunityCoreProductCode = typeof COMMUNITY_V1_CORE_PRODUCT_CODES[number]

export interface CommunityCoreWorkflowDefinition {
  productCode: CommunityCoreProductCode
  route: string
  titleKey: string
  descriptionKey: string
  statusKey: string
  boundaryKey: string
  actionKey: string
}

export interface CommunityAdoptionChecklistItem {
  code: string
  href: string
  external: boolean
  titleKey: string
  descriptionKey: string
  actionKey: string
}

const COMMUNITY_V1_CORE_PRODUCT_CODE_SET = new Set<string>(COMMUNITY_V1_CORE_PRODUCT_CODES)

export const COMMUNITY_V1_CORE_WORKFLOW_MODULES: ReadonlyArray<CommunityCoreWorkflowDefinition> = [
  {
    productCode: 'MAIL',
    route: '/compose',
    titleKey: 'suite.sectionOverview.workflows.mail.title',
    descriptionKey: 'suite.sectionOverview.workflows.mail.description',
    statusKey: 'suite.sectionOverview.workflows.mail.status',
    boundaryKey: 'suite.sectionOverview.workflows.mail.boundary',
    actionKey: 'suite.sectionOverview.workflows.mail.action'
  },
  {
    productCode: 'CALENDAR',
    route: '/calendar',
    titleKey: 'suite.sectionOverview.workflows.calendar.title',
    descriptionKey: 'suite.sectionOverview.workflows.calendar.description',
    statusKey: 'suite.sectionOverview.workflows.calendar.status',
    boundaryKey: 'suite.sectionOverview.workflows.calendar.boundary',
    actionKey: 'suite.sectionOverview.workflows.calendar.action'
  },
  {
    productCode: 'DRIVE',
    route: '/drive',
    titleKey: 'suite.sectionOverview.workflows.drive.title',
    descriptionKey: 'suite.sectionOverview.workflows.drive.description',
    statusKey: 'suite.sectionOverview.workflows.drive.status',
    boundaryKey: 'suite.sectionOverview.workflows.drive.boundary',
    actionKey: 'suite.sectionOverview.workflows.drive.action'
  },
  {
    productCode: 'PASS',
    route: '/pass',
    titleKey: 'suite.sectionOverview.workflows.pass.title',
    descriptionKey: 'suite.sectionOverview.workflows.pass.description',
    statusKey: 'suite.sectionOverview.workflows.pass.status',
    boundaryKey: 'suite.sectionOverview.workflows.pass.boundary',
    actionKey: 'suite.sectionOverview.workflows.pass.action'
  }
]

export const COMMUNITY_V1_ADOPTION_CHECKLIST_ITEMS: ReadonlyArray<CommunityAdoptionChecklistItem> = [
  {
    code: 'MAIL_E2EE',
    href: '/settings#settings-mail-e2ee-panel',
    external: false,
    titleKey: 'settings.adoption.checklist.mailE2ee.title',
    descriptionKey: 'settings.adoption.checklist.mailE2ee.description',
    actionKey: 'settings.adoption.checklist.mailE2ee.action'
  },
  {
    code: 'PWA_WEB_PUSH',
    href: '/settings#settings-pwa-panel',
    external: false,
    titleKey: 'settings.adoption.checklist.pwa.title',
    descriptionKey: 'settings.adoption.checklist.pwa.description',
    actionKey: 'settings.adoption.checklist.pwa.action'
  },
  {
    code: 'BOUNDARY_MAP',
    href: '/suite?section=boundary',
    external: false,
    titleKey: 'settings.adoption.checklist.boundary.title',
    descriptionKey: 'settings.adoption.checklist.boundary.description',
    actionKey: 'settings.adoption.checklist.boundary.action'
  },
  {
    code: 'SELF_HOSTED_GUIDE',
    href: '/self-hosted/install.html',
    external: true,
    titleKey: 'settings.adoption.checklist.selfHosted.title',
    descriptionKey: 'settings.adoption.checklist.selfHosted.description',
    actionKey: 'settings.adoption.checklist.selfHosted.action'
  },
  {
    code: 'SECURE_SHARE',
    href: '/drive',
    external: false,
    titleKey: 'settings.adoption.checklist.secureShare.title',
    descriptionKey: 'settings.adoption.checklist.secureShare.description',
    actionKey: 'settings.adoption.checklist.secureShare.action'
  }
]

export function isCommunityCoreProductCode(code: string | null | undefined): code is CommunityCoreProductCode {
  return Boolean(code && COMMUNITY_V1_CORE_PRODUCT_CODE_SET.has(code))
}

export function filterCommunityCoreProductItems<T extends { code: string }>(items: readonly T[]): T[] {
  return items.filter(item => isCommunityCoreProductCode(item.code))
}

export function filterCommunityCoreScopedItems<T extends { productCode: string | null | undefined }>(
  items: readonly T[]
): T[] {
  return items.filter(item => isCommunityCoreProductCode(item.productCode))
}

export function findCommunityModuleByRoute(route: string): CommunityModuleDefinition | null {
  return COMMUNITY_V1_MODULES.find(item => item.route === route) || null
}
