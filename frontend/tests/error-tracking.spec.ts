import { describe, expect, it, vi } from 'vitest'
import {
  buildUnhandledRejectionPayload,
  buildWindowErrorPayload,
  setupRuntimeErrorTracking,
} from '~/utils/error-tracking'

class MockTarget {
  private listeners = new Map<string, Set<(event: unknown) => void>>()

  addEventListener(type: string, listener: (event: unknown) => void): void {
    const handlers = this.listeners.get(type) ?? new Set()
    handlers.add(listener)
    this.listeners.set(type, handlers)
  }

  removeEventListener(type: string, listener: (event: unknown) => void): void {
    this.listeners.get(type)?.delete(listener)
  }

  dispatch(type: string, event: unknown): void {
    for (const handler of this.listeners.get(type) ?? []) {
      handler(event)
    }
  }
}

describe('error tracking utils', () => {
  it('builds structured payloads for window errors and promise rejections', () => {
    const windowPayload = buildWindowErrorPayload(
      { error: Object.assign(new Error('Window boom'), { requestId: 'req-1' }) },
      '/settings/system-health',
    )
    const rejectionPayload = buildUnhandledRejectionPayload(
      { reason: { message: 'Rejected promise', code: 500 } },
      '/drive',
    )

    expect(windowPayload).toMatchObject({
      category: 'WINDOW_ERROR',
      message: 'Window boom',
      requestId: 'req-1',
      path: '/settings/system-health',
    })
    expect(rejectionPayload).toMatchObject({
      category: 'UNHANDLED_REJECTION',
      message: 'Rejected promise',
      path: '/drive',
    })
  })

  it('registers listeners, reports events, and stops after cleanup', async () => {
    const target = new MockTarget()
    const report = vi.fn(async () => undefined)
    const cleanup = setupRuntimeErrorTracking({
      target,
      enabled: () => true,
      resolvePath: () => '/calendar',
      report,
    })

    target.dispatch('error', { error: new Error('Calendar exploded') })
    target.dispatch('unhandledrejection', { reason: new Error('Async failure') })
    await Promise.resolve()

    expect(report).toHaveBeenCalledTimes(2)
    const firstPayload = report.mock.calls.at(0)?.at(0)
    const secondPayload = report.mock.calls.at(1)?.at(0)

    expect(firstPayload).toMatchObject({
      category: 'WINDOW_ERROR',
      message: 'Calendar exploded',
    })
    expect(secondPayload).toMatchObject({
      category: 'UNHANDLED_REJECTION',
      message: 'Async failure',
    })

    cleanup()
    target.dispatch('error', { error: new Error('Should be ignored') })
    await Promise.resolve()
    expect(report).toHaveBeenCalledTimes(2)
  })
})
