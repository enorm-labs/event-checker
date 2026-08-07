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
