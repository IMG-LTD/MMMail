import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import type { SuiteProductItem } from '~/types/api'

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string) => key
  })
}))

describe('SuiteOverviewSection', () => {
  it('surfaces only core workflow launchers and trims non-mainline products from the hub', async () => {
    const { default: SuiteOverviewSection } = await import('~/components/suite/SuiteOverviewSection.vue')
    const panel = mount(SuiteOverviewSection, {
      props: {
        products: buildProducts(),
        sections: [
          {
            code: 'overview',
            labelKey: 'suite.sectionNav.sections.overview.label',
            descriptionKey: 'suite.sectionNav.sections.overview.description'
          },
          {
            code: 'plans',
            labelKey: 'suite.sectionNav.sections.plans.label',
            descriptionKey: 'suite.sectionNav.sections.plans.description'
          }
        ]
      },
      global: {
        stubs: {
          NuxtLink: {
            props: ['to'],
            template: '<a :href="to"><slot /></a>'
          },
          SuiteProductHubPanel: {
            props: ['products'],
            template: '<div data-testid="suite-product-hub-stub">{{ products.map(item => item.code).join(",") }}</div>'
          }
        }
      }
    })

    expect(panel.get('[data-testid="suite-core-workflow-mail"]').text()).toContain('suite.sectionOverview.workflows.mail.title')
    expect(panel.get('[data-testid="suite-core-workflow-mail"] a').attributes('href')).toBe('/compose')
    expect(panel.get('[data-testid="suite-core-workflow-calendar"] a').attributes('href')).toBe('/calendar')
    expect(panel.get('[data-testid="suite-core-workflow-drive"] a').attributes('href')).toBe('/drive')
    expect(panel.get('[data-testid="suite-core-workflow-pass"] a').attributes('href')).toBe('/pass')
    expect(panel.find('[data-testid="suite-core-workflow-wallet"]').exists()).toBe(false)
    expect(panel.get('[data-testid="suite-product-hub-stub"]').text()).toBe('MAIL,CALENDAR,DRIVE,PASS')
  })
})

function buildProducts(): SuiteProductItem[] {
  return [
    buildProduct('MAIL', 'Mail'),
    buildProduct('CALENDAR', 'Calendar'),
    buildProduct('DRIVE', 'Drive'),
    buildProduct('PASS', 'Pass'),
    buildProduct('WALLET', 'Wallet')
  ]
}

function buildProduct(code: string, name: string): SuiteProductItem {
  return {
    code,
    name,
    category: 'COLLABORATION',
    status: 'ENABLED',
    enabledByPlan: true,
    description: name,
    highlights: [name]
  }
}
