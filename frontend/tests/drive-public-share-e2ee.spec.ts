import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'

const navigateToMock = vi.fn(async () => undefined)

async function mountPublicSharePage() {
  const { default: PublicSharePage } = await import('~/pages/public/drive/shares/[token].vue')
  return mount(PublicSharePage)
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.stubGlobal('useRoute', () => ({
    params: { token: 'share-token' },
    query: {},
  }))
  vi.stubGlobal('navigateTo', navigateToMock)
  vi.stubGlobal('definePageMeta', vi.fn())
})

describe('drive public share e2ee', () => {
  it('redirects legacy public share routes to the current drive share route', async () => {
    await mountPublicSharePage()
    await flushPromises()

    expect(navigateToMock).toHaveBeenCalledWith('/share/drive/share-token')
  })
})
