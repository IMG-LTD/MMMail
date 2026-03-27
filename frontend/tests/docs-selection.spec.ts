import { describe, expect, it } from 'vitest'
import { extractSelectedExcerpt } from '../utils/docs-selection'

describe('extractSelectedExcerpt', () => {
  it('returns trimmed selected text', () => {
    expect(extractSelectedExcerpt('hello collaborative world', 6, 19)).toBe('collaborative')
  })

  it('returns empty string when selection is invalid', () => {
    expect(extractSelectedExcerpt('hello', 4, 4)).toBe('')
  })
})
