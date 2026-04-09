import { describe, expect, it } from 'vitest'
import {
  COLLABORATION_PRODUCT_LABELS,
  buildCollaborationCounts,
  filterCollaborationEvents,
  filterMainlineCollaborationItems,
  formatCollaborationProductList,
  isExternalCollaborationEvent,
  listVisibleCollaborationProductCodes,
  type MainlineCollaborationEvent
} from '../utils/collaboration'

const items: MainlineCollaborationEvent[] = [
  {
    eventId: 1,
    productCode: 'MAIL',
    eventType: 'MAIL_SENT',
    title: 'Mail sent',
    summary: 'Sent secure message',
    routePath: '/sent',
    actorEmail: 'mail@mmmail.local',
    sessionId: '100',
    createdAt: '2026-03-06T10:00:00'
  },
  {
    eventId: 2,
    productCode: 'CALENDAR',
    eventType: 'CAL_EVENT_CREATE',
    title: 'Calendar event created',
    summary: 'Created review checkpoint',
    routePath: '/calendar?eventId=22',
    actorEmail: 'calendar@mmmail.local',
    sessionId: '100',
    createdAt: '2026-03-06T10:03:00'
  },
  {
    eventId: 3,
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
    eventId: 4,
    productCode: 'PASS',
    eventType: 'PASS_SECURE_LINK_CREATE',
    title: 'Secure link created',
    summary: 'Shared final credential',
    routePath: '/pass?tab=secure-links',
    actorEmail: 'pass@mmmail.local',
    sessionId: '200',
    createdAt: '2026-03-06T10:10:00'
  }
]

describe('collaboration utils', () => {
  it('filters collaboration events by product', () => {
    expect(filterCollaborationEvents(items, 'ALL')).toHaveLength(4)
    expect(filterCollaborationEvents(items, 'MAIL')).toEqual([items[0]])
    expect(filterCollaborationEvents(items, 'DRIVE')).toEqual([items[2]])
  })

  it('builds product counts and detects external session', () => {
    expect(buildCollaborationCounts(items)).toEqual({
      ALL: 4,
      MAIL: 1,
      CALENDAR: 1,
      DRIVE: 1,
      PASS: 1
    })
    expect(isExternalCollaborationEvent(items[0], '100')).toBe(false)
    expect(isExternalCollaborationEvent(items[3], '100')).toBe(true)
  })

  it('lists visible collaboration products and formats labels', () => {
    const visible = listVisibleCollaborationProductCodes((productKey) => productKey !== 'MAIL')
    expect(visible).toEqual(['CALENDAR', 'DRIVE', 'PASS'])
    expect(COLLABORATION_PRODUCT_LABELS.MAIL).toBe('Mail')
    expect(formatCollaborationProductList(visible)).toBe('Calendar, Drive, and Pass')
    expect(formatCollaborationProductList(visible, 'slash')).toBe('Calendar / Drive / Pass')
  })

  it('drops non-mainline events before rendering the stream', () => {
    const mixedItems = [
      ...items,
      {
        eventId: 5,
        productCode: 'DOCS',
        eventType: 'DOCS_NOTE_UPDATE',
        title: 'Docs updated',
        summary: 'Docs',
        routePath: '/docs',
        actorEmail: 'docs@mmmail.local',
        sessionId: '300',
        createdAt: '2026-03-06T10:12:00'
      }
    ]
    expect(filterMainlineCollaborationItems(mixedItems).map((item) => item.productCode)).toEqual([
      'MAIL',
      'CALENDAR',
      'DRIVE',
      'PASS'
    ])
  })
})
