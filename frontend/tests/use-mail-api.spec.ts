import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const mockGet = vi.fn()

describe('useMailApi.fetchFolder', () => {
  beforeEach(() => {
    mockGet.mockReset()
    mockGet.mockResolvedValue({
      data: {
        data: {
          items: [],
          total: 0,
          page: 1,
          size: 20,
          unread: 0
        }
      }
    })
    vi.stubGlobal('useNuxtApp', () => ({
      $apiClient: {
        get: mockGet
      }
    }))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('serializes only active triage filters for inbox requests', async () => {
    const { useMailApi } = await import('~/composables/useMailApi')

    await useMailApi().fetchFolder('inbox', 3, 50, 'vip', {
      unread: false,
      needsReply: true,
      starred: false,
      hasAttachments: true,
      importantContact: false
    })

    expect(mockGet).toHaveBeenCalledWith('/api/v1/mails/inbox', {
      params: {
        page: 3,
        size: 50,
        keyword: 'vip',
        unread: undefined,
        needsReply: true,
        starred: undefined,
        hasAttachments: true,
        importantContact: undefined
      }
    })
  })

  it('omits inbox triage params for non-inbox requests', async () => {
    const { useMailApi } = await import('~/composables/useMailApi')

    await useMailApi().fetchFolder('sent', 2, 20, 'release', {
      unread: true,
      needsReply: true,
      starred: true,
      hasAttachments: true,
      importantContact: true
    })

    expect(mockGet).toHaveBeenCalledWith('/api/v1/mails/sent', {
      params: {
        page: 2,
        size: 20,
        keyword: 'release'
      }
    })
  })
})
