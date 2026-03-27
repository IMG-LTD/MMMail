import axios from 'axios'
import type { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from 'axios'
import { resolveApiBase } from './api-base'

export interface ErrorPayload {
  code?: number
  message?: string
}

export interface ApiClientError extends Error {
  status?: number
  code?: number
  config?: InternalAxiosRequestConfig
}

export async function parseApiErrorPayload(data: unknown): Promise<ErrorPayload | null> {
  if (!data) {
    return null
  }
  if (typeof data === 'object' && !(data instanceof Blob)) {
    const payload = data as ErrorPayload
    return payload.message || payload.code != null ? payload : null
  }
  if (typeof data === 'string') {
    return tryParseErrorPayload(data)
  }
  if (typeof Blob !== 'undefined' && data instanceof Blob) {
    const raw = await data.text()
    return tryParseErrorPayload(raw)
  }
  return null
}

export function createApiClient(baseURL: string): AxiosInstance {
  const client = axios.create({
    baseURL: resolveApiBase(baseURL),
    timeout: 10000,
    withCredentials: true
  })

  client.interceptors.response.use(
    (response) => response,
    async (error: AxiosError<ErrorPayload | Blob | string>) => {
      const fallbackMessage = 'Request failed'
      const payload = await parseApiErrorPayload(error.response?.data)
      const normalizedError = new Error(payload?.message || fallbackMessage) as ApiClientError
      normalizedError.status = error.response?.status
      normalizedError.code = payload?.code
      normalizedError.config = error.config || undefined
      return Promise.reject(normalizedError)
    }
  )

  return client
}

function tryParseErrorPayload(raw: string): ErrorPayload | null {
  if (!raw.trim()) {
    return null
  }
  try {
    const payload = JSON.parse(raw) as ErrorPayload
    return payload.message || payload.code != null ? payload : null
  } catch {
    return null
  }
}
