import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import type { SuiteCollaborationEvent } from '~/types/api'
import type { MainlineCollaborationEvent } from '~/utils/collaboration'

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string) => {
      if (key.startsWith('organizations.products.')) {
        return key.split('.').at(-1) || key
      }
      return key
    }
  })
}))

describe('DriveCollaborationLaunchpad', () => {
  it('surfaces handoff progress, owner E2EE readiness, and Pass CTA', async () => {
    const { default: DriveCollaborationLaunchpad } = await import('~/components/drive/DriveCollaborationLaunchpad.vue')
    const wrapper = mount(DriveCollaborationLaunchpad, {
      props: {
        items: buildContextItems(),
        handoffItems: buildHandoffItems(),
        loading: false,
        creatingDoc: false,
        creatingSheet: false,
        ownerE2eeReady: true,
        ownerE2eeLoading: false,
        ownerE2eeError: ''
      },
      global: {
        stubs: {
          ElButton: {
            emits: ['click'],
            template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
          },
          ElSkeleton: {
            template: '<div data-testid="el-skeleton-stub"></div>'
          },
          ElEmpty: {
            props: ['description'],
            template: '<div>{{ description }}</div>'
          }
        }
      }
    })

    expect(wrapper.get('[data-testid="drive-launchpad-handoff"]').text()).toContain('DRIVE')
    expect(wrapper.get('[data-testid="drive-launchpad-readiness-e2ee"]').attributes('data-state')).toBe('ready')
    expect(wrapper.get('[data-testid="drive-launchpad-readiness-pass"]').text()).toContain('drive.launcher.openPass')

    await wrapper.get('[data-testid="drive-launchpad-readiness-pass"] button').trigger('click')

    expect(wrapper.emitted('openPass')).toHaveLength(1)
  })

  it('keeps E2EE readiness explicit when the profile is not ready', async () => {
    const { default: DriveCollaborationLaunchpad } = await import('~/components/drive/DriveCollaborationLaunchpad.vue')
    const wrapper = mount(DriveCollaborationLaunchpad, {
      props: {
        items: [],
        handoffItems: [],
        loading: false,
        creatingDoc: false,
        creatingSheet: false,
        ownerE2eeReady: null,
        ownerE2eeLoading: false,
        ownerE2eeError: 'E2EE check failed'
      },
      global: {
        stubs: {
          ElButton: {
            emits: ['click'],
            template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
          },
          ElSkeleton: {
            template: '<div data-testid="el-skeleton-stub"></div>'
          },
          ElEmpty: {
            props: ['description'],
            template: '<div>{{ description }}</div>'
          }
        }
      }
    })

    expect(wrapper.get('[data-testid="drive-launchpad-readiness-e2ee"]').attributes('data-state')).toBe('error')
    expect(wrapper.get('[data-testid="drive-launchpad-readiness-e2ee"]').text()).toContain('E2EE check failed')
  })
})

function buildContextItems(): SuiteCollaborationEvent[] {
  return [
    {
      eventId: 21,
      productCode: 'DRIVE',
      eventType: 'DRIVE_SHARE_CREATE',
      title: 'Drive share created',
      summary: 'Encrypted file shared',
      routePath: '/drive',
      actorEmail: 'drive@mmmail.local',
      sessionId: 'drive-1',
      createdAt: '2026-04-09T11:00:00'
    }
  ]
}

function buildHandoffItems(): MainlineCollaborationEvent[] {
  return [
    {
      eventId: 1,
      productCode: 'MAIL',
      eventType: 'MAIL_SENT',
      title: 'Mail sent',
      summary: 'Kickoff',
      routePath: '/compose',
      actorEmail: 'mail@mmmail.local',
      sessionId: 'suite-1',
      createdAt: '2026-04-09T10:00:00'
    },
    {
      eventId: 2,
      productCode: 'CALENDAR',
      eventType: 'CAL_SHARE_CREATE',
      title: 'Calendar shared',
      summary: 'Checkpoint',
      routePath: '/calendar',
      actorEmail: 'calendar@mmmail.local',
      sessionId: 'suite-1',
      createdAt: '2026-04-09T10:03:00'
    },
    {
      eventId: 3,
      productCode: 'DRIVE',
      eventType: 'DRIVE_SHARE_CREATE',
      title: 'Drive share created',
      summary: 'Package',
      routePath: '/drive',
      actorEmail: 'drive@mmmail.local',
      sessionId: 'suite-1',
      createdAt: '2026-04-09T10:06:00'
    }
  ]
}
