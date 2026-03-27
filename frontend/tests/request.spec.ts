import { describe, expect, it } from 'vitest'
import { parseApiErrorPayload } from '../utils/request'

describe('request utils', () => {
  it('returns structured payloads directly', async () => {
    await expect(parseApiErrorPayload({ code: 10002, message: 'Drive share password is invalid' })).resolves.toEqual({
      code: 10002,
      message: 'Drive share password is invalid'
    })
  })

  it('parses json blob payloads', async () => {
    const blob = new Blob([
      JSON.stringify({ code: 10002, message: 'Drive share password is invalid' })
    ], { type: 'application/json' })

    await expect(parseApiErrorPayload(blob)).resolves.toEqual({
      code: 10002,
      message: 'Drive share password is invalid'
    })
  })

  it('returns null for non-json strings', async () => {
    await expect(parseApiErrorPayload('plain text failure')).resolves.toBeNull()
  })
})
