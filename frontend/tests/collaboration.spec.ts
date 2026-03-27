import { describe, expect, it } from 'vitest'
import type { SuiteCollaborationEvent } from '~/types/api'
import {
  COLLABORATION_PRODUCT_LABELS,
  buildCollaborationCounts,
  filterCollaborationEvents,
  formatCollaborationProductList,
  isExternalCollaborationEvent,
  listVisibleCollaborationProductCodes
} from '../utils/collaboration'

const items: SuiteCollaborationEvent[] = [
  {
    eventId: 1,
    productCode: 'DOCS',
    eventType: 'DOCS_NOTE_UPDATE',
    title: 'Document updated',
    summary: 'Updated document',
    routePath: '/docs',
    actorEmail: 'docs@mmmail.local',
    sessionId: '100',
    createdAt: '2026-03-06T10:00:00'
  },
  {
    eventId: 2,
    productCode: 'DRIVE',
    eventType: 'DRIVE_SHARE_CREATE',
    title: 'Drive share created',
    summary: 'Created drive share',
    routePath: '/drive',
    actorEmail: 'drive@mmmail.local',
    sessionId: null,
    createdAt: '2026-03-06T10:05:00'
  },
  {
    eventId: 3,
    productCode: 'MEET',
    eventType: 'MEET_ROOM_END',
    title: 'Meeting ended',
    summary: 'Ended room',
    routePath: '/meet',
    actorEmail: 'meet@mmmail.local',
    sessionId: '200',
    createdAt: '2026-03-06T10:10:00'
  }
]

describe('collaboration utils', () => {
  it('filters collaboration events by product', () => {
    expect(filterCollaborationEvents(items, 'ALL')).toHaveLength(3)
    expect(filterCollaborationEvents(items, 'DOCS')).toEqual([items[0]])
    expect(filterCollaborationEvents(items, 'DRIVE')).toEqual([items[1]])
  })

  it('builds product counts and detects external session', () => {
    expect(buildCollaborationCounts(items)).toEqual({
      ALL: 3,
      DOCS: 1,
      DRIVE: 1,
      SHEETS: 0,
      MEET: 1
    })
    expect(isExternalCollaborationEvent(items[0], '100')).toBe(false)
    expect(isExternalCollaborationEvent(items[2], '100')).toBe(true)
  })

  it('lists visible collaboration products and formats labels', () => {
    const visible = listVisibleCollaborationProductCodes((productKey) => productKey !== 'DOCS')
    expect(visible).toEqual(['DRIVE', 'SHEETS', 'MEET'])
    expect(COLLABORATION_PRODUCT_LABELS.DOCS).toBe('Docs')
    expect(formatCollaborationProductList(visible)).toBe('Drive, Sheets, and Meet')
    expect(formatCollaborationProductList(visible, 'slash')).toBe('Drive / Sheets / Meet')
  })
})
