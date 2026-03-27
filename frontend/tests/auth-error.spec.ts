import { describe, expect, it } from 'vitest'
import { isAuthExpiredResponse } from '../utils/auth-error'

describe('isAuthExpiredResponse', () => {
  it('treats bearer auth failures as refreshable', () => {
    expect(isAuthExpiredResponse(401)).toBe(true)
    expect(isAuthExpiredResponse(403)).toBe(true)
    expect(isAuthExpiredResponse(403, 10003)).toBe(true)
    expect(isAuthExpiredResponse(400, 20004)).toBe(true)
  })

  it('does not treat business forbiddens as session expiry', () => {
    expect(isAuthExpiredResponse(403, 30045)).toBe(false)
    expect(isAuthExpiredResponse(403, 30046)).toBe(false)
    expect(isAuthExpiredResponse(409, 30003)).toBe(false)
  })
})
