import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import {
  applyDocsBeforeUnloadGuard,
  createDocsRouteLeaveGuard,
  registerDocsBeforeUnloadGuard,
  shouldBlockDocsBeforeUnload
} from '../utils/docs-leave-guard'

function createBeforeUnloadEvent(): BeforeUnloadEvent & { returnValue: string } {
  const event = new Event('beforeunload', { cancelable: true }) as BeforeUnloadEvent & {
    returnValue: string
  }
  Object.defineProperty(event, 'returnValue', {
    configurable: true,
    writable: true,
    value: ''
  })
  return event
}

describe('docs leave guard utils', () => {
  it('only blocks browser unload when unsaved changes exist', () => {
    expect(shouldBlockDocsBeforeUnload(false)).toBe(false)
    expect(shouldBlockDocsBeforeUnload(true)).toBe(true)

    const cleanEvent = createBeforeUnloadEvent()
    applyDocsBeforeUnloadGuard(cleanEvent, false)
    expect(cleanEvent.defaultPrevented).toBe(false)

    const dirtyEvent = createBeforeUnloadEvent()
    applyDocsBeforeUnloadGuard(dirtyEvent, true)
    expect(dirtyEvent.defaultPrevented).toBe(true)
    expect(dirtyEvent.returnValue).toBe('')
  })

  it('registers beforeunload listener and removes it on cleanup', () => {
    const hasUnsavedChanges = ref(false)
    const removeGuard = registerDocsBeforeUnloadGuard({ hasUnsavedChanges })

    const initialEvent = createBeforeUnloadEvent()
    window.dispatchEvent(initialEvent)
    expect(initialEvent.defaultPrevented).toBe(false)

    hasUnsavedChanges.value = true
    const dirtyEvent = createBeforeUnloadEvent()
    window.dispatchEvent(dirtyEvent)
    expect(dirtyEvent.defaultPrevented).toBe(true)

    removeGuard()

    const removedEvent = createBeforeUnloadEvent()
    window.dispatchEvent(removedEvent)
    expect(removedEvent.defaultPrevented).toBe(false)
  })

  it('returns the discard confirmation result for route leave guards', async () => {
    const confirmDiscardUnsavedChanges = vi.fn(async () => false)
    const leaveGuard = createDocsRouteLeaveGuard(confirmDiscardUnsavedChanges)

    expect(await leaveGuard()).toBe(false)
    expect(confirmDiscardUnsavedChanges).toHaveBeenCalledTimes(1)

    confirmDiscardUnsavedChanges.mockResolvedValueOnce(true)
    expect(await leaveGuard()).toBe(true)
    expect(confirmDiscardUnsavedChanges).toHaveBeenCalledTimes(2)
  })
})
