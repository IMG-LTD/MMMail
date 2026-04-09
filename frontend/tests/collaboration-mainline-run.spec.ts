import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, ref } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const getCollaborationCenter = vi.fn(async () => ({
  generatedAt: '2026-04-09T11:00:00',
  limit: 60,
  total: 5,
  productCounts: {
    ALL: 5,
    MAIL: 1,
    CALENDAR: 1,
    DRIVE: 1,
    PASS: 1
  },
  syncCursor: 40,
  syncVersion: 'COLLAB-40',
  items: [
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
      createdAt: '2026-04-09T10:02:00'
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
      createdAt: '2026-04-09T10:05:00'
    },
    {
      eventId: 4,
      productCode: 'PASS',
      eventType: 'PASS_ITEM_CREATE',
      title: 'Pass item created',
      summary: 'Old secret',
      routePath: '/pass',
      actorEmail: 'pass@mmmail.local',
      sessionId: 'suite-0',
      createdAt: '2026-04-09T09:50:00'
    },
    {
      eventId: 5,
      productCode: 'DOCS',
      eventType: 'DOCS_NOTE_UPDATE',
      title: 'Docs updated',
      summary: 'Ignored',
      routePath: '/docs',
      actorEmail: 'docs@mmmail.local',
      sessionId: 'suite-1',
      createdAt: '2026-04-09T10:06:00'
    }
  ]
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, string | number>) => {
      if (!params) {
        return key
      }
      return Object.entries(params).reduce((message, [paramKey, value]) => {
        return message.replaceAll(`{${paramKey}}`, String(value))
      }, key)
    }
  })
}))

vi.mock('~/composables/useSuiteApi', () => ({
  useSuiteApi: () => ({
    getCollaborationCenter
  })
}))

vi.mock('~/composables/useCollaborationSyncStream', () => ({
  useCollaborationSyncStream: () => ({
    status: ref('IDLE'),
    errorMessage: ref(''),
    connect: vi.fn(async () => undefined),
    reconnect: vi.fn(),
    lastCursor: ref(0)
  })
}))

vi.mock('~/stores/auth', () => ({
  useAuthStore: () => ({
    accessToken: 'access-token'
  })
}))

vi.mock('~/stores/org-access', () => ({
  useOrgAccessStore: () => ({
    activeOrgId: 'org-1',
    isProductEnabled: (productCode: string) => productCode !== 'DOCS'
  })
}))

vi.mock('~/utils/auth-session', () => ({
  resolveSessionIdFromAccessToken: () => 'suite-1'
}))

vi.mock('~/components/collaboration/CollaborationSignalRail.vue', () => ({
  default: defineComponent({
    name: 'CollaborationSignalRail',
    template: '<div data-testid="collaboration-signal-rail-stub"></div>'
  })
}))

vi.mock('~/components/collaboration/CollaborationStreamPanel.vue', () => ({
  default: defineComponent({
    name: 'CollaborationStreamPanel',
    template: '<div data-testid="collaboration-stream-panel-stub"></div>'
  })
}))

vi.mock('~/components/collaboration/CollaborationRealtimePanel.vue', () => ({
  default: defineComponent({
    name: 'CollaborationRealtimePanel',
    template: '<div data-testid="collaboration-realtime-panel-stub"></div>'
  })
}))

vi.mock('~/components/suite/SuiteMainlineHandoffPanel.vue', () => ({
  default: defineComponent({
    name: 'SuiteMainlineHandoffPanel',
    props: {
      run: { type: Object, required: true },
      loading: { type: Boolean, required: true }
    },
    template: '<div data-testid="collaboration-mainline-run">{{ run.currentStage }}|{{ run.nextStage }}|{{ loading }}</div>'
  })
}))

describe('Collaboration page mainline run', () => {
  beforeEach(() => {
    getCollaborationCenter.mockClear()
    ;(globalThis as typeof globalThis & { useHead?: (value: unknown) => void }).useHead = vi.fn()
  })

  afterEach(() => {
    delete (globalThis as typeof globalThis & { useHead?: unknown }).useHead
  })

  it('derives the current run from mainline collaboration events only', async () => {
    const { default: CollaborationPage } = await import('~/pages/collaboration.vue')
    const wrapper = mount(CollaborationPage, {
      global: {
        stubs: {
          ElTag: {
            template: '<span><slot /></span>'
          },
          ElButton: {
            emits: ['click'],
            template: '<button type="button" @click="$emit(\'click\')"><slot /></button>'
          },
          ElAlert: {
            template: '<div><slot /></div>'
          }
        }
      }
    })

    await flushPromises()

    expect(getCollaborationCenter).toHaveBeenCalledWith(60)
    expect(wrapper.get('[data-testid="collaboration-mainline-run"]').text()).toBe('DRIVE|PASS|false')
  })
})
