import { describe, expect, it } from 'vitest'
import {
  buildDocsDraftStorageKey,
  clearDocsDraftSnapshot,
  hasDocsDraftDiff,
  readDocsDraftSnapshot,
  writeDocsDraftSnapshot
} from '../utils/docs-draft'

function createStorage(): Storage {
  const store = new Map<string, string>()
  return {
    length: 0,
    clear() {
      store.clear()
    },
    getItem(key: string) {
      return store.has(key) ? store.get(key)! : null
    },
    key(index: number) {
      return [...store.keys()][index] ?? null
    },
    removeItem(key: string) {
      store.delete(key)
    },
    setItem(key: string, value: string) {
      store.set(key, value)
    }
  }
}

describe('docs draft storage', () => {
  it('writes, reads and clears a draft snapshot', () => {
    const storage = createStorage()
    writeDocsDraftSnapshot({
      noteId: 'note-1',
      title: 'Unsaved title',
      content: 'Unsaved body',
      baseVersion: 4,
      savedAt: '2026-03-28T15:10:00'
    }, storage)

    expect(buildDocsDraftStorageKey('note-1')).toBe('mmmail.docs.draft.note-1')
    expect(readDocsDraftSnapshot('note-1', storage)).toEqual({
      noteId: 'note-1',
      title: 'Unsaved title',
      content: 'Unsaved body',
      baseVersion: 4,
      savedAt: '2026-03-28T15:10:00'
    })

    clearDocsDraftSnapshot('note-1', storage)
    expect(readDocsDraftSnapshot('note-1', storage)).toBeNull()
  })

  it('ignores invalid snapshot payloads', () => {
    const storage = createStorage()
    storage.setItem(buildDocsDraftStorageKey('note-1'), '{"noteId":"note-2"}')
    expect(readDocsDraftSnapshot('note-1', storage)).toBeNull()
  })

  it('detects whether the local draft differs from the server document', () => {
    expect(hasDocsDraftDiff({ title: 'Draft', content: 'Body' }, { title: 'Draft', content: 'Body' })).toBe(false)
    expect(hasDocsDraftDiff({ title: 'Draft', content: 'Body' }, { title: 'Draft', content: 'Other' })).toBe(true)
  })
})
