type QueryValue = string | number | boolean | undefined;

interface RequestOptions {
  body?: unknown;
  headers?: HeadersInit;
  method?: "DELETE" | "GET" | "PATCH" | "POST" | "PUT";
  query?: Record<string, QueryValue>;
  scopeHeaders?: Record<string, string>;
  token?: string;
}

type UnauthorizedHandler = () => void;

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
let unauthorizedHandler: UnauthorizedHandler | null = null;

interface HttpRequestErrorOptions {
  message: string;
  payload: string;
  status: number;
  statusText: string;
}

export class HttpRequestError extends Error {
  readonly payload: string;
  readonly status: number;
  readonly statusText: string;

  constructor(options: HttpRequestErrorOptions) {
    super(options.message);
    this.name = "HttpRequestError";
    this.payload = options.payload;
    this.status = options.status;
    this.statusText = options.statusText;
  }
}

export function isHttpRequestError(error: unknown, status?: number): error is HttpRequestError {
  return error instanceof HttpRequestError && (status === undefined || error.status === status);
}

export function registerUnauthorizedHandler(handler: UnauthorizedHandler) {
  unauthorizedHandler = handler;
}

function createRequestUrl(path: string, query?: RequestOptions["query"]) {
  const baseUrl = API_BASE_URL || window.location.origin;
  const url = new URL(path, baseUrl);

  Object.entries(query || {}).forEach(([key, value]) => {
    if (value !== undefined) {
      url.searchParams.set(key, String(value));
    }
  });

  return url.toString();
}

async function parseResponse<T>(response: Response) {
  if (response.status === 204) {
    return undefined as T;
  }

  const payload = await response.text();
  return payload ? (JSON.parse(payload) as T) : (undefined as T);
}

async function createHttpError(response: Response) {
  const payload = await response.text();
  const message = resolveErrorMessage(response, payload);
  return new HttpRequestError({
    message,
    payload,
    status: response.status,
    statusText: response.statusText,
  });
}

function resolveErrorMessage(response: Response, payload: string) {
  if (!payload) {
    return `HTTP ${response.status} ${response.statusText}`;
  }
  try {
    const parsed = JSON.parse(payload) as { message?: unknown };
    return typeof parsed.message === "string" && parsed.message
      ? parsed.message
      : `HTTP ${response.status} ${response.statusText}`;
  } catch {
    return `HTTP ${response.status} ${response.statusText}`;
  }
}

async function request<T>(path: string, options: RequestOptions = {}) {
  const headers = new Headers(options.headers);

  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  if (options.token) {
    headers.set("Authorization", `Bearer ${options.token}`);
  }

  Object.entries(options.scopeHeaders || {}).forEach(([key, value]) => {
    headers.set(key, value);
  });

  const response = await fetch(createRequestUrl(path, options.query), {
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    headers,
    method: options.method || "GET",
  });

  if (!response.ok) {
    notifyUnauthorized(response, options);
    throw await createHttpError(response);
  }

  return parseResponse<T>(response);
}

async function requestBlob<T extends boolean = false>(
  path: string,
  options: Omit<RequestOptions, "body"> = {},
) {
  const headers = new Headers(options.headers);

  if (options.token) {
    headers.set("Authorization", `Bearer ${options.token}`);
  }

  Object.entries(options.scopeHeaders || {}).forEach(([key, value]) => {
    headers.set(key, value);
  });

  const response = await fetch(createRequestUrl(path, options.query), {
    headers,
    method: options.method || "GET",
  });

  if (!response.ok) {
    notifyUnauthorized(response, options);
    throw await createHttpError(response);
  }

  return {
    blob: await response.blob(),
    contentDisposition: response.headers.get("Content-Disposition") || "",
    contentType: response.headers.get("Content-Type") || "",
  };
}

function notifyUnauthorized(response: Response, options: RequestOptions) {
  if (response.status === 401 && options.token) {
    unauthorizedHandler?.();
  }
}

export const httpClient = {
  delete<T>(path: string, options?: Omit<RequestOptions, "method">) {
    return request<T>(path, { ...options, method: "DELETE" });
  },
  get<T>(path: string, options?: Omit<RequestOptions, "body" | "method">) {
    return request<T>(path, { ...options, method: "GET" });
  },
  getBlob<T extends boolean = false>(
    path: string,
    options?: Omit<RequestOptions, "body" | "method">,
  ) {
    return requestBlob<T>(path, { ...options, method: "GET" });
  },
  post<T>(path: string, options?: Omit<RequestOptions, "method">) {
    return request<T>(path, { ...options, method: "POST" });
  },
  patch<T>(path: string, options?: Omit<RequestOptions, "method">) {
    return request<T>(path, { ...options, method: "PATCH" });
  },
  put<T>(path: string, options?: Omit<RequestOptions, "method">) {
    return request<T>(path, { ...options, method: "PUT" });
  },
};
