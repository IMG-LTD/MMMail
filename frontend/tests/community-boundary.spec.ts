import { describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import SuiteReleaseBoundaryPanel from '../components/suite/SuiteReleaseBoundaryPanel.vue'
import { messages } from '../locales'
import { translate } from '../utils/i18n'
import {
  COMMUNITY_CAPABILITY_STATUS,
  buildCommunityBoundarySections,
  COMMUNITY_BOUNDARY_DOC_PATHS,
  COMMUNITY_HOSTED_ONLY_ITEM_KEYS,
  COMMUNITY_SELF_HOSTED_ITEM_KEYS,
  countCommunityCapabilitiesByStatus,
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
    expect(countCommunityModulesByMaturity('BETA')).toBe(4)
    expect(countCommunityModulesByMaturity('PREVIEW')).toBe(10)
    expect(countCommunityModulesBySurface('DEFAULT_NAV')).toBe(11)
    expect(countCommunityModulesBySurface('SUITE')).toBe(1)
    expect(countCommunityModulesBySurface('LABS')).toBe(10)
    expect(sections[1].modules.some((item) => item.code === 'BILLING_CENTER')).toBe(true)
    expect(sections[1].modules.some((item) => item.code === 'PASS')).toBe(true)
  })

  it('keeps hosted-only and self-hosted boundary registries non-empty', () => {
    expect(COMMUNITY_HOSTED_ONLY_ITEM_KEYS.length).toBeGreaterThan(2)
    expect(COMMUNITY_SELF_HOSTED_ITEM_KEYS.length).toBeGreaterThan(2)
    expect(COMMUNITY_CAPABILITY_STATUS).toHaveLength(8)
    expect(countCommunityCapabilitiesByStatus('IMPLEMENTED')).toBe(1)
    expect(countCommunityCapabilitiesByStatus('LIMITED')).toBe(4)
    expect(countCommunityCapabilitiesByStatus('DISCOVERY')).toBe(1)
    expect(countCommunityCapabilitiesByStatus('NOT_SHIPPED')).toBe(1)
    expect(countCommunityCapabilitiesByStatus('HOSTED_ONLY')).toBe(1)
    expect(COMMUNITY_BOUNDARY_DOC_PATHS).toContain('README.md')
    expect(COMMUNITY_BOUNDARY_DOC_PATHS).toContain('docs/ops/install.md')
    expect(COMMUNITY_BOUNDARY_DOC_PATHS).toContain('docs/release/community-v1-support-boundaries.md')
    expect(COMMUNITY_BOUNDARY_DOC_PATHS).toContain('docs/open-source/module-maturity-matrix.md')
    expect(COMMUNITY_BOUNDARY_DOC_PATHS).toContain('docs/architecture/mail-zero-knowledge-roadmap.md')
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
    expect(wrapper.text()).toContain('Capability status')
    expect(wrapper.text()).toContain('Canonical docs')
    expect(wrapper.text()).toContain('Real billing / payment capture stays out of Community.')
    expect(wrapper.text()).toContain('PWA shell')
    expect(wrapper.text()).toContain('Web Push delivery')
    expect(wrapper.text()).toContain('Mail E2EE')
    expect(wrapper.text()).toContain('Drive E2EE')
    expect(wrapper.text()).toContain('Zero-knowledge mail architecture')
    expect(wrapper.text()).toContain('SMTP / IMAP / Bridge')
    expect(wrapper.text()).toContain('Limited')
    expect(wrapper.text()).toContain('Hosted-only')
    expect(wrapper.text()).toContain('Billing center')
    expect(wrapper.text()).toContain('Suite workspace')
    expect(wrapper.text()).toContain('Docs')
    expect(wrapper.text()).toContain('Pass')
    expect(wrapper.text()).toContain('Labs only')
    expect(wrapper.text()).toContain('README.md')
    expect(wrapper.text()).toContain('docs/ops/install.md')
    expect(wrapper.text()).toContain('docs/release/community-v1-support-boundaries.md')
    expect(wrapper.text()).toContain('docs/open-source/module-maturity-matrix.md')
    expect(wrapper.text()).toContain('docs/architecture/mail-zero-knowledge-roadmap.md')
  })
})
