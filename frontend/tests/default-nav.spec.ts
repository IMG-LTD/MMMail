import { describe, expect, it } from 'vitest'
import { DEFAULT_NAV_ITEMS } from '../utils/default-nav'

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
})
