import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import PassSidebarPanel from '../components/pass/PassSidebarPanel.vue'

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, string | number>) =>
      params ? `${key} ${Object.values(params).join(' ')}` : key,
  }),
}))

function readSource(path: string): string {
  return readFileSync(resolve(process.cwd(), path), 'utf8')
}

describe('a11y baseline', () => {
  it('keeps skip links and landmarks in shell layouts', () => {
    const defaultLayout = readSource('layouts/default.vue')
    const publicPassLayout = readSource('layouts/public-pass.vue')

    expect(defaultLayout).toContain("shell.a11y.skipToContent")
    expect(defaultLayout).toContain("topbar.searchAriaLabel")
    expect(defaultLayout).toContain("shell.a11y.mainNavigation")
    expect(defaultLayout).toContain('id="shell-main-content"')
    expect(defaultLayout).toContain("topbar.securityAriaLabel")

    expect(publicPassLayout).toContain("shell.a11y.skipToContent")
    expect(publicPassLayout).toContain("shell.publicPass.a11y.headerLabel")
    expect(publicPassLayout).toContain('id="public-pass-main"')
    expect(publicPassLayout).toContain("shell.publicPass.a11y.mainLabel")
  })

  it('keeps advanced navigation and labs guidance secondary to the mainline flow', () => {
    const defaultLayout = readSource('layouts/default.vue')
    const labsPage = readSource('pages/labs.vue')
    const communityReleaseLocale = readSource('locales/community-release.ts')

    expect(defaultLayout).toContain('sidebar-secondary')
    expect(defaultLayout).toContain('SECONDARY_NAV_ITEMS')
    expect(defaultLayout).toContain('localizedSecondaryNavItems')
    expect(defaultLayout).toContain('v-for="item in localizedSecondaryNavItems"')

    expect(labsPage).toContain('data-testid="labs-secondary-note"')
    expect(labsPage).toContain('labs.secondary.title')
    expect(labsPage).toContain('labs.secondary.description')

    expect(communityReleaseLocale.match(/'labs\.secondary\.title'/g)?.length).toBe(3)
    expect(communityReleaseLocale.match(/'labs\.secondary\.description'/g)?.length).toBe(3)
  })

  it('keeps public pass share regions labeled for assistive tech', () => {
    const publicSharePage = readSource('pages/share/pass/[token].vue')

    expect(publicSharePage).toContain("aria-busy")
    expect(publicSharePage).toContain("aria-live=\"polite\"")
    expect(publicSharePage).toContain("pass.publicShare.a11y.summaryRegion")
    expect(publicSharePage).toContain("pass.publicShare.a11y.secretRegion")
  })

  it('marks current pass sidebar entries with accessible labels', () => {
    const wrapper = shallowMount(PassSidebarPanel, {
      props: {
        mode: 'PERSONAL',
        activeItemId: 'item-1',
        activeVaultId: '',
        items: [{
          id: 'item-1',
          scopeType: 'PERSONAL',
          sharedVaultId: null,
          title: 'Admin Login',
          itemType: 'LOGIN',
          username: 'admin@mmmail.local',
          website: '',
          updatedAt: '2026-04-03T00:00:00',
          favorite: true,
          secureLinkCount: 2,
        }],
        sharedVaults: [],
      },
      global: {
        stubs: {
          ElEmpty: { props: ['description'], template: '<div>{{ description }}</div>' },
        },
      },
    })

    const button = wrapper.get('button')
    expect(button.attributes('aria-current')).toBe('true')
    expect(button.attributes('aria-label')).toContain('pass.sidebar.itemAriaLabel')
    expect(button.attributes('aria-label')).toContain('Admin Login')
  })
})
