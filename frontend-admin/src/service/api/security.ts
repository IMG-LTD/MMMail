import { request } from '../request';

export function listSecurityEvents(params?: { type?: string; page?: number }) {
  return request<Api.Security.SecurityEvent[]>({
    url: '/api/v1/security/events',
    params
  });
}

export function ackSecurityEvent(id: string) {
  return request<Api.Security.SecurityEvent>({
    url: `/api/v1/security/events/${id}/ack`,
    method: 'post'
  });
}

export function listAdminSecurityAnomalies() {
  return request<Api.Security.SecurityEvent[]>({
    url: '/api/v1/admin/security/anomalies'
  });
}

export function applyAdminSecurityAction(id: string, action: Api.Security.SecurityAction) {
  return request<Api.Security.SecurityEvent>({
    url: `/api/v1/admin/security/anomalies/${id}/action`,
    method: 'post',
    data: { action }
  });
}
