export interface DocsDraftSnapshot {
  noteId: string
  title: string
  content: string
  baseVersion: number
  savedAt: string
}

interface DocsDraftStorageLike {
  getItem(key: string): string | null
  setItem(key: string, value: string): void
  removeItem(key: string): void
}

const DOCS_DRAFT_STORAGE_PREFIX = 'mmmail.docs.draft.'

function isValidDocsDraftSnapshot(value: unknown): value is DocsDraftSnapshot {
  if (!value || typeof value !== 'object') {
    return false
  }
  const snapshot = value as Record<string, unknown>
  return typeof snapshot.noteId === 'string'
    && typeof snapshot.title === 'string'
    && typeof snapshot.content === 'string'
    && typeof snapshot.baseVersion === 'number'
    && typeof snapshot.savedAt === 'string'
}

function browserStorage(): DocsDraftStorageLike | null {
  if (typeof window === 'undefined' || typeof window.localStorage === 'undefined') {
    return null
  }
  return window.localStorage
}

export function buildDocsDraftStorageKey(noteId: string): string {
  return `${DOCS_DRAFT_STORAGE_PREFIX}${noteId}`
}

export function readDocsDraftSnapshot(noteId: string, storage: DocsDraftStorageLike | null = browserStorage()): DocsDraftSnapshot | null {
  if (!storage) {
    return null
  }
  const raw = storage.getItem(buildDocsDraftStorageKey(noteId))
  if (!raw) {
    return null
  }
  try {
    const parsed = JSON.parse(raw)
    if (!isValidDocsDraftSnapshot(parsed) || parsed.noteId !== noteId) {
      return null
    }
    return parsed
  } catch {
    return null
  }
}

export function writeDocsDraftSnapshot(snapshot: DocsDraftSnapshot, storage: DocsDraftStorageLike | null = browserStorage()): void {
  if (!storage) {
    return
  }
  storage.setItem(buildDocsDraftStorageKey(snapshot.noteId), JSON.stringify(snapshot))
}

export function clearDocsDraftSnapshot(noteId: string, storage: DocsDraftStorageLike | null = browserStorage()): void {
  if (!storage) {
    return
  }
  storage.removeItem(buildDocsDraftStorageKey(noteId))
}

export function hasDocsDraftDiff(
  snapshot: Pick<DocsDraftSnapshot, 'title' | 'content'>,
  document: { title: string; content: string }
): boolean {
  return snapshot.title !== document.title || snapshot.content !== document.content
}
