import createClient from 'openapi-fetch'
import type { paths } from './schema'

// Single typed client for the public BFF. In dev, Vite proxies `/api` to the Spring Boot
// backend on :8080 (see vite.config.ts); the generated `paths` come from its OpenAPI spec.
export const api = createClient<paths>({ baseUrl: '/api' })

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
