import { onBeforeUnmount, ref, unref, type Ref } from 'vue'
import type { DocsNoteSync } from '~/types/api'
import { useAuthStore } from '~/stores/auth'
import { resolveApiBase } from '~/utils/api-base'

const STREAM_RETRY_DELAY_MS = 3000

export type DocsSyncStatus = 'IDLE' | 'CONNECTING' | 'CONNECTED' | 'RECONNECTING' | 'ERROR'

interface DocsSyncStreamOptions {
  noteId: string | Ref<string>
  onPayload?: (payload: DocsNoteSync) => void | Promise<void>
}

export function useDocsSyncStream(options: DocsSyncStreamOptions) {
  const authStore = useAuthStore()
  const runtimeConfig = useRuntimeConfig()
  const status = ref<DocsSyncStatus>('IDLE')
  const errorMessage = ref('')
  const lastCursor = ref(0)

  let abortController: AbortController | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let manuallyClosed = false

  async function connect(afterEventId?: number): Promise<void> {
    if (!authStore.accessToken) {
      status.value = 'ERROR'
      errorMessage.value = 'Missing access token for docs sync'
      return
    }
    const currentNoteId = unref(options.noteId)
    if (!currentNoteId) {
      status.value = 'ERROR'
      errorMessage.value = 'Missing note id for docs sync'
      return
    }
    manuallyClosed = false
    clearReconnectTimer()
    status.value = lastCursor.value > 0 ? 'RECONNECTING' : 'CONNECTING'
    errorMessage.value = ''
    abortController = new AbortController()
    const streamCursor = typeof afterEventId === 'number' ? afterEventId : (lastCursor.value > 0 ? lastCursor.value : undefined)
    try {
      const response = await fetch(buildStreamUrl(runtimeConfig.public.apiBase, currentNoteId, streamCursor), {
        method: 'GET',
        headers: {
          Accept: 'text/event-stream',
          Authorization: `Bearer ${authStore.accessToken}`
        },
        cache: 'no-store',
        signal: abortController.signal
      })
      if (!response.ok) {
        throw new Error(`Docs sync stream failed: HTTP ${response.status}`)
      }
      if (!response.body) {
        throw new Error('Docs sync stream returned an empty body')
      }
      status.value = 'CONNECTED'
      await consumeStream(response.body)
      if (!manuallyClosed) {
        scheduleReconnect('Docs sync stream closed')
      }
    } catch (error) {
      if (abortController?.signal.aborted && manuallyClosed) {
        return
      }
      const message = error instanceof Error ? error.message : 'Docs sync stream failed'
      scheduleReconnect(message)
    }
  }

  function disconnect(): void {
    manuallyClosed = true
    clearReconnectTimer()
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    status.value = 'IDLE'
  }

  function reconnect(): void {
    disconnect()
    void connect(lastCursor.value || undefined)
  }

  async function consumeStream(stream: ReadableStream<Uint8Array>): Promise<void> {
    const reader = stream.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) {
        break
      }
      buffer += decoder.decode(value, { stream: true })
      buffer = await flushBuffer(buffer)
    }

    const tail = decoder.decode()
    if (tail) {
      await flushBuffer(buffer + tail)
    }
  }

  async function flushBuffer(buffer: string): Promise<string> {
    const chunks = buffer.split('\n\n')
    const remainder = chunks.pop() ?? ''
    for (const chunk of chunks) {
      const payload = parseSsePayload(chunk)
      if (!payload) {
        continue
      }
      lastCursor.value = payload.syncCursor
      await options.onPayload?.(payload)
    }
    return remainder
  }

  function scheduleReconnect(message: string): void {
    errorMessage.value = message
    if (manuallyClosed) {
      return
    }
    status.value = 'RECONNECTING'
    clearReconnectTimer()
    reconnectTimer = setTimeout(() => {
      void connect(lastCursor.value || undefined)
    }, STREAM_RETRY_DELAY_MS)
  }

  function clearReconnectTimer(): void {
    if (!reconnectTimer) {
      return
    }
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }

  onBeforeUnmount(() => {
    disconnect()
  })

  return {
    status,
    errorMessage,
    lastCursor,
    connect,
    disconnect,
    reconnect
  }
}

function buildStreamUrl(apiBase: string, noteId: string, afterEventId?: number): string {
  const baseUrl = new URL(resolveApiBase(apiBase))
  const streamUrl = new URL(`/api/v1/docs/notes/${noteId}/stream`, baseUrl)
  if (typeof afterEventId === 'number' && afterEventId > 0) {
    streamUrl.searchParams.set('afterEventId', String(afterEventId))
  }
  return streamUrl.toString()
}

function parseSsePayload(chunk: string): DocsNoteSync | null {
  const dataLines = chunk
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trim())
  if (!dataLines.length) {
    return null
  }
  try {
    return JSON.parse(dataLines.join('')) as DocsNoteSync
  } catch {
    return null
  }
}
