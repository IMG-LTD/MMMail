import { describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import SuiteReleaseBoundaryPanel from '../components/suite/SuiteReleaseBoundaryPanel.vue'
import { messages } from '../locales'
import { translate } from '../utils/i18n'
import {
  buildCommunityBoundarySections,
  COMMUNITY_BOUNDARY_DOC_PATHS,
  COMMUNITY_HOSTED_ONLY_ITEM_KEYS,
  COMMUNITY_SELF_HOSTED_ITEM_KEYS,
  countCommunityModulesByMaturity,
  countCommunityModulesBySurface
} from '../utils/community-boundary'

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    locale: { value: 'en' },
    t: (key: string, params?: Record<string, string | number>) => translate(messages, 'en', key, params)
  })
}))

const stubs = {
  ElTag: defineComponent({
    name: 'ElTag',
    template: '<span><slot /></span>'
  })
}

describe('community boundary helpers', () => {
  it('groups modules by maturity with stable counts', () => {
    const sections = buildCommunityBoundarySections()

    expect(sections).toHaveLength(3)
    expect(sections[0].maturity).toBe('GA')
    expect(sections[1].maturity).toBe('BETA')
    expect(sections[2].maturity).toBe('PREVIEW')
    expect(countCommunityModulesByMaturity('GA')).toBe(8)
    expect(countCommunityModulesByMaturity('BETA')).toBe(3)
    expect(countCommunityModulesByMaturity('PREVIEW')).toBe(11)
    expect(countCommunityModulesBySurface('DEFAULT_NAV')).toBe(10)
    expect(countCommunityModulesBySurface('SUITE')).toBe(1)
    expect(sections[1].modules.some((item) => item.code === 'BILLING_CENTER')).toBe(true)
  })

  it('keeps hosted-only and self-hosted boundary registries non-empty', () => {
    expect(COMMUNITY_HOSTED_ONLY_ITEM_KEYS.length).toBeGreaterThan(2)
    expect(COMMUNITY_SELF_HOSTED_ITEM_KEYS.length).toBeGreaterThan(2)
    expect(COMMUNITY_BOUNDARY_DOC_PATHS).toContain('README.md')
  })
})

describe('suite release boundary panel', () => {
  it('renders boundary sections and canonical docs', () => {
    const wrapper = mount(SuiteReleaseBoundaryPanel, {
      global: { stubs }
    })

    expect(wrapper.text()).toContain('Release boundary map')
    expect(wrapper.text()).toContain('Community release surface')
    expect(wrapper.text()).toContain('Hosted-only promises')
    expect(wrapper.text()).toContain('Self-hosting responsibilities')
    expect(wrapper.text()).toContain('Canonical docs')
    expect(wrapper.text()).toContain('Real billing / payment capture stays out of Community.')
    expect(wrapper.text()).toContain('Billing center')
    expect(wrapper.text()).toContain('Suite workspace')
    expect(wrapper.text()).toContain('Docs')
    expect(wrapper.text()).toContain('Pass')
    expect(wrapper.text()).toContain('Labs only')
    expect(wrapper.text()).toContain('README.md')
  })
})
