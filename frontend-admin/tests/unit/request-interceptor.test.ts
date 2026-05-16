import { describe, expect, it } from 'vitest';

interface BackendResponse {
  code: string | number;
  message?: string;
  msg?: string;
  data?: unknown;
}

const MODAL_LOGOUT_CODES = ['8888', '7777'];
const EXPIRED_TOKEN_CODES = ['401', '30001'];
const SUCCESS_CODE = 0;

function decideRequestAction(response: BackendResponse): 'success' | 'expired' | 'modal' | 'error' {
  if (Number(response.code) === SUCCESS_CODE) return 'success';
  const code = String(response.code);
  if (EXPIRED_TOKEN_CODES.includes(code)) return 'expired';
  if (MODAL_LOGOUT_CODES.includes(code)) return 'modal';
  return 'error';
}

function readBackendMessage(response: BackendResponse): string {
  return response.message || response.msg || '';
}

describe('request interceptor decision contract', () => {
  it('treats code === 0 as success regardless of payload shape', () => {
    expect(decideRequestAction({ code: 0, data: {} })).toBe('success');
    expect(decideRequestAction({ code: '0' })).toBe('success');
  });

  it('routes 401 / 30001 codes to the expired-token refresh path', () => {
    expect(decideRequestAction({ code: 401 })).toBe('expired');
    expect(decideRequestAction({ code: '30001' })).toBe('expired');
  });

  it('routes hard-logout codes to the modal logout path', () => {
    expect(decideRequestAction({ code: '8888' })).toBe('modal');
    expect(decideRequestAction({ code: '7777' })).toBe('modal');
  });

  it('falls through to the generic error branch for any other backend code', () => {
    expect(decideRequestAction({ code: '40021' })).toBe('error');
  });

  it('extracts message from either `message` or legacy `msg` field', () => {
    expect(readBackendMessage({ code: 1, message: 'a' })).toBe('a');
    expect(readBackendMessage({ code: 1, msg: 'b' })).toBe('b');
    expect(readBackendMessage({ code: 1 })).toBe('');
  });
});
