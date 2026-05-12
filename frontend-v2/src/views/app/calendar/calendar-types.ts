import type { CalendarAgendaItem, CalendarAvailability, CalendarEvent } from '@/service/api/calendar'

export type CalendarViewMode = 'month' | 'week' | 'day' | 'agenda'

export interface CalendarSurfaceItem {
  allDay: boolean
  attendeeCount: number
  endAt: string
  id: string
  location: string | null
  ownerEmail: string | null
  shared: boolean
  startAt: string
  title: string
}

export interface CalendarDayCell {
  active: boolean
  date: Date
  dayNumber: string
  hasEvents: boolean
  key: string
  label: string
}

export interface CalendarTimeSlot {
  hour: number
  key: string
  label: string
}

export interface PositionedCalendarEvent {
  id: string
  meta: string
  selected: boolean
  shared: boolean
  style: {
    gridColumn: string
    gridRow: string
  }
  title: string
}

export type { CalendarAgendaItem, CalendarAvailability, CalendarEvent }
