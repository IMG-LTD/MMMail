import { request } from '../request';

export function listCalendarEvents(params: Record<string, string | number | undefined> = {}) {
  return request<Api.Calendar.Event[]>({ url: '/api/v2/calendar/events', params });
}

export function createCalendarEvent(data: Api.Calendar.EventPayload) {
  return request<Api.Calendar.Event>({
    url: '/api/v2/calendar/events',
    method: 'post',
    data
  });
}

export function updateCalendarEvent(
  eventId: string,
  data: Api.Calendar.EventPayload,
  params: { scope?: 'all' | 'this' | 'thisAndFollowing' } = {}
) {
  return request<Api.Calendar.Event>({
    url: `/api/v2/calendar/events/${eventId}`,
    method: 'patch',
    params,
    data
  });
}

export function deleteCalendarEvent(eventId: string) {
  return request<void>({
    url: `/api/v2/calendar/events/${eventId}`,
    method: 'delete'
  });
}

export function queryCalendarAvailability(data: Api.Calendar.AvailabilityPayload) {
  return request<Api.Calendar.Availability>({
    url: '/api/v2/calendar/availability',
    method: 'post',
    data
  });
}

export function readCalendarSettings() {
  return request<Api.Calendar.Settings>({ url: '/api/v2/calendar/settings' });
}

export function listCalendarSubscriptions() {
  return request<Api.Calendar.Subscription[]>({ url: '/api/v2/calendar/subscriptions' });
}

export function createCalendarSubscription(data: Api.Calendar.SubscriptionPayload) {
  return request<Api.Calendar.Subscription>({
    url: '/api/v2/calendar/subscriptions',
    method: 'post',
    data
  });
}

export function syncCalendarSubscription(subscriptionId: string) {
  return request<Api.Calendar.SubscriptionSync>({
    url: `/api/v2/calendar/subscriptions/${subscriptionId}/sync`,
    method: 'post'
  });
}

export function deleteCalendarSubscription(subscriptionId: string) {
  return request<void>({
    url: `/api/v2/calendar/subscriptions/${subscriptionId}`,
    method: 'delete'
  });
}

export function exportCalendarIcs(calendarId = 'default') {
  return request<Blob, 'blob'>({
    url: `/api/v2/calendar/${calendarId}/ics`,
    responseType: 'blob'
  });
}
