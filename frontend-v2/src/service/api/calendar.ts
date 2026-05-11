import { httpClient } from '@/service/request/http'
import type { ApiResponse } from '@/shared/types/api'

export interface CalendarEvent {
  id: string
  title: string
  location: string | null
  startAt: string
  endAt: string
  allDay: boolean
  timezone: string
  reminderMinutes: number | null
  attendeeCount: number
  updatedAt: string
  shared: boolean
  ownerEmail: string | null
  sharePermission: string
  canEdit: boolean
  canDelete: boolean
}

export interface CalendarAgendaItem {
  id: string
  title: string
  location: string | null
  startAt: string
  endAt: string
  attendeeCount: number
  shared: boolean
  ownerEmail: string | null
  sharePermission: string
}

export type CalendarAvailabilityStatus = 'BUSY' | 'FREE' | 'UNKNOWN'

export interface CalendarAvailabilitySlot {
  startAt: string
  endAt: string
  allDay: boolean
}

export interface CalendarParticipantAvailability {
  email: string
  availability: CalendarAvailabilityStatus
  overlapCount: number
  busySlots: CalendarAvailabilitySlot[]
}

export interface CalendarAvailabilitySummary {
  attendeeCount: number
  busyCount: number
  freeCount: number
  unknownCount: number
  hasConflicts: boolean
}

export interface CalendarAvailability {
  startAt: string
  endAt: string
  summary: CalendarAvailabilitySummary
  attendees: CalendarParticipantAvailability[]
}

export interface QueryCalendarAvailabilityBody {
  startAt: string
  endAt: string
  attendeeEmails: string[]
  excludeEventId?: string | number | null
}

export interface CalendarResource {
  id: string
  name: string
  capacity: number
  location: string | null
}

export interface CalendarBookingPayload {
  eventId: string
  resourceId: string
  startAt: string
  endAt: string
}

export interface CalendarSettings {
  defaultTimezone: string
  weekStartsOn: string
  workingHours: string[]
}

export function listCalendarEvents(token: string, from?: string, to?: string) {
  return httpClient.get<ApiResponse<CalendarEvent[]>>('/api/v2/calendar/events', {
    token,
    query: { from, to }
  })
}

export function listCalendarAgenda(token: string, days = 7) {
  return httpClient.get<ApiResponse<CalendarAgendaItem[]>>('/api/v2/calendar/events', {
    token,
    query: { days, view: 'agenda' }
  })
}

export function queryCalendarAvailability(token: string, body: QueryCalendarAvailabilityBody) {
  return httpClient.post<ApiResponse<CalendarAvailability>>('/api/v2/calendar/availability', {
    token,
    body
  })
}

export function listCalendarResources(token: string) {
  return httpClient.get<ApiResponse<CalendarResource[]>>('/api/v2/calendar/resources', { token })
}

export function createCalendarBooking(token: string, body: CalendarBookingPayload) {
  return httpClient.post<ApiResponse<CalendarEvent>>('/api/v2/calendar/bookings', { body, token })
}

export function readCalendarSettings(token: string) {
  return httpClient.get<ApiResponse<CalendarSettings>>('/api/v2/calendar/settings', { token })
}

export function updateCalendarSettings(token: string, body: CalendarSettings) {
  return httpClient.put<ApiResponse<CalendarSettings>>('/api/v2/calendar/settings', { body, token })
}
