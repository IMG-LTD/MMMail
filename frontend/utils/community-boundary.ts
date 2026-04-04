import {
  COMMUNITY_V1_MODULES,
  type CommunityModuleDefinition,
  type ModuleMaturity,
  type ModuleSurface
} from '~/constants/module-maturity'

export interface CommunityBoundarySection {
  maturity: ModuleMaturity
  modules: CommunityModuleDefinition[]
}

export type CommunityCapabilityStatus = 'IMPLEMENTED' | 'LIMITED' | 'DISCOVERY' | 'NOT_SHIPPED' | 'HOSTED_ONLY'

export interface CommunityCapabilityStatusItem {
  code: string
  status: CommunityCapabilityStatus
  labelKey: string
  summaryKey: string
}

const MATURITY_ORDER: ReadonlyArray<ModuleMaturity> = ['GA', 'BETA', 'PREVIEW']

export const COMMUNITY_HOSTED_ONLY_ITEM_KEYS = [
  'community.boundary.hostedOnly.realBilling',
  'community.boundary.hostedOnly.taxAndInvoices',
  'community.boundary.hostedOnly.subscriptionOps',
  'community.boundary.hostedOnly.commercialSla'
] as const

export const COMMUNITY_SELF_HOSTED_ITEM_KEYS = [
  'community.boundary.selfHosted.secrets',
  'community.boundary.selfHosted.tls',
  'community.boundary.selfHosted.backup',
  'community.boundary.selfHosted.runners'
] as const

export const COMMUNITY_CAPABILITY_STATUS: ReadonlyArray<CommunityCapabilityStatusItem> = [
  {
    code: 'PWA_BASELINE',
    status: 'IMPLEMENTED',
    labelKey: 'community.boundary.capabilities.pwa.label',
    summaryKey: 'community.boundary.capabilities.pwa.summary'
  },
  {
    code: 'WEB_PUSH',
    status: 'LIMITED',
    labelKey: 'community.boundary.capabilities.webPush.label',
    summaryKey: 'community.boundary.capabilities.webPush.summary'
  },
  {
    code: 'NATIVE_CLIENTS',
    status: 'NOT_SHIPPED',
    labelKey: 'community.boundary.capabilities.nativeClients.label',
    summaryKey: 'community.boundary.capabilities.nativeClients.summary'
  },
  {
    code: 'MAIL_E2EE',
    status: 'LIMITED',
    labelKey: 'community.boundary.capabilities.mailE2ee.label',
    summaryKey: 'community.boundary.capabilities.mailE2ee.summary'
  },
  {
    code: 'DRIVE_E2EE',
    status: 'LIMITED',
    labelKey: 'community.boundary.capabilities.driveE2ee.label',
    summaryKey: 'community.boundary.capabilities.driveE2ee.summary'
  },
  {
    code: 'ZERO_KNOWLEDGE',
    status: 'DISCOVERY',
    labelKey: 'community.boundary.capabilities.zeroKnowledge.label',
    summaryKey: 'community.boundary.capabilities.zeroKnowledge.summary'
  },
  {
    code: 'MAIL_PROTOCOLS',
    status: 'LIMITED',
    labelKey: 'community.boundary.capabilities.mailProtocols.label',
    summaryKey: 'community.boundary.capabilities.mailProtocols.summary'
  },
  {
    code: 'REAL_BILLING',
    status: 'HOSTED_ONLY',
    labelKey: 'community.boundary.capabilities.realBilling.label',
    summaryKey: 'community.boundary.capabilities.realBilling.summary'
  }
] as const

export const COMMUNITY_BOUNDARY_DOC_PATHS = [
  'README.md',
  'docs/release/community-v1-v1.3-mainline-roadmap.md',
  'docs/release/community-v1-v1.2-capability-boundaries.md',
  'docs/architecture/mail-zero-knowledge-roadmap.md',
  'docs/architecture/mail-protocol-stack-discovery.md',
  'docs/release/community-v1-support-boundaries.md',
  'docs/open-source/module-maturity-matrix.md'
] as const

export function buildCommunityBoundarySections(
  modules: ReadonlyArray<CommunityModuleDefinition> = COMMUNITY_V1_MODULES
): CommunityBoundarySection[] {
  return MATURITY_ORDER.map((maturity) => ({
    maturity,
    modules: modules.filter((item) => item.maturity === maturity)
  }))
}

export function countCommunityModulesByMaturity(
  maturity: ModuleMaturity,
  modules: ReadonlyArray<CommunityModuleDefinition> = COMMUNITY_V1_MODULES
): number {
  return modules.filter((item) => item.maturity === maturity).length
}

export function countCommunityModulesBySurface(
  surface: ModuleSurface,
  modules: ReadonlyArray<CommunityModuleDefinition> = COMMUNITY_V1_MODULES
): number {
  return modules.filter((item) => item.surface === surface).length
}

export function countCommunityCapabilitiesByStatus(
  status: CommunityCapabilityStatus,
  items: ReadonlyArray<CommunityCapabilityStatusItem> = COMMUNITY_CAPABILITY_STATUS
): number {
  return items.filter((item) => item.status === status).length
}

export function resolveCommunityCapabilityTagType(status: CommunityCapabilityStatus): 'success' | 'warning' | 'info' | 'danger' {
  switch (status) {
    case 'IMPLEMENTED':
      return 'success'
    case 'LIMITED':
      return 'warning'
    case 'HOSTED_ONLY':
      return 'danger'
    default:
      return 'info'
  }
}
