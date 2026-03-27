import type {
  ApiResponse,
  CalendarAgendaItem,
  CalendarEvent,
  CalendarEventDetail,
  CalendarAvailability,
  CalendarEventShare,
  CalendarImportResult,
  CalendarIncomingShare,
  CreateCalendarEventRequest,
  CreateCalendarShareRequest,
  ImportCalendarIcsRequest,
  QueryCalendarAvailabilityRequest,
  RespondCalendarShareRequest,
  UpdateCalendarEventRequest,
  UpdateCalendarShareRequest
} from '~/types/api'

export function useCalendarApi() {
  const { $apiClient } = useNuxtApp()

  async function listEvents(from?: string, to?: string): Promise<CalendarEvent[]> {
    const response = await $apiClient.get<ApiResponse<CalendarEvent[]>>('/api/v1/calendar/events', {
      params: { from: from || undefined, to: to || undefined }
    })
    return response.data.data
  }

  async function getEvent(eventId: string): Promise<CalendarEventDetail> {
    const response = await $apiClient.get<ApiResponse<CalendarEventDetail>>(`/api/v1/calendar/events/${eventId}`)
    return response.data.data
  }

  async function createEvent(payload: CreateCalendarEventRequest): Promise<CalendarEventDetail> {
    const response = await $apiClient.post<ApiResponse<CalendarEventDetail>>('/api/v1/calendar/events', payload)
    return response.data.data
  }

  async function updateEvent(eventId: string, payload: UpdateCalendarEventRequest): Promise<CalendarEventDetail> {
    const response = await $apiClient.put<ApiResponse<CalendarEventDetail>>(`/api/v1/calendar/events/${eventId}`, payload)
    return response.data.data
  }

  async function deleteEvent(eventId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/calendar/events/${eventId}`)
  }

  async function listAgenda(days = 7): Promise<CalendarAgendaItem[]> {
    const response = await $apiClient.get<ApiResponse<CalendarAgendaItem[]>>('/api/v1/calendar/agenda', {
      params: { days }
    })
    return response.data.data
  }

  async function exportCalendar(format: 'ics' = 'ics'): Promise<string> {
    const response = await $apiClient.get<ApiResponse<string>>('/api/v1/calendar/export', {
      params: { format }
    })
    return response.data.data
  }

  async function importCalendarIcs(payload: ImportCalendarIcsRequest): Promise<CalendarImportResult> {
    const response = await $apiClient.post<ApiResponse<CalendarImportResult>>('/api/v1/calendar/import/ics', payload)
    return response.data.data
  }

  async function shareEvent(eventId: string, payload: CreateCalendarShareRequest): Promise<CalendarEventShare> {
    const response = await $apiClient.post<ApiResponse<CalendarEventShare>>(`/api/v1/calendar/events/${eventId}/shares`, payload)
    return response.data.data
  }

  async function listShares(eventId: string): Promise<CalendarEventShare[]> {
    const response = await $apiClient.get<ApiResponse<CalendarEventShare[]>>(`/api/v1/calendar/events/${eventId}/shares`)
    return response.data.data
  }

  async function updateSharePermission(
    eventId: string,
    shareId: string,
    payload: UpdateCalendarShareRequest
  ): Promise<CalendarEventShare> {
    const response = await $apiClient.put<ApiResponse<CalendarEventShare>>(`/api/v1/calendar/events/${eventId}/shares/${shareId}`, payload)
    return response.data.data
  }

  async function removeShare(eventId: string, shareId: string): Promise<void> {
    await $apiClient.delete(`/api/v1/calendar/events/${eventId}/shares/${shareId}`)
  }

  async function listIncomingShares(): Promise<CalendarIncomingShare[]> {
    const response = await $apiClient.get<ApiResponse<CalendarIncomingShare[]>>('/api/v1/calendar/shares/incoming')
    return response.data.data
  }

  async function respondShare(shareId: string, payload: RespondCalendarShareRequest): Promise<CalendarIncomingShare> {
    const response = await $apiClient.post<ApiResponse<CalendarIncomingShare>>(`/api/v1/calendar/shares/${shareId}/response`, payload)
    return response.data.data
  }

  async function queryAvailability(payload: QueryCalendarAvailabilityRequest): Promise<CalendarAvailability> {
    const response = await $apiClient.post<ApiResponse<CalendarAvailability>>('/api/v1/calendar/availability/query', payload)
    return response.data.data
  }

  return {
    listEvents,
    getEvent,
    createEvent,
    updateEvent,
    deleteEvent,
    listAgenda,
    exportCalendar,
    importCalendarIcs,
    shareEvent,
    listShares,
    updateSharePermission,
    removeShare,
    listIncomingShares,
    respondShare,
    queryAvailability
  }
}
