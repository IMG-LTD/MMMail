import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import type { SystemHealthOverview } from '~/types/system'

const fetchSystemHealthMock = vi.fn()

const authState = {
  user: {
    role: 'ADMIN',
  },
}

const overview: SystemHealthOverview = {
  status: 'DEGRADED',
  applicationName: 'mmmail-server',
  applicationVersion: '0.1.0-SNAPSHOT',
  activeProfiles: ['test'],
  uptimeSeconds: 88,
  generatedAt: '2026-03-13T20:00:00',
  components: [
    { name: 'db', status: 'UP', details: 'database=H2' },
    { name: 'redis', status: 'DOWN', details: 'connection refused' },
  ],
  metrics: {
    totalRequests: 42,
    failedRequests: 4,
    processCpuUsage: 8,
    systemCpuUsage: 14,
    usedMemoryMb: 256,
    maxMemoryMb: 1024,
    liveThreads: 21,
    activeDbConnections: 3,
    maxDbConnections: 10,
    modules: [
      { module: 'mail', totalRequests: 20, failedRequests: 1 },
      { module: 'system', totalRequests: 5, failedRequests: 2 },
    ],
  },
  errorTracking: {
    totalEvents: 3,
    serverEvents: 2,
    clientEvents: 1,
    lastOccurredAt: '2026-03-13T19:59:00',
  },
  recentErrors: [
    {
      eventId: 'error-1',
      source: 'CLIENT',
      category: 'WINDOW_ERROR',
      severity: 'ERROR',
      message: 'ReferenceError: boom',
      detail: 'stack',
      path: '/drive',
      method: 'GET',
      status: null,
      errorCode: null,
      requestId: 'req-1',
      userId: '1',
      sessionId: 's-1',
      orgId: 'org-1',
      occurredAt: '2026-03-13T19:58:00',
    },
  ],
  jobs: {
    activeRuns: 1,
    totalRuns: 7,
    failedRuns: 1,
    lastCompletedAt: '2026-03-13T19:57:00',
  },
  recentJobs: [
    {
      runId: 'job-1',
      jobName: 'MAIL_EASY_SWITCH_IMPORT',
      trigger: 'USER_ACTION',
      status: 'SUCCESS',
      detail: 'session=123',
      actorId: '1',
      orgId: 'org-1',
      durationMs: 120,
      startedAt: '2026-03-13T19:56:00',
      completedAt: '2026-03-13T19:57:00',
    },
  ],
  prometheusPath: '/actuator/prometheus',
}

vi.mock('~/composables/useSystemApi', () => ({
  useSystemApi: () => ({
    fetchSystemHealth: fetchSystemHealthMock,
    reportClientError: vi.fn(),
  }),
}))

vi.mock('~/stores/auth', () => ({
  useAuthStore: () => authState,
}))

vi.mock('~/composables/useI18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}))

describe('system health page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    authState.user.role = 'ADMIN'
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
    vi.restoreAllMocks()
    delete (globalThis as typeof globalThis & { definePageMeta?: unknown }).definePageMeta
    delete (globalThis as typeof globalThis & { useHead?: unknown }).useHead
  })

  it('renders overview for admin sessions', async () => {
    fetchSystemHealthMock.mockResolvedValue(overview)
    const page = await mountPage()
    await flushPromises()

    expect(fetchSystemHealthMock).toHaveBeenCalledTimes(1)
    expect(page.get('[data-testid="system-health-application"]').text()).toContain('mmmail-server')
    expect(page.get('[data-testid="system-health-total-requests"]').text()).toContain('42')
    expect(page.get('[data-testid="system-health-components-table"]').text()).toContain('redis')
    expect(page.get('[data-testid="system-health-errors-list"]').text()).toContain('ReferenceError: boom')
    expect(page.get('[data-testid="system-health-jobs-list"]').text()).toContain('MAIL_EASY_SWITCH_IMPORT')
  })

  it('shows explicit load failure and supports retry', async () => {
    fetchSystemHealthMock
      .mockRejectedValueOnce(new Error('Health API unavailable'))
      .mockResolvedValueOnce(overview)
    const page = await mountPage()
    await flushPromises()

    expect(page.get('[data-testid="system-health-error"]').text()).toContain('Health API unavailable')
    await page.get('[data-testid="system-health-refresh"]').trigger('click')
    await flushPromises()

    expect(fetchSystemHealthMock).toHaveBeenCalledTimes(2)
    expect(page.get('[data-testid="system-health-application"]').text()).toContain('mmmail-server')
  })

  it('blocks non-admin sessions explicitly without calling the API', async () => {
    authState.user.role = 'USER'
    const page = await mountPage()
    await flushPromises()

    expect(fetchSystemHealthMock).not.toHaveBeenCalled()
    expect(page.get('[data-testid="system-health-error"]').text()).toContain('systemHealth.accessDenied')
  })
})

async function mountPage() {
  const { default: SystemHealthPage } = await import('~/pages/settings/system-health.vue')
  return mount(SystemHealthPage, {
    global: {
      stubs: {
        NuxtLink: {
          template: '<a><slot /></a>',
        },
      },
    },
  })
}
