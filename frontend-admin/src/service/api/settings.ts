import { request } from '../request';

export function readUserProfile() {
  return request<Api.Settings.UserProfile>({ url: '/api/v2/settings/profile' });
}

export function updateUserProfile(data: Api.Settings.UserProfile) {
  return request<Api.Settings.UserProfile>({
    url: '/api/v2/settings/profile',
    method: 'patch',
    data
  });
}

export function readSecuritySettings() {
  return request<Api.Settings.SecuritySettings>({ url: '/api/v2/settings/security' });
}

export function updateSecuritySettings(data: Partial<Api.Settings.SecuritySettings>) {
  return request<void>({
    url: '/api/v2/settings/security',
    method: 'patch',
    data
  });
}

export function listDeviceSessions() {
  return request<Api.Settings.DeviceSession[]>({ url: '/api/v2/settings/devices' });
}

export function deleteDeviceSession(deviceId: string) {
  return request<void>({
    url: `/api/v2/settings/devices/${deviceId}`,
    method: 'delete'
  });
}

export function readNotificationSettings() {
  return request<Api.Settings.NotificationSettings>({ url: '/api/v2/settings/notifications' });
}

export function updateNotificationSettings(data: Partial<Api.Settings.NotificationSettings>) {
  return request<void>({
    url: '/api/v2/settings/notifications',
    method: 'patch',
    data
  });
}

export function readWebPushPublicKey() {
  return request<Api.Settings.WebPushPublicKey>({ url: '/api/v1/web-push/vapid-public-key' });
}

export function listWebPushSubscriptions() {
  return request<Api.Settings.WebPushSubscription[]>({ url: '/api/v1/web-push/subscriptions' });
}

export function registerWebPushSubscription(data: Api.Settings.WebPushRegistrationPayload) {
  return request<Api.Settings.WebPushSubscription>({
    url: '/api/v1/web-push/subscriptions',
    method: 'post',
    data: {
      endpoint: data.endpoint,
      keys: {
        p256dh: data.p256dh,
        auth: data.auth
      },
      label: data.label,
      ua: data.userAgent
    }
  });
}

export function deleteWebPushSubscription(subscriptionId: number) {
  return request<boolean>({
    url: `/api/v1/web-push/subscriptions/${subscriptionId}`,
    method: 'delete'
  });
}

export function testWebPushSubscription(subscriptionId?: number) {
  return request<Api.Settings.WebPushTestDelivery>({
    url: '/api/v1/web-push/test',
    method: 'post',
    data: { subscriptionId }
  });
}
