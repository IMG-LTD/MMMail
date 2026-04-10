import { describe, expect, it } from 'vitest'
import {
  COMMUNITY_V1_HOME_ROUTE_CANDIDATES,
  COMMUNITY_V1_LABS_MODULES,
  COMMUNITY_V1_MODULES
} from '../constants/module-maturity'
import { resolveDefaultNavMaturityBadge } from '../utils/default-nav-maturity'
import { DEFAULT_NAV_ITEMS } from '../utils/default-nav'
import { resolveHomeRoute } from '../utils/org-product-access'

describe('community v1 navigation', () => {
  it('removes labs-only modules from the default navigation and keeps labs', () => {
    const navRoutes = new Set(DEFAULT_NAV_ITEMS.map(item => item.to))

    for (const module of COMMUNITY_V1_LABS_MODULES) {
      expect(navRoutes.has(module.route)).toBe(false)
    }

    expect(navRoutes.has('/docs')).toBe(true)
    expect(navRoutes.has('/sheets')).toBe(true)
    expect(navRoutes.has('/labs')).toBe(true)
  })

  it('limits home candidates to ga-safe routes only', () => {
    const passModule = COMMUNITY_V1_MODULES.find(item => item.code === 'PASS')

    expect(passModule?.maturity).toBe('BETA')
    expect(passModule?.surface).toBe('DEFAULT_NAV')
    expect(COMMUNITY_V1_HOME_ROUTE_CANDIDATES.some(item => item.to === '/inbox')).toBe(true)
    expect(COMMUNITY_V1_HOME_ROUTE_CANDIDATES.some(item => item.to === '/pass')).toBe(false)
    expect(resolveHomeRoute(productKey => productKey === 'PASS')).toBe('/suite')
  })

  it('surfaces pass as a beta mainline route while keeping it out of labs and home fallback', () => {
    const navRoutes = new Set(DEFAULT_NAV_ITEMS.map(item => item.to))

    expect(navRoutes.has('/pass')).toBe(true)
    expect(COMMUNITY_V1_LABS_MODULES.some(item => item.code === 'PASS')).toBe(false)
    expect(COMMUNITY_V1_HOME_ROUTE_CANDIDATES.some(item => item.to === '/pass')).toBe(false)
  })

  it('maps beta pills only onto beta routes in the default navigation', () => {
    expect(resolveDefaultNavMaturityBadge('/pass')?.labelKey).toBe('shell.nav.maturity.beta')
    expect(resolveDefaultNavMaturityBadge('/docs')?.labelKey).toBe('shell.nav.maturity.beta')
    expect(resolveDefaultNavMaturityBadge('/sheets')?.labelKey).toBe('shell.nav.maturity.beta')
    expect(resolveDefaultNavMaturityBadge('/suite')).toBeNull()
    expect(resolveDefaultNavMaturityBadge('/labs')).toBeNull()
  })

  it('keeps suite-only beta entries out of default nav', () => {
    const navRoutes = new Set(DEFAULT_NAV_ITEMS.map(item => item.to))
    const billingModule = COMMUNITY_V1_MODULES.find(item => item.code === 'BILLING_CENTER')

    expect(billingModule?.surface).toBe('SUITE')
    expect(navRoutes.has('/suite')).toBe(true)
    expect(DEFAULT_NAV_ITEMS.filter(item => item.to === '/suite')).toHaveLength(1)
  })
})
