type QueryValue = string | number | boolean | undefined

interface RequestOptions {
  body?: unknown
  headers?: HeadersInit
  method?: 'DELETE' | 'GET' | 'POST' | 'PUT'
  query?: Record<string, QueryValue>
  token?: string
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

function createRequestUrl(path: string, query?: RequestOptions['query']) {
  const baseUrl = API_BASE_URL || window.location.origin
  const url = new URL(path, baseUrl)

  Object.entries(query || {}).forEach(([key, value]) => {
    if (value !== undefined) {
      url.searchParams.set(key, String(value))
    }
  })

  return url.toString()
}

async function parseResponse<T>(response: Response) {
  if (response.status === 204) {
    return undefined as T
  }

  const payload = await response.text()
  return payload ? (JSON.parse(payload) as T) : (undefined as T)
}

async function request<T>(path: string, options: RequestOptions = {}) {
  const headers = new Headers(options.headers)

  if (options.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  if (options.token) {
    headers.set('Authorization', `Bearer ${options.token}`)
  }

  const response = await fetch(createRequestUrl(path, options.query), {
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    headers,
    method: options.method || 'GET'
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status} ${response.statusText}`)
  }

  return parseResponse<T>(response)
}

export const httpClient = {
  delete<T>(path: string, options?: Omit<RequestOptions, 'method'>) {
    return request<T>(path, { ...options, method: 'DELETE' })
  },
  get<T>(path: string, options?: Omit<RequestOptions, 'body' | 'method'>) {
    return request<T>(path, { ...options, method: 'GET' })
  },
  post<T>(path: string, options?: Omit<RequestOptions, 'method'>) {
    return request<T>(path, { ...options, method: 'POST' })
  },
  put<T>(path: string, options?: Omit<RequestOptions, 'method'>) {
    return request<T>(path, { ...options, method: 'PUT' })
  }
}
