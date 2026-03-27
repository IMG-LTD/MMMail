import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useOrganizationApi } from '../composables/useOrganizationApi'

const mockPost = vi.fn()

describe('organization api', () => {
  beforeEach(() => {
    mockPost.mockReset()
    vi.stubGlobal('useNuxtApp', () => ({
      $apiClient: {
        post: mockPost
      }
    }))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('preserves 64-bit member ids when sending authentication reminders', async () => {
    mockPost.mockResolvedValueOnce({
      data: {
        data: {
          requestedCount: 1,
          deliveredCount: 1,
          skippedProtectedCount: 0,
          skippedMissingCount: 0,
          deliveredMemberIds: ['2031371532463775745']
        }
      }
    })

    const api = useOrganizationApi()

    await api.sendOrgAuthenticationSecurityReminders('org-1', {
      memberIds: ['2031371532463775745']
    })

    expect(mockPost).toHaveBeenCalledWith(
      '/api/v1/orgs/org-1/admin-console/authentication-security/reminders',
      {
        memberIds: ['2031371532463775745']
      }
    )
  })
})
