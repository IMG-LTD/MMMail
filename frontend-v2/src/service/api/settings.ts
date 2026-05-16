import type { ApiResponse, UserPreference } from "@/shared/types/api";
import { httpClient } from "@/service/request/http";

export interface SecuritySettings {
  mfaEnabled: boolean;
  recoveryEmail: string | null;
}

export interface DeviceSession {
  id: string;
  deviceName: string;
  lastActiveAt: string;
  current: boolean;
}

export interface NotificationSettings {
  emailDigest: boolean;
  productUpdates: boolean;
}

export interface IntegrationSetting {
  id: string;
  provider: string;
  enabled: boolean;
}

export interface AuditEvent {
  id: string;
  action: string;
  occurredAt: string;
}

function unwrapResponse<T>(response: ApiResponse<T>) {
  return response.data;
}

export async function fetchProfile(token?: string) {
  const response = await httpClient.get<ApiResponse<UserPreference>>("/api/v2/settings/profile", {
    token,
  });
  return unwrapResponse(response);
}

export async function updateProfile(payload: UserPreference, token?: string) {
  const response = await httpClient.patch<ApiResponse<UserPreference>>("/api/v2/settings/profile", {
    body: payload,
    token,
  });
  return unwrapResponse(response);
}

export async function fetchSecuritySettings(token?: string) {
  const response = await httpClient.get<ApiResponse<SecuritySettings>>(
    "/api/v2/settings/security",
    { token },
  );
  return unwrapResponse(response);
}

export async function updateSecuritySettings(payload: SecuritySettings, token?: string) {
  const response = await httpClient.patch<ApiResponse<SecuritySettings>>(
    "/api/v2/settings/security",
    {
      body: payload,
      token,
    },
  );
  return unwrapResponse(response);
}

export async function listDevices(token?: string) {
  const response = await httpClient.get<ApiResponse<DeviceSession[]>>("/api/v2/settings/devices", {
    token,
  });
  return unwrapResponse(response);
}

export function deleteDevice(deviceId: string, token?: string) {
  return httpClient.delete<void>(`/api/v2/settings/devices/${deviceId}`, { token });
}

export async function fetchNotificationSettings(token?: string) {
  const response = await httpClient.get<ApiResponse<NotificationSettings>>(
    "/api/v2/settings/notifications",
    { token },
  );
  return unwrapResponse(response);
}

export async function updateNotificationSettings(payload: NotificationSettings, token?: string) {
  const response = await httpClient.patch<ApiResponse<NotificationSettings>>(
    "/api/v2/settings/notifications",
    {
      body: payload,
      token,
    },
  );
  return unwrapResponse(response);
}

export async function listIntegrations(token?: string) {
  const response = await httpClient.get<ApiResponse<IntegrationSetting[]>>(
    "/api/v2/settings/integrations",
    { token },
  );
  return unwrapResponse(response);
}

export function patchIntegration(
  integrationId: string,
  payload: Partial<IntegrationSetting>,
  token?: string,
) {
  return httpClient.patch<ApiResponse<IntegrationSetting>>(
    `/api/v2/settings/integrations/${integrationId}`,
    {
      body: payload,
      token,
    },
  );
}

export async function listSettingsAudit(token?: string) {
  const response = await httpClient.get<ApiResponse<AuditEvent[]>>("/api/v2/settings/audit", {
    token,
  });
  return unwrapResponse(response);
}
