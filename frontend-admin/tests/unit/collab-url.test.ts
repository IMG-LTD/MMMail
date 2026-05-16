import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('@/service/api', () => ({
  getCollabSnapshot: vi.fn(),
  writeCollabSnapshot: vi.fn()
}));

vi.mock('@/store/modules/auth/shared', () => ({
  getToken: () => 'unused'
}));

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
  vi.resetModules();
});

describe('docs collab websocket url builder', () => {
  it('uses wss when service base url is https and embeds resource path + token', async () => {
    vi.stubEnv('VITE_SERVICE_BASE_URL', 'https://api.example.com');
    const { buildDocsCollabWebSocketUrl } = await import('@/hooks/business/docs-collab-crdt');
    const url = new URL(buildDocsCollabWebSocketUrl('tk', 'docs', 'doc_1'));
    expect(url.protocol).toBe('wss:');
    expect(url.pathname).toBe('/ws/collab/docs/doc_1');
    expect(url.searchParams.get('token')).toBe('tk');
  });

  it('uses ws when service base url is plain http', async () => {
    vi.stubEnv('VITE_SERVICE_BASE_URL', 'http://api.example.com');
    vi.resetModules();
    const { buildDocsCollabWebSocketUrl } = await import('@/hooks/business/docs-collab-crdt');
    const url = new URL(buildDocsCollabWebSocketUrl('tk', 'sheets', 's_1'));
    expect(url.protocol).toBe('ws:');
    expect(url.pathname).toBe('/ws/collab/sheets/s_1');
  });
});
