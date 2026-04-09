import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import { flushPromises, mount } from '@vue/test-utils'
import SuiteSectionNav from '~/components/suite/SuiteSectionNav.vue'
import {
  buildSuiteSectionQuery,
  resolveSuiteSection,
  SUITE_SECTIONS
} from '~/utils/suite-sections'

const navigateToMock = vi.fn()
const routeState = {
  query: {} as Record<string, string>
}

const billingWorkspace = {
  loading: ref(false),
  saveDraft: vi.fn(),
  loadBillingData: vi.fn()
}

const billingCenterWorkspace = {
  loading: ref(false),
  loadBillingCenter: vi.fn()
}

const plansWorkspace = {
  loading: ref(false),
  subscription: ref(null),
  usageRows: ref([]),
  showDriveEntityUsage: ref(false),
  upgradeSummary: ref(null),
  visibleProducts: ref([]),
  resolveSubscriptionStatusLabel: vi.fn(() => 'ACTIVE'),
  onChangePlan: vi.fn(async () => true)
}

const operationsWorkspace = {
  loading: ref(false),
  reloadAfterPlanChange: vi.fn(async () => undefined)
}

const overviewWorkspace = {
  loading: ref(false),
  collaborationItems: ref([]),
  loadCollaborationCenter: vi.fn(async () => undefined)
}

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string) => key
  })
}))

vi.mock('~/composables/useSuiteBillingWorkspace', () => ({
  useSuiteBillingWorkspace: () => billingWorkspace
}))

vi.mock('~/composables/useSuiteBillingCenterWorkspace', () => ({
  useSuiteBillingCenterWorkspace: () => billingCenterWorkspace
}))

vi.mock('~/composables/useSuitePlansWorkspace', () => ({
  useSuitePlansWorkspace: () => plansWorkspace
}))

vi.mock('~/composables/useSuiteOperationsWorkspace', () => ({
  useSuiteOperationsWorkspace: () => operationsWorkspace
}))

vi.mock('~/composables/useSuiteOverviewWorkspace', () => ({
  useSuiteOverviewWorkspace: () => overviewWorkspace
}))

vi.mock('~/components/suite/SuitePlansHero.vue', () => ({
  default: defineComponent({
    name: 'SuitePlansHero',
    template: '<div data-testid="suite-hero-stub">hero</div>'
  })
}))

vi.mock('~/components/suite/SuiteOverviewSection.vue', () => ({
  default: defineComponent({
    name: 'SuiteOverviewSection',
    template: '<div data-testid="suite-section-overview-stub">overview</div>'
  })
}))

vi.mock('~/components/suite/SuitePlansSection.vue', () => ({
  default: defineComponent({
    name: 'SuitePlansSection',
    template: '<div data-testid="suite-section-plans-stub">plans</div>'
  })
}))

vi.mock('~/components/suite/SuiteBillingSection.vue', () => ({
  default: defineComponent({
    name: 'SuiteBillingSection',
    template: '<div data-testid="suite-section-billing-stub">billing</div>'
  })
}))

vi.mock('~/components/suite/SuiteOperationsSection.vue', () => ({
  default: defineComponent({
    name: 'SuiteOperationsSection',
    template: '<div data-testid="suite-section-operations-stub">operations</div>'
  })
}))

vi.mock('~/components/suite/SuiteBoundarySection.vue', () => ({
  default: defineComponent({
    name: 'SuiteBoundarySection',
    template: '<div data-testid="suite-section-boundary-stub">boundary</div>'
  })
}))

describe('suite sections', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    routeState.query = {}
    navigateToMock.mockResolvedValue(undefined)
    ;(globalThis as typeof globalThis & {
      useRoute?: () => typeof routeState
      navigateTo?: typeof navigateToMock
      definePageMeta?: (value: unknown) => void
      useHead?: (value: unknown) => void
    }).useRoute = () => routeState
    ;(globalThis as typeof globalThis & {
      useRoute?: () => typeof routeState
      navigateTo?: typeof navigateToMock
      definePageMeta?: (value: unknown) => void
      useHead?: (value: unknown) => void
    }).navigateTo = navigateToMock
    ;(globalThis as typeof globalThis & {
      definePageMeta?: (value: unknown) => void
      useHead?: (value: unknown) => void
    }).definePageMeta = vi.fn()
    ;(globalThis as typeof globalThis & {
      definePageMeta?: (value: unknown) => void
      useHead?: (value: unknown) => void
    }).useHead = vi.fn()
  })

  afterEach(() => {
    delete (globalThis as typeof globalThis & { useRoute?: unknown }).useRoute
    delete (globalThis as typeof globalThis & { navigateTo?: unknown }).navigateTo
    delete (globalThis as typeof globalThis & { definePageMeta?: unknown }).definePageMeta
    delete (globalThis as typeof globalThis & { useHead?: unknown }).useHead
  })

  it('normalizes section query values and preserves unrelated query keys', () => {
    expect(resolveSuiteSection(undefined)).toBe('overview')
    expect(resolveSuiteSection('PLANS')).toBe('plans')
    expect(resolveSuiteSection('unknown')).toBe('overview')

    expect(buildSuiteSectionQuery({ keyword: 'mail' }, 'billing')).toEqual({
      keyword: 'mail',
      section: 'billing'
    })
    expect(buildSuiteSectionQuery({ section: 'operations', keyword: 'mail' }, 'overview')).toEqual({
      keyword: 'mail'
    })
  })

  it('renders section navigation with an explicit active state', async () => {
    const wrapper = mount(SuiteSectionNav, {
      props: {
        activeSection: 'plans',
        sections: SUITE_SECTIONS
      }
    })

    expect(wrapper.get('[data-testid="suite-section-tab-plans"]').attributes('aria-pressed')).toBe('true')
    expect(wrapper.get('[data-testid="suite-section-tab-billing"]').attributes('aria-pressed')).toBe('false')

    await wrapper.get('[data-testid="suite-section-tab-billing"]').trigger('click')

    expect(wrapper.emitted('select')?.[0]).toEqual(['billing'])
  })

  it('defaults /suite to overview and routes to the selected section', async () => {
    const page = await mountPage()

    expect(page.get('[data-testid="suite-section-shell"]').attributes('data-section')).toBe('overview')
    expect(page.find('[data-testid="suite-section-overview-stub"]').exists()).toBe(true)

    await page.get('[data-testid="suite-section-tab-billing"]').trigger('click')
    await flushPromises()

    expect(navigateToMock).toHaveBeenCalledWith({
      path: '/suite',
      query: {
        section: 'billing'
      }
    }, {
      replace: true
    })
  })

  it('respects the route section and keeps boundary as an explicit section', async () => {
    routeState.query = { section: 'boundary' }

    const page = await mountPage()

    expect(page.get('[data-testid="suite-section-shell"]').attributes('data-section')).toBe('boundary')
    expect(page.find('[data-testid="suite-section-boundary-stub"]').exists()).toBe(true)
  })
})

async function mountPage() {
  const { default: SuitePage } = await import('~/pages/suite.vue')
  return mount(SuitePage, {
    global: {
      directives: {
        loading: () => undefined
      }
    }
  })
}
