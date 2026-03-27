import { describe, expect, it } from 'vitest'
import { resolveSessionIdFromAccessToken } from '../utils/auth-session'

function encodeBase64Url(payload: object): string {
  const raw = JSON.stringify(payload)
  const encoded = globalThis.btoa(raw)
  return encoded.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
}

describe('resolveSessionIdFromAccessToken', () => {
  it('returns sessionId from a JWT payload', () => {
    const token = ['header', encodeBase64Url({ sessionId: 9527 }), 'signature'].join('.')
    expect(resolveSessionIdFromAccessToken(token)).toBe('9527')
  })

  it('returns empty string for invalid tokens', () => {
    expect(resolveSessionIdFromAccessToken('invalid-token')).toBe('')
  })
})
