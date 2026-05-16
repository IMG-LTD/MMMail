import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/service/api', () => ({
  listNotificationEventsSince: vi.fn()
}));

vi.mock('@/store/modules/auth/shared', () => ({
  getToken: () => 'unused'
}));

vi.stubEnv('VITE_SERVICE_BASE_URL', 'https://api.example.com');

const restoreLocation = window.location;

beforeEach(() => {
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: new URL('https://app.example.com/')
  });
});

afterEach(() => {
  Object.defineProperty(window, 'location', { configurable: true, value: restoreLocation });
  vi.unstubAllEnvs();
});

describe('notification realtime websocket url builder', () => {
  it('builds wss url with token and cursor query string when API base is https', async () => {
    const { buildNotificationWebSocketUrl } = await import('@/hooks/business/notification-realtime');
    const url = new URL(buildNotificationWebSocketUrl('abc.token', 42));
    expect(url.protocol).toBe('wss:');
    expect(url.pathname).toBe('/ws/notifications');
    expect(url.searchParams.get('token')).toBe('abc.token');
    expect(url.searchParams.get('since')).toBe('42');
  });

  it('falls back to ws when service base url is plain http', async () => {
    vi.stubEnv('VITE_SERVICE_BASE_URL', 'http://api.example.com');
    vi.resetModules();
    const { buildNotificationWebSocketUrl } = await import('@/hooks/business/notification-realtime');
    const url = new URL(buildNotificationWebSocketUrl('t', 0));
    expect(url.protocol).toBe('ws:');
    expect(url.searchParams.get('since')).toBe('0');
  });
});
