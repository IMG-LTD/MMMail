import { describe, expect, it } from 'vitest'
import type { CalendarEvent } from '~/types/api'
import {
  buildCalendarEventGroups,
  buildCalendarRangeWindow,
  createCalendarAnchor,
  normalizeAllDayRange,
  shiftCalendarAnchor,
} from '../utils/calendar-workspace'

describe('calendar workspace utils', () => {
  it('builds deterministic day week and month windows', () => {
    expect(createCalendarAnchor(new Date(2026, 2, 13))).toBe('2026-03-13')

    expect(buildCalendarRangeWindow('2026-03-13', 'day')).toEqual({
      from: '2026-03-13T00:00:00',
      to: '2026-03-14T00:00:00',
      label: '2026-03-13',
    })

    expect(buildCalendarRangeWindow('2026-03-13', 'week')).toEqual({
      from: '2026-03-08T00:00:00',
      to: '2026-03-15T00:00:00',
      label: '2026-03-08 → 2026-03-14',
    })

    expect(buildCalendarRangeWindow('2026-03-13', 'month')).toEqual({
      from: '2026-03-01T00:00:00',
      to: '2026-04-01T00:00:00',
      label: '2026-03-01 → 2026-03-31',
    })
  })

  it('shifts anchors and normalizes all-day ranges', () => {
    expect(shiftCalendarAnchor('2026-03-13', 'day', 1)).toBe('2026-03-14')
    expect(shiftCalendarAnchor('2026-03-13', 'week', -1)).toBe('2026-03-06')
    expect(shiftCalendarAnchor('2026-03-13', 'month', 1)).toBe('2026-04-01')

    expect(normalizeAllDayRange('2026-03-13T09:30:00', '2026-03-13T10:30:00')).toEqual({
      startAt: '2026-03-13T00:00:00',
      endAt: '2026-03-14T00:00:00',
    })
  })

  it('groups events by start date for release-blocking views', () => {
    const events: CalendarEvent[] = [
      {
        id: '2',
        title: 'Night deploy',
        location: 'Ops',
        startAt: '2026-03-13T23:00:00',
        endAt: '2026-03-14T01:00:00',
        allDay: false,
        timezone: 'UTC',
        reminderMinutes: 30,
        attendeeCount: 1,
        updatedAt: '2026-03-13T10:00:00',
        shared: false,
        ownerEmail: null,
        sharePermission: 'OWNER',
        canEdit: true,
        canDelete: true,
      },
      {
        id: '1',
        title: 'Standup',
        location: null,
        startAt: '2026-03-13T09:00:00',
        endAt: '2026-03-13T09:30:00',
        allDay: false,
        timezone: 'UTC',
        reminderMinutes: 15,
        attendeeCount: 3,
        updatedAt: '2026-03-13T08:00:00',
        shared: true,
        ownerEmail: 'owner@example.com',
        sharePermission: 'EDIT',
        canEdit: true,
        canDelete: false,
      },
    ]

    expect(buildCalendarEventGroups(events, 'day')).toEqual([
      {
        key: '2026-03-13',
        label: '2026-03-13 · Fri',
        items: [events[1], events[0]],
      },
    ])
  })
})
