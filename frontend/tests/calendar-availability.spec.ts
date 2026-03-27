import { describe, expect, it } from 'vitest'
import type { CalendarAvailability, CalendarIncomingShare } from '~/types/api'
import {
  buildCalendarAvailabilitySummary,
  buildCalendarIncomingShareCounts,
  filterCalendarIncomingShares,
  hasCalendarAvailabilityQuery,
  parseCalendarAttendeesInput
} from '../utils/calendar-availability'

describe('calendar availability utils', () => {
  it('parses attendee input and de-duplicates by email', () => {
    expect(parseCalendarAttendeesInput('Alice <alice@example.com>\nalice@example.com\ninvalid')).toEqual([
      {
        email: 'alice@example.com',
        displayName: 'Alice'
      }
    ])
  })

  it('filters incoming shares and builds counts', () => {
    const shares: CalendarIncomingShare[] = [
      {
        shareId: '1',
        eventId: 'e1',
        eventTitle: 'Review',
        ownerEmail: 'owner@example.com',
        permission: 'VIEW',
        responseStatus: 'NEEDS_ACTION',
        updatedAt: '2026-03-09T10:00:00'
      },
      {
        shareId: '2',
        eventId: 'e2',
        eventTitle: 'Standup',
        ownerEmail: 'owner@example.com',
        permission: 'EDIT',
        responseStatus: 'ACCEPTED',
        updatedAt: '2026-03-09T11:00:00'
      }
    ]

    expect(filterCalendarIncomingShares(shares, 'ALL')).toHaveLength(2)
    expect(filterCalendarIncomingShares(shares, 'NEEDS_ACTION')).toEqual([shares[0]])
    expect(buildCalendarIncomingShareCounts(shares)).toEqual({
      ALL: 2,
      NEEDS_ACTION: 1,
      ACCEPTED: 1,
      DECLINED: 0
    })
  })

  it('builds availability summary and detects valid query inputs', () => {
    const availability: CalendarAvailability = {
      startAt: '2026-03-10T10:00:00',
      endAt: '2026-03-10T11:00:00',
      summary: {
        attendeeCount: 2,
        busyCount: 1,
        freeCount: 1,
        unknownCount: 0,
        hasConflicts: true
      },
      attendees: []
    }

    expect(hasCalendarAvailabilityQuery('2026-03-10T10:00:00', '2026-03-10T11:00:00', 2)).toBe(true)
    expect(hasCalendarAvailabilityQuery('', '2026-03-10T11:00:00', 2)).toBe(false)
    expect(buildCalendarAvailabilitySummary(availability)).toEqual(availability.summary)
    expect(buildCalendarAvailabilitySummary(null)).toEqual({
      attendeeCount: 0,
      busyCount: 0,
      freeCount: 0,
      unknownCount: 0,
      hasConflicts: false
    })
  })
})
