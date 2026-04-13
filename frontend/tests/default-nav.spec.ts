import { describe, expect, it } from 'vitest'
import { DEFAULT_NAV_ITEMS, SECONDARY_NAV_ITEMS } from '../utils/default-nav'

describe('default nav', () => {
  it('keeps the default rail focused on mature mainline entries', () => {
    expect(DEFAULT_NAV_ITEMS.map((item) => item.to)).toEqual([
      '/inbox',
      '/unread',
      '/sent',
      '/drafts',
      '/search',
      '/compose',
      '/calendar',
      '/drive',
      '/pass',
      '/suite',
      '/settings',
      '/security',
      '/labs',
    ])
  })

  it('keeps advanced and overflow links in a separately filterable secondary rail', () => {
    expect(SECONDARY_NAV_ITEMS.map((item) => item.to)).toEqual([
      '/starred',
      '/labels',
      '/business',
      '/organizations',
      '/docs',
      '/sheets',
    ])
    expect(SECONDARY_NAV_ITEMS.find((item) => item.to === '/starred')?.productKey).toBe('MAIL')
    expect(SECONDARY_NAV_ITEMS.find((item) => item.to === '/labels')?.productKey).toBe('MAIL')
    expect(SECONDARY_NAV_ITEMS.find((item) => item.to === '/docs')?.productKey).toBe('DOCS')
    expect(SECONDARY_NAV_ITEMS.find((item) => item.to === '/sheets')?.productKey).toBe('SHEETS')
  })
})
