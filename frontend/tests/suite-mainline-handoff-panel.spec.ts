import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { buildMainlineHandoffRun } from '~/utils/mainline-handoff'
import type { MainlineCollaborationEvent } from '~/utils/collaboration'

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, string | number>) => {
      if (key.startsWith('organizations.products.')) {
        return key.split('.').at(-1) || key
      }
      if (!params) {
        return key
      }
      return Object.entries(params).reduce((message, [paramKey, value]) => {
        return message.replaceAll(`{${paramKey}}`, String(value))
      }, key)
    }
  })
}))

describe('SuiteMainlineHandoffPanel', () => {
  it('renders the current run from real collaboration evidence', async () => {
    const { default: SuiteMainlineHandoffPanel } = await import('~/components/suite/SuiteMainlineHandoffPanel.vue')
    const run = buildMainlineHandoffRun(buildEvents())
    const wrapper = mount(SuiteMainlineHandoffPanel, {
      props: {
        run,
        loading: false
      },
      global: {
        stubs: {
          NuxtLink: {
            props: ['to'],
            template: '<a :href="to"><slot /></a>'
          }
        }
      }
    })

    expect(wrapper.get('[data-testid="suite-mainline-handoff-panel"]').attributes('data-status')).toBe('active')
    expect(wrapper.get('[data-testid="suite-mainline-handoff-stage-mail"]').attributes('data-status')).toBe('done')
    expect(wrapper.get('[data-testid="suite-mainline-handoff-stage-calendar"]').attributes('data-status')).toBe('done')
    expect(wrapper.get('[data-testid="suite-mainline-handoff-stage-drive"]').attributes('data-status')).toBe('active')
    expect(wrapper.get('[data-testid="suite-mainline-handoff-stage-pass"]').attributes('data-status')).toBe('next')
    expect(wrapper.get('[data-testid="suite-mainline-handoff-stage-drive"] a').attributes('href')).toBe('/drive')
    expect(wrapper.get('[data-testid="suite-mainline-handoff-signals"]').text()).toContain('DRIVE')
  })

  it('stays idle until the first mainline kickoff exists', async () => {
    const { default: SuiteMainlineHandoffPanel } = await import('~/components/suite/SuiteMainlineHandoffPanel.vue')
    const run = buildMainlineHandoffRun([])
    const wrapper = mount(SuiteMainlineHandoffPanel, {
      props: {
        run,
        loading: false
      },
      global: {
        stubs: {
          NuxtLink: {
            props: ['to'],
            template: '<a :href="to"><slot /></a>'
          }
        }
      }
    })

    expect(wrapper.get('[data-testid="suite-mainline-handoff-panel"]').attributes('data-status')).toBe('idle')
    expect(wrapper.get('[data-testid="suite-mainline-handoff-stage-mail"]').attributes('data-status')).toBe('next')
    expect(wrapper.get('[data-testid="suite-mainline-handoff-stage-calendar"]').attributes('data-status')).toBe('pending')
  })
})

function buildEvents(): MainlineCollaborationEvent[] {
  return [
    {
      eventId: 10,
      productCode: 'MAIL',
      eventType: 'MAIL_SENT',
      title: 'Mail sent',
      summary: 'Secure kickoff',
      routePath: '/compose',
      actorEmail: 'mail@mmmail.local',
      sessionId: 'handoff-1',
      createdAt: '2026-04-09T10:00:00'
    },
    {
      eventId: 11,
      productCode: 'CALENDAR',
      eventType: 'CAL_SHARE_CREATE',
      title: 'Calendar share created',
      summary: 'Checkpoint shared',
      routePath: '/calendar',
      actorEmail: 'calendar@mmmail.local',
      sessionId: 'handoff-1',
      createdAt: '2026-04-09T10:03:00'
    },
    {
      eventId: 12,
      productCode: 'DRIVE',
      eventType: 'DRIVE_SHARE_CREATE',
      title: 'Drive share created',
      summary: 'Encrypted package shared',
      routePath: '/drive',
      actorEmail: 'drive@mmmail.local',
      sessionId: 'handoff-1',
      createdAt: '2026-04-09T10:06:00'
    },
    {
      eventId: 9,
      productCode: 'PASS',
      eventType: 'PASS_ITEM_CREATE',
      title: 'Pass item created',
      summary: 'Old secret handoff',
      routePath: '/pass',
      actorEmail: 'pass@mmmail.local',
      sessionId: 'handoff-0',
      createdAt: '2026-04-09T09:55:00'
    }
  ]
}
