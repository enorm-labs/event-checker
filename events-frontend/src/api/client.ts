import createClient from 'openapi-fetch'
import type { paths } from './schema'

/** Abort a request that has not responded within this window, so the UI never hangs forever. */
const REQUEST_TIMEOUT_MS = 8000

/**
 * Wraps the global fetch with a per-request timeout, merged with any caller-supplied signal so a
 * future per-request abort still works. A fresh `AbortSignal.timeout` is created on every call —
 * a single shared one would fire once and then abort all subsequent requests. `timeoutMs` is
 * injectable so tests can trigger the timeout without waiting the full production window.
 */
export function fetchWithTimeout(
  request: Request,
  timeoutMs = REQUEST_TIMEOUT_MS,
): Promise<Response> {
  const signal = AbortSignal.any([request.signal, AbortSignal.timeout(timeoutMs)])
  return fetch(new Request(request, { signal }))
}

// Single typed client for the public BFF. In dev, Vite proxies `/api` to the Spring Boot
// backend on :8080 (see vite.config.ts); the generated `paths` come from its OpenAPI spec.
export const api = createClient<paths>({ baseUrl: '/api', fetch: fetchWithTimeout })

/** Error carrying the HTTP status so callers can distinguish e.g. 404 from other failures. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

interface FetchResult<T> {
  data?: T
  error?: unknown
  response: Response
}

/** Unwraps an openapi-fetch result, returning the body or throwing {@link ApiError} on failure. */
export async function unwrap<T>(request: Promise<FetchResult<T>>): Promise<T> {
  const { data, error, response } = await request
  if (!response.ok || error !== undefined) {
    throw new ApiError(response.status, `Request failed with status ${response.status}`)
  }
  return data as T
}

/**
 * Maps a thrown request failure to a user-facing message about loading `label` (e.g.
 * "tonight's events"). Transient problems (server errors, rate limiting, timeouts, connectivity)
 * are worth a reload and say so; other failures get a plain message. Callers handle 404 separately
 * via their own `notFound` state.
 */
export function describeError(e: unknown, label = 'this content'): string {
  if (e instanceof ApiError) {
    if (e.status === 429 || e.status >= 500) {
      return `Couldn't load ${label}. The server is having trouble right now — please try reloading in a moment.`
    }
    return `Couldn't load ${label}. Please try reloading.`
  }
  if (e instanceof DOMException && e.name === 'TimeoutError') {
    return `Couldn't load ${label}. The request timed out — please check your connection and reload.`
  }
  return `Couldn't load ${label}. Please check your connection and reload.`
}
