import type { ClassValue } from 'clsx'
import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/**
 * Chrome shared by the native form controls in the filter bars — see BaseInput and BaseSelect,
 * the only two things that should reference it. It lives here rather than being duplicated in
 * both components (and rather than becoming an `@apply` rule, which would move the styling out
 * of the components that own it).
 */
export const FIELD_CLASS =
  'h-8 rounded-lg border border-border bg-background px-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50'

/**
 * The raised surface: what makes something read as sitting above the page. Only the card gets it,
 * which is the point — where everything is raised, nothing is.
 *
 * Tailwind still finds every class here: it scans the source text, and each utility appears
 * literally in one of these strings even though they are assembled.
 */
const SURFACE_CLASS = 'rounded-xl border border-border bg-card'

/**
 * An interactive card — the event and venue tiles, which are links. It lifts on hover, gated
 * behind `motion-safe`.
 *
 * The two cards carried this verbatim in both files. Extracted for the same reason as
 * {@link FIELD_CLASS}: a 160-character class string copied twice drifts the first time only one
 * of them is touched.
 */
export const CARD_CLASS = `group flex gap-4 ${SURFACE_CLASS} p-3 shadow-sm transition-all hover:border-primary/40 hover:shadow-md motion-safe:hover:-translate-y-0.5`

/**
 * The two filter bars: chrome, not an object. It shared {@link SURFACE_CLASS} with the card until
 * #1240, which made a list page a bordered box of controls above a grid of bordered boxes. No
 * horizontal padding, because the page's own `p-4 sm:p-8` already sets that edge.
 */
export const PANEL_CLASS = 'flex flex-wrap items-end gap-3 border-b border-border pb-4'
