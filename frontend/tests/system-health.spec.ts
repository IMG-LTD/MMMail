import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const navigateToMock = vi.fn(async () => undefined)

describe('system health page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    ;(globalThis as typeof globalThis & {
      definePageMeta?: (value: unknown) => void
      useHead?: (value: unknown) => void
      navigateTo?: typeof navigateToMock
    }).definePageMeta = vi.fn()
    ;(globalThis as typeof globalThis & {
      definePageMeta?: (value: unknown) => void
      useHead?: (value: unknown) => void
      navigateTo?: typeof navigateToMock
    }).useHead = vi.fn()
    ;(globalThis as typeof globalThis & {
      definePageMeta?: (value: unknown) => void
      useHead?: (value: unknown) => void
      navigateTo?: typeof navigateToMock
    }).navigateTo = navigateToMock
  })

  afterEach(() => {
    vi.restoreAllMocks()
    delete (globalThis as typeof globalThis & { definePageMeta?: unknown }).definePageMeta
    delete (globalThis as typeof globalThis & { useHead?: unknown }).useHead
    delete (globalThis as typeof globalThis & { navigateTo?: unknown }).navigateTo
  })

  it('redirects legacy system health routes to the settings panel baseline', async () => {
    await mountPage()
    await flushPromises()

    expect(navigateToMock).toHaveBeenCalledWith({
      path: '/settings',
      query: { panel: 'system-health' }
    })
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
