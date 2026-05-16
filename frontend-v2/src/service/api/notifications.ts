import { httpClient } from "@/service/request/http";
import type { ApiResponse } from "@/shared/types/api";

interface ScopedRequestOptions {
  scopeHeaders?: Record<string, string>;
  token: string;
}

export type NotificationSeverity = "INFO" | "WARNING" | "CRITICAL";
export type NotificationStatus = "READ" | "UNREAD";

export interface NotificationItem {
  id: string;
  title: string;
  body: string | null;
  product: string;
  severity: NotificationSeverity;
  status: NotificationStatus;
  createdAt: string;
  readAt: string | null;
}

export interface NotificationRule {
  id: string;
  name: string;
  channel: string;
  enabled: boolean;
}

export interface NotificationSubscription {
  id: string;
  product: string;
  channel: string;
  enabled: boolean;
}

export interface NotificationTemplate {
  id: string;
  name: string;
  channel: string;
  updatedAt: string;
}

export interface NotificationAnalytics {
  totalCount: number;
  unreadCount: number;
  criticalCount: number;
  deliveryRate: number;
}

function unwrapResponse<T>(response: ApiResponse<T>) {
  return response.data;
}

export async function listNotifications(options: ScopedRequestOptions) {
  const response = await httpClient.get<ApiResponse<NotificationItem[]>>(
    "/api/v2/notifications",
    options,
  );
  return unwrapResponse(response);
}

export function patchNotification(
  notificationId: string,
  body: Record<string, unknown>,
  options: ScopedRequestOptions,
) {
  return httpClient.patch<ApiResponse<NotificationItem>>(
    `/api/v2/notifications/${notificationId}`,
    { ...options, body },
  );
}

export async function listNotificationRules(options: ScopedRequestOptions) {
  const response = await httpClient.get<ApiResponse<NotificationRule[]>>(
    "/api/v2/notifications/rules",
    options,
  );
  return unwrapResponse(response);
}

export function createNotificationRule(
  body: Record<string, unknown>,
  options: ScopedRequestOptions,
) {
  return httpClient.post<ApiResponse<NotificationRule>>("/api/v2/notifications/rules", {
    ...options,
    body,
  });
}

export async function listNotificationSubscriptions(options: ScopedRequestOptions) {
  const response = await httpClient.get<ApiResponse<NotificationSubscription[]>>(
    "/api/v2/notifications/subscriptions",
    options,
  );
  return unwrapResponse(response);
}

export function patchNotificationSubscription(
  subscriptionId: string,
  body: Record<string, unknown>,
  options: ScopedRequestOptions,
) {
  return httpClient.patch<ApiResponse<NotificationSubscription>>(
    `/api/v2/notifications/subscriptions/${subscriptionId}`,
    { ...options, body },
  );
}

export async function listNotificationTemplates(options: ScopedRequestOptions) {
  const response = await httpClient.get<ApiResponse<NotificationTemplate[]>>(
    "/api/v2/notifications/templates",
    options,
  );
  return unwrapResponse(response);
}

export function sendNotification(body: Record<string, unknown>, options: ScopedRequestOptions) {
  return httpClient.post<ApiResponse<NotificationItem>>("/api/v2/notifications/send", {
    ...options,
    body,
  });
}

export async function readNotificationAnalytics(options: ScopedRequestOptions) {
  const response = await httpClient.get<ApiResponse<NotificationAnalytics>>(
    "/api/v2/notifications/analytics",
    options,
  );
  return unwrapResponse(response);
}
