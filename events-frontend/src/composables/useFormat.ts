import { useI18n } from 'vue-i18n'

import { formatDate, humaniseEventType } from '@/lib/format'

/**
 * Locale-aware wrappers around the pure helpers in `lib/format.ts`.
 *
 * The helpers stay pure and take the locale as an argument so they remain unit-testable without an
 * app; this composable is the thin layer that supplies it from the active i18n instance.
 */
export function useFormat() {
  const { locale, t, te } = useI18n()

  return {
    /** `formatDate` bound to the active locale — "Fri, 12 Jun 2026" / "Fr., 12. Juni 2026". */
    formatDate: (isoDate?: string | null) => formatDate(isoDate, locale.value),

    /**
     * The display label for an event type.
     *
     * Looks the enum value up in the `eventType.*` catalogue, and falls back to sentence-casing
     * the constant when the BFF sends a value the frontend has not been taught yet. That fallback
     * matters: `EventType` lives in `events-core` and can gain a value in a backend release that
     * ships before the frontend does — "Silent disco" reads acceptably in the meantime, whereas
     * `SILENT_DISCO` or an empty label does not.
     */
    formatEventType: (eventType?: string | null) => {
      if (!eventType) return ''
      const key = `eventType.${eventType}`
      return te(key) ? t(key) : humaniseEventType(eventType)
    },
  }
}
