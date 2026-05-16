import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

beforeEach(() => {
  vi.stubEnv('VITE_STORAGE_PREFIX', 'mm.');
  window.localStorage.clear();
  window.sessionStorage.clear();
  vi.resetModules();
});

afterEach(() => {
  vi.unstubAllEnvs();
  window.localStorage.clear();
  window.sessionStorage.clear();
});

describe('storage helpers', () => {
  it('round-trips JSON values with the configured prefix', async () => {
    const { localStg } = await import('@/utils/storage');
    localStg.set('token', 'abc');
    expect(window.localStorage.getItem('mm.token')).toBe(JSON.stringify('abc'));
    expect(localStg.get('token')).toBe('abc');
  });

  it('returns null and cleans up entries that hold malformed json', async () => {
    const { localStg } = await import('@/utils/storage');
    window.localStorage.setItem('mm.token', '<broken>');
    expect(localStg.get('token')).toBeNull();
    expect(window.localStorage.getItem('mm.token')).toBeNull();
  });

  it('removes entries on demand', async () => {
    const { localStg } = await import('@/utils/storage');
    localStg.set('token', 'abc');
    localStg.remove('token');
    expect(window.localStorage.getItem('mm.token')).toBeNull();
  });

  it('uses sessionStorage for the session-scoped helper', async () => {
    const { sessionStg } = await import('@/utils/storage');
    sessionStg.set('themeColor', '#fff');
    expect(window.sessionStorage.getItem('mm.themeColor')).toBe(JSON.stringify('#fff'));
  });
});
