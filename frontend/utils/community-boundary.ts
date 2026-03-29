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

export const COMMUNITY_BOUNDARY_DOC_PATHS = [
  'README.md',
  'docs/release/community-v1-support-boundaries.md',
  'docs/release/community-v1-roadmap.md',
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
