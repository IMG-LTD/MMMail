import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '../stores/auth'
import { useOrgAccessStore } from '../stores/org-access'
import { DEFAULT_NAV_ITEMS } from '../utils/default-nav'
import { filterNavItemsByAccess, resolveHomeRoute, resolveProductKeyFromPath } from '../utils/org-product-access'
import type { OrgAccessScope } from '../types/org-access'

const STORAGE_KEY = 'mmmail.active-org-scope.v1'
const mockListAccessContext = vi.fn<() => Promise<OrgAccessScope[]>>()

vi.mock('../composables/useOrgAccessApi', () => ({
  useOrgAccessApi: () => ({
    listAccessContext: mockListAccessContext
  })
}))

const scopes: OrgAccessScope[] = [
  {
    orgId: 'org-1',
    orgName: 'Parity Org',
    orgSlug: 'parity-org',
    role: 'MEMBER',
    enabledProductCount: 2,
    products: [
      { productKey: 'MAIL', accessState: 'ENABLED' },
      { productKey: 'DOCS', accessState: 'DISABLED' },
      { productKey: 'CALENDAR', accessState: 'ENABLED' }
    ]
  }
]

describe('org product access', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mockListAccessContext.mockReset()
    window.localStorage.removeItem(STORAGE_KEY)
  })

  it('maps product routes and resolves a safe home route', () => {
    expect(resolveProductKeyFromPath('/docs')).toBe('DOCS')
    expect(resolveProductKeyFromPath('/folders/123')).toBe('MAIL')
    expect(resolveProductKeyFromPath('/organizations')).toBeNull()
    expect(resolveHomeRoute((productKey) => productKey !== 'MAIL')).toBe('/calendar')
  })

  it('filters nav items by enabled products', () => {
    const filtered = filterNavItemsByAccess(DEFAULT_NAV_ITEMS, (productKey) => productKey !== 'DOCS')
    expect(filtered.some(item => item.to === '/docs')).toBe(false)
    expect(filtered.some(item => item.to === '/calendar')).toBe(true)
    expect(filtered.some(item => item.to === '/organizations')).toBe(false)
  })

  it('loads scopes, auto-selects a single org, and persists personal fallback', async () => {
    mockListAccessContext.mockResolvedValueOnce(scopes)

    const authStore = useAuthStore()
    authStore.applySession({
      accessToken: 'token-1',
      refreshToken: 'refresh-1',
      user: {
        id: 'user-1',
        email: 'member@example.com',
        displayName: 'Member',
        role: 'USER',
        mailAddressMode: 'PROTON_ADDRESS'
      }
    })

    const store = useOrgAccessStore()
    await store.ensureLoaded()

    expect(store.activeOrgId).toBe('org-1')
    expect(store.isProductEnabled('MAIL')).toBe(true)
    expect(store.isProductEnabled('DOCS')).toBe(false)
    expect(window.localStorage.getItem(STORAGE_KEY)).toBe('org-1')

    store.setPersonalScope()

    expect(store.activeOrgId).toBe('')
    expect(store.isProductEnabled('DOCS')).toBe(true)
    expect(window.localStorage.getItem(STORAGE_KEY)).toBeNull()
  })
})
