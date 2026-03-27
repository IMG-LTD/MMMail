import { describe, expect, it } from 'vitest'
import {
  buildSearchFolderOptions,
  buildSearchReadStateOptions,
  buildSearchStarStateOptions,
  buildSearchUsageText,
  formatSearchDateTime
} from '../utils/search-workspace'

describe('search workspace utils', () => {
  it('builds translated folder and filter options', () => {
    const tMock = (key: string) => `t:${key}`

    expect(buildSearchFolderOptions(tMock)[0]).toEqual({ value: 'INBOX', label: 't:nav.inbox' })
    expect(buildSearchReadStateOptions(tMock)).toEqual([
      { value: 'ALL', label: 't:search.readState.ALL' },
      { value: 'UNREAD', label: 't:search.readState.UNREAD' },
      { value: 'READ', label: 't:search.readState.READ' }
    ])
    expect(buildSearchStarStateOptions(tMock)).toEqual([
      { value: 'ALL', label: 't:search.starState.ALL' },
      { value: 'STARRED', label: 't:search.starState.STARRED' },
      { value: 'UNSTARRED', label: 't:search.starState.UNSTARRED' }
    ])
  })

  it('formats usage text with and without last used timestamp', () => {
    const tMock = (key: string, params?: Record<string, string | number>) => {
      if (key === 'search.meta.usedOnly') {
        return `used:${params?.count}`
      }
      return `used:${params?.count} last:${params?.time}`
    }

    expect(buildSearchUsageText({ usageCount: 3, lastUsedAt: null }, 'en', tMock)).toBe('used:3')
    expect(buildSearchUsageText({ usageCount: 2, lastUsedAt: 'invalid-date' }, 'en', tMock)).toBe('used:2 last:invalid-date')
  })

  it('keeps invalid dates explicit', () => {
    expect(formatSearchDateTime('invalid-date')).toBe('invalid-date')
    expect(formatSearchDateTime(null)).toBe('')
  })
})
