import { ref, shallowRef } from 'vue'

import { ApiError } from '@/api/client'

/**
 * Wraps an async loader in reactive `data` / `error` / `loading` state with a `run()` trigger.
 * `notFound` is true when the request failed with a 404, so detail pages can show a tailored
 * empty state instead of a generic error.
 */
export function useAsync<T>(loader: () => Promise<T>) {
  const data = shallowRef<T | null>(null)
  const error = ref<string | null>(null)
  const notFound = ref(false)
  const loading = ref(false)

  async function run() {
    loading.value = true
    error.value = null
    notFound.value = false
    try {
      data.value = await loader()
    } catch (e) {
      data.value = null
      notFound.value = e instanceof ApiError && e.status === 404
      error.value = e instanceof Error ? e.message : 'Request failed'
    } finally {
      loading.value = false
    }
  }

  return { data, error, notFound, loading, run }
}
