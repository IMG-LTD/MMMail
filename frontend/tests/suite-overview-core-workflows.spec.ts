import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import type { SuiteProductItem } from '~/types/api'
import type { MainlineCollaborationEvent } from '~/utils/collaboration'

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
        collaborationItems: buildCollaborationItems(),
        collaborationLoading: false,
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

    expect(panel.get('[data-testid="suite-core-workflow-mail"]').text())
      .toContain('suite.sectionOverview.workflows.mail.title')
    expect(panel.get('[data-testid="suite-core-workflow-mail"] a').attributes('href')).toBe('/compose')
    expect(panel.get('[data-testid="suite-core-workflow-calendar"] a').attributes('href')).toBe('/calendar')
    expect(panel.get('[data-testid="suite-core-workflow-drive"] a').attributes('href')).toBe('/drive')
    expect(panel.get('[data-testid="suite-core-workflow-pass"] a').attributes('href')).toBe('/pass')
    expect(panel.get('[data-testid="suite-mainline-stage-mail"] a').attributes('href')).toBe('/compose')
    expect(panel.get('[data-testid="suite-mainline-stage-pass"] a').attributes('href')).toBe('/pass')
    expect(panel.get('[data-testid="suite-mainline-journey-panel"]').text())
      .toContain('suite.sectionOverview.mainline.title')
    expect(panel.get('[data-testid="suite-mainline-handoff-panel"]').attributes('data-status')).toBe('active')
    expect(panel.get('[data-testid="suite-mainline-handoff-stage-drive"]').attributes('data-status')).toBe('active')
    expect(panel.get('[data-testid="suite-mainline-handoff-stage-pass"]').attributes('data-status')).toBe('next')
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

function buildCollaborationItems(): MainlineCollaborationEvent[] {
  return [
    {
      eventId: 101,
      productCode: 'MAIL',
      eventType: 'MAIL_SENT',
      title: 'Mail sent',
      summary: 'Sent secure kickoff',
      routePath: '/compose',
      actorEmail: 'mail@mmmail.local',
      sessionId: 'suite-1',
      createdAt: '2026-04-09T10:00:00'
    },
    {
      eventId: 102,
      productCode: 'CALENDAR',
      eventType: 'CAL_SHARE_CREATE',
      title: 'Calendar shared',
      summary: 'Shared review checkpoint',
      routePath: '/calendar',
      actorEmail: 'calendar@mmmail.local',
      sessionId: 'suite-1',
      createdAt: '2026-04-09T10:03:00'
    },
    {
      eventId: 103,
      productCode: 'DRIVE',
      eventType: 'DRIVE_SHARE_CREATE',
      title: 'Drive share created',
      summary: 'Shared encrypted file',
      routePath: '/drive',
      actorEmail: 'drive@mmmail.local',
      sessionId: 'suite-1',
      createdAt: '2026-04-09T10:05:00'
    },
    {
      eventId: 104,
      productCode: 'PASS',
      eventType: 'PASS_ITEM_CREATE',
      title: 'Pass item created',
      summary: 'Old credential handoff',
      routePath: '/pass',
      actorEmail: 'pass@mmmail.local',
      sessionId: 'suite-0',
      createdAt: '2026-04-09T09:55:00'
    }
  ]
}
