import { describe, expect, it } from 'vitest'
import { resolveApiBase } from '../utils/api-base'

describe('api base utils', () => {
  it('keeps explicit remote hosts unchanged', () => {
    expect(resolveApiBase('https://api.example.com', '127.0.0.1')).toBe('https://api.example.com/')
  })

  it('aligns localhost api host with local frontend hostname', () => {
    expect(resolveApiBase('http://localhost:8080', '127.0.0.1')).toBe('http://127.0.0.1:8080/')
  })

  it('aligns 127 api host with localhost frontend hostname', () => {
    expect(resolveApiBase('http://127.0.0.1:8080', 'localhost')).toBe('http://localhost:8080/')
  })
})
