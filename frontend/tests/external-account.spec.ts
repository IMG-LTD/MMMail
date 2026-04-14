import { describe, expect, it } from 'vitest'
import { DEFAULT_NAV_ITEMS, SECONDARY_NAV_ITEMS } from '../utils/default-nav'
import {
  filterNavItemsByAccess,
  isProductEnabledForMailAddressMode,
  resolveHomeRoute
} from '../utils/org-product-access'

describe('external account boundary', () => {
  it('locks mail and calendar surfaces for external accounts', () => {
    expect(isProductEnabledForMailAddressMode('MAIL', 'EXTERNAL_ACCOUNT')).toBe(false)
    expect(isProductEnabledForMailAddressMode('CALENDAR', 'EXTERNAL_ACCOUNT')).toBe(false)
    expect(isProductEnabledForMailAddressMode('DRIVE', 'EXTERNAL_ACCOUNT')).toBe(true)

    const filteredNav = filterNavItemsByAccess(DEFAULT_NAV_ITEMS, () => true, 'EXTERNAL_ACCOUNT')
    const filteredSecondaryNav = filterNavItemsByAccess(SECONDARY_NAV_ITEMS, () => true, 'EXTERNAL_ACCOUNT')
    expect(filteredNav.some(item => item.to === '/inbox')).toBe(false)
    expect(filteredNav.some(item => item.to === '/calendar')).toBe(false)
    expect(filteredNav.some(item => item.to === '/drive')).toBe(true)
    expect(filteredSecondaryNav.some(item => item.to === '/docs')).toBe(true)
    expect(filteredSecondaryNav.some(item => item.to === '/sheets')).toBe(true)
    expect(resolveHomeRoute(() => true, 'EXTERNAL_ACCOUNT')).toBe('/drive')
  })

  it('still respects org-level product switches after account filtering', () => {
    const orgEnabled = (productKey: string) => productKey !== 'DRIVE'

    expect(resolveHomeRoute(orgEnabled, 'EXTERNAL_ACCOUNT')).toBe('/docs')
  })
})
