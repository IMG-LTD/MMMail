import type { OrgProductKey } from '~/types/organization-admin'

export type PreviewRegistryStrategy = 'PLUGIN' | 'EXTERNALIZED' | 'EXPERIMENT'

export interface PreviewRegistryEntry {
  code: string
  route: string
  labelKey: string
  productKey?: OrgProductKey
  strategy: PreviewRegistryStrategy
  curatedDefault: boolean
}

export const COMMUNITY_V1_PREVIEW_REGISTRY: ReadonlyArray<PreviewRegistryEntry> = [
  { code: 'AUTHENTICATOR', route: '/authenticator', labelKey: 'nav.authenticator', productKey: 'AUTHENTICATOR', strategy: 'PLUGIN', curatedDefault: true },
  { code: 'SIMPLELOGIN', route: '/simplelogin', labelKey: 'nav.simpleLogin', productKey: 'SIMPLELOGIN', strategy: 'PLUGIN', curatedDefault: true },
  { code: 'STANDARD_NOTES', route: '/standard-notes', labelKey: 'nav.standardNotes', productKey: 'STANDARD_NOTES', strategy: 'PLUGIN', curatedDefault: true },
  { code: 'VPN', route: '/vpn', labelKey: 'nav.vpn', productKey: 'VPN', strategy: 'EXTERNALIZED', curatedDefault: false },
  { code: 'MEET', route: '/meet', labelKey: 'nav.meet', productKey: 'MEET', strategy: 'EXTERNALIZED', curatedDefault: false },
  { code: 'WALLET', route: '/wallet', labelKey: 'nav.wallet', productKey: 'WALLET', strategy: 'EXTERNALIZED', curatedDefault: false },
  { code: 'LUMO', route: '/lumo', labelKey: 'nav.lumo', productKey: 'LUMO', strategy: 'EXTERNALIZED', curatedDefault: false },
  { code: 'COLLABORATION', route: '/collaboration', labelKey: 'nav.collaboration', strategy: 'EXPERIMENT', curatedDefault: false },
  { code: 'COMMAND_CENTER', route: '/command-center', labelKey: 'nav.commandCenter', strategy: 'EXPERIMENT', curatedDefault: false },
  { code: 'NOTIFICATIONS', route: '/notifications', labelKey: 'nav.notifications', strategy: 'EXPERIMENT', curatedDefault: false }
]

export const COMMUNITY_V1_CURATED_PREVIEW_MODULE_CODES = COMMUNITY_V1_PREVIEW_REGISTRY
  .filter(entry => entry.curatedDefault)
  .map(entry => entry.code)

const PREVIEW_REGISTRY_MAP = new Map(COMMUNITY_V1_PREVIEW_REGISTRY.map(entry => [entry.code, entry]))

export function getPreviewRegistryEntry(code: string): PreviewRegistryEntry {
  const entry = PREVIEW_REGISTRY_MAP.get(code)
  if (!entry) {
    throw new Error(`Missing preview registry entry for code: ${code}`)
  }
  return entry
}

export function countPreviewRegistryByStrategy(
  entries: readonly PreviewRegistryEntry[] = COMMUNITY_V1_PREVIEW_REGISTRY
): Record<PreviewRegistryStrategy, number> {
  return entries.reduce<Record<PreviewRegistryStrategy, number>>((accumulator, entry) => {
    accumulator[entry.strategy] += 1
    return accumulator
  }, { PLUGIN: 0, EXTERNALIZED: 0, EXPERIMENT: 0 })
}
