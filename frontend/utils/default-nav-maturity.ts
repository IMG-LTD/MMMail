import { findCommunityModuleByRoute } from '~/constants/module-maturity'

export type DefaultNavMaturityTone = 'beta' | 'preview'

export interface DefaultNavMaturityBadge {
  labelKey: string
  tone: DefaultNavMaturityTone
}

const DEFAULT_NAV_MATURITY_LABEL_KEYS = {
  BETA: 'shell.nav.maturity.beta',
  PREVIEW: 'shell.nav.maturity.preview'
} as const

export function resolveDefaultNavMaturityBadge(route: string): DefaultNavMaturityBadge | null {
  const module = findCommunityModuleByRoute(route)

  if (!module || module.surface !== 'DEFAULT_NAV' || module.maturity === 'GA') {
    return null
  }

  return {
    labelKey: DEFAULT_NAV_MATURITY_LABEL_KEYS[module.maturity],
    tone: module.maturity.toLowerCase() as DefaultNavMaturityTone
  }
}
