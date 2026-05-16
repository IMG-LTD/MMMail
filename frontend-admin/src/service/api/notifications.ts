import { request } from '../request';

export function listNotifications(params: Api.Notifications.Query = {}) {
  return request<Api.Notifications.Notification[]>({ url: '/api/v2/notifications', params });
}

export function patchNotification(notificationId: string, data: Api.Notifications.PatchPayload) {
  return request<Api.Notifications.Notification>({
    url: `/api/v2/notifications/${notificationId}`,
    method: 'patch',
    data
  });
}

export function listNotificationSubscriptions() {
  return request<Api.Notifications.Subscription[]>({ url: '/api/v2/notifications/subscriptions' });
}

export function listNotificationEventsSince(params: Api.Notifications.RealtimeQuery = {}) {
  return request<Api.Notifications.RealtimeReplay>({ url: '/api/v2/notifications/since', params });
}
